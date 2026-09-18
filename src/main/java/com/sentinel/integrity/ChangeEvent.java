package com.sentinel.integrity;

import java.time.LocalDateTime;

/**
 * A single detected difference between the stored baseline and the current
 * state of a watched directory.
 */
public final class ChangeEvent {

    private final String relativePath;
    private final ChangeType changeType;
    private final String previousHash;
    private final String currentHash;
    private final LocalDateTime detectedAt;

    public ChangeEvent(String relativePath, ChangeType changeType, String previousHash, String currentHash) {
        this(relativePath, changeType, previousHash, currentHash, LocalDateTime.now());
    }

    /**
     * Full constructor used when reconstructing an event from storage, where
     * the original detection time must be preserved rather than reset to now.
     */
    public ChangeEvent(String relativePath, ChangeType changeType, String previousHash, String currentHash,
                        LocalDateTime detectedAt) {
        this.relativePath = relativePath;
        this.changeType = changeType;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
        this.detectedAt = detectedAt;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    @Override
    public String toString() {
        return String.format("[%s] %-10s %s", detectedAt, changeType, relativePath);
    }
}
