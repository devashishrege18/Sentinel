package com.sentinel.persistence;

import com.sentinel.auth.PasswordAuditResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists only the aggregate outcome of a password audit (score, max
 * score, strength label) - never the password itself, since plaintext
 * passwords should never be written to disk or a database.
 */
public class PasswordAuditRepository {

    private final Connection connection;

    public PasswordAuditRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(PasswordAuditResult result) {
        String sql = "INSERT INTO password_audits (score, max_score, strength_label, audited_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, result.getScore());
            statement.setInt(2, result.getMaxScore());
            statement.setString(3, result.getStrengthLabel());
            statement.setString(4, result.getAuditedAt().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save password audit result", e);
        }
    }

    public List<AuditSummary> findAll() {
        String sql = "SELECT score, max_score, strength_label, audited_at FROM password_audits ORDER BY audited_at DESC";
        List<AuditSummary> summaries = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                summaries.add(new AuditSummary(
                        rs.getInt("score"),
                        rs.getInt("max_score"),
                        rs.getString("strength_label"),
                        rs.getString("audited_at")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load password audit history", e);
        }
        return summaries;
    }

    /**
     * Lightweight read-only view of a stored audit row - deliberately
     * separate from {@link PasswordAuditResult}, which carries full
     * per-rule detail that is never persisted.
     */
    public record AuditSummary(int score, int maxScore, String strengthLabel, String auditedAt) {
    }
}
