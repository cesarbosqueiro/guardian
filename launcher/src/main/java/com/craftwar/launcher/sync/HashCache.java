package com.craftwar.launcher.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public final class HashCache {

    private final File file;
    private final Properties entries = new Properties();

    public HashCache(File file) {
        this.file = file;
        if (file.isFile()) {
            try (InputStream in = new FileInputStream(file)) {
                entries.load(in);
            } catch (IOException ignored) {
                entries.clear();
            }
        }
    }

    public synchronized String sha1(File target, String key) throws IOException {
        String stamp = target.length() + ":" + target.lastModified();
        String cached = entries.getProperty(key);
        if (cached != null && cached.startsWith(stamp + "=")) return cached.substring(stamp.length() + 1);
        String hash = Hashing.sha1(target);
        entries.setProperty(key, stamp + "=" + hash);
        return hash;
    }

    public synchronized void save() throws IOException {
        Files2.mkdirs(file.getParentFile());
        File tmp = new File(file.getPath() + ".tmp");
        try (OutputStream out = new FileOutputStream(tmp)) {
            entries.store(out, null);
        }
        Files2.replace(tmp, file);
    }
}
