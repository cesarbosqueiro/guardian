package com.craftwar.guardian.plugin.evidence;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.craftwar.guardian.plugin.track.ServerTrack;
import com.craftwar.guardian.protocol.blackbox.BlackBox;

final class BlackBoxCsv {

    private BlackBoxCsv() {
    }

    static double yawQuantum(float sensitivity) {
        double f = sensitivity * 0.6 + 0.2;
        return f * f * f * 8.0 * 0.15;
    }

    static void write(File dir, BlackBox b, ServerTrack.Snapshot server) throws IOException {
        ticks(new File(dir, "client-ticks.csv"), b);
        clicks(new File(dir, "client-clicks.csv"), b);
        frames(new File(dir, "client-frames.csv"), b);
        server(new File(dir, "server-ticks.csv"), server);
    }

    private static void ticks(File file, BlackBox b) throws IOException {
        double q = yawQuantum(b.sensitivity);
        PrintWriter w = open(file);
        try {
            w.println("wall_ms,yaw,pitch,dx,dy,frames,flags,yaw_delta,expected_delta,residual");
            for (int i = 0; i < b.tickCount; i++) {
                double delta = i == 0 ? 0 : b.tickYaw[i] - b.tickYaw[i - 1];
                double expected = b.tickDx[i] * q;
                w.println(String.format(Locale.ROOT, "%.3f,%.4f,%.4f,%d,%d,%d,%d,%.5f,%.5f,%.5f",
                    wall(b, b.tickMicrosBeforeEnd[i]), b.tickYaw[i], b.tickPitch[i], b.tickDx[i], b.tickDy[i],
                    b.tickFrames[i], b.tickFlags[i], delta, expected, i == 0 || b.smoothCamera ? 0 : delta - expected));
            }
        } finally {
            w.close();
        }
    }

    private static void clicks(File file, BlackBox b) throws IOException {
        PrintWriter w = open(file);
        try {
            w.println("wall_ms,button,down,gui,interval_ms");
            double lastLeftDown = Double.NaN;
            for (int i = 0; i < b.clickCount; i++) {
                int code = b.clickCode[i];
                double t = wall(b, b.clickMicrosBeforeEnd[i]);
                boolean leftDown = BlackBox.clickButton(code) == 0 && BlackBox.clickDown(code) && !BlackBox.clickInGui(code);
                double interval = leftDown && !Double.isNaN(lastLeftDown) ? t - lastLeftDown : Double.NaN;
                if (leftDown) lastLeftDown = t;
                w.println(String.format(Locale.ROOT, "%.3f,%d,%d,%d,%s", t, BlackBox.clickButton(code),
                    BlackBox.clickDown(code) ? 1 : 0, BlackBox.clickInGui(code) ? 1 : 0,
                    Double.isNaN(interval) ? "" : String.format(Locale.ROOT, "%.3f", interval)));
            }
        } finally {
            w.close();
        }
    }

    private static void frames(File file, BlackBox b) throws IOException {
        PrintWriter w = open(file);
        try {
            w.println("wall_ms,dx,dy");
            for (int i = 0; i < b.frameCount; i++) {
                w.println(String.format(Locale.ROOT, "%.3f,%d,%d", wall(b, b.frameMicrosBeforeEnd[i]),
                    BlackBox.deltaX(b.frameDelta[i]), BlackBox.deltaY(b.frameDelta[i])));
            }
        } finally {
            w.close();
        }
    }

    private static void server(File file, ServerTrack.Snapshot s) throws IOException {
        PrintWriter w = open(file);
        try {
            w.println("wall_ms,yaw,pitch,attacks");
            for (int i = 0; i < s.millis.length; i++) {
                w.println(String.format(Locale.ROOT, "%d,%.4f,%.4f,%d", s.millis[i], s.yaw[i], s.pitch[i], s.attacks[i]));
            }
        } finally {
            w.close();
        }
    }

    private static double wall(BlackBox b, int microsBeforeEnd) {
        return b.endWallMillis - microsBeforeEnd / 1000.0;
    }

    private static PrintWriter open(File file) throws IOException {
        return new PrintWriter(java.nio.file.Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8));
    }
}
