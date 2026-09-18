package com.sentinel.integrity;

/**
 * Checked exception covering failures while baselining or checking the
 * integrity of a watched directory (unreadable files, I/O errors, a
 * missing baseline, etc.).
 */
public class IntegrityException extends Exception {

    public IntegrityException(String message) {
        super(message);
    }

    public IntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
