package com.craftwar.guardian.plugin.session;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

import com.craftwar.guardian.plugin.Guardian;
import com.craftwar.guardian.protocol.Wire;

public final class ConnectionListener implements Listener {

    private final Guardian g;
    private final Gateway gateway;

    public ConnectionListener(Guardian g, Gateway gateway) {
        this.g = g;
        this.gateway = gateway;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        g.sessions.open(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        g.sessions.close(e.getPlayer().getUniqueId());
        g.requests.dropAll(e.getPlayer().getUniqueId());
        g.badges.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRegister(PlayerRegisterChannelEvent e) {
        if (!Wire.CHANNEL.equals(e.getChannel())) return;
        PlayerSession s = g.sessions.get(e.getPlayer());
        if (s == null) s = g.sessions.open(e.getPlayer());
        gateway.startHandshake(s);
    }
}
