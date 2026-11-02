package com.craftwar.guardian.plugin.request;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class Requests {

    private static final long TIMEOUT_MILLIS = 120_000L;

    private final Map<Integer, Pending> pending = new HashMap<Integer, Pending>();
    private int nextId = (int) (System.nanoTime() & 0x3FFFFFFF);

    public Pending create(UUID player, String playerName, int kind, String actor, String ticket) {
        expire();
        Pending p = new Pending(++nextId, player, playerName, kind, actor, ticket, System.currentTimeMillis());
        pending.put(p.id, p);
        return p;
    }

    public Pending take(int id, UUID player) {
        Pending p = pending.get(id);
        if (p == null || !p.player.equals(player)) return null;
        pending.remove(id);
        return p;
    }

    public boolean expects(int id, UUID player, int kind) {
        Pending p = pending.get(id);
        return p != null && p.player.equals(player) && p.kind == kind;
    }

    public void dropAll(UUID player) {
        for (Iterator<Pending> it = pending.values().iterator(); it.hasNext(); ) {
            if (it.next().player.equals(player)) it.remove();
        }
    }

    private void expire() {
        long now = System.currentTimeMillis();
        for (Iterator<Pending> it = pending.values().iterator(); it.hasNext(); ) {
            if (now - it.next().createdMillis > TIMEOUT_MILLIS) it.remove();
        }
    }
}
