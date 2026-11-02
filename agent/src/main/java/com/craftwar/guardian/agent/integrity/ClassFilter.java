package com.craftwar.guardian.agent.integrity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ClassFilter {

    private static final List<String> DEFAULT_PREFIXES = Arrays.asList(
        "sun.reflect.Generated",
        "sun.reflect.GeneratedSerializationConstructorAccessor",
        "jdk.internal.reflect.Generated",
        "com.sun.proxy.$Proxy",
        "java.lang.invoke.LambdaForm$",
        "net.minecraftforge.fml.common.eventhandler.ASMEventHandler_");
    private static final String LAMBDA_MARKER = "$$Lambda$";

    private final String[] prefixes;

    private ClassFilter(List<String> prefixes) {
        this.prefixes = prefixes.toArray(new String[0]);
    }

    public static ClassFilter defaults() {
        return new ClassFilter(DEFAULT_PREFIXES);
    }

    public static ClassFilter withExtra(List<String> extra) {
        List<String> all = new ArrayList<String>(DEFAULT_PREFIXES);
        all.addAll(extra);
        return new ClassFilter(all);
    }

    public boolean ignored(String dottedName) {
        if (dottedName.contains(LAMBDA_MARKER)) return true;
        for (String p : prefixes) {
            if (dottedName.startsWith(p)) return true;
        }
        return false;
    }
}
