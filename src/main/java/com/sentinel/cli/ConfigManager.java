package com.sentinel.cli;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Singleton holding the small set of configuration values the application
 * needs (where the database lives, default scan parameters, etc.). Kept
 * deliberately simple - a real deployment might load these from a
 * properties file, but a single shared instance is enough to demonstrate
 * the pattern and avoid passing the same values through every constructor.
 */
public final class ConfigManager {

    private static ConfigManager instance;

    private final Path databasePath = Paths.get("sentinel.db");
    private final int defaultConnectTimeoutMillis = 500;
    private final int defaultScanPoolSize = 100;

    private ConfigManager() {
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public int getDefaultConnectTimeoutMillis() {
        return defaultConnectTimeoutMillis;
    }

    public int getDefaultScanPoolSize() {
        return defaultScanPoolSize;
    }
}
