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
    private final String cdnBase;
    private final String profileId;
    private final boolean ownRepository;

    Staging(File outDir, String cdnBase, String profileId, boolean ownRepository) {
        this.outDir = outDir;
        this.cdnBase = cdnBase.endsWith("/") ? cdnBase : cdnBase + "/";
        this.profileId = profileId;
        this.ownRepository = ownRepository;
    }

    ManifestFile stage(File source, String relativePath, String kind, String os) throws IOException {
        Paths.requireSafe(relativePath);
        File target = new File(outDir, "files/" + profileId + "/" + relativePath);
        Files2.mkdirs(target.getParentFile());
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return new ManifestFile(relativePath, cdnBase + (ownRepository ? "" : profileId + "/") + relativePath, Hashing.sha1(source), source.length(), kind, os);
    }
}
