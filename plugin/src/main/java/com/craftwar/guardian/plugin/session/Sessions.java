package com.craftwar.guardian.plugin.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

public final class Sessions {

    private final Map<UUID, PlayerSession> byId = new HashMap<UUID, PlayerSession>();

    public PlayerSession open(Player player) {
        PlayerSession s = new PlayerSession(player, System.currentTimeMillis());
        byId.put(s.id, s);
        return s;
    }

    public PlayerSession close(UUID id) {
        return byId.remove(id);
    }

    public PlayerSession get(Player player) {
        return byId.get(player.getUniqueId());
    }

    public PlayerSession get(UUID id) {
        return byId.get(id);
    }

    public Collection<PlayerSession> all() {
        return byId.values();
    }
}
