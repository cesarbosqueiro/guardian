package com.craftwar.launcher.manifest;

import java.util.ArrayList;
import java.util.List;

public final class Profile {

    public String id;
    public String label;
    public String mainClass;
    public List<String> gameArguments = new ArrayList<String>();
    public String agentMapping;
    public Manifest.AssetIndex assetIndex;
    public boolean patchedJar;
    public List<ManifestFile> files = new ArrayList<ManifestFile>();

    @Override
    public String toString() {
        return label == null ? id : label;
    }
}
