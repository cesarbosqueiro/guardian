package com.craftwar.guardian.plugin.papi;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.craftwar.guardian.plugin.session.PlayerSession;

public final class BadgeBoard {

    private static final Badge UNKNOWN = new Badge(false, Badge.NO_AGENT, 0);

    private final Map<UUID, Badge> badges = new ConcurrentHashMap<UUID, Badge>();

    public void publish(PlayerSession s, double score) {
        int state = s.lastHeartbeat() == null || !s.attested() ? Badge.NO_AGENT : s.lastHeartbeat().agentState;
        badges.put(s.id, new Badge(s.attested(), state, Math.round(score)));
    }

    public void attestation(PlayerSession s) {
        Badge old = get(s.id);
        int state = s.attested() ? (s.lastHeartbeat() == null ? 0 : s.lastHeartbeat().agentState) : Badge.NO_AGENT;
        badges.put(s.id, new Badge(s.attested(), state, old.score));
    }

    public void remove(UUID id) {
        badges.remove(id);
    }

    public Badge get(UUID id) {
        Badge b = badges.get(id);
        return b == null ? UNKNOWN : b;
    }
}
