package com.craftwar.guardian.plugin.cases;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.craftwar.guardian.plugin.Settings;

public final class Cases {

    public interface Listener {
        void onFinding(UUID player, String name, ServerFinding finding, double before, double after);
    }

    private final Map<UUID, CaseFile> files = new HashMap<UUID, CaseFile>();
    private final Settings settings;
    private final Listener listener;

    public Cases(Settings settings, Listener listener) {
        this.settings = settings;
        this.listener = listener;
    }

    public void report(UUID player, String name, String kind, Source source, String subject, String detail) {
        long now = System.currentTimeMillis();
        CaseFile file = file(player);
        double before = file.score(now);
        ServerFinding f = new ServerFinding(now, kind, source, settings.weight(kind), subject, detail);
        double after = file.add(f);
        listener.onFinding(player, name, f, before, after);
    }

    public CaseFile file(UUID player) {
        CaseFile f = files.get(player);
        if (f == null) files.put(player, f = new CaseFile(settings.halfLifeMinutes));
        return f;
    }
}
