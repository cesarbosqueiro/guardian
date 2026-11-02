package com.craftwar.client.guardian;

import java.util.List;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import com.craftwar.guardian.agent.Bridge;

final class GameStateReporter {

    private static final int FOCUS = 1;
    private static final int GUI = 1 << 1;
    private static final int SMOOTH = 1 << 2;

    private String gpu;

    void report(Minecraft mc) {
        GameSettings s = mc.gameSettings;
        int flags = (Display.isActive() ? FOCUS : 0) | (mc.currentScreen != null ? GUI : 0) | (s.smoothCamera ? SMOOTH : 0);
        Bridge.gameState(s.mouseSensitivity, s.smoothCamera, s.gammaSetting, Minecraft.getDebugFPS(), flags);
    }

    void publishEnvironment(Minecraft mc) {
        if (gpu == null) gpu = GL11.glGetString(GL11.GL_RENDERER) + " / " + GL11.glGetString(GL11.GL_VERSION);
        Bridge.environment(gpu, mc.displayWidth, mc.displayHeight, mods(), packs(mc));
    }

    private static String[] mods() {
        List<ModContainer> list = Loader.instance().getActiveModList();
        String[] out = new String[list.size()];
        for (int i = 0; i < out.length; i++) {
            ModContainer m = list.get(i);
            out[i] = m.getModId() + " " + m.getVersion() + (m.getSource() == null ? "" : " (" + m.getSource().getName() + ")");
        }
        return out;
    }

    private static String[] packs(Minecraft mc) {
        List<ResourcePackRepository.Entry> entries = mc.getResourcePackRepository().getRepositoryEntries();
        String[] out = new String[entries.size()];
        for (int i = 0; i < out.length; i++) out[i] = entries.get(i).getResourcePackName();
        return out;
    }
}
