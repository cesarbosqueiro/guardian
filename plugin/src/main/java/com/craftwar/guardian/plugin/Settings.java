package com.craftwar.guardian.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class Settings {

    public final boolean enforceRestrictedMode;
    public final Set<String> restrictedWorlds;
    public final String restrictedMessage;
    public final long attestationTimeoutMillis;
    public final int heartbeatSeconds;
    public final int silenceHeartbeats;
    public final int inventorySeconds;
    public final List<String> ignoredClassPrefixes;
    public final int attackWithoutClickTolerance;
    public final double halfLifeMinutes;
    public final double alertScore;
    public final double reviewScore;
    public final double autoBoxMinWeight;
    public final long autoBoxCooldownMillis;
    public final long captureRetentionMillis;
    public final long blackBoxRetentionMillis;
    public final long heldRetentionMillis;
    public final boolean grimEnabled;
    public final String grimEventClass;
    private final Map<String, Double> weights = new HashMap<String, Double>();

    public Settings(FileConfiguration c) {
        enforceRestrictedMode = c.getBoolean("enforce-restricted-mode", false);
        restrictedWorlds = new HashSet<String>(c.getStringList("restricted-worlds"));
        restrictedMessage = ChatColor.translateAlternateColorCodes('&', c.getString("restricted-message", ""));
        attestationTimeoutMillis = c.getLong("attestation-timeout-seconds", 20) * 1000L;
        heartbeatSeconds = c.getInt("heartbeat-seconds", 5);
        silenceHeartbeats = c.getInt("silence-heartbeats", 3);
        inventorySeconds = c.getInt("inventory-seconds", 60);
        ignoredClassPrefixes = c.getStringList("ignored-class-prefixes");
        attackWithoutClickTolerance = c.getInt("attack-without-click-tolerance", 3);
        halfLifeMinutes = c.getDouble("score.half-life-minutes", 60);
        alertScore = c.getDouble("score.alert", 20);
        reviewScore = c.getDouble("score.review", 50);
        autoBoxMinWeight = c.getDouble("auto-black-box.min-weight", 10);
        autoBoxCooldownMillis = c.getLong("auto-black-box.cooldown-seconds", 120) * 1000L;
        captureRetentionMillis = c.getLong("retention.capture-hours", 72) * 3_600_000L;
        blackBoxRetentionMillis = c.getLong("retention.black-box-days", 7) * 86_400_000L;
        heldRetentionMillis = c.getLong("retention.held-days", 90) * 86_400_000L;
        grimEnabled = c.getBoolean("grim.enabled", true);
        grimEventClass = c.getString("grim.event-class", "ac.grim.grimac.api.events.FlagEvent");
        ConfigurationSection w = c.getConfigurationSection("weights");
        if (w != null) {
            for (String key : w.getKeys(false)) weights.put(key, w.getDouble(key));
        }
    }

    public double weight(String kind) {
        Double w = weights.get(kind);
        return w == null ? 1 : w;
    }

    public long silenceMillis() {
        return (long) heartbeatSeconds * silenceHeartbeats * 1000L + 2000L;
    }
}
