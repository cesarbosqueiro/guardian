package com.craftwar.guardian.agent.patch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.craftwar.guardian.agent.Mapping;
import com.craftwar.guardian.agent.probe.Probes;
import com.craftwar.guardian.agent.record.RecorderProbe;

public class PatchesTest {

    private static Mapping mapping() throws Exception {
        Properties p = new Properties();
        InputStream in = PatchesTest.class.getResourceAsStream("/mappings/forge-1.8.9.properties");
        try {
            p.load(in);
        } finally {
            in.close();
        }
        return Mapping.of("forge-1.8.9", p);
    }

    private static Patch named(List<Patch> patches, String name) {
        for (Patch p : patches) if (p.name().equals(name)) return p;
        throw new AssertionError(name);
    }

    @Test
    public void mouseDeltaReachesRecorder() throws Exception {
        String owner = mapping().get("mouseHelper.class");
        Patch patch = named(Patches.all(mapping()), "mouse-delta");
        byte[] patched = Patcher.apply(FakeGame.mouseHelper(owner, 7, -3), Collections.singletonList(patch));
        assertTrue(patch.applied());
        Class<?> c = new FakeGame().add(owner, patched).loadClass(owner.replace('/', '.'));
        long before = RecorderProbe.frames();
        c.getMethod("func_74374_c").invoke(c.newInstance());
        assertEquals(before + 1, RecorderProbe.frames());
        assertEquals(7, RecorderProbe.lastFrameDx());
        assertEquals(-3, RecorderProbe.lastFrameDy());
    }

    @Test
    public void mouseDeltaSkipsWhenFieldsMissing() throws Exception {
        String owner = mapping().get("mouseHelper.class");
        Patch patch = new MouseDeltaPatch(owner, "func_74374_c", "()V", "nope", "field_74375_b");
        assertNull(Patcher.apply(FakeGame.mouseHelper(owner, 1, 1), Collections.singletonList(patch)));
        assertFalse(patch.applied());
    }

    @Test
    public void movementRecordsRotation() throws Exception {
        Mapping m = mapping();
        Probes.configure(m.get("entity.yaw"), m.get("entity.pitch"));
        Patch patch = named(Patches.all(m), "movement");
        byte[] patched = Patcher.apply(FakeGame.player(), Collections.singletonList(patch));
        FakeGame game = new FakeGame().add("net/minecraft/entity/Entity", FakeGame.entity())
            .add("net/minecraft/client/entity/EntityPlayerSP", patched);
        Class<?> c = game.loadClass("net.minecraft.client.entity.EntityPlayerSP");
        Object player = c.newInstance();
        c.getField("field_70177_z").setFloat(player, 123.25f);
        c.getField("field_70125_A").setFloat(player, -45.5f);
        long before = RecorderProbe.ticks();
        c.getMethod("func_175161_p").invoke(player);
        assertEquals(before + 1, RecorderProbe.ticks());
        assertEquals(123.25f, RecorderProbe.lastYaw(), 0f);
        assertEquals(-45.5f, RecorderProbe.lastPitch(), 0f);
    }

    /**
     * O jogo de verdade vem com stack map frames. Se o Patcher ler a classe sem
     * EXPAND_FRAMES, o LocalVariablesSorter dentro do PatchAdapter rejeita o frame
     * comprimido e o patch nao aplica -- sem quebrar o jogo, so deixando o agente cego.
     */
    @Test
    public void patchesClassThatHasStackMapFrames() throws Exception {
        Patch patch = named(Patches.all(mapping()), "movement");
        byte[] patched = Patcher.apply(FakeGame.playerWithFrames(), Collections.singletonList(patch));
        assertTrue(patch.applied());
        // Carregar e inicializar forca o verificador da JVM a conferir o StackMapTable
        // que saiu do patch. Nao invoca func_175161_p de proposito: o Probes guarda o
        // Field da primeira Entity que ve, e este teste traz a sua propria num
        // classloader separado -- invocar aqui envenenaria o cache para os outros testes.
        FakeGame game = new FakeGame().add("net/minecraft/entity/Entity", FakeGame.entity())
            .add("net/minecraft/client/entity/EntityPlayerSP", patched);
        Class<?> c = game.loadClass("net.minecraft.client.entity.EntityPlayerSP");
        assertNotNull(c.newInstance());
    }

    @Test
    public void launchClassLoaderGetsExclusion() throws Exception {
        assertExclusion(LoaderExclusionPatch.OWNER, Patches.all(mapping()));
    }

    @Test
    public void mappingCanNameEveryLoaderToExclude() throws Exception {
        Properties p = new Properties();
        p.load(PatchesTest.class.getResourceAsStream("/mappings/forge-1.8.9.properties"));
        p.setProperty("loader.classes", LoaderExclusionPatch.OWNER + ",cpw/mods/fml/relauncher/RelaunchClassLoader");
        List<Patch> patches = Patches.all(Mapping.of("test", p));
        assertExclusion("cpw/mods/fml/relauncher/RelaunchClassLoader", patches);
        assertExclusion(LoaderExclusionPatch.OWNER, patches);
    }

    private static void assertExclusion(String owner, List<Patch> patches) throws Exception {
        byte[] patched = Patcher.apply(FakeGame.launchClassLoader(owner), patches);
        Class<?> c = new FakeGame().add(owner, patched).loadClass(owner.replace('/', '.'));
        c.newInstance();
        assertEquals(LoaderExclusionPatch.PREFIX, c.getField("excluded").get(null));
    }
}
