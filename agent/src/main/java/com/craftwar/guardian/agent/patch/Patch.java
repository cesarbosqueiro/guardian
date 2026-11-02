package com.craftwar.guardian.agent.patch;

import java.util.Collections;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;

public abstract class Patch {

    private final String name;
    private final String owner;
    private final String method;
    private final String descriptor;
    private volatile boolean applied;

    protected Patch(String name, String owner, String method, String descriptor) {
        this.name = name;
        this.owner = owner;
        this.method = method;
        this.descriptor = descriptor;
    }

    public final String name() {
        return name;
    }

    public final String owner() {
        return owner;
    }

    public final boolean applied() {
        return applied;
    }

    final void markApplied() {
        applied = true;
    }

    final boolean matches(String methodName, String methodDescriptor) {
        return method.equals(methodName) && (descriptor == null || descriptor.equals(methodDescriptor));
    }

    protected Set<String> requiredFields() {
        return Collections.emptySet();
    }

    protected void onEnter(MethodVisitor mv) {
    }

    protected void onReturn(MethodVisitor mv, int opcode) {
    }
}
