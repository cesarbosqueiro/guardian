package com.craftwar.guardian.agent.patch;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class FakeGame extends ClassLoader {

    private final Map<String, byte[]> classes = new HashMap<String, byte[]>();

    FakeGame() {
        super(FakeGame.class.getClassLoader());
    }

    FakeGame add(String internalName, byte[] bytes) {
        classes.put(internalName.replace('/', '.'), bytes);
        return this;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] b = classes.get(name);
        if (b == null) throw new ClassNotFoundException(name);
        return defineClass(name, b, 0, b.length);
    }

    static byte[] mouseHelper(String owner, int dx, int dy) {
        ClassWriter cw = begin(owner, "java/lang/Object");
        cw.visitField(Opcodes.ACC_PUBLIC, "field_74377_a", "I", null, null).visitEnd();
        cw.visitField(Opcodes.ACC_PUBLIC, "field_74375_b", "I", null, null).visitEnd();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "func_74374_c", "()V", null, null);
        mv.visitCode();
        setInt(mv, owner, "field_74377_a", dx);
        setInt(mv, owner, "field_74375_b", dy);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return cw.toByteArray();
    }

    static byte[] entity() {
        ClassWriter cw = begin("net/minecraft/entity/Entity", "java/lang/Object");
        cw.visitField(Opcodes.ACC_PUBLIC, "field_70177_z", "F", null, null).visitEnd();
        cw.visitField(Opcodes.ACC_PUBLIC, "field_70125_A", "F", null, null).visitEnd();
        return cw.toByteArray();
    }

    static byte[] player() {
        ClassWriter cw = begin("net/minecraft/client/entity/EntityPlayerSP", "net/minecraft/entity/Entity");
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "func_175161_p", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Como {@link #player()}, mas com um salto e um stack map frame explicito.
     *
     * Classe compilada de verdade tem frames. As outras classes falsas daqui nao tem, e
     * sem elas o {@link PatchAdapter} -- que herda de LocalVariablesSorter -- nunca
     * recebe um visitFrame e portanto nunca cobra frames expandidos do ClassReader.
     */
    static byte[] playerWithFrames() {
        ClassWriter cw = begin("net/minecraft/client/entity/EntityPlayerSP", "net/minecraft/entity/Entity");
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "func_175161_p", "()V", null, null);
        mv.visitCode();
        Label skip = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitJumpInsn(Opcodes.IFNULL, skip);
        mv.visitLabel(skip);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return cw.toByteArray();
    }

    static byte[] launchClassLoader(String owner) {
        ClassWriter cw = begin(owner, "java/lang/Object");
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "excluded", "Ljava/lang/String;", null, null).visitEnd();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "addClassLoaderExclusion", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, "excluded", "Ljava/lang/String;");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return cw.toByteArray();
    }

    private static ClassWriter begin(String name, String superName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, superName, null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        return cw;
    }

    private static void setInt(MethodVisitor mv, String owner, String field, int value) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn(value);
        mv.visitFieldInsn(Opcodes.PUTFIELD, owner, field, "I");
    }
}
