package com.sentinel.persistence;

import com.sentinel.integrity.ChangeEvent;
import com.sentinel.integrity.ChangeType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IntegrityEventRepository implements Repository<ChangeEvent> {

    private final Connection connection;

    public IntegrityEventRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(ChangeEvent event) {
        String sql = "INSERT INTO integrity_events (relative_path, change_type, previous_hash, current_hash, detected_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.getRelativePath());
            statement.setString(2, event.getChangeType().name());
            statement.setString(3, event.getPreviousHash());
            statement.setString(4, event.getCurrentHash());
            statement.setString(5, event.getDetectedAt().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save integrity event for " + event.getRelativePath(), e);
        }
    }

    public void saveAll(List<ChangeEvent> events) {
        for (ChangeEvent event : events) {
            save(event);
        }
    }

    @Override
    public List<ChangeEvent> findAll() {
        String sql = "SELECT relative_path, change_type, previous_hash, current_hash, detected_at "
                + "FROM integrity_events ORDER BY detected_at DESC";
        List<ChangeEvent> events = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                events.add(new ChangeEvent(
                        rs.getString("relative_path"),
                        ChangeType.valueOf(rs.getString("change_type")),
                        rs.getString("previous_hash"),
                        rs.getString("current_hash"),
                        LocalDateTime.parse(rs.getString("detected_at"))
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load integrity events", e);
        }
        return events;
    }
}
