package com.craftwar.launcher.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonObject;

import com.craftwar.launcher.manifest.Manifest;
import com.craftwar.launcher.manifest.ManifestFile;

final class ManifestBuilder {

    private static final String AGENT_PATH = "guardian/guardian-agent.jar";

    private final File instance;
    private final Template template;
    private final Staging staging;

    ManifestBuilder(File instance, Template template, Staging staging) {
        this.instance = instance;
        this.template = template;
        this.staging = staging;
    }

    Manifest build() throws IOException {
        VersionChain chain = VersionChain.load(new File(instance, "versions"), template.versionId);
        Manifest m = new Manifest();
        m.version = template.version;
        m.name = template.name;
        m.server = template.server;
        m.consent = template.consent;
        m.java = template.java;
        m.agentMapping = template.agentMapping;
        m.mainClass = chain.first("mainClass");
        m.gameArguments = new ArrayList<String>(Arrays.asList(chain.first("minecraftArguments").trim().split("\\s+")));
        m.assetIndex = assetIndex(chain.root().getAsJsonObject("assetIndex"));
        m.files.addAll(new Libraries(instance, template, staging).resolve(chain));
        m.files.add(client(chain));
        m.files.addAll(tree("mods", ManifestFile.MOD, ""));
        m.files.addAll(tree("config-defaults", ManifestFile.CONFIG, null));
        m.files.add(staging.stage(new File(template.agentJar), AGENT_PATH, template.cdn(AGENT_PATH), ManifestFile.AGENT, null));
        return m;
    }

    private ManifestFile client(VersionChain chain) throws IOException {
        String id = chain.jarId();
        String rel = "versions/" + id + "/" + id + ".jar";
        JsonObject downloads = chain.root().getAsJsonObject("downloads");
        if (downloads != null && downloads.has("client")) {
            JsonObject c = downloads.getAsJsonObject("client");
            return new ManifestFile(rel, VersionChain.string(c, "url"), VersionChain.string(c, "sha1"), c.get("size").getAsLong(), ManifestFile.GAME, null);
        }
        throw new IOException("vanilla version json has no client download; refusing to redistribute the client jar");
    }

    private List<ManifestFile> tree(String dirName, String kind, String keepPrefix) throws IOException {
        File dir = new File(instance, dirName);
        List<ManifestFile> out = new ArrayList<ManifestFile>();
        if (!dir.isDirectory()) return out;
        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            files = walk.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        }
        for (Path p : files) {
            String inside = dir.toPath().relativize(p).toString().replace('\\', '/');
            String rel = keepPrefix == null ? inside : dirName + "/" + inside;
            out.add(staging.stage(p.toFile(), rel, template.cdn(rel), kind, null));
        }
        return out;
    }

    private static Manifest.AssetIndex assetIndex(JsonObject json) {
        Manifest.AssetIndex a = new Manifest.AssetIndex();
        a.id = VersionChain.string(json, "id");
        a.url = VersionChain.string(json, "url");
        a.sha1 = VersionChain.string(json, "sha1");
        a.size = json.get("size").getAsLong();
        return a;
    }
}
