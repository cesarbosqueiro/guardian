package com.craftwar.launcher.sync;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.craftwar.launcher.GameLayout;
import com.craftwar.launcher.manifest.ServerEntry;

public class LocalFilesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File zip(String name, String... entries) throws IOException {
        File f = new File(tmp.getRoot(), name);
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
            for (int i = 0; i < entries.length; i += 2) {
                out.putNextEntry(new ZipEntry(entries[i]));
                out.write(entries[i + 1].getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return f;
    }

    private static String read(ZipFile z, String name) throws IOException {
        byte[] b = new byte[64];
        int n = z.getInputStream(z.getEntry(name)).read(b);
        return new String(b, 0, n, StandardCharsets.UTF_8);
    }

    @Test
    public void jarModOverridesVanillaAndDropsSignature() throws IOException {
        File vanilla = zip("vanilla.jar", "aab.class", "vanilla", "net/Main.class", "main", "META-INF/MOJANG_C.SF", "sig");
        File forge = zip("forge.zip", "aab.class", "forge", "cpw/mods/fml/X.class", "fml");
        File out = new File(tmp.getRoot(), "out/patched.jar");
        JarMod.merge(Arrays.asList(forge, vanilla), out);
        try (ZipFile z = new ZipFile(out)) {
            assertEquals("forge", read(z, "aab.class"));
            assertEquals("main", read(z, "net/Main.class"));
            assertEquals("fml", read(z, "cpw/mods/fml/X.class"));
            assertEquals(null, z.getEntry("META-INF/MOJANG_C.SF"));
        }
    }

    @Test
    public void serversDatIsNbtWithEveryServerInOrder() throws IOException {
        List<ServerEntry> servers = Arrays.asList(new ServerEntry("pvp", "PvP", "pvp.test", 25565, "k"),
            new ServerEntry("rk", "RankUP", "rk.test", 25566, "k"));
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(ServersDat.encode(servers)));
        assertEquals(10, in.readByte());
        assertEquals("", in.readUTF());
        assertEquals(9, in.readByte());
        assertEquals("servers", in.readUTF());
        assertEquals(10, in.readByte());
        assertEquals(2, in.readInt());
        assertEquals(8, in.readByte());
        assertEquals("name", in.readUTF());
        assertEquals("PvP", in.readUTF());
        assertEquals(8, in.readByte());
        assertEquals("ip", in.readUTF());
        assertEquals("pvp.test", in.readUTF());
    }

    @Test
    public void serversDatWritesLockCopy() throws IOException {
        File dir = tmp.newFolder("p");
        ServersDat.write(dir, Collections.singletonList(new ServerEntry("a", "A", "h", 1, "k")));
        assertArrayEquals(Files.readAllBytes(new File(dir, ServersDat.FILE).toPath()),
            Files.readAllBytes(new File(dir, ServersDat.LOCK).toPath()));
    }

    @Test
    public void modsLockOnlyTouchesItsOwnProfile() throws IOException {
        GameLayout layout = new GameLayout(tmp.newFolder("game"));
        File legacyMod = new File(layout.profile("1.5.2"), "mods/OptiFine-152.jar");
        File modernMod = new File(layout.profile("1.8.9"), "mods/OptiFine.jar");
        File stray = new File(layout.profile("1.8.9"), "mods/cheat.jar");
        File config = new File(layout.profile("1.8.9"), "mods/OptiFine.cfg");
        for (File f : new File[] {legacyMod, modernMod, stray, config}) {
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
        List<String> moved = ModsLock.enforce(layout.profile("1.8.9"), Collections.singletonList("mods/OptiFine.jar"));
        assertEquals(Collections.singletonList("mods/cheat.jar"), moved);
        assertTrue(legacyMod.isFile());
        assertTrue(modernMod.isFile());
        assertFalse(stray.isFile());
        assertTrue(config.isFile());
        assertTrue(new File(layout.profile("1.8.9"), "mods-disabled/cheat.jar").isFile());
    }
}
