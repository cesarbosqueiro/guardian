package com.craftwar.launcher.manifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import com.craftwar.launcher.sync.Http;

public final class ManifestSource {

    private static final Gson GSON = new Gson();

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
        try {
            Manifest m = GSON.fromJson(new String(body, StandardCharsets.UTF_8), Manifest.class);
            if (m == null || m.mainClass == null || m.files == null) throw new IOException("manifest incomplete");
            for (ManifestFile f : m.files) Paths.requireSafe(f.path);
            return m;
        } catch (JsonParseException e) {
            throw new IOException("manifest malformed", e);
        }
    }
}
