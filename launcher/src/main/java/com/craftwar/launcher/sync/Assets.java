package com.craftwar.launcher.sync;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.craftwar.launcher.manifest.Manifest;

final class Assets {

    private static final String OBJECTS_URL = "https://resources.download.minecraft.net/";
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{40}");

    private final File assetsDir;
    private final Manifest.AssetIndex index;

    Assets(File gameDir, Manifest.AssetIndex index) {
        this.assetsDir = new File(gameDir, "assets");
        this.index = index;
    }

    List<Callable<Void>> missingDownloads() throws IOException {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
        if (index == null) return tasks;
        if (!index.id.matches("[A-Za-z0-9._\\-]+")) throw new IOException("invalid asset index id");
        File indexFile = new File(assetsDir, "indexes/" + index.id + ".json");
        if (!indexFile.isFile() || !Hashing.sha1(indexFile).equalsIgnoreCase(index.sha1)) {
            Http.download(index.url, indexFile, index.sha1, index.size);
        }
        for (Map.Entry<String, JsonElement> e : objects(indexFile)) {
            JsonObject o = e.getValue().getAsJsonObject();
            String hash = o.get("hash").getAsString();
            long size = o.get("size").getAsLong();
            if (!HASH.matcher(hash).matches()) throw new IOException("invalid asset hash");
            String rel = hash.substring(0, 2) + "/" + hash;
            File target = new File(assetsDir, "objects/" + rel);
            if (target.isFile() && target.length() == size) continue;
            tasks.add(() -> {
                Http.download(OBJECTS_URL + rel, target, hash, size);
                return null;
            });
        }
        return tasks;
    }

    private static Iterable<Map.Entry<String, JsonElement>> objects(File indexFile) throws IOException {
        try (Reader r = new InputStreamReader(Files.newInputStream(indexFile.toPath()), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("objects").entrySet();
        }
    }
}
