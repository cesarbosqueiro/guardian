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
        // EXPAND_FRAMES porque o PatchAdapter herda de LocalVariablesSorter, que recusa
        // frame comprimido. COMPUTE_MAXS e nao COMPUTE_FRAMES de proposito: recalcular
        // frames faz o ClassWriter carregar classes para achar supertipo comum, e um
        // agente carregando classes no meio do startup do jogo da ClassCircularityError.
        // Nenhum patch cria salto nem aloca local, entao os frames continuam validos.
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return visitor.changed() ? writer.toByteArray() : null;
    }
}
