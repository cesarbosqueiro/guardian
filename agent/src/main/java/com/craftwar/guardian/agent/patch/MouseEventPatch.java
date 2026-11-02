package com.craftwar.guardian.agent.patch;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class MouseEventPatch extends Patch {

    public MouseEventPatch(String owner) {
        super("mouse-event", owner, "next", "()Z");
    }

    @Override
    protected void onReturn(MethodVisitor mv, int opcode) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Patches.PROBES, "mouseEvent", "(Z)Z", false);
    }
}
