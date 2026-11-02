package com.craftwar.launcher.manifest;

import java.io.File;
import java.io.IOException;

public final class Paths {

    private Paths() {
    }

    public static void requireSafe(String relative) throws IOException {
        if (relative == null || relative.isEmpty() || relative.startsWith("/") || relative.startsWith("\\")
            || relative.contains("..") || relative.contains(":") || relative.contains("\\")) {
            throw new IOException("unsafe path in manifest: " + relative);
        }
    }

    public static File resolve(File root, String relative) throws IOException {
        requireSafe(relative);
        File f = new File(root, relative);
        if (!f.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)) {
            throw new IOException("path escapes root: " + relative);
        }
        return f;
    }
}
