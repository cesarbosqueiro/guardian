package com.craftwar.launcher.sync;

public interface Progress {

    void stage(String description);

    void advance(int done, int total);
}
