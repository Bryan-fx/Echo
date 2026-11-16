package com.Bryanfx.Echo;

import com.Bryanfx.Echo.database.Database;
import com.Bryanfx.Echo.model.HistoryEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;

public class EchoPlugin extends JavaPlugin {

    private Database database;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        database = new Database(this);
        try {
            database.connect();
        } catch (SQLException e) {
            getLogger().severe("Could not connect to the database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new HistoryListener(this), this);
        scheduleCleanup();
        var command = getCommand("echo");
        if (command != null) {
            command.setExecutor((sender, cmd, label, args) -> executeCommand(sender, label, args));
        }

        getLogger().info("Echo enabled. The server now remembers.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("Echo disabled.");
    }

    public Database getDatabase() {
        return database;
    }

    public void record(HistoryEvent event) {
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                database.record(event);
            } catch (SQLException exception) {
                getLogger().warning("Could not record history event: " + exception.getMessage());
            }
        });
    }

    private boolean executeCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "Echo " + getPluginMeta().getVersion()
                    + ChatColor.GRAY + " - usage: /" + label + " search <query>");
            return true;
        }
        if (!args[0].equalsIgnoreCase("search")) {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " search <query>.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " search <query>");
            return true;
        }

        String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        int maxResults = getConfig().getInt("search.max-results", 20);
        sender.sendMessage(ChatColor.GRAY + "Searching Echo history...");
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                List<HistoryEvent> events = database.search(query, maxResults);
                getServer().getScheduler().runTask(this, () -> sendResults(sender, query, events));
            } catch (SQLException exception) {
                getLogger().warning("Could not search history: " + exception.getMessage());
                getServer().getScheduler().runTask(this,
                        () -> sender.sendMessage(ChatColor.RED + "Echo could not search its database."));
            }
        });
        return true;
    }

    private void sendResults(CommandSender sender, String query, List<HistoryEvent> events) {
        if (events.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No history found for \"" + query + "\".");
            return;
        }
        sender.sendMessage(ChatColor.AQUA + "Echo history for \"" + query + "\" (" + events.size() + "):");
        for (HistoryEvent event : events) {
            String item = event.material() == null ? "" : " " + event.material() + " x" + event.amount();
            sender.sendMessage(ChatColor.GRAY + "- " + event.playerName() + " " + event.action() + item
                    + " in " + event.world() + " [" + event.importance().name().toLowerCase() + "]");
        }
    }

    private void scheduleCleanup() {
        long intervalMinutes = Math.max(1, getConfig().getLong("retention.cleanup-interval-minutes", 60));
        long ticks = intervalMinutes * 60L * 20L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                int deleted = database.cleanup(getConfig().getInt("retention.critical-days", 90),
                        getConfig().getInt("retention.normal-days", 14));
                if (deleted > 0) {
                    getLogger().info("Removed " + deleted + " expired history events.");
                }
            } catch (SQLException exception) {
                getLogger().warning("Could not clean up history: " + exception.getMessage());
            }
        }, ticks, ticks);
    }
}