package com.craftwar.guardian.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.craftwar.guardian.agent.integrity.IntegrityMonitor;
import com.craftwar.guardian.agent.patch.Patch;
import com.craftwar.guardian.agent.patch.Patcher;

final class GuardianTransformer implements ClassFileTransformer {

    private final Map<String, List<Patch>> byOwner = new HashMap<String, List<Patch>>();
    private final IntegrityMonitor integrity;

    GuardianTransformer(List<Patch> patches, IntegrityMonitor integrity) {
        this.integrity = integrity;
        for (Patch p : patches) {
            List<Patch> list = byOwner.get(p.owner());
            if (list == null) byOwner.put(p.owner(), list = new ArrayList<Patch>(2));
            list.add(p);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> redefined, ProtectionDomain domain, byte[] bytes) {
        integrity.observe(loader, className, redefined, domain);
        if (className == null) return null;
        List<Patch> patches = byOwner.get(className);
        if (patches == null) return null;
        try {
            return Patcher.apply(bytes, patches);
        } catch (Throwable t) {
            Health.degrade("patch failed for " + className + ": " + t);
            return null;
        }
    }
}
