package com.craftwar.client.hud;

import net.minecraftforge.common.config.Configuration;

public final class ClientSettings {

    private static final String HUD = "hud";

    public final boolean showFps;
    public final boolean showPing;
    public final boolean showCps;
    public final boolean showKeystrokes;
    public final boolean toggleSprintEnabled;
    public final int x;
    public final int y;

    public ClientSettings(Configuration c) {
        c.load();
        showFps = c.getBoolean("fps", HUD, true, "Mostrar FPS");
        showPing = c.getBoolean("ping", HUD, true, "Mostrar ping");
        showCps = c.getBoolean("cps", HUD, true, "Mostrar cliques por segundo");
        showKeystrokes = c.getBoolean("keystrokes", HUD, true, "Mostrar teclas WASD e cliques");
        toggleSprintEnabled = c.getBoolean("toggleSprint", HUD, true, "Permitir toggle sprint");
        x = c.getInt("x", HUD, 4, 0, 4000, "Posicao horizontal do HUD");
        y = c.getInt("y", HUD, 4, 0, 4000, "Posicao vertical do HUD");
        if (c.hasChanged()) c.save();
    }
}
