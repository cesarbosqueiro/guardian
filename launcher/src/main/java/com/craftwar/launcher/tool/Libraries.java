package com.craftwar.launcher.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.craftwar.launcher.manifest.ManifestFile;

final class Libraries {

    private final File instance;
    private final Staging staging;

    Libraries(File instance, Staging staging) {
        this.instance = instance;
        this.staging = staging;
    }

    List<ManifestFile> resolve(VersionChain chain) throws IOException {
        Map<String, JsonObject> merged = new LinkedHashMap<String, JsonObject>();
        for (JsonObject version : chain.childFirst) {
            if (!version.has("libraries")) continue;
            for (JsonElement e : version.getAsJsonArray("libraries")) {
                JsonObject lib = e.getAsJsonObject();
                String[] c = VersionChain.string(lib, "name").split(":");
                String key = c[0] + ":" + c[1] + (lib.has("natives") ? ":natives" : "");
                if (!merged.containsKey(key) && clientSide(lib)) merged.put(key, lib);
            }
        }
        List<ManifestFile> out = new ArrayList<ManifestFile>();
        for (JsonObject lib : merged.values()) {
            if (lib.has("natives")) natives(lib, out);
            else artifact(lib, out);
        }
        return out;
    }

    private static boolean clientSide(JsonObject lib) {
        return !lib.has("clientreq") || lib.get("clientreq").getAsBoolean();
    }

    private void artifact(JsonObject lib, List<ManifestFile> out) throws IOException {
        List<String> allowed = new ArrayList<String>();
        for (String os : Rules.OPERATING_SYSTEMS) if (Rules.allows(lib, os)) allowed.add(os);
        if (allowed.isEmpty()) return;
        JsonObject meta = downloads(lib, "artifact");
        String path = meta != null ? VersionChain.string(meta, "path") : Maven.path(VersionChain.string(lib, "name"), null);
        if (allowed.size() == Rules.OPERATING_SYSTEMS.length) out.add(entry(path, meta, ManifestFile.LIBRARY, null));
        else for (String os : allowed) out.add(entry(path, meta, ManifestFile.LIBRARY, os));
    }

    private void natives(JsonObject lib, List<ManifestFile> out) throws IOException {
        JsonObject map = lib.getAsJsonObject("natives");
        for (String os : Rules.OPERATING_SYSTEMS) {
            if (!map.has(os) || !Rules.allows(lib, os)) continue;
            String classifier = map.get(os).getAsString().replace("${arch}", "64");
            JsonObject meta = classifier(lib, classifier);
            String path = meta != null ? VersionChain.string(meta, "path") : Maven.path(VersionChain.string(lib, "name"), classifier);
            ManifestFile f = entry(path, meta, ManifestFile.NATIVES, os);
            if (f != null) out.add(f);
        }
    }

    private ManifestFile entry(String path, JsonObject meta, String kind, String os) throws IOException {
        String rel = "libraries/" + path;
        if (meta != null && meta.has("url") && meta.has("sha1")) {
            return new ManifestFile(rel, VersionChain.string(meta, "url"), VersionChain.string(meta, "sha1"), meta.get("size").getAsLong(), kind, os);
        }
        File local = new File(instance, rel);
        if (!local.isFile()) {
            if (ManifestFile.NATIVES.equals(kind)) return null;
            throw new IOException("library not found locally and has no download metadata: " + rel);
        }
        return staging.stage(local, rel, kind, os);
    }

    private static JsonObject downloads(JsonObject lib, String key) {
        if (!lib.has("downloads")) return null;
        JsonObject d = lib.getAsJsonObject("downloads");
        return d.has(key) ? d.getAsJsonObject(key) : null;
    }

    private static JsonObject classifier(JsonObject lib, String classifier) {
        JsonObject classifiers = downloads(lib, "classifiers");
        return classifiers != null && classifiers.has(classifier) ? classifiers.getAsJsonObject(classifier) : null;
    }
}
