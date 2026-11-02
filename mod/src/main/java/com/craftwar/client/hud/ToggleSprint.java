package com.craftwar.client.hud;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class ToggleSprint {

    private final KeyBinding toggle = new KeyBinding("Toggle Sprint", Keyboard.KEY_V, "CraftWar");
    private final boolean allowed;
    private boolean active;

    public ToggleSprint(ClientSettings settings) {
        allowed = settings.toggleSprintEnabled;
        if (allowed) ClientRegistry.registerKeyBinding(toggle);
    }

    public boolean active() {
        return allowed && active;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (!allowed || e.phase != TickEvent.Phase.END) return;
        if (toggle.isPressed()) active = !active;
        Minecraft mc = Minecraft.getMinecraft();
        if (!active || mc.thePlayer == null || mc.currentScreen != null) return;
        KeyBinding sprint = mc.gameSettings.keyBindSprint;
        boolean forward = mc.gameSettings.keyBindForward.isKeyDown() && !mc.thePlayer.isSneaking();
        KeyBinding.setKeyBindState(sprint.getKeyCode(), forward);
    }
}
