package com.sentinel.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton owning the single SQLite connection used by the whole
 * application. SQLite is file-based, so no separate database server needs
 * to be installed or started; the database file is created automatically
 * the first time the application runs.
 */
public final class DatabaseManager {

    private static volatile DatabaseManager instance;

    private final Connection connection;

    private DatabaseManager(String databaseFilePath) {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFilePath);
            initializeSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not open SQLite database at " + databaseFilePath, e);
        }
    }

    public static DatabaseManager getInstance(String databaseFilePath) {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager(databaseFilePath);
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void initializeSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS scan_results (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    host TEXT NOT NULL,
                    port INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    latency_millis INTEGER NOT NULL,
                    scanned_at TEXT NOT NULL
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS integrity_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    relative_path TEXT NOT NULL,
                    change_type TEXT NOT NULL,
                    previous_hash TEXT,
                    current_hash TEXT,
                    detected_at TEXT NOT NULL
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS password_audits (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    score INTEGER NOT NULL,
                    max_score INTEGER NOT NULL,
                    strength_label TEXT NOT NULL,
                    audited_at TEXT NOT NULL
                )
            """);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // Nothing further to do on shutdown if the connection won't close cleanly.
            System.err.println("Warning: failed to close database connection cleanly: " + e.getMessage());
        }
    }
}
