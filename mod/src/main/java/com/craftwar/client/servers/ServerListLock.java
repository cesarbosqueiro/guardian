package com.craftwar.client.servers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreenAddServer;
import net.minecraft.client.gui.GuiScreenServerList;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ServerListLock {

    private static final String SERVERS = "servers.dat";
    private static final String LOCK = "guardian/servers.lock";
    private static final int DELETE = 2;
    private static final int ADD = 3;
    private static final int DIRECT = 4;
    private static final int EDIT = 7;

    @SubscribeEvent
    public void onOpen(GuiOpenEvent e) {
        if (e.gui instanceof GuiScreenAddServer || e.gui instanceof GuiScreenServerList) {
            e.setCanceled(true);
            return;
        }
        if (e.gui instanceof GuiMultiplayer) restore();
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post e) {
        if (!(e.gui instanceof GuiMultiplayer)) return;
        for (int i = e.buttonList.size() - 1; i >= 0; i--) {
            GuiButton b = e.buttonList.get(i);
            if (b.id == ADD || b.id == DIRECT || b.id == EDIT || b.id == DELETE) e.buttonList.remove(i);
        }
    }

    private static void restore() {
        File dir = Minecraft.getMinecraft().mcDataDir;
        File lock = new File(dir, LOCK);
        if (!lock.isFile()) return;
        try {
            Files.copy(lock.toPath(), new File(dir, SERVERS).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException keepCurrentList) {
            return;
        }
    }
}
