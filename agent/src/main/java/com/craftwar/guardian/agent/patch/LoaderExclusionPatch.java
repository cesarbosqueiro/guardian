package com.craftwar.guardian.agent.patch;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class LoaderExclusionPatch extends Patch {

    public static final String OWNER = "net/minecraft/launchwrapper/LaunchClassLoader";
    public static final String PREFIX = "com.craftwar.guardian.";

    public LoaderExclusionPatch() {
        super("loader-exclusion", OWNER, "<init>", null);
    }

    @Override
    protected void onReturn(MethodVisitor mv, int opcode) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn(PREFIX);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, OWNER, "addClassLoaderExclusion", "(Ljava/lang/String;)V", false);
    }
}
