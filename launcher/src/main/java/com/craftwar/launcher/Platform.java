package com.craftwar.launcher;

import java.io.File;
import java.util.Locale;

public final class Platform {

    private Platform() {
    }

    public static String os() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (name.contains("win")) return "windows";
        if (name.contains("mac")) return "osx";
        return "linux";
    }

    public static File launcherDir() {
        try {
            File jar = new File(Platform.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return jar.isFile() ? jar.getParentFile() : jar;
        } catch (Exception e) {
            return new File(System.getProperty("user.dir"));
        }
    }

    public static long physicalMemoryMb() {
        try {
            java.lang.management.OperatingSystemMXBean os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) os).getTotalPhysicalMemorySize() >> 20;
            }
        } catch (Throwable ignored) {
        }
        return 4096;
    }
}
