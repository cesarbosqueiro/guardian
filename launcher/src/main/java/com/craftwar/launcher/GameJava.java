package com.craftwar.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acha o executavel de Java que vai rodar o jogo.
 *
 * O jogo precisa de Java 8: o 1.8.9 vanilla sobe mancando numa JVM moderna e o
 * Forge nao sobe (launchwrapper e o ASM 5 nao leem class files novos). Por isso o
 * JRE portatil que vem no pacote vem antes do {@code java.home} do proprio
 * launcher -- quem abre o jar com duplo clique cai no Java registrado no sistema,
 * e nao e ele que pode decidir a JVM do jogo.
 *
 * Em cada pasta tenta {@code javaw.exe} antes de {@code java.exe}: o javaw nao
 * abre uma janela de console junto com o jogo.
 */
public final class GameJava {

    /** Pastas de JRE portatil, relativas a pasta do launcher, em ordem de preferencia. */
    static final String[] PORTABLE_BINS = {
        "jre/bin",                               // layout de distribuicao do CraftWar
        "CraftLandia/bin/java/windows-x64/bin",  // layout do pacote antigo da CraftLandia
        "j8/bin",                                // layout mais antigo ainda
    };

    private static final String[] EXECUTABLES = {"javaw.exe", "java.exe", "java"};

    private GameJava() {
    }

    /**
     * @param launcherDir pasta onde esta o jar do launcher
     * @param override    caminho escolhido a mao, absoluto ou relativo a {@code launcherDir};
     *                    pode ser nulo ou vazio. Aponta para o executavel ou para a pasta
     *                    que o contem.
     */
    public static File find(File launcherDir, String override) throws IOException {
        List<File> tried = new ArrayList<File>();
        for (File candidate : candidates(launcherDir, override)) {
            if (candidate.isFile()) return candidate;
            tried.add(candidate);
        }
        StringBuilder sb = new StringBuilder("Java nao encontrado. O launcher procurou em:");
        for (File f : tried) sb.append("\n  ").append(f.getPath());
        sb.append("\n\nVerifique se a pasta jre do pacote veio junto.");
        throw new IOException(sb.toString());
    }

    static List<File> candidates(File launcherDir, String override) {
        List<File> out = new ArrayList<File>();
        if (override != null && !override.trim().isEmpty()) {
            File chosen = resolve(launcherDir, override.trim());
            // O override pode apontar para o executavel ou para a pasta dele.
            if (chosen.isDirectory()) addExecutables(out, chosen);
            else out.add(chosen);
        }
        for (String bin : PORTABLE_BINS) addExecutables(out, resolve(launcherDir, bin));
        String home = System.getProperty("java.home");
        if (home != null && !home.isEmpty()) addExecutables(out, new File(home, "bin"));
        return out;
    }

    private static void addExecutables(List<File> out, File bin) {
        for (String exe : EXECUTABLES) out.add(new File(bin, exe));
    }

    private static File resolve(File launcherDir, String path) {
        File f = new File(path.replace('\\', '/'));
        return f.isAbsolute() ? f : new File(launcherDir, path.replace('\\', '/'));
    }
}
