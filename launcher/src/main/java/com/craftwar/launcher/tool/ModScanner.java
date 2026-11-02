package com.craftwar.launcher.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.craftwar.launcher.manifest.ManifestFile;
import com.craftwar.launcher.manifest.Profile;
import com.craftwar.launcher.sync.ModsLock;

final class ModScanner {

    static final String CLASSPATH_DIR = "guardian/mods/";
    private static final String MODS = "mods";

    private final Staging staging;
    private final boolean onClasspath;

    ModScanner(Staging staging, boolean onClasspath) {
        this.staging = staging;
        this.onClasspath = onClasspath;
    }

    void scan(File instance, Profile p) throws IOException {
        File dir = new File(instance, MODS);
        if (!dir.isDirectory()) return;
        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            files = walk.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        }
        for (Path f : files) {
            String inside = dir.toPath().relativize(f).toString().replace('\\', '/');
            if (!ModsLock.isArchive(inside)) p.files.add(staging.stage(f.toFile(), MODS + "/" + inside, ManifestFile.CONFIG, null));
            else if (onClasspath) addClasspathMod(f.toFile(), inside, p);
            else p.files.add(staging.stage(f.toFile(), MODS + "/" + inside, ManifestFile.MOD, null));
        }
    }

    private void addClasspathMod(File jar, String inside, Profile p) throws IOException {
        p.files.add(staging.stage(jar, CLASSPATH_DIR + inside, ManifestFile.CLASSMOD, null));
        Attributes a = attributes(jar);
        if (a == null) return;
        addOnce(p.tweakClasses, a.getValue("TweakClass"));
        addOnce(p.coreMods, a.getValue("FMLCorePlugin"));
    }

    private static Attributes attributes(File jar) throws IOException {
        try (JarFile j = new JarFile(jar)) {
            Manifest m = j.getManifest();
            return m == null ? null : m.getMainAttributes();
        }
    }

    private static void addOnce(List<String> list, String value) {
        if (value != null && !value.trim().isEmpty() && !list.contains(value.trim())) list.add(value.trim());
    }
}
