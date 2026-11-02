package com.craftwar.launcher;

import java.io.File;

public final class GameLayout {

    private final File root;

    public GameLayout(File root) {
        this.root = root;
    }

    public File root() {
        return root;
    }

    public File profile(String profileId) {
        return new File(root, profileId);
    }

    public File assets() {
        return new File(root, "assets");
    }

    public File settings() {
        return new File(root, "launcher/settings.properties");
    }

    public File hashCache(String profileId) {
        return new File(root, "launcher/hashes-" + profileId + ".properties");
    }
}
