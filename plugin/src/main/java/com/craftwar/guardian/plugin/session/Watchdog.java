package com.craftwar.guardian.plugin.session;

import com.craftwar.guardian.plugin.Guardian;
import com.craftwar.guardian.plugin.cases.Source;

public final class Watchdog implements Runnable {

    private final Guardian g;
    private final Attestation attestation;

    public Watchdog(Guardian g, Attestation attestation) {
        this.g = g;
        this.attestation = attestation;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (PlayerSession s : g.sessions.all()) {
            g.badges.publish(s, g.cases.file(s.id).score(now));
            if (s.attested && now - s.lastHeartbeatMillis > g.settings.silenceMillis()) {
                s.silent = true;
                attestation.revoke(s);
                g.cases.report(s.id, s.name, "SILENT_AGENT", Source.CROSS, "heartbeat",
                    (now - s.lastHeartbeatMillis) / 1000 + "s sem heartbeat");
            } else if (!s.attested && !s.warned && now - s.joinedMillis > g.settings.attestationTimeoutMillis) {
                s.warned = true;
                if (g.settings.enforceRestrictedMode) s.player.sendMessage(g.settings.restrictedMessage);
            }
        }
    }
}
