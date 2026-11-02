package com.craftwar.launcher.sync;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.craftwar.launcher.manifest.Manifest;
import com.craftwar.launcher.manifest.Paths;

public final class Assets {

    private static final String OBJECTS_URL = "https://resources.download.minecraft.net/";
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern INDEX_ID = Pattern.compile("[A-Za-z0-9._\\-]+");

    private final File assetsDir;
    private final File profileDir;
    private final Manifest.AssetIndex index;
    private final List<Asset> entries = new ArrayList<Asset>();
    private boolean virtual;
    private boolean mapToResources;

    public Assets(File assetsDir, File profileDir, Manifest.AssetIndex index) {
        this.assetsDir = assetsDir;
        this.profileDir = profileDir;
        this.index = index;
    }

    public static File virtualDir(File assetsDir, String indexId) {
        return new File(assetsDir, "virtual/" + indexId);
    }

    List<Callable<Void>> missingDownloads() throws IOException {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
        if (index == null) return tasks;
        if (!INDEX_ID.matcher(index.id).matches()) throw new IOException("invalid asset index id");
        File indexFile = new File(assetsDir, "indexes/" + index.id + ".json");
        if (!indexFile.isFile() || !Hashing.sha1(indexFile).equalsIgnoreCase(index.sha1)) {
            Http.download(index.url, indexFile, index.sha1, index.size);
        }
        load(indexFile);
        for (Asset a : entries) {
            if (a.object.isFile() && a.object.length() == a.size) continue;
            tasks.add(() -> {
                Http.download(OBJECTS_URL + a.hash.substring(0, 2) + "/" + a.hash, a.object, a.hash, a.size);
                return null;
            });
        }
        return tasks;
    }

    void materialize() throws IOException {
        if (!virtual && !mapToResources) return;
        File virtualRoot = virtualDir(assetsDir, index.id);
        File resources = new File(profileDir, "resources");
        for (Asset a : entries) {
            copyIfDifferent(a, Paths.resolve(virtualRoot, a.name));
            if (mapToResources) copyIfDifferent(a, Paths.resolve(resources, a.name));
        }
    }

    private static void copyIfDifferent(Asset a, File target) throws IOException {
        if (target.isFile() && target.length() == a.size) return;
        Files2.mkdirs(target.getParentFile());
        Files.copy(a.object.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void load(File indexFile) throws IOException {
        JsonObject root;
        try (Reader r = Files.newBufferedReader(indexFile.toPath(), StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(r).getAsJsonObject();
        }
        virtual = flag(root, "virtual");
        mapToResources = flag(root, "map_to_resources");
        entries.clear();
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("objects").entrySet()) {
            JsonObject o = e.getValue().getAsJsonObject();
            String hash = o.get("hash").getAsString();
            if (!HASH.matcher(hash).matches()) throw new IOException("invalid asset hash");
            File object = new File(assetsDir, "objects/" + hash.substring(0, 2) + "/" + hash);
            entries.add(new Asset(e.getKey(), hash, o.get("size").getAsLong(), object));
        }
    }

    private static boolean flag(JsonObject root, String key) {
        return root.has(key) && root.get(key).getAsBoolean();
    }

    private static final class Asset {
        final String name;
        final String hash;
        final long size;
        final File object;

        Asset(String name, String hash, long size, File object) {
            this.name = name;
            this.hash = hash;
            this.size = size;
            this.object = object;
        }
    }
}
