package com.craftwar.guardian.agent.patch;

import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public final class Patcher {

    private Patcher() {
    }

    public static byte[] apply(byte[] classFile, List<Patch> patches) {
        ClassReader reader = new ClassReader(classFile);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        PatchingVisitor visitor = new PatchingVisitor(writer, patches);
        reader.accept(visitor, 0);
        return visitor.changed() ? writer.toByteArray() : null;
    }
}
