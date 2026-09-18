package com.sentinel.scanner;

import java.time.LocalDateTime;

/**
 * Immutable record of the outcome of probing one port on one host.
 * Built through the nested {@link Builder} rather than a large constructor,
 * since scan results accumulate several optional/derived fields
 * (latency, status, timestamp) that read poorly as positional arguments.
 */
public final class PortScanResult {

    private final String host;
    private final int port;
    private final PortStatus status;
    private final long latencyMillis;
    private final LocalDateTime scannedAt;

    private PortScanResult(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.status = builder.status;
        this.latencyMillis = builder.latencyMillis;
        this.scannedAt = builder.scannedAt;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public PortStatus getStatus() {
        return status;
    }

    public long getLatencyMillis() {
        return latencyMillis;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    @Override
    public String toString() {
        return String.format("%-15s port %-5d [%-8s] %4d ms", host, port, status, latencyMillis);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String host;
        private int port;
        private PortStatus status;
        private long latencyMillis;
        private LocalDateTime scannedAt = LocalDateTime.now();

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder status(PortStatus status) {
            this.status = status;
            return this;
        }

        public Builder latencyMillis(long latencyMillis) {
            this.latencyMillis = latencyMillis;
            return this;
        }

        public Builder scannedAt(LocalDateTime scannedAt) {
            this.scannedAt = scannedAt;
            return this;
        }

        public PortScanResult build() {
            if (host == null || host.isBlank()) {
                throw new IllegalStateException("host must be set before building a PortScanResult");
            }
            if (status == null) {
                throw new IllegalStateException("status must be set before building a PortScanResult");
            }
            return new PortScanResult(this);
        }
    }
}
