package com.craftwar.launcher.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.craftwar.launcher.manifest.ManifestFile;
import com.craftwar.launcher.manifest.Paths;
import com.craftwar.launcher.sync.Files2;
import com.craftwar.launcher.sync.Hashing;

final class Staging {

    private final File outDir;

    Staging(File outDir) {
        this.outDir = outDir;
    }

    ManifestFile stage(File source, String relativePath, String url, String kind, String os) throws IOException {
        Paths.requireSafe(relativePath);
        File target = new File(outDir, "files/" + relativePath);
        Files2.mkdirs(target.getParentFile());
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return new ManifestFile(relativePath, url, Hashing.sha1(source), source.length(), kind, os);
    }
}
