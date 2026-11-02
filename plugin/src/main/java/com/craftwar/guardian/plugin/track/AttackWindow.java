package com.craftwar.guardian.plugin.track;

public final class AttackWindow {

    private static final int MAX_TARGETS = 4;
    private static final int EVENTS = 8;

    private final int[] targets = new int[MAX_TARGETS];
    private final long[] multiTicks = new long[EVENTS];
    private long tick = -1;
    private int distinct;
    private int multiCount;
    private int thisTick;
    private int sinceHeartbeat;

    public int record(long serverTick, int targetId) {
        thisTick++;
        sinceHeartbeat++;
        if (serverTick != tick) {
            tick = serverTick;
            targets[0] = targetId;
            distinct = 1;
            return 1;
        }
        for (int i = 0; i < distinct; i++) {
            if (targets[i] == targetId) return distinct;
        }
        if (distinct < MAX_TARGETS) targets[distinct] = targetId;
        distinct++;
        if (distinct == 2) multiTicks[multiCount++ & (EVENTS - 1)] = serverTick;
        return distinct;
    }

    public int multiTargetTicksSince(long fromTick) {
        int n = 0;
        int stored = Math.min(multiCount, EVENTS);
        for (int i = 0; i < stored; i++) {
            if (multiTicks[i] >= fromTick) n++;
        }
        return n;
    }

    public int drainTick() {
        int n = thisTick;
        thisTick = 0;
        return n;
    }

    public int drainHeartbeat() {
        int n = sinceHeartbeat;
        sinceHeartbeat = 0;
        return n;
    }
}
