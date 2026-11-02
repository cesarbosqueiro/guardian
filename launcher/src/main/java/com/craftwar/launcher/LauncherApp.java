package com.craftwar.launcher;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.craftwar.launcher.launch.ConsentHash;
import com.craftwar.launcher.launch.GameProcess;
import com.craftwar.launcher.launch.LaunchCommand;
import com.craftwar.launcher.launch.LaunchTarget;
import com.craftwar.launcher.manifest.Manifest;
import com.craftwar.launcher.manifest.ManifestSource;
import com.craftwar.launcher.manifest.Profile;
import com.craftwar.launcher.manifest.ServerEntry;
import com.craftwar.launcher.sync.HashCache;
import com.craftwar.launcher.sync.Progress;
import com.craftwar.launcher.sync.ServersDat;
import com.craftwar.launcher.sync.Synchronizer;

public final class LauncherApp {

    public interface View extends Progress {
        boolean askConsent(String text);

        void notice(String message);

        void failed(String message);

        void launched();
    }

    private static final String NICK = "[A-Za-z0-9_]{3,16}";

    private final LauncherConfig config;
    private final GameLayout layout;
    private final UserSettings settings;
    private volatile Manifest manifest;

    public LauncherApp(LauncherConfig config, GameLayout layout) {
        this.config = config;
        this.layout = layout;
        this.settings = new UserSettings(layout.settings());
    }

    public String title() {
        return config.name;
    }

    public UserSettings settings() {
        return settings;
    }

    public GameLayout layout() {
        return layout;
    }

    public static boolean validNick(String nick) {
        return nick != null && nick.matches(NICK);
    }

    public Manifest refresh() throws IOException {
        Manifest m = new ManifestSource(config.manifestUrl, config.manifestKey).fetch();
        manifest = m;
        return m;
    }

    public void play(Selection s, View view) {
        try {
            view.stage("Buscando atualizacoes");
            Manifest m = refresh();
            if (!consent(m, view)) {
                view.failed("O anti-cheat e condicao de acesso ao servidor.");
                return;
            }
            Profile p = m.profile(s.profileId);
            remember(s, p, m.server(s.serverId));
            File profileDir = layout.profile(p.id);
            String os = Platform.os();
            HashCache cache = new HashCache(layout.hashCache(p.id));
            List<String> removed = new Synchronizer(profileDir, layout.assets(), os, cache, view).sync(p, s.forceUpdate);
            if (!removed.isEmpty()) view.notice(removed.size() + " mod(s) nao autorizados movidos para mods-disabled.");
            ServersDat.write(profileDir, ordered(m.servers, m.server(s.serverId)));
            view.stage("Iniciando o jogo");
            File java = GameJava.find(Platform.launcherDir(), settings.javaPath());
            int memory = Math.max(m.java.minimumMemoryMb, s.memoryMb);
            LaunchTarget target = new LaunchTarget(java, profileDir, layout.assets(), os, s.nick, memory);
            GameProcess.start(LaunchCommand.build(m, p, target), profileDir);
            view.launched();
        } catch (IOException e) {
            view.failed(e.getMessage());
        } catch (RuntimeException e) {
            view.failed("Erro inesperado: " + e);
        }
    }

    static List<ServerEntry> ordered(List<ServerEntry> servers, ServerEntry first) {
        List<ServerEntry> out = new ArrayList<ServerEntry>(servers.size());
        out.add(first);
        for (ServerEntry s : servers) {
            if (s != first) out.add(s);
        }
        return out;
    }

    private void remember(Selection s, Profile p, ServerEntry server) throws IOException {
        settings.nick(s.nick);
        settings.memoryMb(s.memoryMb);
        settings.profile(p.id);
        settings.server(server.id);
        settings.save();
    }

    private boolean consent(Manifest m, View view) throws IOException {
        if (m.consent == null || m.consent.text == null) return true;
        String hash = ConsentHash.of(m.consent.text);
        if (settings.consented(m.consent.version, hash)) return true;
        if (!view.askConsent(m.consent.text)) return false;
        settings.consent(m.consent.version, hash, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
        settings.save();
        return true;
    }
}
