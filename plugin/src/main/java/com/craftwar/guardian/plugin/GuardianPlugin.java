package com.craftwar.guardian.plugin;

import java.io.File;
import java.io.IOException;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.craftwar.guardian.plugin.cases.Cases;
import com.craftwar.guardian.plugin.check.CaseAlerts;
import com.craftwar.guardian.plugin.check.GrimBridge;
import com.craftwar.guardian.plugin.check.MultiAuraCheck;
import com.craftwar.guardian.plugin.check.RestrictedMode;
import com.craftwar.guardian.plugin.command.GuardianCommand;
import com.craftwar.guardian.plugin.evidence.EvidenceStore;
import com.craftwar.guardian.plugin.evidence.Retention;
import com.craftwar.guardian.plugin.request.Notifier;
import com.craftwar.guardian.plugin.request.Requester;
import com.craftwar.guardian.plugin.session.Attestation;
import com.craftwar.guardian.plugin.session.ConnectionListener;
import com.craftwar.guardian.plugin.session.Gateway;
import com.craftwar.guardian.plugin.session.Inbound;
import com.craftwar.guardian.plugin.session.Watchdog;
import com.craftwar.guardian.protocol.Wire;

public final class GuardianPlugin extends JavaPlugin {

    private static final long HOUR_TICKS = 72_000L;

    private EvidenceStore evidence;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Settings settings = new Settings(getConfig());
        ServerIdentity identity;
        try {
            identity = ServerIdentity.loadOrCreate(getDataFolder());
        } catch (IOException e) {
            getLogger().severe("cannot load server identity: " + e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        evidence = new EvidenceStore(new File(getDataFolder(), "evidence"), getLogger());
        Guardian g = new Guardian(this, settings, identity, evidence);
        wire(g);
        getLogger().info("server public key: " + identity.publicKeyBase64());
    }

    private void wire(Guardian g) {
        CaseAlerts alerts = new CaseAlerts(g);
        g.cases = new Cases(g.settings, alerts);
        Attestation attestation = new Attestation(g);
        Gateway gateway = new Gateway(g, attestation);
        Notifier notifier = new Notifier(g);
        gateway.inbound(new Inbound(g, attestation, notifier));
        Requester requester = new Requester(g, gateway);
        alerts.requester(requester);

        getServer().getMessenger().registerOutgoingPluginChannel(this, Wire.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, Wire.CHANNEL, gateway);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ConnectionListener(g, gateway), this);
        pm.registerEvents(new MultiAuraCheck(g), this);
        pm.registerEvents(new RestrictedMode(g), this);
        if (GrimBridge.install(g)) getLogger().info("Grim integration active");
        getCommand("guardian").setExecutor(new GuardianCommand(g, requester));

        getServer().getScheduler().runTaskTimer(this, g.tracker, 1L, 1L);
        getServer().getScheduler().runTaskTimer(this, new Watchdog(g, attestation), 20L, 20L);
        Retention retention = new Retention(g.settings.captureRetentionMillis, g.settings.blackBoxRetentionMillis,
            g.settings.heldRetentionMillis);
        getServer().getScheduler().runTaskTimer(this, () -> evidence.purge(retention), 200L, HOUR_TICKS);
    }

    @Override
    public void onDisable() {
        if (evidence != null) evidence.shutdown();
    }
}
