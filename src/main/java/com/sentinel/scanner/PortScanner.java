package com.sentinel.scanner;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Scans a range of TCP ports on a target host concurrently.
 *
 * <p>Each port is probed on its own task submitted to an {@link ExecutorService}.
 * A fixed-size thread pool is used deliberately: scanning thousands of ports with
 * one thread each would exhaust OS resources, so the pool size is capped and
 * reused across the whole scan.</p>
 */
public class PortScanner {

    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;
    private static final int DEFAULT_POOL_SIZE = 100;

    private final int connectTimeoutMillis;
    private final int poolSize;

    public PortScanner() {
        this(500, DEFAULT_POOL_SIZE);
    }

    public PortScanner(int connectTimeoutMillis, int poolSize) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.poolSize = poolSize;
    }

    /**
     * Scans every port in [startPort, endPort] (inclusive) on the given host.
     *
     * @param host      hostname or IP address to scan
     * @param startPort lower bound of the port range, inclusive
     * @param endPort   upper bound of the port range, inclusive
     * @return scan results sorted by port number
     * @throws InvalidPortRangeException if the range is malformed
     * @throws HostUnreachableException  if the host name cannot be resolved
     */
    public List<PortScanResult> scanRange(String host, int startPort, int endPort)
            throws ScanException {

        validateRange(startPort, endPort);
        InetAddress address = resolveHost(host);

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(poolSize, endPort - startPort + 1));
        List<Future<PortScanResult>> futures = new ArrayList<>();

        try {
            for (int port = startPort; port <= endPort; port++) {
                int currentPort = port;
                Callable<PortScanResult> task = () -> probePort(host, address, currentPort);
                futures.add(executor.submit(task));
            }

            List<PortScanResult> results = new ArrayList<>();
            for (Future<PortScanResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    // A single port's probe failing unexpectedly shouldn't abort the whole scan;
                    // record it as filtered so the caller still sees a complete port list.
                    results.add(PortScanResult.builder()
                            .host(host)
                            .port(-1)
                            .status(PortStatus.FILTERED)
                            .latencyMillis(-1)
                            .build());
                }
            }

            results.sort(Comparator.comparingInt(PortScanResult::getPort));
            return results;
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private PortScanResult probePort(String host, InetAddress address, int port) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address, port), connectTimeoutMillis);
            long latency = System.currentTimeMillis() - start;
            return PortScanResult.builder()
                    .host(host)
                    .port(port)
                    .status(PortStatus.OPEN)
                    .latencyMillis(latency)
                    .scannedAt(LocalDateTime.now())
                    .build();
        } catch (java.net.SocketTimeoutException e) {
            return PortScanResult.builder()
                    .host(host)
                    .port(port)
                    .status(PortStatus.FILTERED)
                    .latencyMillis(System.currentTimeMillis() - start)
                    .scannedAt(LocalDateTime.now())
                    .build();
        } catch (java.net.ConnectException e) {
            return PortScanResult.builder()
                    .host(host)
                    .port(port)
                    .status(PortStatus.CLOSED)
                    .latencyMillis(System.currentTimeMillis() - start)
                    .scannedAt(LocalDateTime.now())
                    .build();
        } catch (java.io.IOException e) {
            return PortScanResult.builder()
                    .host(host)
                    .port(port)
                    .status(PortStatus.CLOSED)
                    .latencyMillis(System.currentTimeMillis() - start)
                    .scannedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            return PortScanResult.builder()
                    .host(host)
                    .port(port)
                    .status(PortStatus.CLOSED)
                    .latencyMillis(System.currentTimeMillis() - start)
                    .scannedAt(LocalDateTime.now())
                    .build();
        }
    }

    private void validateRange(int startPort, int endPort) throws InvalidPortRangeException {
        if (startPort < MIN_PORT || endPort > MAX_PORT || startPort > endPort) {
            throw new InvalidPortRangeException(startPort, endPort);
        }
    }

    private InetAddress resolveHost(String host) throws HostUnreachableException {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new HostUnreachableException(host, e);
        }
    }
}
