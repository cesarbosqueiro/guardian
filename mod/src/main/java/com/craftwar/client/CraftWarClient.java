package com.craftwar.client;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.craftwar.client.guardian.GuardianLink;
import com.craftwar.client.hud.ClientSettings;
import com.craftwar.client.hud.ClicksPerSecond;
import com.craftwar.client.hud.HudRenderer;
import com.craftwar.client.hud.ToggleSprint;
import com.craftwar.client.servers.ServerListLock;

@Mod(modid = CraftWarClient.ID, name = "CraftWar Client", version = "0.1.0", clientSideOnly = true, acceptableRemoteVersions = "*")
public final class CraftWarClient {

    public static final String ID = "craftwarclient";

    private ClientSettings settings;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        settings = new ClientSettings(new Configuration(e.getSuggestedConfigurationFile()));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        ClicksPerSecond cps = new ClicksPerSecond();
        ToggleSprint sprint = new ToggleSprint(settings);
        MinecraftForge.EVENT_BUS.register(cps);
        MinecraftForge.EVENT_BUS.register(sprint);
        MinecraftForge.EVENT_BUS.register(new HudRenderer(settings, cps, sprint));
        MinecraftForge.EVENT_BUS.register(new ServerListLock());
        GuardianLink link = GuardianLink.create();
        if (link != null) MinecraftForge.EVENT_BUS.register(link);
    }
}
