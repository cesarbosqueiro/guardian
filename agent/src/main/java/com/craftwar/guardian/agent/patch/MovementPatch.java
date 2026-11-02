package com.craftwar.guardian.agent.patch;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class MovementPatch extends Patch {

    public MovementPatch(String owner, String method, String descriptor) {
        super("movement", owner, method, descriptor);
    }

    @Override
    protected void onEnter(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Patches.PROBES, "tick", "(Ljava/lang/Object;)V", false);
    }
}
