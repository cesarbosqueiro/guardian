package com.craftwar.guardian.agent.record;

import com.craftwar.guardian.protocol.blackbox.BlackBox;

public final class RecorderProbe {

    private static final Recorder R = Recorder.INSTANCE;

    private RecorderProbe() {
    }

    public static long frames() {
        return R.frames.get();
    }

    public static long ticks() {
        return R.ticks.get();
    }

    public static int lastFrameDx() {
        return BlackBox.deltaX(R.frameDelta[(int) (R.frames.get() - 1) & Recorder.FRAME_MASK]);
    }

    public static int lastFrameDy() {
        return BlackBox.deltaY(R.frameDelta[(int) (R.frames.get() - 1) & Recorder.FRAME_MASK]);
    }

    public static float lastYaw() {
        return R.tickYaw[(int) (R.ticks.get() - 1) & Recorder.TICK_MASK];
    }

    public static float lastPitch() {
        return R.tickPitch[(int) (R.ticks.get() - 1) & Recorder.TICK_MASK];
    }
}
