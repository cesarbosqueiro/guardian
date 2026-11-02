package com.craftwar.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import com.craftwar.launcher.sync.Files2;

public final class UserSettings {

    private final File file;
    private final Properties p = new Properties();

    public UserSettings(File file) {
        this.file = file;
        if (file.isFile()) {
            try (InputStream in = new FileInputStream(file)) {
                p.load(in);
            } catch (IOException ignored) {
                p.clear();
            }
        }
    }

    public String nick() {
        return p.getProperty("nick", "");
    }

    public int memoryMbOr(int fallback) {
        try {
            return Integer.parseInt(p.getProperty("memoryMb", Integer.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean consented(int version, String textHash) {
        return Integer.toString(version).equals(p.getProperty("consent.version")) && textHash.equals(p.getProperty("consent.hash"));
    }

    public void nick(String nick) {
        p.setProperty("nick", nick);
    }

    public void memoryMb(int mb) {
        p.setProperty("memoryMb", Integer.toString(mb));
    }

    public void consent(int version, String textHash, String isoTime) {
        p.setProperty("consent.version", Integer.toString(version));
        p.setProperty("consent.hash", textHash);
        p.setProperty("consent.at", isoTime);
    }

    public void save() throws IOException {
        Files2.mkdirs(file.getParentFile());
        try (OutputStream out = new FileOutputStream(file)) {
            p.store(out, null);
        }
    }
}
