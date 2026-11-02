package com.craftwar.guardian.agent.patch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        Patch patch = named(Patches.all(mapping()), "mouse-delta");
        byte[] patched = Patcher.apply(FakeGame.mouseHelper(7, -3), Collections.singletonList(patch));
        assertTrue(patch.applied());
        Class<?> c = new FakeGame().add("net/minecraft/client/MouseHelper", patched).loadClass("net.minecraft.client.MouseHelper");
        long before = RecorderProbe.frames();
        c.getMethod("func_74374_c").invoke(c.newInstance());
        assertEquals(before + 1, RecorderProbe.frames());
        assertEquals(7, RecorderProbe.lastFrameDx());
        assertEquals(-3, RecorderProbe.lastFrameDy());
    }

    @Test
    public void mouseDeltaSkipsWhenFieldsMissing() throws Exception {
        Patch patch = new MouseDeltaPatch("net/minecraft/client/MouseHelper", "func_74374_c", "()V", "nope", "field_74375_b");
        assertNull(Patcher.apply(FakeGame.mouseHelper(1, 1), Collections.singletonList(patch)));
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

    @Test
    public void launchClassLoaderGetsExclusion() throws Exception {
        LoaderExclusionPatch patch = new LoaderExclusionPatch();
        byte[] patched = Patcher.apply(FakeGame.launchClassLoader(), Collections.<Patch>singletonList(patch));
        Class<?> c = new FakeGame().add(LoaderExclusionPatch.OWNER, patched).loadClass(LoaderExclusionPatch.OWNER.replace('/', '.'));
        c.newInstance();
        assertEquals(LoaderExclusionPatch.PREFIX, c.getField("excluded").get(null));
    }
}
