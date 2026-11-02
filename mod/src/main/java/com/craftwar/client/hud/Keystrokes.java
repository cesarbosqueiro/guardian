package com.craftwar.client.hud;

import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

final class Keystrokes {

    private static final int KEY = 18;
    private static final int GAP = 2;
    private static final int IDLE = 0x80000000;
    private static final int PRESSED = 0xC0FFFFFF;
    private static final int TEXT_IDLE = 0xFFFFFFFF;
    private static final int TEXT_PRESSED = 0xFF000000;

    void draw(Minecraft mc, FontRenderer font, int x, int y) {
        GameSettings s = mc.gameSettings;
        int step = KEY + GAP;
        key(font, "W", s.keyBindForward, x + step, y);
        key(font, "A", s.keyBindLeft, x, y + step);
        key(font, "S", s.keyBindBack, x + step, y + step);
        key(font, "D", s.keyBindRight, x + 2 * step, y + step);
        int wide = (3 * KEY + 2 * GAP - GAP) / 2;
        box(font, "LMB", Mouse.isButtonDown(0), x, y + 2 * step, wide);
        box(font, "RMB", Mouse.isButtonDown(1), x + wide + GAP, y + 2 * step, wide);
    }

    private static void key(FontRenderer font, String label, KeyBinding binding, int x, int y) {
        box(font, label, binding.isKeyDown(), x, y, KEY);
    }

    private static void box(FontRenderer font, String label, boolean down, int x, int y, int width) {
        Gui.drawRect(x, y, x + width, y + KEY, down ? PRESSED : IDLE);
        int tx = x + (width - font.getStringWidth(label)) / 2;
        font.drawString(label, tx, y + (KEY - 8) / 2, down ? TEXT_PRESSED : TEXT_IDLE);
    }
}
