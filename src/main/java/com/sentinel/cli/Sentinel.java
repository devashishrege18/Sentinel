package com.sentinel.cli;

import com.sentinel.auth.PasswordAuditResult;
import com.sentinel.auth.PasswordAuditor;
import com.sentinel.integrity.ChangeEvent;
import com.sentinel.integrity.FileIntegrityMonitor;
import com.sentinel.integrity.IntegrityException;
import com.sentinel.persistence.DatabaseManager;
import com.sentinel.persistence.IntegrityEventRepository;
import com.sentinel.persistence.PasswordAuditRepository;
import com.sentinel.persistence.ScanResultRepository;
import com.sentinel.report.ReportExporter;
import com.sentinel.report.SecurityReportGenerator;
import com.sentinel.scanner.PortScanResult;
import com.sentinel.scanner.PortScanner;
import com.sentinel.scanner.ScanException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point and menu loop for the Sentinel security toolkit.
 *
 * <p>Sentinel bundles three independent security utilities - a concurrent
 * TCP port scanner, a SHA-256 file integrity monitor, and a rule-based
 * password strength auditor - behind one command-line menu, persisting all
 * results to a local SQLite database and letting the user export a combined
 * report across everything that has been recorded.</p>
 */
public class Sentinel {

    private final Scanner input = new Scanner(System.in);
    private final ConfigManager config = ConfigManager.getInstance();

    private DatabaseManager databaseManager;
    private ScanResultRepository scanResultRepository;
    private IntegrityEventRepository integrityEventRepository;
    private PasswordAuditRepository passwordAuditRepository;

    public static void main(String[] args) {
        new Sentinel().run();
    }

    public void run() {
        printBanner();
        initializePersistence();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = input.nextLine().trim();
            switch (choice) {
                case "1" -> runPortScan();
                case "2" -> runIntegrityBaseline();
                case "3" -> runIntegrityCheck();
                case "4" -> runPasswordAudit();
                case "5" -> runReport();
                case "6" -> running = false;
                default -> System.out.println("Unrecognized option, please choose 1-6.\n");
            }
        }

        databaseManager.close();
        System.out.println("Goodbye.");
    }

    private void printBanner() {
        System.out.println("=".repeat(60));
        System.out.println("  SENTINEL - Local Security Monitoring Toolkit");
        System.out.println("=".repeat(60));
    }

    private void printMenu() {
        System.out.println("\n1) Scan ports on a host");
        System.out.println("2) Create/refresh a file integrity baseline");
        System.out.println("3) Check a directory against its baseline");
        System.out.println("4) Audit a password's strength");
        System.out.println("5) Generate and export a security report");
        System.out.println("6) Exit");
        System.out.print("Choose an option: ");
    }

    private void initializePersistence() {
        databaseManager = DatabaseManager.getInstance(config.getDatabasePath().toString());
        scanResultRepository = new ScanResultRepository(databaseManager.getConnection());
        integrityEventRepository = new IntegrityEventRepository(databaseManager.getConnection());
        passwordAuditRepository = new PasswordAuditRepository(databaseManager.getConnection());
    }

    private void runPortScan() {
        System.out.print("Target host (e.g. localhost): ");
        String host = input.nextLine().trim();

        int startPort = readInt("Start port: ");
        int endPort = readInt("End port: ");

        PortScanner scanner = new PortScanner(config.getDefaultConnectTimeoutMillis(), config.getDefaultScanPoolSize());

        System.out.println("Scanning " + host + " ports " + startPort + "-" + endPort + " ...");
        try {
            long start = System.currentTimeMillis();
            List<PortScanResult> results = scanner.scanRange(host, startPort, endPort);
            long elapsed = System.currentTimeMillis() - start;

            results.stream()
                    .filter(r -> r.getStatus() == com.sentinel.scanner.PortStatus.OPEN)
                    .forEach(System.out::println);

            long openCount = results.stream().filter(r -> r.getStatus() == com.sentinel.scanner.PortStatus.OPEN).count();
            System.out.println("Scan complete in " + elapsed + " ms. Open ports found: " + openCount);

            scanResultRepository.saveAll(results);
            System.out.println("Results saved to database.");
        } catch (ScanException e) {
            System.out.println("Scan failed: " + e.getMessage());
        }
    }

    private void runIntegrityBaseline() {
        Path directory = readDirectory("Directory to baseline: ");
        Path baselineFile = FileIntegrityMonitor.defaultBaselinePathFor(directory);
        FileIntegrityMonitor monitor = new FileIntegrityMonitor(directory, baselineFile);

        try {
            var records = monitor.createBaseline();
            System.out.println("Baseline created for " + records.size() + " file(s) at " + baselineFile);
        } catch (IntegrityException e) {
            System.out.println("Could not create baseline: " + e.getMessage());
        }
    }

    private void runIntegrityCheck() {
        Path directory = readDirectory("Directory to check: ");
        Path baselineFile = FileIntegrityMonitor.defaultBaselinePathFor(directory);
        FileIntegrityMonitor monitor = new FileIntegrityMonitor(directory, baselineFile);

        if (!monitor.baselineExists()) {
            System.out.println("No baseline exists yet for this directory - create one first (option 2).");
            return;
        }

        try {
            List<ChangeEvent> events = monitor.checkIntegrity();
            if (events.isEmpty()) {
                System.out.println("No changes detected. Directory matches its baseline.");
            } else {
                System.out.println("Detected " + events.size() + " change(s):");
                events.forEach(System.out::println);
                integrityEventRepository.saveAll(events);
                System.out.println("Change events saved to database.");
            }
        } catch (IntegrityException e) {
            System.out.println("Integrity check failed: " + e.getMessage());
        }
    }

    private void runPasswordAudit() {
        System.out.print("Password to audit (not stored in plaintext anywhere): ");
        String password = input.nextLine();

        PasswordAuditor auditor = PasswordAuditor.withDefaultRules();
        PasswordAuditResult result = auditor.audit(password);

        System.out.println("\nStrength: " + result.getStrengthLabel()
                + " (" + result.getScore() + "/" + result.getMaxScore() + " checks passed)");
        result.getRuleResults().forEach((name, ruleResult) ->
                System.out.println("  [" + (ruleResult.isPassed() ? "PASS" : "FAIL") + "] " + name + " - " + ruleResult.getMessage()));

        passwordAuditRepository.save(result);
        System.out.println("Audit summary saved to database (password itself is never stored).");
    }

    private void runReport() {
        SecurityReportGenerator generator = new SecurityReportGenerator();
        String report = generator.generate(
                scanResultRepository.findAll(),
                integrityEventRepository.findAll(),
                passwordAuditRepository.findAll()
        );

        System.out.println("\n" + report);

        System.out.print("Export this report to a file? (y/n): ");
        String answer = input.nextLine().trim();
        if (answer.equalsIgnoreCase("y")) {
            try {
                ReportExporter exporter = new ReportExporter();
                Path exported = exporter.export(report, Paths.get("reports"));
                System.out.println("Report exported to " + exported.toAbsolutePath());
            } catch (Exception e) {
                System.out.println("Could not export report: " + e.getMessage());
            }
        }
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = input.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private Path readDirectory(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = input.nextLine().trim();
            Path path = Paths.get(line);
            if (path.toFile().isDirectory()) {
                return path;
            }
            System.out.println("That path does not exist or is not a directory. Try again.");
        }
    }
}
