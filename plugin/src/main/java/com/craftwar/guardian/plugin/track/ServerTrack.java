package com.craftwar.guardian.plugin.track;

public final class ServerTrack {

    static final int SIZE = 1 << 11;
    static final int MASK = SIZE - 1;
    public static final int WINDOW = 1200;

    private final long[] millis = new long[SIZE];
    private final float[] yaw = new float[SIZE];
    private final float[] pitch = new float[SIZE];
    private final int[] attacks = new int[SIZE];
    private long count;

    public void record(long nowMillis, float y, float p, int attackCount) {
        int i = (int) count & MASK;
        millis[i] = nowMillis;
        yaw[i] = y;
        pitch[i] = p;
        attacks[i] = attackCount;
        count++;
    }

    public Snapshot snapshot() {
        int n = (int) Math.min(count, WINDOW);
        Snapshot s = new Snapshot(n);
        long from = count - n;
        for (int k = 0; k < n; k++) {
            int i = (int) (from + k) & MASK;
            s.millis[k] = millis[i];
            s.yaw[k] = yaw[i];
            s.pitch[k] = pitch[i];
            s.attacks[k] = attacks[i];
        }
        return s;
    }

    public static final class Snapshot {
        public final long[] millis;
        public final float[] yaw;
        public final float[] pitch;
        public final int[] attacks;

        Snapshot(int n) {
            millis = new long[n];
            yaw = new float[n];
            pitch = new float[n];
            attacks = new int[n];
        }
    }
}
