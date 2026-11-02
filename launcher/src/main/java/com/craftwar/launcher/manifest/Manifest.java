package com.craftwar.launcher.manifest;

import java.util.ArrayList;
import java.util.List;

public final class Manifest {

    public static final int FORMAT = 2;

    public int version;
    public String name;
    public Consent consent = new Consent();
    public Java java = new Java();
    public List<ServerEntry> servers = new ArrayList<ServerEntry>();
    public List<Profile> profiles = new ArrayList<Profile>();

    public Profile profile(String id) {
        for (Profile p : profiles) {
            if (p.id.equals(id)) return p;
        }
        return profiles.get(0);
    }

    public ServerEntry server(String id) {
        for (ServerEntry s : servers) {
            if (s.id.equals(id)) return s;
        }
        return servers.get(0);
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
