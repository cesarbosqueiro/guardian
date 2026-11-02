package com.craftwar.launcher.launch;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.craftwar.launcher.sync.Files2;

public final class GameProcess {

    private GameProcess() {
    }

    public static Process start(List<String> command, File gameDir) throws IOException {
        File logs = new File(gameDir, "logs");
        Files2.mkdirs(logs);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(gameDir);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(new File(logs, "launcher-game.log")));
        return pb.start();
    }
}
