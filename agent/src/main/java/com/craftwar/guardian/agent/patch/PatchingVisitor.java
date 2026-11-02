package com.craftwar.guardian.agent.patch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class PatchingVisitor extends ClassVisitor {

    private final List<Patch> patches;
    private final Set<String> fields = new HashSet<String>();
    private boolean changed;

    PatchingVisitor(ClassVisitor next, List<Patch> patches) {
        super(Opcodes.ASM9, next);
        this.patches = patches;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        fields.add(name + ':' + descriptor);
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) return mv;
        for (Patch p : patches) {
            if (p.matches(name, descriptor) && fields.containsAll(p.requiredFields())) {
                mv = new PatchAdapter(mv, access, name, descriptor, p);
                p.markApplied();
                changed = true;
            }
        }
        return mv;
    }

    boolean changed() {
        return changed;
    }
}
