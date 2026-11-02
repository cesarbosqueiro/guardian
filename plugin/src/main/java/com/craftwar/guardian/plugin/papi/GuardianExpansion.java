package com.craftwar.guardian.plugin.papi;

import org.bukkit.OfflinePlayer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

final class GuardianExpansion extends PlaceholderExpansion {

    private final BadgeBoard board;
    private final PlaceholderTexts texts;
    private final String version;

    GuardianExpansion(BadgeBoard board, PlaceholderTexts texts, String version) {
        this.board = board;
        this.texts = texts;
        this.version = version;
    }

    @Override
    public String getIdentifier() {
        return "guardian";
    }

    @Override
    public String getAuthor() {
        return "CraftWar";
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        Badge b = board.get(player.getUniqueId());
        switch (params.toLowerCase()) {
            case "attested": return b.attested ? texts.attestedYes : texts.attestedNo;
            case "tag": return b.attested ? texts.tagYes : texts.tagNo;
            case "agent": return texts.agent(b.agentState);
            case "score": return Long.toString(b.score);
            default: return null;
        }
    }
}
