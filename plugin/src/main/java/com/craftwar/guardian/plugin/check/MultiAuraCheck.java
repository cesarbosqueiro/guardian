package com.craftwar.guardian.plugin.check;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.craftwar.guardian.plugin.Guardian;
import com.craftwar.guardian.plugin.cases.Source;
import com.craftwar.guardian.plugin.session.PlayerSession;

public final class MultiAuraCheck implements Listener {

    private static final int IMMEDIATE_TARGETS = 3;
    private static final int REPEATS = 3;
    private static final long REPEAT_WINDOW_TICKS = 200;

    private final Guardian g;

    public MultiAuraCheck(Guardian g) {
        this.g = g;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || !(e.getDamager() instanceof Player)) return;
        PlayerSession s = g.sessions.get((Player) e.getDamager());
        if (s == null) return;
        long tick = g.tracker.tick();
        int distinct = s.attacks.record(tick, e.getEntity().getEntityId());
        if (distinct == IMMEDIATE_TARGETS) {
            g.cases.report(s.id, s.name, "MULTI_AURA", Source.SERVER, "tick", distinct + " alvos no mesmo tick");
        } else if (distinct == 2 && s.attacks.multiTargetTicksSince(tick - REPEAT_WINDOW_TICKS) >= REPEATS) {
            g.cases.report(s.id, s.name, "MULTI_AURA", Source.SERVER, "window", REPEATS + "+ ticks com 2 alvos em 10s");
        }
    }
}
