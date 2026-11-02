package com.craftwar.launcher.sync;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public final class Http {

    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 30_000;
    private static final int BUFFER = 64 * 1024;

    private Http() {
    }

    public static byte[] get(String url, int maxBytes) throws IOException {
        HttpURLConnection c = open(url);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxBytes, 1 << 16));
            byte[] buf = new byte[BUFFER];
            for (int n; (n = in.read(buf)) > 0; ) {
                if (out.size() + n > maxBytes) throw new IOException("response too large: " + url);
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    public static void download(String url, File target, String expectedSha1, long expectedSize) throws IOException {
        File tmp = new File(target.getPath() + ".part");
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("cannot create " + parent);
        HttpURLConnection c = open(url);
        MessageDigest sha1 = Hashing.sha1();
        long total = 0;
        try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[BUFFER];
            for (int n; (n = in.read(buf)) > 0; ) {
                total += n;
                if (expectedSize > 0 && total > expectedSize) throw new IOException("larger than expected: " + url);
                sha1.update(buf, 0, n);
                out.write(buf, 0, n);
            }
        } finally {
            c.disconnect();
        }
        String actual = Hashing.hex(sha1.digest());
        if (expectedSha1 != null && !expectedSha1.equalsIgnoreCase(actual)) {
            tmp.delete();
            throw new IOException("sha1 mismatch for " + url + ": " + actual);
        }
        Files2.replace(tmp, target);
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(CONNECT_TIMEOUT);
        c.setReadTimeout(READ_TIMEOUT);
        c.setRequestProperty("User-Agent", "CraftWar-Launcher");
        c.setInstanceFollowRedirects(true);
        int code = c.getResponseCode();
        if (code != 200) {
            c.disconnect();
            throw new IOException("HTTP " + code + " for " + url);
        }
        return c;
    }
}
