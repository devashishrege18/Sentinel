package com.sentinel.integrity;

/**
 * Describes how a monitored file's state differs from the stored baseline.
 */
public enum ChangeType {
    ADDED,
    MODIFIED,
    DELETED,
    UNCHANGED
}
