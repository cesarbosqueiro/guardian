package com.craftwar.launcher;

public final class Selection {

    public final String nick;
    public final int memoryMb;
    public final String profileId;
    public final String serverId;
    public final boolean forceUpdate;

    public Selection(String nick, int memoryMb, String profileId, String serverId, boolean forceUpdate) {
        this.nick = nick;
        this.memoryMb = memoryMb;
        this.profileId = profileId;
        this.serverId = serverId;
        this.forceUpdate = forceUpdate;
    }
}
