package com.craftwar.guardian.agent.patch;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class MouseDeltaPatch extends Patch {

    private final String owner;
    private final String deltaX;
    private final String deltaY;
    private final Set<String> required = new HashSet<String>();

    public MouseDeltaPatch(String owner, String method, String descriptor, String deltaX, String deltaY) {
        super("mouse-delta", owner, method, descriptor);
        this.owner = owner;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        required.add(deltaX + ":I");
        required.add(deltaY + ":I");
    }

    @Override
    protected Set<String> requiredFields() {
        return required;
    }

    @Override
    protected void onReturn(MethodVisitor mv, int opcode) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, owner, deltaX, "I");
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, owner, deltaY, "I");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Patches.PROBES, "mouse", "(II)V", false);
    }
}
