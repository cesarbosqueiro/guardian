package com.craftwar.guardian.agent.patch;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

final class PatchAdapter extends AdviceAdapter {

    private final Patch patch;

    PatchAdapter(MethodVisitor next, int access, String name, String descriptor, Patch patch) {
        super(Opcodes.ASM9, next, access, name, descriptor);
        this.patch = patch;
    }

    @Override
    protected void onMethodEnter() {
        patch.onEnter(mv);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != ATHROW) patch.onReturn(mv, opcode);
    }
}
