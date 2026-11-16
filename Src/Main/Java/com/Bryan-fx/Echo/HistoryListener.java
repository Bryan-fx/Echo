package com.Bryanfx.Echo;

import com.Bryanfx.Echo.model.HistoryEvent;
import com.Bryanfx.Echo.model.Importance;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class HistoryListener implements Listener {
    private final EchoPlugin plugin;

    public HistoryListener(EchoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("tracking.normal.block-break", true)
                || isIgnoredWorld(event.getBlock().getWorld().getName())) {
            return;
        }
        record(event.getPlayer(), "broke", event.getBlock().getType(), event.getBlock().getLocation(),
                Importance.NORMAL);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("tracking.normal.block-place", true)
                || isIgnoredWorld(event.getBlock().getWorld().getName())) {
            return;
        }
        record(event.getPlayer(), "placed", event.getBlock().getType(), event.getBlock().getLocation(),
                Importance.NORMAL);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("tracking.critical.player-deaths", true)
                || isIgnoredWorld(event.getEntity().getWorld().getName())) {
            return;
        }
        Player player = event.getEntity();
        Player killer = player.getKiller();
        String actor = killer == null ? player.getName() : killer.getName();
        String uuid = killer == null ? player.getUniqueId().toString() : killer.getUniqueId().toString();
        plugin.record(new HistoryEvent(uuid, actor, "killed " + player.getName(), null, 1,
                player.getWorld().getName(), player.getLocation(), Importance.CRITICAL));
    }

    private boolean isIgnoredWorld(String world) {
        return plugin.getConfig().getStringList("ignored-worlds").contains(world);
    }

    private void record(Player player, String action, Material material,
                        org.bukkit.Location location, Importance importance) {
        plugin.record(new HistoryEvent(player.getUniqueId().toString(), player.getName(), action,
                material.name(), 1, location.getWorld().getName(), location, importance));
    }
}
