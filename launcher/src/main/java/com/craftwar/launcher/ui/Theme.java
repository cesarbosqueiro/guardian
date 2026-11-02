package com.craftwar.launcher.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.Border;

final class Theme {

    static final Color BACKGROUND = new Color(0x12, 0x14, 0x18);
    static final Color SURFACE = new Color(0x1C, 0x1F, 0x26);
    static final Color TEXT = new Color(0xE8, 0xEA, 0xED);
    static final Color MUTED = new Color(0x9A, 0xA0, 0xA6);
    static final Color ACCENT = new Color(0xE5, 0x48, 0x3B);
    static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 26);
    static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font BUTTON = new Font(Font.SANS_SERIF, Font.BOLD, 15);

    private Theme() {
    }

    static Border padding(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    static void field(JComponent c) {
        c.setBackground(SURFACE);
        c.setForeground(TEXT);
        c.setFont(BODY);
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0x2C, 0x30, 0x3A)), padding(6, 8)));
    }

    static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFont(BUTTON);
        b.setFocusPainted(false);
        b.setBorder(padding(10, 24));
        b.setOpaque(true);
        return b;
    }
}
