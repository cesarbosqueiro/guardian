package com.craftwar.guardian.agent.session;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.craftwar.guardian.agent.Agent;
import com.craftwar.guardian.agent.Environment;
import com.craftwar.guardian.agent.GameState;
import com.craftwar.guardian.agent.Health;
import com.craftwar.guardian.protocol.FindingKind;
import com.craftwar.guardian.protocol.message.Context;
import com.craftwar.guardian.protocol.message.Finding;

final class ContextFactory {

    private static final String[] SUSPICIOUS_ARGUMENTS = {"-agentpath", "-agentlib", "-Xbootclasspath", "-Xverify:none", "-noverify"};
    private static final String OWN_AGENT = "guardian-agent";

    private ContextFactory() {
    }

    static List<String> jvmArguments() {
        try {
            return ManagementFactory.getRuntimeMXBean().getInputArguments();
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    static Context build(String mapping, List<String> jvmArguments) {
        Environment env = Environment.current();
        List<String> none = Collections.emptyList();
        return new Context(Agent.VERSION, mapping, Health.state(), System.getProperty("java.version"),
            System.getProperty("os.name") + " " + System.getProperty("os.arch"),
            env == null ? "" : env.gpu, env == null ? 0 : env.width, env == null ? 0 : env.height,
            GameState.sensitivity(), GameState.smoothCamera(), GameState.gamma(),
            jvmArguments, env == null ? none : env.mods, env == null ? none : env.resourcePacks);
    }

    static List<Finding> suspiciousArguments(List<String> jvmArguments) {
        List<Finding> out = new ArrayList<Finding>();
        for (String arg : jvmArguments) {
            if (isSuspicious(arg)) out.add(new Finding(FindingKind.JVM_ARGUMENT, arg, ""));
        }
        return out;
    }

    private static boolean isSuspicious(String arg) {
        if (arg.startsWith("-javaagent")) return !arg.contains(OWN_AGENT);
        for (String s : SUSPICIOUS_ARGUMENTS) {
            if (arg.startsWith(s)) return true;
        }
        return false;
    }
}
