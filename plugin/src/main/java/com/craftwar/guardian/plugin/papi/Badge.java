package com.craftwar.guardian.plugin.papi;

public final class Badge {

    public static final int NO_AGENT = -1;

    public final boolean attested;
    public final int agentState;
    public final long score;

    public Badge(boolean attested, int agentState, long score) {
        this.attested = attested;
        this.agentState = agentState;
        this.score = score;
    }
}
