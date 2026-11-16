package com.Bryanfx.Echo.model;

import org.bukkit.Location;

public record HistoryEvent(
        String playerUuid,
        String playerName,
        String action,
        String material,
        int amount,
        String world,
        Location location,
        Importance importance) {
}
