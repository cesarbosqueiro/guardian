package com.craftwar.guardian.agent;

public final class GameState {

    private static volatile float sensitivity = 0.5f;
    private static volatile boolean smoothCamera;
    private static volatile float gamma = 1f;
    private static volatile int fps;
    private static volatile int flags;

    private GameState() {
    }

    static void update(float sens, boolean smooth, float gammaSetting, int framesPerSecond, int stateFlags) {
        sensitivity = sens;
        smoothCamera = smooth;
        gamma = gammaSetting;
        fps = framesPerSecond;
        flags = stateFlags;
    }

    public static float sensitivity() {
        return sensitivity;
    }

    public static boolean smoothCamera() {
        return smoothCamera;
    }

    public static float gamma() {
        return gamma;
    }

    public static int fps() {
        return fps;
    }

    public static int flags() {
        return flags;
    }
}
