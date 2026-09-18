package com.sentinel.report;

import com.sentinel.integrity.ChangeEvent;
import com.sentinel.integrity.ChangeType;
import com.sentinel.persistence.PasswordAuditRepository;
import com.sentinel.persistence.PasswordAuditRepository.AuditSummary;
import com.sentinel.scanner.PortScanResult;
import com.sentinel.scanner.PortStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds a single, human-readable summary report by pulling together the
 * scan history, integrity events, and password audit history stored in the
 * database. All aggregation here is done with the Stream API rather than
 * manual loops, in line with Module 4 of the course.
 */
public class SecurityReportGenerator {

    public String generate(List<PortScanResult> scanHistory,
                            List<ChangeEvent> integrityHistory,
                            List<AuditSummary> passwordHistory) {

        StringBuilder report = new StringBuilder();
        report.append("=".repeat(60)).append("\n");
        report.append("SENTINEL SECURITY REPORT\n");
        report.append("=".repeat(60)).append("\n\n");

        appendScanSection(report, scanHistory);
        appendIntegritySection(report, integrityHistory);
        appendPasswordSection(report, passwordHistory);

        return report.toString();
    }

    private void appendScanSection(StringBuilder report, List<PortScanResult> scanHistory) {
        report.append("-- Port Scan Summary --\n");
        if (scanHistory.isEmpty()) {
            report.append("No scans recorded yet.\n\n");
            return;
        }

        Map<PortStatus, Long> countByStatus = scanHistory.stream()
                .collect(Collectors.groupingBy(PortScanResult::getStatus, Collectors.counting()));

        report.append("Total probes recorded: ").append(scanHistory.size()).append("\n");
        countByStatus.forEach((status, count) -> report.append("  ").append(status).append(": ").append(count).append("\n"));

        List<String> openPorts = scanHistory.stream()
                .filter(r -> r.getStatus() == PortStatus.OPEN)
                .sorted((a, b) -> Integer.compare(a.getPort(), b.getPort()))
                .map(r -> r.getHost() + ":" + r.getPort())
                .distinct()
                .collect(Collectors.toList());

        report.append("Currently known open ports: ")
                .append(openPorts.isEmpty() ? "none" : String.join(", ", openPorts))
                .append("\n\n");
    }

    private void appendIntegritySection(StringBuilder report, List<ChangeEvent> integrityHistory) {
        report.append("-- File Integrity Summary --\n");
        if (integrityHistory.isEmpty()) {
            report.append("No integrity events recorded yet.\n\n");
            return;
        }

        Map<ChangeType, Long> countByType = integrityHistory.stream()
                .collect(Collectors.groupingBy(ChangeEvent::getChangeType, Collectors.counting()));

        report.append("Total change events recorded: ").append(integrityHistory.size()).append("\n");
        countByType.forEach((type, count) -> report.append("  ").append(type).append(": ").append(count).append("\n"));

        long recentModifications = integrityHistory.stream()
                .filter(e -> e.getChangeType() == ChangeType.MODIFIED || e.getChangeType() == ChangeType.DELETED)
                .count();

        if (recentModifications > 0) {
            report.append("WARNING: ").append(recentModifications)
                    .append(" file(s) were modified or deleted since the baseline was created.\n");
        }
        report.append("\n");
    }

    private void appendPasswordSection(StringBuilder report, List<AuditSummary> passwordHistory) {
        report.append("-- Password Audit Summary --\n");
        if (passwordHistory.isEmpty()) {
            report.append("No password audits recorded yet.\n\n");
            return;
        }

        double averageRatio = passwordHistory.stream()
                .mapToDouble(a -> (double) a.score() / a.maxScore())
                .average()
                .orElse(0.0);

        Map<String, Long> countByStrength = passwordHistory.stream()
                .collect(Collectors.groupingBy(AuditSummary::strengthLabel, Collectors.counting()));

        report.append("Total audits recorded: ").append(passwordHistory.size()).append("\n");
        report.append(String.format("Average pass ratio: %.0f%%%n", averageRatio * 100));
        countByStrength.forEach((label, count) -> report.append("  ").append(label).append(": ").append(count).append("\n"));
        report.append("\n");
    }
}
