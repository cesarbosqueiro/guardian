package com.craftwar.guardian.plugin.session;

import com.craftwar.guardian.plugin.Guardian;

public final class Attestation {

    public static final String PERMISSION = "craftwar.guardian.attested";

    private final Guardian g;

    public Attestation(Guardian g) {
        this.g = g;
    }

    void grant(PlayerSession s) {
        s.attested = true;
        s.silent = false;
        s.lastHeartbeatMillis = System.currentTimeMillis();
        if (s.attachment == null) s.attachment = s.player.addAttachment(g.plugin, PERMISSION, true);
    }

    void revoke(PlayerSession s) {
        s.attested = false;
        if (s.attachment != null) {
            s.player.removeAttachment(s.attachment);
            s.attachment = null;
        }
    }
}
