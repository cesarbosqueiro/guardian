package com.craftwar.launcher.manifest;

import java.util.ArrayList;
import java.util.List;

public final class Manifest {

    public int version;
    public String name;
    public Server server = new Server();
    public Consent consent = new Consent();
    public Java java = new Java();
    public String mainClass;
    public List<String> gameArguments = new ArrayList<String>();
    public String agentMapping = "forge-1.8.9";
    public AssetIndex assetIndex;
    public List<ManifestFile> files = new ArrayList<ManifestFile>();

    public static final class Server {
        public String host;
        public int port = 25565;
        public String publicKey;
    }

    public static final class Consent {
        public int version;
        public String text;
    }

    public static final class Java {
        public int minimumMemoryMb = 1024;
        public int defaultMemoryMb = 2048;
        public List<String> flags = new ArrayList<String>();
    }

    public static final class AssetIndex {
        public String id;
        public String url;
        public String sha1;
        public long size;
    }
}
