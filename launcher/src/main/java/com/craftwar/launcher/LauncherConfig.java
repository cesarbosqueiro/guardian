package com.craftwar.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.Properties;

import com.craftwar.guardian.protocol.Keys;

public final class LauncherConfig {

    public final String name;
    public final String manifestUrl;
    public final PublicKey manifestKey;

    private LauncherConfig(String name, String manifestUrl, PublicKey manifestKey) {
        this.name = name;
        this.manifestUrl = manifestUrl;
        this.manifestKey = manifestKey;
    }

    public static LauncherConfig load() throws IOException {
        Properties p = new Properties();
        try (InputStream in = LauncherConfig.class.getResourceAsStream("/launcher.properties")) {
            if (in == null) throw new IOException("launcher.properties missing");
            p.load(in);
        }
        return new LauncherConfig(p.getProperty("name", "Launcher"), p.getProperty("manifestUrl"),
            Keys.publicFromBase64(p.getProperty("manifestKey", "")));
    }
}
