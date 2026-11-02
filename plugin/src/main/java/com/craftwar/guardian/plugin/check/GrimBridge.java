package com.craftwar.guardian.plugin.check;

import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.craftwar.guardian.plugin.Guardian;
import com.craftwar.guardian.plugin.cases.Source;
import com.craftwar.guardian.plugin.session.PlayerSession;

public final class GrimBridge implements Listener {

    private final Guardian g;
    private boolean warned;

    private GrimBridge(Guardian g) {
        this.g = g;
    }

    public static boolean install(Guardian g) {
        if (!g.settings.grimEnabled) return false;
        try {
            Class<? extends Event> type = Class.forName(g.settings.grimEventClass).asSubclass(Event.class);
            GrimBridge bridge = new GrimBridge(g);
            g.plugin.getServer().getPluginManager().registerEvent(type, bridge, EventPriority.MONITOR,
                (listener, event) -> bridge.onFlag(event), g.plugin, true);
            return true;
        } catch (ClassNotFoundException | ClassCastException e) {
            return false;
        }
    }

    private void onFlag(Event event) {
        try {
            UUID id = playerId(invoke(event, "getPlayer"));
            if (id == null) return;
            Object check = invoke(event, "getCheck");
            String checkName = check == null ? "?" : String.valueOf(invoke(check, "getCheckName"));
            g.sync(() -> {
                PlayerSession s = g.sessions.get(id);
                if (s != null) g.cases.report(s.id, s.name, "GRIM_FLAG", Source.SERVER, checkName, "");
            });
        } catch (Exception e) {
            if (!warned) {
                warned = true;
                g.plugin.getLogger().warning("Grim event shape not recognized: " + e);
            }
        }
    }

    private static UUID playerId(Object player) throws Exception {
        if (player == null) return null;
        if (player instanceof Player) return ((Player) player).getUniqueId();
        Object id = invoke(player, "getUniqueId");
        return id instanceof UUID ? (UUID) id : null;
    }

    private static Object invoke(Object target, String method) throws Exception {
        Method m = target.getClass().getMethod(method);
        m.setAccessible(true);
        return m.invoke(target);
    }
}
