package com.craftwar.guardian.agent.patch;

import java.util.Arrays;
import java.util.List;

import com.craftwar.guardian.agent.Mapping;

public final class Patches {

    public static final String PROBES = "com/craftwar/guardian/agent/probe/Probes";

    private Patches() {
    }

    public static List<Patch> all(Mapping m) {
        return Arrays.<Patch>asList(
            new LoaderExclusionPatch(),
            new MouseEventPatch(),
            new SwapPatch(),
            new MouseDeltaPatch(m.get("mouseHelper.class"), m.get("mouseHelper.update"), m.get("mouseHelper.update.desc"),
                m.get("mouseHelper.deltaX"), m.get("mouseHelper.deltaY")),
            new MovementPatch(m.get("player.class"), m.get("player.sendMovement"), m.get("player.sendMovement.desc")));
    }
}
