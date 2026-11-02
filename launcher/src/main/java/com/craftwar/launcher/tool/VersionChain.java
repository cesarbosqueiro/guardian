package com.craftwar.launcher.tool;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

final class VersionChain {

    private static final int MAX_DEPTH = 8;

    final List<JsonObject> childFirst;

    private VersionChain(List<JsonObject> childFirst) {
        this.childFirst = childFirst;
    }

    static VersionChain load(File versionsDir, String id) throws IOException {
        List<JsonObject> chain = new ArrayList<JsonObject>();
        for (String current = id; current != null; ) {
            if (chain.size() == MAX_DEPTH) throw new IOException("inheritsFrom chain too deep");
            JsonObject json = read(new File(versionsDir, current + "/" + current + ".json"));
            chain.add(json);
            current = string(json, "inheritsFrom");
        }
        return new VersionChain(chain);
    }

    String first(String key) {
        for (JsonObject j : childFirst) {
            String v = string(j, key);
            if (v != null) return v;
        }
        return null;
    }

    JsonObject firstObject(String key) {
        for (JsonObject j : childFirst) {
            if (j.has(key) && j.get(key).isJsonObject()) return j.getAsJsonObject(key);
        }
        return null;
    }

    JsonObject root() {
        return childFirst.get(childFirst.size() - 1);
    }

    String jarId() {
        String jar = first("jar");
        return jar != null ? jar : string(root(), "id");
    }

    static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static JsonObject read(File f) throws IOException {
        if (!f.isFile()) throw new IOException("version json not found: " + f);
        try (Reader r = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }
}
