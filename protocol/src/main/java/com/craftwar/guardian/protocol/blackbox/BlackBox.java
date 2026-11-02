package com.craftwar.guardian.protocol.blackbox;

public final class BlackBox {

    public float sensitivity;
    public boolean smoothCamera;
    public long endWallMillis;

    public int tickCount;
    public int[] tickMicrosBeforeEnd;
    public float[] tickYaw;
    public float[] tickPitch;
    public int[] tickDx;
    public int[] tickDy;
    public int[] tickFrames;
    public int[] tickFlags;

    public int clickCount;
    public int[] clickMicrosBeforeEnd;
    public int[] clickCode;

    public int frameCount;
    public int[] frameMicrosBeforeEnd;
    public int[] frameDelta;

    public BlackBox(int ticks, int clicks, int frames) {
        tickMicrosBeforeEnd = new int[ticks];
        tickYaw = new float[ticks];
        tickPitch = new float[ticks];
        tickDx = new int[ticks];
        tickDy = new int[ticks];
        tickFrames = new int[ticks];
        tickFlags = new int[ticks];
        clickMicrosBeforeEnd = new int[clicks];
        clickCode = new int[clicks];
        frameMicrosBeforeEnd = new int[frames];
        frameDelta = new int[frames];
    }

    public static int packDelta(int dx, int dy) {
        return dx << 16 | dy & 0xFFFF;
    }

    public static int deltaX(int packed) {
        return packed >> 16;
    }

    public static int deltaY(int packed) {
        return (short) packed;
    }

    public static int clickCode(int button, boolean down, boolean gui) {
        return button << 2 | (gui ? 2 : 0) | (down ? 1 : 0);
    }

    public static int clickButton(int code) {
        return code >>> 2;
    }

    public static boolean clickDown(int code) {
        return (code & 1) != 0;
    }

    public static boolean clickInGui(int code) {
        return (code & 2) != 0;
    }
}
