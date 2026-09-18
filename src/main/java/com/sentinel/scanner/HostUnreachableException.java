package com.sentinel.scanner;

/**
 * Thrown when the target host name or IP address cannot be resolved or
 * reached at all, as opposed to a single port simply being closed.
 */
public class HostUnreachableException extends ScanException {

    public HostUnreachableException(String host, Throwable cause) {
        super("Could not reach host: " + host, cause);
    }
}
