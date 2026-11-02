package com.craftwar.guardian.agent.patch;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class SwapPatch extends Patch {

    public SwapPatch(String owner) {
        super("swap", owner, "update", "(Z)V");
    }

    @Override
    protected void onEnter(MethodVisitor mv) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Patches.PROBES, "beforeSwap", "()V", false);
    }
}
