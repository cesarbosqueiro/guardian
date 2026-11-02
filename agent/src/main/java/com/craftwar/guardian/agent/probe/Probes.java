package com.craftwar.guardian.agent.probe;

import org.lwjgl.input.Mouse;

import com.craftwar.guardian.agent.GameState;
import com.craftwar.guardian.agent.Health;
import com.craftwar.guardian.agent.capture.GameCapture;
import com.craftwar.guardian.agent.integrity.IntegrityMonitor;
import com.craftwar.guardian.agent.record.Recorder;
import com.craftwar.guardian.protocol.Flags;

public final class Probes {

    private static final PlayerRotation ROTATION = new PlayerRotation();

    private Probes() {
    }

    public static void configure(String yawField, String pitchField) {
        ROTATION.configure(yawField, pitchField);
    }

    public static void mouse(int dx, int dy) {
        try {
            Recorder.INSTANCE.frame(dx, dy, System.nanoTime());
        } catch (Throwable t) {
            Health.probeFailed(t);
        }
    }

    public static void tick(Object player) {
        try {
            ROTATION.read(player);
            Recorder.INSTANCE.tick(ROTATION.yaw, ROTATION.pitch, GameState.flags(), System.nanoTime());
            IntegrityMonitor.markReady();
        } catch (Throwable t) {
            Health.probeFailed(t);
        }
    }

    public static boolean mouseEvent(boolean hasEvent) {
        if (hasEvent) {
            try {
                int button = Mouse.getEventButton();
                if (button >= 0) {
                    boolean gui = (GameState.flags() & Flags.GUI) != 0;
                    Recorder.INSTANCE.click(button, Mouse.getEventButtonState(), gui, Mouse.getEventNanoseconds(), System.nanoTime());
                }
            } catch (Throwable t) {
                Health.probeFailed(t);
            }
        }
        return hasEvent;
    }

    public static void beforeSwap() {
        try {
            GameCapture.INSTANCE.onFrame();
        } catch (Throwable t) {
            Health.probeFailed(t);
        }
    }
}
