package com.craftwar.launcher.ui;

import java.awt.Color;
import java.awt.Font;

final class Theme {

    static final int WIDTH = 854;
    static final int HEIGHT = 480;
    static final Color PANEL = new Color(0xC6, 0xC6, 0xC6);
    static final Color TEXT = Color.BLACK;
    static final Color TITLE = Color.LIGHT_GRAY;
    static final Color ERROR = new Color(128, 0, 0);
    static final Color PROGRESS = new Color(0x3C, 0x8D, 0x2F);
    static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    static final Font BODY = new Font(Font.DIALOG, Font.PLAIN, 12);
    static final Font ERROR_FONT = new Font(Font.DIALOG, Font.ITALIC, 16);

    private Theme() {
    }
}
