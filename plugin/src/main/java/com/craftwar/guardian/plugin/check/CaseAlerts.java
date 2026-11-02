package com.craftwar.guardian.plugin.check;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.craftwar.guardian.plugin.Guardian;
import com.craftwar.guardian.plugin.cases.Cases;
import com.craftwar.guardian.plugin.cases.ServerFinding;
import com.craftwar.guardian.plugin.request.Notifier;
import com.craftwar.guardian.plugin.request.Requester;
import com.craftwar.guardian.plugin.session.PlayerSession;
import com.craftwar.guardian.protocol.RequestKind;

public final class CaseAlerts implements Cases.Listener {

    public static final String PERMISSION = "craftwar.guardian.alerts";
    private static final String SYSTEM = "system";

    private final Guardian g;
    private Requester requester;

    public CaseAlerts(Guardian g) {
        this.g = g;
    }

    public void requester(Requester r) {
        requester = r;
    }

    @Override
    public void onFinding(UUID player, String name, ServerFinding f, double before, double after) {
        g.evidence.appendFinding(player, f);
        if (f.weight <= 0) return;
        broadcast(String.format("%s%s %s%s %s(%s) [%s] score %.0f", Notifier.PREFIX, name, ChatColor.YELLOW, f.kind,
            ChatColor.GRAY, f.detail, f.source, after));
        boolean crossedAlert = before < g.settings.alertScore && after >= g.settings.alertScore;
        if (f.weight >= g.settings.autoBoxMinWeight || crossedAlert) autoBox(player, f.kind);
        if (before < g.settings.reviewScore && after >= g.settings.reviewScore) {
            broadcast(Notifier.PREFIX + ChatColor.RED + name + " entrou na fila de revisao (score " + Math.round(after) + ")");
        }
    }

    private void autoBox(UUID player, String reason) {
        PlayerSession s = g.sessions.get(player);
        long now = System.currentTimeMillis();
        if (s == null || requester == null || now - s.lastAutoBoxMillis < g.settings.autoBoxCooldownMillis) return;
        s.lastAutoBoxMillis = now;
        requester.request(s, RequestKind.BLACK_BOX, SYSTEM, "auto:" + reason);
    }

    private static void broadcast(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(PERMISSION)) p.sendMessage(message);
        }
    }
}
