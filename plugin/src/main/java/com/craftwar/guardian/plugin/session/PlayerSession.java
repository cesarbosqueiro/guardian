package com.craftwar.guardian.plugin.session;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import com.craftwar.guardian.plugin.track.AttackWindow;
import com.craftwar.guardian.plugin.track.ServerTrack;
import com.craftwar.guardian.protocol.Assembler;
import com.craftwar.guardian.protocol.SecureChannel;
import com.craftwar.guardian.protocol.ServerHandshake;
import com.craftwar.guardian.protocol.message.Context;
import com.craftwar.guardian.protocol.message.Heartbeat;

public final class PlayerSession {

    public final UUID id;
    public final String name;
    public final Player player;
    public final long joinedMillis;
    public final ServerTrack track = new ServerTrack();
    public final AttackWindow attacks = new AttackWindow();
    public final Assembler assembler = new Assembler(4 << 20, 60_000L);

    ServerHandshake handshake;
    SecureChannel channel;
    PermissionAttachment attachment;
    volatile boolean attested;
    boolean silent;
    boolean warned;
    long lastHeartbeatMillis;
    Context context;
    volatile Heartbeat lastHeartbeat;
    public long lastAutoBoxMillis;

    PlayerSession(Player player, long joinedMillis) {
        this.id = player.getUniqueId();
        this.name = player.getName();
        this.player = player;
        this.joinedMillis = joinedMillis;
    }

    public boolean attested() {
        return attested;
    }

    public boolean handshakeStarted() {
        return handshake != null;
    }

    public Context context() {
        return context;
    }

    public Heartbeat lastHeartbeat() {
        return lastHeartbeat;
    }

    public long lastHeartbeatMillis() {
        return lastHeartbeatMillis;
    }
}
