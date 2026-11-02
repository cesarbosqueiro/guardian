package com.craftwar.launcher.tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.craftwar.guardian.protocol.Ec;
import com.craftwar.guardian.protocol.Keys;
import com.craftwar.launcher.launch.LaunchCommand;
import com.craftwar.launcher.launch.LaunchTarget;
import com.craftwar.launcher.manifest.Manifest;
import com.craftwar.launcher.manifest.ManifestFile;
import com.craftwar.launcher.manifest.ManifestSource;
import com.craftwar.launcher.manifest.Profile;
import com.craftwar.launcher.manifest.Signatures;

public class ManifestToolTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    static final String VANILLA_189 = "{\"id\":\"1.8.9\",\"mainClass\":\"net.minecraft.client.main.Main\","
        + "\"minecraftArguments\":\"--username ${auth_player_name}\","
        + "\"assetIndex\":{\"id\":\"1.8\",\"url\":\"https://x/1.8.json\",\"sha1\":\"aa\",\"size\":10},"
        + "\"downloads\":{\"client\":{\"url\":\"https://launcher.mojang.com/client.jar\",\"sha1\":\"3870888a6c3d349d3771a3e9d16c9bf5e076b908\",\"size\":8461484}},"
        + "\"libraries\":[{\"name\":\"com.google.guava:guava:17.0\",\"downloads\":{\"artifact\":{\"path\":\"com/google/guava/guava/17.0/guava-17.0.jar\",\"url\":\"https://libraries.minecraft.net/g.jar\",\"sha1\":\"bb\",\"size\":5}}},"
        + "{\"name\":\"org.lwjgl.lwjgl:lwjgl-platform:2.9.4\",\"natives\":{\"windows\":\"natives-windows\",\"linux\":\"natives-linux\"},"
        + "\"downloads\":{\"classifiers\":{\"natives-windows\":{\"path\":\"org/lwjgl/w.jar\",\"url\":\"https://l/w.jar\",\"sha1\":\"cc\",\"size\":1},"
        + "\"natives-linux\":{\"path\":\"org/lwjgl/l.jar\",\"url\":\"https://l/l.jar\",\"sha1\":\"dd\",\"size\":1}}}},"
        + "{\"name\":\"tv.twitch:twitch:6.5\",\"rules\":[{\"action\":\"allow\"},{\"action\":\"disallow\",\"os\":{\"name\":\"osx\"}}],"
        + "\"downloads\":{\"artifact\":{\"path\":\"tv/twitch/t.jar\",\"url\":\"https://l/t.jar\",\"sha1\":\"ee\",\"size\":1}}}]}";

    static final String FORGE_189 = "{\"id\":\"forge\",\"inheritsFrom\":\"1.8.9\",\"jar\":\"1.8.9\","
        + "\"mainClass\":\"net.minecraft.launchwrapper.Launch\","
        + "\"minecraftArguments\":\"--username ${auth_player_name} --tweakClass net.minecraftforge.fml.common.launcher.FMLTweaker\","
        + "\"libraries\":[{\"name\":\"net.minecraft:launchwrapper:1.12\"},{\"name\":\"jline:jline:2.13\",\"clientreq\":false}]}";

    static final String VANILLA_152 = "{\"id\":\"1.5.2\",\"mainClass\":\"net.minecraft.launchwrapper.Launch\","
        + "\"minecraftArguments\":\"${auth_player_name} ${auth_session} --gameDir ${game_directory} --assetsDir ${game_assets}\","
        + "\"assetIndex\":{\"id\":\"pre-1.6\",\"url\":\"https://x/pre.json\",\"sha1\":\"ff\",\"size\":10},"
        + "\"downloads\":{\"client\":{\"url\":\"https://launcher.mojang.com/152.jar\",\"sha1\":\"465378c9dc2f779ae1d6e8046ebc46fb53a57968\",\"size\":5000000}},"
        + "\"libraries\":[]}";

    private static final String TEMPLATE = "{\"name\":\"CraftWar\",\"cdnBaseUrl\":\"https://cdn.test/files\",\"agentJar\":\"agent.jar\","
        + "\"servers\":[{\"id\":\"pvp\",\"name\":\"PvP\",\"host\":\"pvp.test\",\"port\":25565,\"publicKey\":\"%s\"},"
        + "{\"id\":\"rankup\",\"name\":\"RankUP\",\"host\":\"rankup.test\",\"port\":25566,\"publicKey\":\"%s\"}],"
        + "\"profiles\":[{\"id\":\"1.8.9\",\"label\":\"1.8.9 PvP\",\"instance\":\"i189\",\"versionId\":\"forge\",\"agentMapping\":\"forge-1.8.9\"},"
        + "{\"id\":\"1.5.2\",\"label\":\"1.5.2\",\"instance\":\"i152\",\"versionId\":\"1.5.2\",\"agentMapping\":\"forge-1.5.2\"}],"
        + "\"consent\":{\"version\":1,\"text\":\"aceite\"},\"java\":{\"flags\":[\"-XX:+UseG1GC\"]}}";

    static File write(File root, String rel, String content) throws IOException {
        File f = new File(root, rel);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private String key() {
        return Keys.publicToBase64(Ec.newKeyPair().getPublic());
    }

    private Manifest buildTwoProfiles(File out) throws Exception {
        File root = tmp.getRoot();
        write(root, "i189/versions/1.8.9/1.8.9.json", VANILLA_189);
        write(root, "i189/versions/forge/forge.json", FORGE_189);
        write(root, "i189/libraries/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar", "lw");
        write(root, "i189/mods/OptiFine.jar", "of");
        write(root, "i189/config-defaults/options.txt", "fov:0");
        write(root, "i152/versions/1.5.2/1.5.2.json", VANILLA_152);
        write(root, "i152/jarmods/forge-1.5.2-universal.zip", "forge");
        write(root, "i152/mods/OptiFine-152.jar", "of152");
        write(root, "agent.jar", "agent");
        File template = write(root, "template.json", String.format(TEMPLATE, key(), key()));
        File keys = tmp.newFolder("keys");
        ManifestTool.main(new String[] {"keygen", keys.getPath()});
        ManifestTool.main(new String[] {"build", template.getPath(), new File(keys, "manifest.key").getPath(), out.getPath()});
        byte[] json = Files.readAllBytes(new File(out, "manifest.json").toPath());
        byte[] sig = Files.readAllBytes(new File(out, "manifest.json.sig").toPath());
        assertTrue(Signatures.verify(Keys.publicFromDer(Files.readAllBytes(new File(keys, "manifest.pub").toPath())), json, sig));
        return ManifestSource.parse(json);
    }

    @Test
    public void buildsOneSignedManifestWithTwoProfiles() throws Exception {
        File out = tmp.newFolder("out");
        Manifest m = buildTwoProfiles(out);
        assertEquals(2, m.profiles.size());
        assertEquals(2, m.servers.size());
        Profile modern = m.profile("1.8.9");
        Profile legacy = m.profile("1.5.2");
        assertEquals("net.minecraft.launchwrapper.Launch", modern.mainClass);
        assertFalse(modern.patchedJar);
        assertTrue(legacy.patchedJar);
        assertEquals(0, count(modern, "jline"));
        assertEquals(2, count(modern, "tv/twitch"));
        assertEquals(1, count(legacy, "jarmods/forge-1.5.2-universal.zip"));
        assertEquals(0, count(legacy, "OptiFine.jar"));
        assertTrue(new File(out, "files/1.8.9/mods/OptiFine.jar").isFile());
        assertTrue(new File(out, "files/1.5.2/mods/OptiFine-152.jar").isFile());
        assertEquals("https://cdn.test/files/1.8.9/mods/OptiFine.jar", find(modern, "mods/OptiFine.jar").url);
        assertEquals("https://launcher.mojang.com/client.jar", find(modern, "versions/1.8.9/1.8.9.jar").url);
    }

    @Test
    public void launchCommandTrustsEveryServerAndNeverConnectsDirectly() throws Exception {
        Manifest m = buildTwoProfiles(tmp.newFolder("out"));
        File profileDir = tmp.newFolder("game", "1.8.9");
        LaunchTarget t = new LaunchTarget(new File("java"), profileDir, new File(tmp.getRoot(), "game/assets"), "windows", "Player_1", 2048);
        List<String> cmd = LaunchCommand.build(m, m.profile("1.8.9"), t);
        String line = String.join(" ", cmd);
        assertTrue(cmd.contains("-XX:+DisableAttachMechanism"));
        assertEquals("Player_1", cmd.get(cmd.indexOf("--username") + 1));
        assertFalse(cmd.contains("--server"));
        assertTrue(line.contains("=servers=" + m.servers.get(0).publicKey + ":" + m.servers.get(1).publicKey + ",mapping=forge-1.8.9"));
    }

    @Test
    public void legacyProfileUsesPatchedJarAndVirtualAssets() throws Exception {
        Manifest m = buildTwoProfiles(tmp.newFolder("out"));
        File profileDir = tmp.newFolder("game", "1.5.2");
        File assets = new File(tmp.getRoot(), "game/assets");
        List<String> cmd = LaunchCommand.build(m, m.profile("1.5.2"), new LaunchTarget(new File("java"), profileDir, assets, "windows", "Player_1", 1024));
        String cp = cmd.get(cmd.indexOf("-cp") + 1);
        assertTrue(cp.contains("1.5.2-patched.jar"));
        assertFalse(cp.contains("forge-1.5.2-universal.zip"));
        assertEquals("0", cmd.get(cmd.indexOf("Player_1") + 1));
        assertEquals(new File(assets, "virtual/pre-1.6").getAbsolutePath(), cmd.get(cmd.indexOf("--assetsDir") + 1));
    }

    private static long count(Profile p, String fragment) {
        return p.files.stream().filter(f -> f.path.contains(fragment)).count();
    }

    private static ManifestFile find(Profile p, String path) {
        return p.files.stream().filter(f -> f.path.equals(path)).findFirst().get();
    }
}
