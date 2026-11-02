package com.craftwar.launcher.sync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.craftwar.launcher.manifest.ManifestFile;
import com.craftwar.launcher.manifest.Paths;
import com.craftwar.launcher.manifest.Profile;

public final class Synchronizer {

    private final File profileDir;
    private final File assetsDir;
    private final String os;
    private final HashCache cache;
    private final Progress progress;

    public Synchronizer(File profileDir, File assetsDir, String os, HashCache cache, Progress progress) {
        this.profileDir = profileDir;
        this.assetsDir = assetsDir;
        this.os = os;
        this.cache = cache;
        this.progress = progress;
    }

    public List<String> sync(Profile p, boolean force) throws IOException {
        progress.stage("Verificando arquivos");
        List<ManifestFile> files = forOs(p);
        List<ManifestFile> missing = verify(files, force);
        progress.stage("Baixando " + missing.size() + " arquivos");
        Parallel.run(downloads(missing), progress);
        progress.stage("Verificando assets");
        Assets assets = new Assets(assetsDir, profileDir, p.assetIndex);
        Parallel.run(assets.missingDownloads(), progress);
        assets.materialize();
        List<String> removed = ModsLock.enforce(profileDir, paths(files, ManifestFile.MOD));
        progress.stage("Preparando nativos");
        Natives.extract(profileDir, resolve(files, ManifestFile.NATIVES));
        if (p.patchedJar) {
            progress.stage("Aplicando jar-mods");
            JarMod.build(profileDir, files);
        }
        cache.save();
        return removed;
    }

    private List<ManifestFile> verify(List<ManifestFile> files, boolean force) throws IOException {
        List<ManifestFile> out = new ArrayList<ManifestFile>();
        int done = 0;
        for (ManifestFile f : files) {
            File target = Paths.resolve(profileDir, f.path);
            if (!valid(f, target, force)) out.add(f);
            progress.advance(++done, files.size());
        }
        return out;
    }

    private boolean valid(ManifestFile f, File target, boolean force) throws IOException {
        if (!target.isFile()) return false;
        if (f.is(ManifestFile.CONFIG)) return true;
        if (f.size > 0 && target.length() != f.size) return false;
        boolean cacheable = !force && (f.is(ManifestFile.LIBRARY) || f.is(ManifestFile.NATIVES));
        String actual = cacheable ? cache.sha1(target, f.path) : Hashing.sha1(target);
        return actual.equalsIgnoreCase(f.sha1);
    }

    private List<Callable<Void>> downloads(List<ManifestFile> files) throws IOException {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(files.size());
        for (ManifestFile f : files) {
            File target = Paths.resolve(profileDir, f.path);
            tasks.add(() -> {
                Http.download(f.url, target, f.sha1, f.size);
                return null;
            });
        }
        return tasks;
    }

    private List<ManifestFile> forOs(Profile p) {
        List<ManifestFile> out = new ArrayList<ManifestFile>();
        for (ManifestFile f : p.files) {
            if (f.forOs(os)) out.add(f);
        }
        return out;
    }

    private static List<String> paths(List<ManifestFile> files, String kind) {
        List<String> out = new ArrayList<String>();
        for (ManifestFile f : files) {
            if (f.is(kind)) out.add(f.path);
        }
        return out;
    }

    private List<File> resolve(List<ManifestFile> files, String kind) throws IOException {
        List<File> out = new ArrayList<File>();
        for (String path : paths(files, kind)) out.add(Paths.resolve(profileDir, path));
        return out;
    }
}
