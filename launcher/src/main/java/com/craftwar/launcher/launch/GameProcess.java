package com.craftwar.launcher.launch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.craftwar.launcher.sync.Files2;

public final class GameProcess {

    private GameProcess() {
    }

    public static Process start(List<String> command, File gameDir) throws IOException {
        File logs = new File(gameDir, "logs");
        Files2.mkdirs(logs);
        File log = new File(logs, "launcher-game.log");
        header(log, command);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(gameDir);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        return pb.start();
    }

    /**
     * Abre o log com o comando, um argumento por linha. E o registro de qual JVM, qual
     * agente e qual classpath subiram -- a primeira coisa a conferir quando o jogo nao
     * abre ou quando o agente reporta degradado.
     */
    private static void header(File log, List<String> command) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(log, false), StandardCharsets.UTF_8)) {
            for (String arg : command) {
                w.write(arg);
                w.write('\n');
            }
            w.write('\n');
        }
    }
}
