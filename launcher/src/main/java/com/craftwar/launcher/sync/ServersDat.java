package com.craftwar.launcher.sync;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import com.craftwar.launcher.manifest.ServerEntry;

public final class ServersDat {

    public static final String FILE = "servers.dat";
    public static final String LOCK = "guardian/servers.lock";

    private static final int TAG_END = 0;
    private static final int TAG_BYTE = 1;
    private static final int TAG_STRING = 8;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;

    private ServersDat() {
    }

    public static void write(File profileDir, List<ServerEntry> servers) throws IOException {
        byte[] nbt = encode(servers);
        for (String rel : new String[] {FILE, LOCK}) {
            File target = new File(profileDir, rel);
            Files2.mkdirs(target.getParentFile());
            File tmp = new File(target.getPath() + ".tmp");
            Files.write(tmp.toPath(), nbt);
            Files2.replace(tmp, target);
        }
    }

    public static byte[] encode(List<ServerEntry> servers) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(256);
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(TAG_COMPOUND);
        name(out, "");
        out.writeByte(TAG_LIST);
        name(out, "servers");
        out.writeByte(TAG_COMPOUND);
        out.writeInt(servers.size());
        for (ServerEntry s : servers) {
            string(out, "name", s.name == null ? s.id : s.name);
            string(out, "ip", s.address());
            flag(out, "hideAddress", true);
            flag(out, "acceptTextures", true);
            out.writeByte(TAG_END);
        }
        out.writeByte(TAG_END);
        out.flush();
        return bytes.toByteArray();
    }

    private static void string(DataOutputStream out, String key, String value) throws IOException {
        out.writeByte(TAG_STRING);
        name(out, key);
        name(out, value);
    }

    private static void flag(DataOutputStream out, String key, boolean value) throws IOException {
        out.writeByte(TAG_BYTE);
        name(out, key);
        out.writeByte(value ? 1 : 0);
    }

    private static void name(DataOutputStream out, String value) throws IOException {
        byte[] b = value.getBytes(StandardCharsets.UTF_8);
        out.writeShort(b.length);
        out.write(b);
    }
}
