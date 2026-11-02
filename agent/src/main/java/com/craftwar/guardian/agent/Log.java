package com.craftwar.guardian.agent;

public final class Log {

    private static final String PREFIX = "[Guardian] ";

    private Log() {
    }

    public static void info(String message) {
        System.out.println(PREFIX + message);
    }

    public static void warn(String message) {
        System.err.println(PREFIX + message);
    }
}
