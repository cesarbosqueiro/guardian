package com.craftwar.guardian.plugin.request;

import com.craftwar.guardian.plugin.Guardian;
import com.craftwar.guardian.plugin.session.Gateway;
import com.craftwar.guardian.plugin.session.PlayerSession;
import com.craftwar.guardian.protocol.MessageType;
import com.craftwar.guardian.protocol.message.Request;

public final class Requester {

    private final Guardian g;
    private final Gateway gateway;

    public Requester(Guardian g, Gateway gateway) {
        this.g = g;
        this.gateway = gateway;
    }

    public String request(PlayerSession s, int kind, String actor, String ticket) {
        if (!s.attested()) return "jogador sem agente atestado";
        Pending p = g.requests.create(s.id, s.name, kind, actor, ticket);
        if (!gateway.send(s, MessageType.REQUEST, new Request(p.id, kind, "").encode())) {
            g.requests.take(p.id, s.id);
            return "canal indisponivel";
        }
        g.evidence.audit(actor, "request", s.name, ticket, "request " + p.id + " kind " + kind);
        return null;
    }
}
