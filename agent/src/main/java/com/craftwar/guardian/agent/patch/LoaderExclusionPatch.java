package com.craftwar.guardian.agent.patch;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class LoaderExclusionPatch extends Patch {

    public static final String OWNER = "net/minecraft/launchwrapper/LaunchClassLoader";
    public static final String METHOD = "addClassLoaderExclusion";
    public static final String PREFIX = "com.craftwar.guardian.";

    private final String owner;
    private final String method;

    public LoaderExclusionPatch(String owner, String method) {
        super("loader-exclusion:" + owner, owner, "<init>", null);
        this.owner = owner;
        this.method = method;
    }

    @Override
    protected void onReturn(MethodVisitor mv, int opcode) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn(PREFIX);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, method, "(Ljava/lang/String;)V", false);
    }
}
