package com.craftwar.launcher.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;

final class RetroBorder extends AbstractBorder {

    private static final long serialVersionUID = 1L;
    private static final Insets INSETS = new Insets(12, 24, 16, 32);

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w - 1, h - 1);
        g.drawRect(x + 1, y + 1, w - 3, h - 3);
        g.setColor(Color.WHITE);
        g.drawRect(x + 2, y + 2, w - 5, h - 5);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return (Insets) INSETS.clone();
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(INSETS.top, INSETS.left, INSETS.bottom, INSETS.right);
        return insets;
    }
}
