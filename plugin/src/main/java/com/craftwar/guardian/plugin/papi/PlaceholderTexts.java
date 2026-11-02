package com.craftwar.guardian.plugin.papi;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public final class PlaceholderTexts {

    private static final String ROOT = "placeholders.";

    public final String attestedYes;
    public final String attestedNo;
    public final String tagYes;
    public final String tagNo;
    public final String agentOk;
    public final String agentDegraded;
    public final String agentDisabled;
    public final String agentMissing;

    public PlaceholderTexts(FileConfiguration c) {
        attestedYes = text(c, "attested-yes", "sim");
        attestedNo = text(c, "attested-no", "nao");
        tagYes = text(c, "tag-attested", "&a✔");
        tagNo = text(c, "tag-not-attested", "&c✖");
        agentOk = text(c, "agent-ok", "OK");
        agentDegraded = text(c, "agent-degraded", "DEGRADADO");
        agentDisabled = text(c, "agent-disabled", "DESLIGADO");
        agentMissing = text(c, "agent-missing", "SEM AGENTE");
    }

    private static String text(FileConfiguration c, String key, String fallback) {
        return ChatColor.translateAlternateColorCodes('&', c.getString(ROOT + key, fallback));
    }

    String agent(int state) {
        switch (state) {
            case 0: return agentOk;
            case 1: return agentDegraded;
            case 2: return agentDisabled;
            default: return agentMissing;
        }
    }
}
