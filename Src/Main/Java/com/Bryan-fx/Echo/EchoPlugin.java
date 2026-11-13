package me.bryanfx.echo;

import me.bryanfx.echo.database.Database;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

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

        var command = getCommand("echo");
        if (command != null) {
            command.setExecutor((sender, cmd, label, args) -> {
                if (args.length == 0) {
                    sender.sendMessage(ChatColor.AQUA + "Echo " + getPluginMeta().getVersion()
                            + ChatColor.GRAY + " - usage: /" + label + " search <query>");
                    return true;
                }
                if (args[0].equalsIgnoreCase("search")) {
                    sender.sendMessage(ChatColor.YELLOW + "Search is not built yet.");
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                return true;
            });
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
}