package com.craftwar.guardian.agent.record;

import java.util.concurrent.atomic.AtomicLong;

import com.craftwar.guardian.protocol.blackbox.BlackBox;

public final class Recorder {

    public static final Recorder INSTANCE = new Recorder();

    static final int FRAMES = 1 << 12;
    static final int FRAME_MASK = FRAMES - 1;
    static final int TICKS = 1 << 11;
    static final int TICK_MASK = TICKS - 1;
    static final int CLICKS = 1 << 10;
    static final int CLICK_MASK = CLICKS - 1;
    private static final int MAX_FRAMES_PER_TICK = 0xFFFF;

    final long[] frameNanos = new long[FRAMES];
    final int[] frameDelta = new int[FRAMES];
    final AtomicLong frames = new AtomicLong();

    final long[] tickNanos = new long[TICKS];
    final float[] tickYaw = new float[TICKS];
    final float[] tickPitch = new float[TICKS];
    final int[] tickDx = new int[TICKS];
    final int[] tickDy = new int[TICKS];
    final int[] tickFrames = new int[TICKS];
    final int[] tickFlags = new int[TICKS];
    final AtomicLong ticks = new AtomicLong();

    final long[] clickNanos = new long[CLICKS];
    final int[] clickCode = new int[CLICKS];
    final AtomicLong clicks = new AtomicLong();

    private long frameSeq;
    private long tickSeq;
    private long clickSeq;
    private int sumDx;
    private int sumDy;
    private int framesInTick;
    private long clockOffset = Long.MAX_VALUE;

    public void frame(int dx, int dy, long now) {
        int i = (int) frameSeq & FRAME_MASK;
        frameNanos[i] = now;
        frameDelta[i] = BlackBox.packDelta(dx, dy);
        frames.lazySet(++frameSeq);
        sumDx += dx;
        sumDy += dy;
        framesInTick++;
    }

    public void tick(float yaw, float pitch, int flags, long now) {
        int i = (int) tickSeq & TICK_MASK;
        tickNanos[i] = now;
        tickYaw[i] = yaw;
        tickPitch[i] = pitch;
        tickDx[i] = sumDx;
        tickDy[i] = sumDy;
        tickFrames[i] = framesInTick < MAX_FRAMES_PER_TICK ? framesInTick : MAX_FRAMES_PER_TICK;
        tickFlags[i] = flags;
        ticks.lazySet(++tickSeq);
        sumDx = 0;
        sumDy = 0;
        framesInTick = 0;
    }

    public void click(int button, boolean down, boolean gui, long eventNanos, long now) {
        long offset = now - eventNanos;
        if (offset < clockOffset) clockOffset = offset;
        int i = (int) clickSeq & CLICK_MASK;
        clickNanos[i] = eventNanos + clockOffset;
        clickCode[i] = BlackBox.clickCode(button, down, gui);
        clicks.lazySet(++clickSeq);
    }
}
