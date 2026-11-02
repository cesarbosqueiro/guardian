package com.craftwar.guardian.plugin.papi;

import org.bukkit.plugin.Plugin;

public final class PlaceholderHook {

    private static final String PLACEHOLDER_API = "PlaceholderAPI";

    private PlaceholderHook() {
    }

    public static boolean install(Plugin plugin, BadgeBoard board, PlaceholderTexts texts) {
        Plugin papi = plugin.getServer().getPluginManager().getPlugin(PLACEHOLDER_API);
        if (papi == null || !papi.isEnabled()) return false;
        return new GuardianExpansion(board, texts, plugin.getDescription().getVersion()).register();
    }
}
