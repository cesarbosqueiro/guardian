package com.craftwar.launcher.manifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import com.craftwar.guardian.protocol.Keys;
import com.craftwar.launcher.sync.Http;

public final class ManifestSource {

    private static final Gson GSON = new Gson();
    private static final String ID = "[A-Za-z0-9._\\-]{1,48}";

    private final String url;
    private final PublicKey key;

    public ManifestSource(String url, PublicKey key) {
        this.url = url;
        this.key = key;
    }

    public Manifest fetch() throws IOException {
        byte[] body = Http.get(url, 4 << 20);
        byte[] sig = Http.get(url + ".sig", 4096);
        if (!Signatures.verify(key, body, sig)) throw new IOException("manifest signature rejected");
        return parse(body);
    }

    public static Manifest parse(byte[] body) throws IOException {
        Manifest m;
        try {
            m = GSON.fromJson(new String(body, StandardCharsets.UTF_8), Manifest.class);
        } catch (JsonParseException e) {
            throw new IOException("manifest malformed", e);
        }
        if (m == null || m.version != Manifest.FORMAT) throw new IOException("manifest format not supported; update the launcher");
        validateProfiles(m);
        validateServers(m);
        return m;
    }

    private static void validateProfiles(Manifest m) throws IOException {
        if (m.profiles == null || m.profiles.isEmpty()) throw new IOException("manifest has no profiles");
        Set<String> ids = new HashSet<String>();
        for (Profile p : m.profiles) {
            if (p.id == null || !p.id.matches(ID) || !ids.add(p.id)) throw new IOException("invalid profile id: " + p.id);
            if (p.mainClass == null || p.files == null) throw new IOException("profile incomplete: " + p.id);
            for (ManifestFile f : p.files) Paths.requireSafe(f.path);
        }
    }

    private static void validateServers(Manifest m) throws IOException {
        if (m.servers == null || m.servers.isEmpty()) throw new IOException("manifest has no servers");
        Set<String> ids = new HashSet<String>();
        for (ServerEntry s : m.servers) {
            if (s.id == null || !s.id.matches(ID) || !ids.add(s.id)) throw new IOException("invalid server id: " + s.id);
            if (s.host == null || s.host.isEmpty() || s.port <= 0 || s.port > 65535) throw new IOException("invalid server address: " + s.id);
            if (s.publicKey == null || s.publicKey.isEmpty()) throw new IOException("server without public key: " + s.id);
            try {
                Keys.publicFromBase64(s.publicKey);
            } catch (IllegalArgumentException e) {
                throw new IOException("invalid public key for server " + s.id);
            }
        }
    }
}
