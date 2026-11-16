package com.Bryanfx.Echo.database;

import com.Bryanfx.Echo.model.HistoryEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Database implements AutoCloseable {
    private final JavaPlugin plugin;
    private Connection connection;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        String databaseType = plugin.getConfig().getString("database.type", "sqlite");
        if (!"sqlite".equalsIgnoreCase(databaseType)) {
            throw new SQLException("Unsupported database type '" + databaseType
                    + "'. SQLite is currently the only supported backend.");
        }
        File file = new File(plugin.getDataFolder(), plugin.getConfig().getString("database.sqlite.file", "echo.db"));
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new SQLException("Unable to create database directory: " + parent);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS history (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      player_uuid TEXT,
                      player_name TEXT NOT NULL,
                      action TEXT NOT NULL,
                      material TEXT,
                      amount INTEGER NOT NULL DEFAULT 1,
                      world TEXT,
                      x INTEGER,
                      y INTEGER,
                      z INTEGER,
                      importance TEXT NOT NULL,
                      created_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_history_created_at ON history(created_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_history_search ON history(material, player_name, action)");
        }
    }

    public synchronized void record(HistoryEvent event) throws SQLException {
        ensureConnected();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO history
                (player_uuid, player_name, action, material, amount, world, x, y, z, importance, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, event.playerUuid());
            statement.setString(2, event.playerName());
            statement.setString(3, event.action());
            statement.setString(4, event.material());
            statement.setInt(5, event.amount());
            statement.setString(6, event.world());
            if (event.location() == null) {
                statement.setNull(7, Types.INTEGER);
                statement.setNull(8, Types.INTEGER);
                statement.setNull(9, Types.INTEGER);
            } else {
                statement.setInt(7, event.location().getBlockX());
                statement.setInt(8, event.location().getBlockY());
                statement.setInt(9, event.location().getBlockZ());
            }
            statement.setString(10, event.importance().name());
            statement.setLong(11, Instant.now().getEpochSecond());
            statement.executeUpdate();
        }
    }

    public synchronized List<HistoryEvent> search(String query, int limit) throws SQLException {
        ensureConnected();
        String pattern = "%" + query.trim() + "%";
        List<HistoryEvent> events = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT player_uuid, player_name, action, material, amount, world, importance
                FROM history
                WHERE player_name LIKE ? COLLATE NOCASE
                   OR action LIKE ? COLLATE NOCASE
                   OR material LIKE ? COLLATE NOCASE
                ORDER BY created_at DESC LIMIT ?
                """)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setInt(4, Math.max(1, limit));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    events.add(new HistoryEvent(result.getString(1), result.getString(2),
                            result.getString(3), result.getString(4), result.getInt(5),
                            result.getString(6), null,
                            com.Bryanfx.Echo.model.Importance.valueOf(result.getString(7))));
                }
            }
        }
        return events;
    }

    public synchronized int cleanup(int criticalDays, int normalDays) throws SQLException {
        ensureConnected();
        long now = Instant.now().getEpochSecond();
        int deleted = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM history WHERE (importance = 'CRITICAL' AND ? > 0 AND created_at < ?) " +
                        "OR (importance = 'NORMAL' AND ? > 0 AND created_at < ?)")) {
            statement.setInt(1, criticalDays);
            statement.setLong(2, now - criticalDays * 86400L);
            statement.setInt(3, normalDays);
            statement.setLong(4, now - normalDays * 86400L);
            deleted = statement.executeUpdate();
        }
        return deleted;
    }

    private void ensureConnected() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database is not connected");
        }
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not close database: " + exception.getMessage());
            }
        }
    }
}
