package com.craftwar.guardian.agent.record;

import com.craftwar.guardian.protocol.blackbox.BlackBox;

public final class BlackBoxExporter {

    private static final int WINDOW_TICKS = 1200;
    private static final long WINDOW_NANOS = 60_000_000_000L;
    private static final long FRAME_WINDOW_NANOS = 10_000_000_000L;
    private static final int RING_MARGIN = 256;

    private BlackBoxExporter() {
    }

    public static BlackBox export(Recorder r, float sensitivity, boolean smoothCamera) {
        long end = System.nanoTime();
        long tickEnd = r.ticks.get();
        long frameEnd = r.frames.get();
        long clickEnd = r.clicks.get();
        int ticks = (int) Math.min(tickEnd, WINDOW_TICKS);
        int frames = countBack(r.frameNanos, Recorder.FRAME_MASK, frameEnd, Recorder.FRAMES - RING_MARGIN, end - FRAME_WINDOW_NANOS);
        int clicks = countBack(r.clickNanos, Recorder.CLICK_MASK, clickEnd, Recorder.CLICKS - RING_MARGIN, end - WINDOW_NANOS);
        BlackBox b = new BlackBox(ticks, clicks, frames);
        b.sensitivity = sensitivity;
        b.smoothCamera = smoothCamera;
        b.endWallMillis = System.currentTimeMillis();
        b.tickCount = ticks;
        b.clickCount = clicks;
        b.frameCount = frames;
        copyTicks(r, b, tickEnd - ticks, end);
        copyClicks(r, b, clickEnd - clicks, end);
        copyFrames(r, b, frameEnd - frames, end);
        return b;
    }

    private static int countBack(long[] nanos, int mask, long endSeq, int limit, long oldest) {
        int n = 0;
        for (long seq = endSeq - 1; seq >= 0 && n < limit && nanos[(int) seq & mask] - oldest >= 0; seq--) n++;
        return n;
    }

    private static void copyTicks(Recorder r, BlackBox b, long from, long end) {
        for (int k = 0; k < b.tickCount; k++) {
            int i = (int) (from + k) & Recorder.TICK_MASK;
            b.tickMicrosBeforeEnd[k] = micros(end - r.tickNanos[i]);
            b.tickYaw[k] = r.tickYaw[i];
            b.tickPitch[k] = r.tickPitch[i];
            b.tickDx[k] = r.tickDx[i];
            b.tickDy[k] = r.tickDy[i];
            b.tickFrames[k] = r.tickFrames[i];
            b.tickFlags[k] = r.tickFlags[i];
        }
    }

    private static void copyClicks(Recorder r, BlackBox b, long from, long end) {
        for (int k = 0; k < b.clickCount; k++) {
            int i = (int) (from + k) & Recorder.CLICK_MASK;
            b.clickMicrosBeforeEnd[k] = micros(end - r.clickNanos[i]);
            b.clickCode[k] = r.clickCode[i];
        }
    }

    private static void copyFrames(Recorder r, BlackBox b, long from, long end) {
        for (int k = 0; k < b.frameCount; k++) {
            int i = (int) (from + k) & Recorder.FRAME_MASK;
            b.frameMicrosBeforeEnd[k] = micros(end - r.frameNanos[i]);
            b.frameDelta[k] = r.frameDelta[i];
        }
    }

    private static int micros(long nanos) {
        long us = nanos / 1000;
        return us < 0 ? 0 : us > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) us;
    }
}
