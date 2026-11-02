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
import com.craftwar.launcher.manifest.Profile;

final class ManifestBuilder {

    private static final String AGENT_PATH = "guardian/guardian-agent.jar";

    private final File templateFile;
    private final Template template;
    private final File outDir;

    ManifestBuilder(File templateFile, Template template, File outDir) {
        this.templateFile = templateFile;
        this.template = template;
        this.outDir = outDir;
    }

    Manifest build() throws IOException {
        Manifest m = new Manifest();
        m.version = Manifest.FORMAT;
        m.name = template.name;
        m.consent = template.consent;
        m.java = template.java;
        m.servers = template.servers;
        File agent = Template.relativeTo(templateFile, template.agentJar);
        for (Template.ProfileTemplate pt : template.profiles) m.profiles.add(profile(pt, agent));
        return m;
    }

    private Profile profile(Template.ProfileTemplate pt, File agent) throws IOException {
        File instance = Template.relativeTo(templateFile, pt.instance);
        boolean ownRepository = pt.cdnBaseUrl != null && !pt.cdnBaseUrl.isEmpty();
        Staging staging = new Staging(outDir, ownRepository ? pt.cdnBaseUrl : template.cdnBaseUrl, pt.id, ownRepository);
        VersionChain chain = VersionChain.load(new File(instance, "versions"), pt.versionId);
        Profile p = new Profile();
        p.id = pt.id;
        p.label = pt.label;
        p.agentMapping = pt.agentMapping;
        p.mainClass = chain.first("mainClass");
        p.gameArguments = new ArrayList<String>(Arrays.asList(chain.first("minecraftArguments").trim().split("\\s+")));
        p.assetIndex = assetIndex(chain.root().getAsJsonObject("assetIndex"));
        p.files.addAll(new Libraries(instance, staging).resolve(chain));
        p.files.add(client(chain));
        List<ManifestFile> jarMods = tree(instance, staging, "jarmods", ManifestFile.JARMOD, true);
        p.patchedJar = !jarMods.isEmpty();
        p.files.addAll(jarMods);
        p.files.addAll(tree(instance, staging, "mods", ManifestFile.MOD, true));
        p.files.addAll(tree(instance, staging, "config-defaults", ManifestFile.CONFIG, false));
        p.files.add(staging.stage(agent, AGENT_PATH, ManifestFile.AGENT, null));
        return p;
    }

    private static ManifestFile client(VersionChain chain) throws IOException {
        String id = chain.jarId();
        String rel = "versions/" + id + "/" + id + ".jar";
        JsonObject downloads = chain.root().getAsJsonObject("downloads");
        if (downloads != null && downloads.has("client")) {
            JsonObject c = downloads.getAsJsonObject("client");
            return new ManifestFile(rel, VersionChain.string(c, "url"), VersionChain.string(c, "sha1"), c.get("size").getAsLong(), ManifestFile.GAME, null);
        }
        throw new IOException("vanilla version json has no client download; refusing to redistribute the client jar");
    }

    private static List<ManifestFile> tree(File instance, Staging staging, String dirName, String kind, boolean keepPrefix) throws IOException {
        File dir = new File(instance, dirName);
        List<ManifestFile> out = new ArrayList<ManifestFile>();
        if (!dir.isDirectory()) return out;
        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            files = walk.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        }
        for (Path f : files) {
            String inside = dir.toPath().relativize(f).toString().replace('\\', '/');
            out.add(staging.stage(f.toFile(), keepPrefix ? dirName + "/" + inside : inside, kind, null));
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
