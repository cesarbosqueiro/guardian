package com.craftwar.guardian.plugin.request;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.craftwar.guardian.plugin.Guardian;

public final class Notifier {

    public static final String PREFIX = ChatColor.DARK_AQUA + "[Guardian] " + ChatColor.GRAY;

    private final Guardian g;

    public Notifier(Guardian g) {
        this.g = g;
    }

    public void done(Pending p, String message) {
        g.sync(() -> {
            Player actor = Bukkit.getPlayerExact(p.actor);
            String text = PREFIX + p.playerName + " #" + p.id + ": " + message;
            if (actor != null) actor.sendMessage(text);
            else g.plugin.getLogger().info(ChatColor.stripColor(text));
        });
    }
}
