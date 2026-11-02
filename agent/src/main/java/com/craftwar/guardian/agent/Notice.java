package com.craftwar.guardian.agent;

public final class Notice {

    private static volatile String text;
    private static volatile long untilNanos;

    private Notice() {
    }

    public static void show(String message, long durationMillis) {
        untilNanos = System.nanoTime() + durationMillis * 1_000_000L;
        text = message;
    }

    static String current() {
        String t = text;
        if (t == null) return null;
        if (System.nanoTime() - untilNanos > 0) {
            text = null;
            return null;
        }
        return t;
    }
}
