package com.craftwar.guardian.plugin.command;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.craftwar.guardian.plugin.Guardian;
import com.craftwar.guardian.plugin.request.Notifier;
import com.craftwar.guardian.plugin.request.Requester;
import com.craftwar.guardian.plugin.session.PlayerSession;
import com.craftwar.guardian.protocol.RequestKind;

public final class GuardianCommand implements CommandExecutor {

    private static final String USAGE = "/guardian <info|case|box|ss|window|hold|release> <jogador> [ticket] | key";

    private final Guardian g;
    private final Requester requester;
    private final InfoView info;

    public GuardianCommand(Guardian g, Requester requester) {
        this.g = g;
        this.requester = requester;
        this.info = new InfoView(g);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("key")) {
            sender.sendMessage(Notifier.PREFIX + "chave publica do servidor:");
            sender.sendMessage(g.identity.publicKeyBase64());
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Notifier.PREFIX + USAGE);
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        PlayerSession s = target == null ? null : g.sessions.get(target);
        if (s == null) {
            sender.sendMessage(Notifier.PREFIX + "jogador offline");
            return true;
        }
        String ticket = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";
        switch (args[0].toLowerCase()) {
            case "info":
                info.show(sender, s);
                break;
            case "case":
                info.findings(sender, s, 15);
                break;
            case "box":
                request(sender, s, RequestKind.BLACK_BOX, ticket, false);
                break;
            case "ss":
                request(sender, s, RequestKind.GAME_CAPTURE, ticket, true);
                break;
            case "window":
                request(sender, s, RequestKind.WINDOW_CAPTURE, ticket, true);
                break;
            case "hold":
            case "release":
                boolean on = args[0].equalsIgnoreCase("hold");
                g.evidence.hold(s.id, on);
                g.evidence.audit(sender.getName(), on ? "hold" : "release", s.name, ticket, "");
                sender.sendMessage(Notifier.PREFIX + (on ? "evidencias retidas" : "retencao normal") + " para " + s.name);
                break;
            default:
                sender.sendMessage(Notifier.PREFIX + USAGE);
        }
        return true;
    }

    private void request(CommandSender sender, PlayerSession s, int kind, String ticket, boolean ticketRequired) {
        if (ticketRequired && ticket.isEmpty()) {
            sender.sendMessage(Notifier.PREFIX + ChatColor.RED + "captura exige ticket: /guardian ss <jogador> <ticket>");
            return;
        }
        String error = requester.request(s, kind, sender.getName(), ticket);
        sender.sendMessage(Notifier.PREFIX + (error == null ? "pedido enviado para " + s.name : ChatColor.RED + error));
    }
}
