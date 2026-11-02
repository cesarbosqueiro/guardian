package com.craftwar.launcher.sync;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class Natives {

    public static final String DIR = "natives";

    private Natives() {
    }

    static void extract(File gameDir, List<File> jars) throws IOException {
        File out = new File(gameDir, DIR);
        Files2.mkdirs(out);
        for (File jar : jars) {
            try (ZipFile zip = new ZipFile(jar)) {
                for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements(); ) {
                    ZipEntry e = it.nextElement();
                    if (e.isDirectory() || e.getName().startsWith("META-INF/") || e.getName().contains("..")) continue;
                    File target = new File(out, new File(e.getName()).getName());
                    if (target.isFile() && target.length() == e.getSize()) continue;
                    try (InputStream in = zip.getInputStream(e)) {
                        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }
}
