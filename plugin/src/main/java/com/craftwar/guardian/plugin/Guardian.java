package com.craftwar.guardian.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.craftwar.guardian.plugin.cases.Cases;
import com.craftwar.guardian.plugin.evidence.EvidenceStore;
import com.craftwar.guardian.plugin.request.Requests;
import com.craftwar.guardian.plugin.session.Sessions;
import com.craftwar.guardian.plugin.track.Tracker;

public final class Guardian {

    public final Plugin plugin;
    public final Settings settings;
    public final ServerIdentity identity;
    public final Sessions sessions = new Sessions();
    public final Requests requests = new Requests();
    public final Tracker tracker;
    public final EvidenceStore evidence;
    public Cases cases;

    Guardian(Plugin plugin, Settings settings, ServerIdentity identity, EvidenceStore evidence) {
        this.plugin = plugin;
        this.settings = settings;
        this.identity = identity;
        this.evidence = evidence;
        this.tracker = new Tracker(sessions);
    }

    public void sync(Runnable task) {
        if (Bukkit.isPrimaryThread()) task.run();
        else Bukkit.getScheduler().runTask(plugin, task);
    }

    public void async(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
}
