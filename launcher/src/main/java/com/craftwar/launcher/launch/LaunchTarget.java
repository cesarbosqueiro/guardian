package com.craftwar.launcher.launch;

import java.io.File;

public final class LaunchTarget {

    public final File java;
    public final File profileDir;
    public final File assetsDir;
    public final String os;
    public final String nick;
    public final int memoryMb;

    public LaunchTarget(File java, File profileDir, File assetsDir, String os, String nick, int memoryMb) {
        this.java = java;
        this.profileDir = profileDir;
        this.assetsDir = assetsDir;
        this.os = os;
        this.nick = nick;
        this.memoryMb = memoryMb;
    }
}
