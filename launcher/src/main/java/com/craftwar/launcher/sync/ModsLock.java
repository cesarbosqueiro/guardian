package com.craftwar.launcher.sync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ModsLock {

    private static final String MODS = "mods";
    private static final String QUARANTINE = "mods-disabled";

    private ModsLock() {
    }

    static List<String> enforce(File gameDir, List<String> allowedPaths) throws IOException {
        Set<String> allowed = new HashSet<String>(allowedPaths);
        List<String> moved = new ArrayList<String>();
        File mods = new File(gameDir, MODS);
        if (!mods.isDirectory()) return moved;
        sweep(gameDir, mods, allowed, moved);
        return moved;
    }

    private static void sweep(File gameDir, File dir, Set<String> allowed, List<String> moved) throws IOException {
        File[] entries = dir.listFiles();
        if (entries == null) return;
        for (File f : entries) {
            if (f.isDirectory()) {
                sweep(gameDir, f, allowed, moved);
                continue;
            }
            String rel = gameDir.toPath().relativize(f.toPath()).toString().replace('\\', '/');
            if (allowed.contains(rel)) continue;
            File target = new File(new File(gameDir, QUARANTINE), rel.substring(MODS.length() + 1));
            Files2.mkdirs(target.getParentFile());
            Files2.replace(f, target);
            moved.add(rel);
        }
    }
}
