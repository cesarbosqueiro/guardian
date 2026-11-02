package com.craftwar.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.craftwar.client.guardian.GuardianLink;

public final class HudRenderer {

    private static final int TEXT = 0xFFFFFFFF;
    private static final int LINE = 10;
    private static final int NOTICE_BACKGROUND = 0xC0B71C1C;

    private final ClientSettings settings;
    private final ClicksPerSecond cps;
    private final ToggleSprint sprint;
    private final Keystrokes keystrokes = new Keystrokes();
    private final CachedText fps = new CachedText("FPS ");
    private final CachedText ping = new CachedText("Ping ");
    private final CachedText clicks = new CachedText("CPS ", " | ");

    public HudRenderer(ClientSettings settings, ClicksPerSecond cps, ToggleSprint sprint) {
        this.settings = settings;
        this.cps = cps;
        this.sprint = sprint;
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo || mc.thePlayer == null) return;
        FontRenderer font = mc.fontRendererObj;
        int y = settings.y;
        if (settings.showFps) y = line(font, fps.of(Minecraft.getDebugFPS()), y);
        if (settings.showPing) y = line(font, ping.of(ping(mc)), y);
        if (settings.showCps) y = line(font, clicks.of(cps.left(), cps.right()), y);
        if (sprint.active()) y = line(font, "Sprint (toggle)", y);
        if (settings.showKeystrokes) keystrokes.draw(mc, font, settings.x, y + 2);
        notice(mc, font, e.resolution);
    }

    private int line(FontRenderer font, String text, int y) {
        font.drawStringWithShadow(text, settings.x, y, TEXT);
        return y + LINE;
    }

    private static int ping(Minecraft mc) {
        if (mc.getNetHandler() == null) return 0;
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return info == null ? 0 : info.getResponseTime();
    }

    private static void notice(Minecraft mc, FontRenderer font, ScaledResolution res) {
        String text = GuardianLink.notice();
        if (text == null) return;
        int w = font.getStringWidth(text) + 12;
        int x = (res.getScaledWidth() - w) / 2;
        Gui.drawRect(x, 4, x + w, 20, NOTICE_BACKGROUND);
        font.drawStringWithShadow(text, x + 6, 8, TEXT);
    }
}
