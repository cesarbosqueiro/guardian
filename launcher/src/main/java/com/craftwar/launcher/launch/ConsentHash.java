package com.craftwar.launcher.launch;

import java.nio.charset.StandardCharsets;

import com.craftwar.launcher.sync.Hashing;

public final class ConsentHash {

    private ConsentHash() {
    }

    public static String of(String text) {
        return Hashing.hex(Hashing.sha256().digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
