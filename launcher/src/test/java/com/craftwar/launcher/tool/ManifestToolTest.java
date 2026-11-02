package com.craftwar.launcher.tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.craftwar.guardian.protocol.Keys;
import com.craftwar.launcher.launch.LaunchCommand;
import com.craftwar.launcher.manifest.Manifest;
import com.craftwar.launcher.manifest.ManifestFile;
import com.craftwar.launcher.manifest.ManifestSource;
import com.craftwar.launcher.manifest.Signatures;

public class ManifestToolTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String VANILLA = "{\"id\":\"1.8.9\",\"mainClass\":\"net.minecraft.client.main.Main\","
        + "\"minecraftArguments\":\"--username ${auth_player_name}\","
        + "\"assetIndex\":{\"id\":\"1.8\",\"url\":\"https://x/1.8.json\",\"sha1\":\"aa\",\"size\":10},"
        + "\"downloads\":{\"client\":{\"url\":\"https://launcher.mojang.com/client.jar\",\"sha1\":\"3870888a6c3d349d3771a3e9d16c9bf5e076b908\",\"size\":8461484}},"
        + "\"libraries\":[{\"name\":\"com.google.guava:guava:17.0\",\"downloads\":{\"artifact\":{\"path\":\"com/google/guava/guava/17.0/guava-17.0.jar\",\"url\":\"https://libraries.minecraft.net/g.jar\",\"sha1\":\"bb\",\"size\":5}}},"
        + "{\"name\":\"org.lwjgl.lwjgl:lwjgl-platform:2.9.4\",\"natives\":{\"windows\":\"natives-windows\",\"linux\":\"natives-linux\"},"
        + "\"downloads\":{\"classifiers\":{\"natives-windows\":{\"path\":\"org/lwjgl/w.jar\",\"url\":\"https://l/w.jar\",\"sha1\":\"cc\",\"size\":1},"
        + "\"natives-linux\":{\"path\":\"org/lwjgl/l.jar\",\"url\":\"https://l/l.jar\",\"sha1\":\"dd\",\"size\":1}}}},"
        + "{\"name\":\"tv.twitch:twitch:6.5\",\"rules\":[{\"action\":\"allow\"},{\"action\":\"disallow\",\"os\":{\"name\":\"osx\"}}],"
        + "\"downloads\":{\"artifact\":{\"path\":\"tv/twitch/t.jar\",\"url\":\"https://l/t.jar\",\"sha1\":\"ee\",\"size\":1}}}]}";

    private static final String FORGE = "{\"id\":\"forge\",\"inheritsFrom\":\"1.8.9\",\"jar\":\"1.8.9\","
        + "\"mainClass\":\"net.minecraft.launchwrapper.Launch\","
        + "\"minecraftArguments\":\"--username ${auth_player_name} --tweakClass net.minecraftforge.fml.common.launcher.FMLTweaker\","
        + "\"libraries\":[{\"name\":\"net.minecraft:launchwrapper:1.12\"},{\"name\":\"jline:jline:2.13\",\"clientreq\":false}]}";

    private static final String TEMPLATE = "{\"version\":3,\"name\":\"CraftWar\",\"versionId\":\"forge\",\"cdnBaseUrl\":\"https://cdn.test/l\","
        + "\"agentJar\":\"%s\",\"server\":{\"host\":\"pvp.test\",\"port\":25565,\"publicKey\":\"KEY\"},"
        + "\"consent\":{\"version\":1,\"text\":\"aceite\"},\"java\":{\"flags\":[\"-XX:+UseG1GC\"]}}";

    private File write(File root, String rel, String content) throws IOException {
        File f = new File(root, rel);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test
    public void buildsSignedManifestFromForgeInstance() throws Exception {
        File instance = tmp.newFolder("instance");
        write(instance, "versions/1.8.9/1.8.9.json", VANILLA);
        write(instance, "versions/forge/forge.json", FORGE);
        write(instance, "libraries/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar", "lw");
        write(instance, "mods/OptiFine.jar", "of");
        write(instance, "config-defaults/options.txt", "fov:0");
        File agent = write(tmp.getRoot(), "agent.jar", "agent");
        File template = write(tmp.getRoot(), "template.json", String.format(TEMPLATE, agent.getAbsolutePath().replace("\\", "\\\\")));
        File keys = tmp.newFolder("keys");
        File out = tmp.newFolder("out");
        ManifestTool.main(new String[] {"keygen", keys.getPath()});
        ManifestTool.main(new String[] {"build", instance.getPath(), template.getPath(), new File(keys, "manifest.key").getPath(), out.getPath()});

        byte[] json = Files.readAllBytes(new File(out, "manifest.json").toPath());
        byte[] sig = Files.readAllBytes(new File(out, "manifest.json.sig").toPath());
        assertTrue(Signatures.verify(Keys.publicFromDer(Files.readAllBytes(new File(keys, "manifest.pub").toPath())), json, sig));
        Manifest m = ManifestSource.parse(json);
        assertEquals("net.minecraft.launchwrapper.Launch", m.mainClass);
        assertTrue(new File(out, "files/mods/OptiFine.jar").isFile());
        assertTrue(new File(out, "files/libraries/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar").isFile());
        assertEquals(0, count(m, "jline"));
        assertEquals(2, count(m, "tv/twitch"));
        assertEquals("https://launcher.mojang.com/client.jar", find(m, ManifestFile.GAME).url);

        List<String> cmd = LaunchCommand.build(m, new File("java"), tmp.newFolder("game"), "windows", "Player_1", 2048);
        assertTrue(cmd.contains("-XX:+DisableAttachMechanism"));
        assertTrue(cmd.get(cmd.indexOf("--username") + 1).equals("Player_1"));
        assertTrue(cmd.contains("net.minecraftforge.fml.common.launcher.FMLTweaker"));
        assertTrue(String.join(" ", cmd).contains("guardian-agent.jar=server=KEY,mapping=forge-1.8.9"));
    }

    private static long count(Manifest m, String fragment) {
        return m.files.stream().filter(f -> f.path.contains(fragment)).count();
    }

    private static ManifestFile find(Manifest m, String kind) {
        return m.files.stream().filter(f -> kind.equals(f.kind)).findFirst().get();
    }
}
