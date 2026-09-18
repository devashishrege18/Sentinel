package com.sentinel.scanner;

/**
 * Thrown when the caller asks the scanner to scan a port range that is
 * outside the valid TCP port space (0-65535) or where the lower bound is
 * greater than the upper bound.
 */
public class InvalidPortRangeException extends ScanException {

    public InvalidPortRangeException(int startPort, int endPort) {
        super("Invalid port range: " + startPort + "-" + endPort
                + " (ports must be between 0 and 65535, start <= end)");
    }
}
