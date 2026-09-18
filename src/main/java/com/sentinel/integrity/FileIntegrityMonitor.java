package com.sentinel.integrity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Monitors a directory for unauthorized changes by hashing every file with
 * SHA-256 and comparing against a previously stored baseline.
 *
 * <p>Hashing is CPU/IO bound per file, so a directory with many files is
 * hashed using a fixed thread pool rather than sequentially.</p>
 */
public class FileIntegrityMonitor {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int POOL_SIZE = 8;

    private final Path watchedDirectory;
    private final Path baselineFile;

    public FileIntegrityMonitor(Path watchedDirectory, Path baselineFile) {
        this.watchedDirectory = watchedDirectory;
        this.baselineFile = baselineFile;
    }

    /**
     * Hashes every regular file under the watched directory and writes the
     * result out as the new baseline, overwriting any previous one.
     */
    public Map<String, FileRecord> createBaseline() throws IntegrityException {
        Map<String, FileRecord> records = hashDirectory();
        saveBaseline(records);
        return records;
    }

    /**
     * Compares the current state of the watched directory against the stored
     * baseline and returns every detected change (added, modified, deleted).
     * Unchanged files are not included in the result.
     */
    public List<ChangeEvent> checkIntegrity() throws IntegrityException {
        Map<String, FileRecord> baseline = loadBaseline();
        Map<String, FileRecord> current = hashDirectory();

        List<ChangeEvent> events = new ArrayList<>();

        for (Map.Entry<String, FileRecord> entry : current.entrySet()) {
            String path = entry.getKey();
            FileRecord currentRecord = entry.getValue();
            FileRecord baselineRecord = baseline.get(path);

            if (baselineRecord == null) {
                events.add(new ChangeEvent(path, ChangeType.ADDED, null, currentRecord.getSha256Hash()));
            } else if (!baselineRecord.getSha256Hash().equals(currentRecord.getSha256Hash())) {
                events.add(new ChangeEvent(path, ChangeType.MODIFIED,
                        baselineRecord.getSha256Hash(), currentRecord.getSha256Hash()));
            }
        }

        for (String path : baseline.keySet()) {
            if (!current.containsKey(path)) {
                events.add(new ChangeEvent(path, ChangeType.DELETED, baseline.get(path).getSha256Hash(), null));
            }
        }

        return events;
    }

    public boolean baselineExists() {
        return Files.exists(baselineFile);
    }

    private Map<String, FileRecord> hashDirectory() throws IntegrityException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(watchedDirectory)) {
            files = walk.filter(Files::isRegularFile)
                    // The baseline file itself lives inside the watched directory by default;
                    // it must never be treated as monitored content.
                    .filter(path -> !path.equals(baselineFile))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IntegrityException("Could not walk directory: " + watchedDirectory, e);
        }

        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
        Map<String, FileRecord> records = new HashMap<>();

        try {
            List<Future<FileRecord>> futures = new ArrayList<>();
            for (Path file : files) {
                Callable<FileRecord> task = () -> hashFile(file);
                futures.add(executor.submit(task));
            }

            for (Future<FileRecord> future : futures) {
                FileRecord record = future.get();
                records.put(record.getRelativePath(), record);
            }
        } catch (Exception e) {
            throw new IntegrityException("Failed while hashing files under " + watchedDirectory, e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        return records;
    }

    private FileRecord hashFile(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        
        try (InputStream is = Files.newInputStream(file);
             DigestInputStream dis = new DigestInputStream(is, digest)) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // Read file to update digest
            }
        }
        
        byte[] hashBytes = digest.digest();

        StringBuilder hexHash = new StringBuilder();
        for (byte b : hashBytes) {
            hexHash.append(String.format("%02x", b));
        }

        String relativePath = watchedDirectory.relativize(file).toString();
        long size = Files.size(file);
        LocalDateTime modified = LocalDateTime.ofInstant(
                Files.getLastModifiedTime(file).toInstant(), java.time.ZoneId.systemDefault());

        return new FileRecord(relativePath, hexHash.toString(), size, modified);
    }

    private void saveBaseline(Map<String, FileRecord> records) throws IntegrityException {
        List<String> lines = records.values().stream()
                .map(FileRecord::toBaselineLine)
                .collect(Collectors.toList());
        try {
            Files.write(baselineFile, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IntegrityException("Could not write baseline file: " + baselineFile, e);
        }
    }

    private Map<String, FileRecord> loadBaseline() throws IntegrityException {
        if (!Files.exists(baselineFile)) {
            throw new IntegrityException(
                    "No baseline found at " + baselineFile + ". Run 'create baseline' first.");
        }
        try {
            List<String> lines = Files.readAllLines(baselineFile, StandardCharsets.UTF_8);
            Map<String, FileRecord> baseline = new HashMap<>();
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                FileRecord record = FileRecord.fromBaselineLine(line);
                baseline.put(record.getRelativePath(), record);
            }
            return baseline;
        } catch (IOException e) {
            throw new IntegrityException("Could not read baseline file: " + baselineFile, e);
        }
    }

    public static Path defaultBaselinePathFor(Path watchedDirectory) {
        return Paths.get(watchedDirectory.toString(), ".sentinel-baseline.txt");
    }
}
