package com.sentinel.scanner;

/**
 * Result of probing a single TCP port.
 */
public enum PortStatus {
    OPEN,
    CLOSED,
    FILTERED // connection attempt timed out - likely blocked by a firewall
}
