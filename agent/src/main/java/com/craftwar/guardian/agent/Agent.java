package com.craftwar.guardian.agent;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;

import com.craftwar.guardian.agent.integrity.IntegrityMonitor;
import com.craftwar.guardian.agent.patch.Patch;
import com.craftwar.guardian.agent.patch.Patches;
import com.craftwar.guardian.agent.probe.Probes;
import com.craftwar.guardian.agent.session.Session;

public final class Agent {

    public static final String VERSION = "0.1.0";

    private Agent() {
    }

    public static void premain(String args, Instrumentation instrumentation) {
        try {
            AgentConfig config = AgentConfig.parse(args);
            List<Patch> patches = patchesFor(config);
            IntegrityMonitor integrity = new IntegrityMonitor(instrumentation, CodeRoots.fromEnvironment());
            instrumentation.addTransformer(new GuardianTransformer(patches, integrity), true);
            Session session = new Session(config, integrity, patches);
            Bridge.install(session);
            session.start();
        } catch (Throwable t) {
            Health.disable("premain failed: " + t);
        }
    }

    private static List<Patch> patchesFor(AgentConfig config) {
        try {
            Mapping mapping = Mapping.load(config.mapping());
            Probes.configure(mapping.get("entity.yaw"), mapping.get("entity.pitch"));
            return Patches.all(mapping);
        } catch (Throwable t) {
            Health.degrade("mapping unavailable: " + t.getMessage());
            return Collections.emptyList();
        }
    }
}
