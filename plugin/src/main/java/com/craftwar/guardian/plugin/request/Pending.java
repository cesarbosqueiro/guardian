package com.craftwar.guardian.plugin.request;

import java.util.UUID;

public final class Pending {

    public final int id;
    public final UUID player;
    public final String playerName;
    public final int kind;
    public final String actor;
    public final String ticket;
    public final long createdMillis;

    Pending(int id, UUID player, String playerName, int kind, String actor, String ticket, long createdMillis) {
        this.id = id;
        this.player = player;
        this.playerName = playerName;
        this.kind = kind;
        this.actor = actor;
        this.ticket = ticket;
        this.createdMillis = createdMillis;
    }
}
