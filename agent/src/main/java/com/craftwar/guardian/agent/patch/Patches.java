package com.craftwar.guardian.agent.patch;

import java.util.ArrayList;
import java.util.List;

import com.craftwar.guardian.agent.Mapping;

public final class Patches {

    public static final String PROBES = "com/craftwar/guardian/agent/probe/Probes";
    private static final String MOUSE = "org/lwjgl/input/Mouse";
    private static final String DISPLAY = "org/lwjgl/opengl/Display";

    private Patches() {
    }

    public static List<Patch> all(Mapping m) {
        List<Patch> out = new ArrayList<Patch>();
        String exclusion = m.get("loader.exclusionMethod", LoaderExclusionPatch.METHOD);
        for (String loader : m.get("loader.classes", LoaderExclusionPatch.OWNER).split(",")) {
            if (!loader.trim().isEmpty()) out.add(new LoaderExclusionPatch(loader.trim(), exclusion));
        }
        out.add(new MouseEventPatch(m.get("mouse.class", MOUSE)));
        out.add(new SwapPatch(m.get("display.class", DISPLAY)));
        out.add(new MouseDeltaPatch(m.get("mouseHelper.class"), m.get("mouseHelper.update"), m.get("mouseHelper.update.desc"),
            m.get("mouseHelper.deltaX"), m.get("mouseHelper.deltaY")));
        out.add(new MovementPatch(m.get("player.class"), m.get("player.sendMovement"), m.get("player.sendMovement.desc")));
        return out;
    }
}
