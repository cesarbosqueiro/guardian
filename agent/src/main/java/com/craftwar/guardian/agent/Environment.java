package com.craftwar.guardian.agent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Environment {

    private static volatile Environment current;

    public final String gpu;
    public final int width;
    public final int height;
    public final List<String> mods;
    public final List<String> resourcePacks;

    private Environment(String gpu, int width, int height, List<String> mods, List<String> resourcePacks) {
        this.gpu = gpu;
        this.width = width;
        this.height = height;
        this.mods = mods;
        this.resourcePacks = resourcePacks;
    }

    static void publish(String gpu, int width, int height, String[] mods, String[] packs) {
        current = new Environment(gpu, width, height, copy(mods), copy(packs));
    }

    private static List<String> copy(String[] values) {
        return values == null ? Collections.<String>emptyList() : Collections.unmodifiableList(Arrays.asList(values.clone()));
    }

    public static Environment current() {
        return current;
    }
}
