package com.craftwar.guardian.agent.integrity;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.craftwar.guardian.agent.CodeRoots;
import com.craftwar.guardian.protocol.FindingKind;
import com.craftwar.guardian.protocol.message.Finding;

public final class IntegrityMonitor {

    private static final int MAX_REPORTED = 4096;
    private static volatile boolean ready;

    private final Instrumentation instrumentation;
    private final CodeRoots roots;
    private final ArrayBlockingQueue<Observation> queue = new ArrayBlockingQueue<Observation>(512);
    private final AtomicInteger dropped = new AtomicInteger();
    private final Set<String> reported = new HashSet<String>();
    private volatile ClassFilter filter = ClassFilter.defaults();
    private volatile int loadedClasses;

    public IntegrityMonitor(Instrumentation instrumentation, CodeRoots roots) {
        this.instrumentation = instrumentation;
        this.roots = roots;
    }

    public static void markReady() {
        if (!ready) ready = true;
    }

    public void filter(ClassFilter f) {
        filter = f;
    }

    public int loadedClasses() {
        return loadedClasses;
    }

    public void observe(ClassLoader loader, String internalName, Class<?> redefined, ProtectionDomain domain) {
        if (!ready || loader == null) return;
        URL location = locationOf(domain);
        boolean redefinition = redefined != null;
        if (!redefinition && location != null) return;
        String name = internalName == null ? (redefined == null ? "<anonymous>" : redefined.getName()) : internalName.replace('/', '.');
        if (!queue.offer(new Observation(redefinition, name, loader.getClass().getName(), location))) dropped.incrementAndGet();
    }

    public List<Finding> drain() {
        List<Finding> out = new ArrayList<Finding>();
        ClassFilter f = filter;
        for (Observation o; (o = queue.poll()) != null; ) {
            if (o.redefinition) add(out, FindingKind.REDEFINITION, o.className, o.loader);
            else if (!f.ignored(o.className)) add(out, FindingKind.ORPHAN_CLASS, o.className, o.loader);
        }
        int lost = dropped.getAndSet(0);
        if (lost > 0) out.add(new Finding(FindingKind.FINDINGS_DROPPED, "queue", Integer.toString(lost)));
        return out;
    }

    public List<Finding> scan() {
        List<Finding> out = new ArrayList<Finding>();
        Class<?>[] classes = instrumentation.getAllLoadedClasses();
        loadedClasses = classes.length;
        ClassFilter f = filter;
        for (Class<?> c : classes) {
            ClassLoader loader = c.getClassLoader();
            if (loader == null) continue;
            String name = c.getName();
            if (f.ignored(name)) continue;
            URL location = locationOf(c.getProtectionDomain());
            if (location == null) add(out, FindingKind.ORPHAN_CLASS, name, loader.getClass().getName());
            else if (!roots.trusted(location)) add(out, FindingKind.FOREIGN_SOURCE, String.valueOf(location), name);
        }
        return out;
    }

    private void add(List<Finding> out, int kind, String subject, String detail) {
        if (reported.size() >= MAX_REPORTED || !reported.add(kind + ":" + subject)) return;
        out.add(new Finding(kind, subject, detail));
    }

    private static URL locationOf(ProtectionDomain domain) {
        if (domain == null) return null;
        CodeSource source = domain.getCodeSource();
        return source == null ? null : source.getLocation();
    }
}
