package com.craftwar.launcher.manifest;

public final class ServerEntry {

    public String id;
    public String name;
    public String host;
    public int port = 25565;
    public String publicKey;

    public ServerEntry() {
    }

    public ServerEntry(String id, String name, String host, int port, String publicKey) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.publicKey = publicKey;
    }

    public String address() {
        return port == 25565 ? host : host + ":" + port;
    }

    @Override
    public String toString() {
        return name == null ? id : name;
    }
}
