package com.craftwar.guardian.plugin.evidence;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.craftwar.guardian.plugin.cases.ServerFinding;
import com.craftwar.guardian.plugin.track.ServerTrack;
import com.craftwar.guardian.protocol.blackbox.BlackBoxCodec;

public final class EvidenceStore {

    static final String HOLD = ".hold";

    private final File root;
    private final Logger log;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Guardian-IO");
        t.setDaemon(true);
        return t;
    });

    public EvidenceStore(File root, Logger log) {
        this.root = root;
        this.log = log;
    }

    public File root() {
        return root;
    }

    public void saveCapture(UUID player, int requestId, byte[] jpeg, Runnable done) {
        submit(() -> {
            File f = new File(dir(player), stamp() + "-" + requestId + "-capture.jpg");
            Files.write(f.toPath(), jpeg);
            done.run();
        });
    }

    public void saveBlackBox(UUID player, int requestId, byte[] data, ServerTrack.Snapshot server, Runnable done) {
        submit(() -> {
            File box = new File(dir(player), stamp() + "-" + requestId + "-box");
            Files.createDirectories(box.toPath());
            Files.write(new File(box, "black-box.bin").toPath(), data);
            BlackBoxCsv.write(box, BlackBoxCodec.decode(data), server);
            done.run();
        });
    }

    public void appendFinding(UUID player, ServerFinding finding) {
        submit(() -> append(new File(dir(player), "findings.log"), finding.line()));
    }

    public void audit(String actor, String action, String target, String ticket, String detail) {
        String line = System.currentTimeMillis() + "\t" + actor + "\t" + action + "\t" + target + "\t" + ticket + "\t" + detail;
        submit(() -> append(new File(root, "audit.log"), line));
    }

    public void hold(UUID player, boolean on) {
        submit(() -> {
            File f = new File(dir(player), HOLD);
            if (on) Files.write(f.toPath(), new byte[0]);
            else Files.deleteIfExists(f.toPath());
        });
    }

    public void purge(Retention retention) {
        submit(() -> retention.purge(root));
    }

    public void shutdown() {
        io.shutdown();
        try {
            io.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private File dir(UUID player) throws IOException {
        File d = new File(root, player.toString());
        Files.createDirectories(d.toPath());
        return d;
    }

    private static void append(File file, String line) throws IOException {
        Files.write(file.toPath(), (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String stamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    }

    private void submit(IoTask task) {
        io.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.log(Level.WARNING, "evidence io failed", e);
            }
        });
    }

    private interface IoTask {
        void run() throws Exception;
    }
}
