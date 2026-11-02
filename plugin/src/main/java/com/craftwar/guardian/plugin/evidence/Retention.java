package com.craftwar.guardian.plugin.evidence;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class Retention {

    private final long captureMillis;
    private final long blackBoxMillis;
    private final long heldMillis;

    public Retention(long captureMillis, long blackBoxMillis, long heldMillis) {
        this.captureMillis = captureMillis;
        this.blackBoxMillis = blackBoxMillis;
        this.heldMillis = heldMillis;
    }

    void purge(File root) throws IOException {
        File[] players = root.listFiles(File::isDirectory);
        if (players == null) return;
        long now = System.currentTimeMillis();
        for (File player : players) purgePlayer(player, now);
    }

    private void purgePlayer(File dir, long now) throws IOException {
        File hold = new File(dir, EvidenceStore.HOLD);
        boolean held = hold.isFile() && now - hold.lastModified() < heldMillis;
        File[] entries = dir.listFiles();
        if (entries == null) return;
        for (File f : entries) {
            String name = f.getName();
            long age = now - f.lastModified();
            if (name.endsWith("-capture.jpg") && age > (held ? heldMillis : captureMillis)) Files.deleteIfExists(f.toPath());
            else if (name.endsWith("-box") && f.isDirectory() && age > (held ? heldMillis : blackBoxMillis)) delete(f.toPath());
        }
    }

    private static void delete(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
