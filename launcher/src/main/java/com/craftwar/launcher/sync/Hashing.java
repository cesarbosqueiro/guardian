package com.craftwar.launcher.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hashing {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Hashing() {
    }

    public static MessageDigest sha1() {
        return digest("SHA-1");
    }

    public static MessageDigest sha256() {
        return digest("SHA-256");
    }

    private static MessageDigest digest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha1(File file) throws IOException {
        MessageDigest d = sha1();
        byte[] buf = new byte[64 * 1024];
        try (InputStream in = new FileInputStream(file)) {
            for (int n; (n = in.read(buf)) > 0; ) d.update(buf, 0, n);
        }
        return hex(d.digest());
    }

    public static String hex(byte[] bytes) {
        char[] out = new char[bytes.length << 1];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            out[j++] = HEX[(bytes[i] >>> 4) & 0xF];
            out[j++] = HEX[bytes[i] & 0xF];
        }
        return new String(out);
    }
}
