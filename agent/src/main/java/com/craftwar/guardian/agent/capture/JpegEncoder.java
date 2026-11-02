package com.craftwar.guardian.agent.capture;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

final class JpegEncoder {

    private static final int MAX_WIDTH = 1280;
    private static final float QUALITY = 0.7f;

    private JpegEncoder() {
    }

    static byte[] encode(BufferedImage image) throws IOException {
        BufferedImage scaled = downscale(image);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IOException("no jpeg writer");
        ImageWriter writer = writers.next();
        ByteArrayOutputStream out = new ByteArrayOutputStream(256 * 1024);
        ImageIO.setUseCache(false);
        ImageOutputStream stream = ImageIO.createImageOutputStream(out);
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(QUALITY);
            writer.setOutput(stream);
            writer.write(null, new IIOImage(scaled, null, null), param);
        } finally {
            writer.dispose();
            stream.close();
        }
        return out.toByteArray();
    }

    private static BufferedImage downscale(BufferedImage image) {
        if (image.getWidth() <= MAX_WIDTH) return image;
        int h = (int) ((long) image.getHeight() * MAX_WIDTH / image.getWidth());
        BufferedImage out = new BufferedImage(MAX_WIDTH, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, 0, 0, MAX_WIDTH, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }
}
