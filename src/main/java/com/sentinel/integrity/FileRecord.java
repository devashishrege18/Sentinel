package com.sentinel.integrity;

import java.time.LocalDateTime;

/**
 * A single file's fingerprint: its path, content hash, size, and the last
 * modified time reported by the filesystem. Two FileRecords for the same
 * path taken at different times are compared to detect tampering.
 */
public final class FileRecord {

    private final String relativePath;
    private final String sha256Hash;
    private final long sizeBytes;
    private final LocalDateTime lastModified;

    public FileRecord(String relativePath, String sha256Hash, long sizeBytes, LocalDateTime lastModified) {
        this.relativePath = relativePath;
        this.sha256Hash = sha256Hash;
        this.sizeBytes = sizeBytes;
        this.lastModified = lastModified;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    /**
     * Serializes this record to a single pipe-delimited line for the flat-file
     * baseline store. Kept deliberately simple (no external library) so the
     * baseline format has no dependency beyond java.nio.
     */
    public String toBaselineLine() {
        return relativePath + "|" + sha256Hash + "|" + sizeBytes + "|" + lastModified;
    }

    public static FileRecord fromBaselineLine(String line) {
        String[] parts = line.split("\\|", 4);
        return new FileRecord(parts[0], parts[1], Long.parseLong(parts[2]), LocalDateTime.parse(parts[3]));
    }

    @Override
    public String toString() {
        return relativePath + " (" + sizeBytes + " bytes, modified " + lastModified + ")";
    }
}
