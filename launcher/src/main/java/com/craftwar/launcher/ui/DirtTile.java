package com.craftwar.launcher.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

final class DirtTile {

    static final int SIZE = 32;
    private static final int TEXELS = 16;
    private static final long SEED = 0x43574449L;
    private static final int[] PALETTE = {0x593D29, 0x6C4A32, 0x79553A, 0x86603F, 0x966C4A, 0x4F3624, 0x8B6948};

    private DirtTile() {
    }

    static BufferedImage load(File launcherDir) {
        BufferedImage custom = readCustom(new File(launcherDir, "dirt.png"));
        return scale(custom != null ? custom : procedural());
    }

    private static BufferedImage readCustom(File file) {
        if (!file.isFile()) return null;
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage procedural() {
        BufferedImage img = new BufferedImage(TEXELS, TEXELS, BufferedImage.TYPE_INT_RGB);
        Random r = new Random(SEED);
        for (int y = 0; y < TEXELS; y++) {
            for (int x = 0; x < TEXELS; x++) {
                int base = PALETTE[r.nextInt(PALETTE.length)];
                img.setRGB(x, y, r.nextInt(9) == 0 ? darken(base) : base);
            }
        }
        return img;
    }

    private static int darken(int rgb) {
        return ((rgb >> 1) & 0x7F7F7F);
    }

    private static BufferedImage scale(BufferedImage src) {
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, SIZE, SIZE, null);
        g.dispose();
        return out;
    }
}
