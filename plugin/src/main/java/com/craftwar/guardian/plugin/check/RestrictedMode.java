package com.craftwar.guardian.plugin.check;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.craftwar.guardian.plugin.Guardian;
import com.craftwar.guardian.plugin.session.PlayerSession;

public final class RestrictedMode implements Listener {

    private static final long MESSAGE_INTERVAL_MILLIS = 3000;

    private final Guardian g;
    private final Map<UUID, Long> lastMessage = new HashMap<UUID, Long>();

    public RestrictedMode(Guardian g) {
        this.g = g;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!g.settings.enforceRestrictedMode || !(e.getEntity() instanceof Player)) return;
        Player attacker = attacker(e.getDamager());
        if (attacker == null) return;
        if (!g.settings.restrictedWorlds.isEmpty() && !g.settings.restrictedWorlds.contains(attacker.getWorld().getName())) return;
        PlayerSession s = g.sessions.get(attacker);
        if (s != null && s.attested()) return;
        e.setCancelled(true);
        warn(attacker);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastMessage.remove(e.getPlayer().getUniqueId());
    }

    private void warn(Player p) {
        long now = System.currentTimeMillis();
        Long last = lastMessage.get(p.getUniqueId());
        if (last != null && now - last < MESSAGE_INTERVAL_MILLIS) return;
        lastMessage.put(p.getUniqueId(), now);
        p.sendMessage(g.settings.restrictedMessage);
    }

    private static Player attacker(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
            return (Player) ((Projectile) damager).getShooter();
        }
        return null;
    }
}
