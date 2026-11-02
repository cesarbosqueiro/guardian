package com.craftwar.guardian.plugin.track;

import org.bukkit.Location;

import com.craftwar.guardian.plugin.session.PlayerSession;
import com.craftwar.guardian.plugin.session.Sessions;

public final class Tracker implements Runnable {

    private final Sessions sessions;
    private final Location scratch = new Location(null, 0, 0, 0);
    private long tick;

    public Tracker(Sessions sessions) {
        this.sessions = sessions;
    }

    public long tick() {
        return tick;
    }

    @Override
    public void run() {
        tick++;
        long now = System.currentTimeMillis();
        for (PlayerSession s : sessions.all()) {
            s.player.getLocation(scratch);
            s.track.record(now, scratch.getYaw(), scratch.getPitch(), s.attacks.drainTick());
        }
    }
}
