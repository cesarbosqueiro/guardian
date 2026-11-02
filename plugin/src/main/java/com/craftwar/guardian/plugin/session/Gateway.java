package com.craftwar.guardian.plugin.session;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.craftwar.guardian.plugin.Guardian;
import com.craftwar.guardian.plugin.cases.Source;
import com.craftwar.guardian.protocol.Frame;
import com.craftwar.guardian.protocol.MessageType;
import com.craftwar.guardian.protocol.ProtocolException;
import com.craftwar.guardian.protocol.SecureChannel;
import com.craftwar.guardian.protocol.ServerHandshake;
import com.craftwar.guardian.protocol.Wire;
import com.craftwar.guardian.protocol.message.Policy;

public final class Gateway implements PluginMessageListener {

    private final Guardian g;
    private final Attestation attestation;
    private Inbound inbound;

    public Gateway(Guardian g, Attestation attestation) {
        this.g = g;
        this.attestation = attestation;
    }

    public void inbound(Inbound handler) {
        inbound = handler;
    }

    public boolean send(PlayerSession s, int type, byte[] payload) {
        SecureChannel c = s.channel;
        if (c == null || !s.player.isOnline()) return false;
        s.player.sendPluginMessage(g.plugin, Wire.CHANNEL, c.seal(type, payload));
        return true;
    }

    void startHandshake(PlayerSession s) {
        if (s.handshake != null) return;
        g.async(() -> {
            ServerHandshake hs = new ServerHandshake(g.identity.privateKey());
            byte[] hello = hs.hello();
            g.sync(() -> {
                if (g.sessions.get(s.id) != s || s.handshake != null || !s.player.isOnline()) return;
                s.handshake = hs;
                s.player.sendPluginMessage(g.plugin, Wire.CHANNEL, hello);
            });
        });
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        PlayerSession s = g.sessions.get(player);
        if (s == null || !Wire.CHANNEL.equals(channel)) return;
        try {
            if (Frame.typeOf(message) == MessageType.REPLY) accept(s, message);
            else if (s.channel != null) inbound.handle(s, s.channel.open(message));
        } catch (ProtocolException e) {
            violation(s, e.getMessage());
        }
    }

    private void accept(PlayerSession s, byte[] reply) {
        ServerHandshake hs = s.handshake;
        if (hs == null || s.channel != null) return;
        g.async(() -> {
            try {
                SecureChannel c = hs.accept(reply);
                g.sync(() -> established(s, hs, c));
            } catch (ProtocolException e) {
                g.sync(() -> violation(s, "handshake: " + e.getMessage()));
            }
        });
    }

    private void established(PlayerSession s, ServerHandshake hs, SecureChannel c) {
        if (g.sessions.get(s.id) != s || s.handshake != hs || s.channel != null) return;
        s.channel = c;
        attestation.grant(s);
        send(s, MessageType.POLICY, new Policy(g.settings.heartbeatSeconds, g.settings.inventorySeconds,
            g.settings.ignoredClassPrefixes).encode());
    }

    private void violation(PlayerSession s, String detail) {
        g.cases.report(s.id, s.name, "PROTOCOL_VIOLATION", Source.CLIENT, "channel", detail);
    }
}
