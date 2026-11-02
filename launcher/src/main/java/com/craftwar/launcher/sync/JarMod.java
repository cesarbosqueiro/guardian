package com.craftwar.launcher.sync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.craftwar.launcher.manifest.ManifestFile;
import com.craftwar.launcher.manifest.Paths;

public final class JarMod {

    private static final String SUFFIX = "-patched.jar";

    private JarMod() {
    }

    public static String patchedPath(ManifestFile game) {
        return game.path.substring(0, game.path.length() - ".jar".length()) + SUFFIX;
    }

    static void build(File profileDir, List<ManifestFile> files) throws IOException {
        ManifestFile game = null;
        List<ManifestFile> mods = new ArrayList<ManifestFile>();
        StringBuilder key = new StringBuilder();
        for (ManifestFile f : files) {
            if (f.is(ManifestFile.GAME)) game = f;
            else if (f.is(ManifestFile.JARMOD)) mods.add(f);
        }
        if (game == null) throw new IOException("patched profile without game jar");
        key.append(game.sha1);
        for (ManifestFile m : mods) key.append(':').append(m.sha1);
        File out = Paths.resolve(profileDir, patchedPath(game));
        File marker = new File(out.getPath() + ".inputs");
        if (out.isFile() && marker.isFile() && new String(Files.readAllBytes(marker.toPath()), StandardCharsets.US_ASCII).equals(key.toString())) return;
        List<File> layers = new ArrayList<File>();
        for (int i = mods.size() - 1; i >= 0; i--) layers.add(Paths.resolve(profileDir, mods.get(i).path));
        layers.add(Paths.resolve(profileDir, game.path));
        merge(layers, out);
        Files.write(marker.toPath(), key.toString().getBytes(StandardCharsets.US_ASCII));
    }

    static void merge(List<File> topFirst, File out) throws IOException {
        File tmp = new File(out.getPath() + ".part");
        Files2.mkdirs(out.getParentFile());
        Set<String> written = new HashSet<String>();
        byte[] buf = new byte[64 * 1024];
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(tmp))) {
            for (File layer : topFirst) {
                try (ZipFile in = new ZipFile(layer)) {
                    for (Enumeration<? extends ZipEntry> it = in.entries(); it.hasMoreElements(); ) {
                        ZipEntry e = it.nextElement();
                        String name = e.getName();
                        if (e.isDirectory() || name.startsWith("META-INF/") || !written.add(name)) continue;
                        zip.putNextEntry(new ZipEntry(name));
                        try (InputStream data = in.getInputStream(e)) {
                            for (int n; (n = data.read(buf)) > 0; ) zip.write(buf, 0, n);
                        }
                        zip.closeEntry();
                    }
                }
            }
        }
        Files2.replace(tmp, out);
    }
}
