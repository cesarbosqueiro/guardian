package com.craftwar.launcher.manifest;

public final class ManifestFile {

    public static final String GAME = "game";
    public static final String LIBRARY = "library";
    public static final String NATIVES = "natives";
    public static final String MOD = "mod";
    public static final String AGENT = "agent";
    public static final String CONFIG = "config";
    public static final String JARMOD = "jarmod";

    public String path;
    public String url;
    public String sha1;
    public long size;
    public String kind;
    public String os;

    public ManifestFile() {
    }

    public ManifestFile(String path, String url, String sha1, long size, String kind, String os) {
        this.path = path;
        this.url = url;
        this.sha1 = sha1;
        this.size = size;
        this.kind = kind;
        this.os = os;
    }

    public boolean is(String k) {
        return k.equals(kind);
    }

    public boolean forOs(String current) {
        return os == null || os.equals(current);
    }
}
