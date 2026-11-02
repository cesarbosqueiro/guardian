package com.craftwar.guardian.plugin.session;

import com.craftwar.guardian.plugin.Guardian;
import com.craftwar.guardian.plugin.cases.Source;
import com.craftwar.guardian.plugin.request.Pending;
import com.craftwar.guardian.plugin.request.Notifier;
import com.craftwar.guardian.protocol.Assembler;
import com.craftwar.guardian.protocol.FindingKind;
import com.craftwar.guardian.protocol.Frame;
import com.craftwar.guardian.protocol.MessageType;
import com.craftwar.guardian.protocol.RequestKind;
import com.craftwar.guardian.protocol.message.Abort;
import com.craftwar.guardian.protocol.message.Chunk;
import com.craftwar.guardian.protocol.message.Context;
import com.craftwar.guardian.protocol.message.Finding;
import com.craftwar.guardian.protocol.message.Heartbeat;

public final class Inbound {

    private final Guardian g;
    private final Attestation attestation;
    private final Notifier notifier;

    public Inbound(Guardian g, Attestation attestation, Notifier notifier) {
        this.g = g;
        this.attestation = attestation;
        this.notifier = notifier;
    }

    void handle(PlayerSession s, Frame f) {
        switch (f.type) {
            case MessageType.CONTEXT:
                s.context = Context.decode(f.payload);
                break;
            case MessageType.HEARTBEAT:
                heartbeat(s, Heartbeat.decode(f.payload));
                break;
            case MessageType.FINDING:
                Finding fd = Finding.decode(f.payload);
                g.cases.report(s.id, s.name, FindingKind.name(fd.kind), Source.CLIENT, fd.subject, fd.detail);
                break;
            case MessageType.CHUNK:
                chunk(s, Chunk.decode(f.payload));
                break;
            case MessageType.ABORT:
                abort(s, Abort.decode(f.payload));
                break;
            default:
                break;
        }
    }

    private void heartbeat(PlayerSession s, Heartbeat h) {
        if (s.silent) attestation.grant(s);
        s.lastHeartbeat = h;
        s.lastHeartbeatMillis = System.currentTimeMillis();
        int attacks = s.attacks.drainHeartbeat();
        if (attacks > h.leftClicks + g.settings.attackWithoutClickTolerance) {
            g.cases.report(s.id, s.name, "ATTACK_WITHOUT_CLICK", Source.CROSS, "heartbeat",
                attacks + " ataques, " + h.leftClicks + " cliques");
        }
    }

    private void chunk(PlayerSession s, Chunk c) {
        if (!g.requests.expects(c.requestId, s.id, c.kind)) return;
        Assembler.Transfer t = s.assembler.accept(c, System.currentTimeMillis());
        if (t == null) return;
        Pending p = g.requests.take(t.requestId, s.id);
        if (p == null) return;
        if (t.kind == RequestKind.BLACK_BOX) {
            g.evidence.saveBlackBox(s.id, p.id, t.data, s.track.snapshot(), () -> notifier.done(p, "caixa-preta salva"));
        } else {
            g.evidence.saveCapture(s.id, p.id, t.data, () -> notifier.done(p, "captura salva (" + t.data.length / 1024 + " KB)"));
        }
        g.evidence.audit(p.actor, "received", p.playerName, p.ticket, "request " + p.id + " kind " + p.kind);
    }

    private void abort(PlayerSession s, Abort a) {
        Pending p = g.requests.take(a.requestId, s.id);
        if (p == null) return;
        g.evidence.audit(p.actor, "aborted", p.playerName, p.ticket, a.reason);
        notifier.done(p, "abortado pelo cliente: " + a.reason);
    }
}
