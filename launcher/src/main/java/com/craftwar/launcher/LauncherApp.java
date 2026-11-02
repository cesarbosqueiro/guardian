package com.craftwar.launcher;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.craftwar.launcher.launch.ConsentHash;
import com.craftwar.launcher.launch.GameProcess;
import com.craftwar.launcher.launch.LaunchCommand;
import com.craftwar.launcher.manifest.Manifest;
import com.craftwar.launcher.manifest.ManifestSource;
import com.craftwar.launcher.sync.HashCache;
import com.craftwar.launcher.sync.Progress;
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
    private final File gameDir;
    private final UserSettings settings;

    public LauncherApp(LauncherConfig config, File gameDir) {
        this.config = config;
        this.gameDir = gameDir;
        this.settings = new UserSettings(new File(gameDir, "launcher/settings.properties"));
    }

    public String title() {
        return config.name;
    }

    public UserSettings settings() {
        return settings;
    }

    public static boolean validNick(String nick) {
        return nick != null && nick.matches(NICK);
    }

    public void play(String nick, int memoryMb, View view) {
        try {
            view.stage("Buscando atualizacoes");
            Manifest m = new ManifestSource(config.manifestUrl, config.manifestKey).fetch();
            if (!consent(m, view)) {
                view.failed("O anti-cheat e condicao de acesso ao servidor.");
                return;
            }
            settings.nick(nick);
            settings.memoryMb(memoryMb);
            settings.save();
            String os = Platform.os();
            HashCache cache = new HashCache(new File(gameDir, "launcher/hashes.properties"));
            List<String> removed = new Synchronizer(gameDir, os, cache, view).sync(m);
            if (!removed.isEmpty()) view.notice(removed.size() + " mod(s) nao autorizados movidos para mods-disabled.");
            view.stage("Iniciando o jogo");
            int memory = Math.max(m.java.minimumMemoryMb, memoryMb);
            GameProcess.start(LaunchCommand.build(m, Platform.javaExecutable(), gameDir, os, nick, memory), gameDir);
            view.launched();
        } catch (IOException e) {
            view.failed(e.getMessage());
        } catch (RuntimeException e) {
            view.failed("Erro inesperado: " + e);
        }
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
