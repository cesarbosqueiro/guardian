package com.craftwar.client.hud;

import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ClicksPerSecond {

    private static final int SIZE = 64;
    private static final int MASK = SIZE - 1;
    private static final long WINDOW_NANOS = 1_000_000_000L;

    private final long[] left = new long[SIZE];
    private final long[] right = new long[SIZE];
    private int leftCount;
    private int rightCount;

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate) return;
        long now = System.nanoTime();
        if (e.button == 0) left[leftCount++ & MASK] = now;
        else if (e.button == 1) right[rightCount++ & MASK] = now;
    }

    public int left() {
        return count(left, leftCount);
    }

    public int right() {
        return count(right, rightCount);
    }

    private static int count(long[] ring, int total) {
        long oldest = System.nanoTime() - WINDOW_NANOS;
        int n = 0;
        for (int i = total - 1, seen = 0; i >= 0 && seen < SIZE && ring[i & MASK] - oldest > 0; i--, seen++) n++;
        return n;
    }
}
