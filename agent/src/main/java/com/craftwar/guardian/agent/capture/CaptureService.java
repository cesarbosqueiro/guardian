package com.craftwar.guardian.agent.capture;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.lwjgl.opengl.Display;

public final class CaptureService {

    private static final long TIMEOUT_NANOS = 3_000_000_000L;
    private static final long POLL_MILLIS = 5;

    private final GameCapture gl;

    public CaptureService(GameCapture gl) {
        this.gl = gl;
    }

    public byte[] captureGame() throws CaptureAbortedException {
        recoverStale();
        if (!Display.isActive()) throw new CaptureAbortedException("game not focused");
        int w = Display.getWidth();
        int h = Display.getHeight();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        if (!gl.request(pixels, w, h)) throw new CaptureAbortedException("capture busy");
        awaitGl();
        Frames.flipVertically(pixels, w, h);
        return encode(image);
    }

    public byte[] captureWindow() throws CaptureAbortedException {
        if (!Display.isActive()) throw new CaptureAbortedException("game not focused");
        Rectangle area = new Rectangle(Display.getX(), Display.getY(), Display.getWidth(), Display.getHeight());
        BufferedImage image;
        try {
            image = new Robot().createScreenCapture(area);
        } catch (Exception e) {
            throw new CaptureAbortedException("robot unavailable: " + e);
        }
        if (!Display.isActive()) throw new CaptureAbortedException("focus lost during capture");
        return encode(image);
    }

    private void awaitGl() throws CaptureAbortedException {
        long deadline = System.nanoTime() + TIMEOUT_NANOS;
        while (gl.state() < GameCapture.DONE) {
            if (System.nanoTime() - deadline > 0) throw new CaptureAbortedException("timeout");
            sleep();
        }
        String failure = gl.failure();
        int state = gl.state();
        gl.release();
        if (state == GameCapture.FAILED) throw new CaptureAbortedException(failure);
    }

    private void recoverStale() {
        if (gl.state() >= GameCapture.DONE) gl.release();
    }

    private static byte[] encode(BufferedImage image) throws CaptureAbortedException {
        try {
            return JpegEncoder.encode(image);
        } catch (Exception e) {
            throw new CaptureAbortedException("encode failed: " + e);
        }
    }

    private static void sleep() throws CaptureAbortedException {
        try {
            Thread.sleep(POLL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CaptureAbortedException("interrupted");
        }
    }
}
