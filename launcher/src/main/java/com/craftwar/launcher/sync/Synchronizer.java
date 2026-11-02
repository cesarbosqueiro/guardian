package com.craftwar.launcher.sync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.craftwar.launcher.manifest.Manifest;
import com.craftwar.launcher.manifest.ManifestFile;
import com.craftwar.launcher.manifest.Paths;

public final class Synchronizer {

    private static final int THREADS = 6;

    private final File gameDir;
    private final String os;
    private final HashCache cache;
    private final Progress progress;

    public Synchronizer(File gameDir, String os, HashCache cache, Progress progress) {
        this.gameDir = gameDir;
        this.os = os;
        this.cache = cache;
        this.progress = progress;
    }

    public List<String> sync(Manifest m) throws IOException {
        progress.stage("Verificando arquivos");
        List<ManifestFile> missing = verify(m);
        progress.stage("Baixando " + missing.size() + " arquivos");
        runAll(downloads(missing));
        progress.stage("Verificando assets");
        runAll(new Assets(gameDir, m.assetIndex).missingDownloads());
        List<String> removed = ModsLock.enforce(gameDir, allowedMods(m));
        progress.stage("Preparando nativos");
        Natives.extract(gameDir, nativesFor(m));
        cache.save();
        return removed;
    }

    private List<ManifestFile> verify(Manifest m) throws IOException {
        List<ManifestFile> out = new ArrayList<ManifestFile>();
        List<ManifestFile> files = forOs(m);
        int done = 0;
        for (ManifestFile f : files) {
            File target = Paths.resolve(gameDir, f.path);
            if (!valid(f, target)) out.add(f);
            progress.advance(++done, files.size());
        }
        return out;
    }

    private boolean valid(ManifestFile f, File target) throws IOException {
        if (!target.isFile()) return false;
        if (ManifestFile.CONFIG.equals(f.kind)) return true;
        if (f.size > 0 && target.length() != f.size) return false;
        boolean cacheable = ManifestFile.LIBRARY.equals(f.kind) || ManifestFile.NATIVES.equals(f.kind);
        String actual = cacheable ? cache.sha1(target, f.path) : Hashing.sha1(target);
        return actual.equalsIgnoreCase(f.sha1);
    }

    private List<Callable<Void>> downloads(List<ManifestFile> files) throws IOException {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(files.size());
        for (ManifestFile f : files) {
            File target = Paths.resolve(gameDir, f.path);
            tasks.add(() -> {
                Http.download(f.url, target, f.sha1, f.size);
                return null;
            });
        }
        return tasks;
    }

    private void runAll(List<Callable<Void>> tasks) throws IOException {
        if (tasks.isEmpty()) return;
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        AtomicInteger done = new AtomicInteger();
        try {
            List<Future<Void>> futures = new ArrayList<Future<Void>>(tasks.size());
            for (Callable<Void> t : tasks) {
                futures.add(pool.submit(() -> {
                    t.call();
                    progress.advance(done.incrementAndGet(), tasks.size());
                    return null;
                }));
            }
            for (Future<Void> f : futures) f.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw cause instanceof IOException ? (IOException) cause : new IOException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        } finally {
            pool.shutdownNow();
        }
    }

    private List<ManifestFile> forOs(Manifest m) {
        List<ManifestFile> out = new ArrayList<ManifestFile>();
        for (ManifestFile f : m.files) {
            if (f.forOs(os)) out.add(f);
        }
        return out;
    }

    private List<String> allowedMods(Manifest m) {
        List<String> out = new ArrayList<String>();
        for (ManifestFile f : forOs(m)) {
            if (ManifestFile.MOD.equals(f.kind)) out.add(f.path);
        }
        return out;
    }

    private List<File> nativesFor(Manifest m) throws IOException {
        List<File> out = new ArrayList<File>();
        for (ManifestFile f : forOs(m)) {
            if (ManifestFile.NATIVES.equals(f.kind)) out.add(Paths.resolve(gameDir, f.path));
        }
        return out;
    }
}
