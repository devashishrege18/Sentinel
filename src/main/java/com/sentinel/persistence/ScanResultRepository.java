package com.sentinel.persistence;

import com.sentinel.scanner.PortScanResult;
import com.sentinel.scanner.PortStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScanResultRepository implements Repository<PortScanResult> {

    private final Connection connection;

    public ScanResultRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(PortScanResult result) {
        String sql = "INSERT INTO scan_results (host, port, status, latency_millis, scanned_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, result.getHost());
            statement.setInt(2, result.getPort());
            statement.setString(3, result.getStatus().name());
            statement.setLong(4, result.getLatencyMillis());
            statement.setString(5, result.getScannedAt().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save scan result for " + result, e);
        }
    }

    public void saveAll(List<PortScanResult> results) {
        for (PortScanResult result : results) {
            save(result);
        }
    }

    @Override
    public List<PortScanResult> findAll() {
        String sql = "SELECT host, port, status, latency_millis, scanned_at FROM scan_results ORDER BY scanned_at DESC";
        List<PortScanResult> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                results.add(PortScanResult.builder()
                        .host(rs.getString("host"))
                        .port(rs.getInt("port"))
                        .status(PortStatus.valueOf(rs.getString("status")))
                        .latencyMillis(rs.getLong("latency_millis"))
                        .scannedAt(LocalDateTime.parse(rs.getString("scanned_at")))
                        .build());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load scan results", e);
        }
        return results;
    }
}
