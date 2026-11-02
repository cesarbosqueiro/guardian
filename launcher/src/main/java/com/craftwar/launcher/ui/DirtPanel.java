package com.craftwar.launcher.ui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

final class DirtPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient BufferedImage tile;
    private final String title;
    private transient BufferedImage half;

    DirtPanel(BufferedImage tile, String title) {
        super(new GridBagLayout());
        this.tile = tile;
        this.title = title;
        setPreferredSize(new Dimension(Theme.WIDTH, Theme.HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth() / 2 + 1;
        int h = getHeight() / 2 + 1;
        if (half == null || half.getWidth() != w || half.getHeight() != h) {
            half = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            render(half);
        }
        g.drawImage(half, 0, 0, w * 2, h * 2, null);
    }

    private void render(BufferedImage target) {
        Graphics2D g = target.createGraphics();
        int w = target.getWidth();
        int h = target.getHeight();
        for (int x = 0; x < w; x += DirtTile.SIZE) {
            for (int y = 0; y < h; y += DirtTile.SIZE) g.drawImage(tile, x, y, null);
        }
        g.setColor(Theme.TITLE);
        g.setFont(Theme.TITLE_FONT);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, w / 2 - fm.stringWidth(title) / 2, h / 2 - fm.getHeight() * 2);
        g.dispose();
    }
}
