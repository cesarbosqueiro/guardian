package com.craftwar.guardian.plugin.command;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.craftwar.guardian.plugin.Guardian;
import com.craftwar.guardian.plugin.cases.ServerFinding;
import com.craftwar.guardian.plugin.request.Notifier;
import com.craftwar.guardian.plugin.session.PlayerSession;
import com.craftwar.guardian.protocol.message.Context;
import com.craftwar.guardian.protocol.message.Heartbeat;

final class InfoView {

    private static final String[] AGENT_STATES = {"OK", "DEGRADADO", "DESLIGADO"};

    private final Guardian g;

    InfoView(Guardian g) {
        this.g = g;
    }

    void show(CommandSender to, PlayerSession s) {
        long now = System.currentTimeMillis();
        to.sendMessage(Notifier.PREFIX + ChatColor.WHITE + s.name + ChatColor.GRAY + " | atestado: "
            + (s.attested() ? ChatColor.GREEN + "sim" : ChatColor.RED + "nao") + ChatColor.GRAY
            + " | score " + Math.round(g.cases.file(s.id).score(now)));
        Context c = s.context();
        if (c != null) {
            to.sendMessage(ChatColor.GRAY + " agente " + c.agentVersion + " (" + state(c.agentState) + ") java " + c.javaVersion + " | " + c.os);
            to.sendMessage(ChatColor.GRAY + " gpu " + c.gpu + " " + c.width + "x" + c.height + " | sens " + c.sensitivity
                + " smooth " + c.smoothCamera + " gamma " + c.gamma);
            to.sendMessage(ChatColor.GRAY + " mods: " + String.join(", ", c.mods));
            to.sendMessage(ChatColor.GRAY + " packs: " + String.join(", ", c.resourcePacks));
        }
        Heartbeat h = s.lastHeartbeat();
        if (h != null) {
            to.sendMessage(ChatColor.GRAY + " heartbeat ha " + (now - s.lastHeartbeatMillis()) / 1000 + "s | fps " + h.fps
                + " | cliques E/D " + h.leftClicks + "/" + h.rightClicks + " | classes " + h.loadedClasses);
        }
        findings(to, s, 5);
    }

    void findings(CommandSender to, PlayerSession s, int n) {
        List<ServerFinding> list = g.cases.file(s.id).latest(n);
        if (list.isEmpty()) {
            to.sendMessage(ChatColor.GRAY + " sem achados");
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
        for (ServerFinding f : list) {
            to.sendMessage(ChatColor.GRAY + " " + fmt.format(new Date(f.millis)) + " " + ChatColor.YELLOW + f.kind
                + ChatColor.GRAY + " [" + f.source + "] " + f.subject + " " + f.detail);
        }
    }

    private static String state(int s) {
        return s >= 0 && s < AGENT_STATES.length ? AGENT_STATES[s] : "?";
    }
}
