package com.craftwar.guardian.agent.capture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GLContext;

public final class GameCapture {

    public static final GameCapture INSTANCE = new GameCapture();

    static final int IDLE = 0;
    static final int REQUESTED = 1;
    static final int READING = 2;
    static final int DONE = 3;
    static final int FAILED = 4;
    private static final int FRAMES_BEFORE_MAP = 2;

    private volatile int state = IDLE;
    private volatile String failure;
    private int[] target;
    private int width;
    private int height;
    private int pbo;
    private long pboBytes;
    private int framesLeft;

    boolean request(int[] pixels, int w, int h) {
        if (state != IDLE) return false;
        target = pixels;
        width = w;
        height = h;
        failure = null;
        state = REQUESTED;
        return true;
    }

    int state() {
        return state;
    }

    String failure() {
        return failure;
    }

    void release() {
        target = null;
        state = IDLE;
    }

    public void onFrame() {
        int s = state;
        if (s == IDLE || s >= DONE) return;
        if (s == REQUESTED) begin();
        else if (--framesLeft <= 0) finish();
    }

    private void begin() {
        if (!Display.isActive()) {
            fail("game not focused");
            return;
        }
        if (Display.getWidth() != width || Display.getHeight() != height) {
            fail("window resized");
            return;
        }
        if (GLContext.getCapabilities().OpenGL21) beginAsync();
        else readSync();
    }

    private void beginAsync() {
        long bytes = (long) width * height * 4;
        if (pbo == 0) pbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo);
        if (pboBytes != bytes) {
            GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, bytes, GL15.GL_STREAM_READ);
            pboBytes = bytes;
        }
        GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, 0L);
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
        framesLeft = FRAMES_BEFORE_MAP;
        state = READING;
    }

    private void finish() {
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo);
        ByteBuffer mapped = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_READ_ONLY, pboBytes, null);
        if (mapped == null) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            fail("map failed");
            return;
        }
        mapped.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(target, 0, width * height);
        GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
        state = DONE;
    }

    private void readSync() {
        ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buf);
        buf.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(target, 0, width * height);
        state = DONE;
    }

    private void fail(String reason) {
        failure = reason;
        state = FAILED;
    }
}
