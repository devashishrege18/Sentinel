package com.sentinel.scanner;

/**
 * Base checked exception for anything that can go wrong during a port scan.
 * Kept as a checked exception on purpose: callers of the scanner module are
 * forced to decide how to react to a failed scan rather than letting it
 * propagate silently.
 */
public class ScanException extends Exception {

    public ScanException(String message) {
        super(message);
    }

    public ScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
