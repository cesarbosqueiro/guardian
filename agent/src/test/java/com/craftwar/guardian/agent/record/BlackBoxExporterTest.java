package com.craftwar.guardian.agent.record;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.craftwar.guardian.protocol.blackbox.BlackBox;
import com.craftwar.guardian.protocol.blackbox.BlackBoxCodec;

public class BlackBoxExporterTest {

    @Test
    public void aggregatesFramesPerTickAndKeepsWindow() {
        Recorder r = new Recorder();
        long now = System.nanoTime() - 80_000_000_000L;
        for (int t = 0; t < 1500; t++) {
            for (int f = 0; f < 3; f++) r.frame(2, -1, now += 1_000_000L);
            r.click(0, true, false, now, now);
            r.tick(t * 0.15f, 0f, 1, now += 47_000_000L);
        }
        BlackBox b = BlackBoxCodec.decode(BlackBoxCodec.encode(BlackBoxExporter.export(r, 0.5f, false)));
        assertEquals(1200, b.tickCount);
        assertEquals(6, b.tickDx[b.tickCount - 1]);
        assertEquals(-3, b.tickDy[b.tickCount - 1]);
        assertEquals(3, b.tickFrames[b.tickCount - 1]);
        assertEquals(1499 * 0.15f, b.tickYaw[b.tickCount - 1], 0f);
        assertEquals(Recorder.CLICKS - 256, b.clickCount);
    }

    @Test
    public void clickCounterIgnoresReleasesAndGui() {
        Recorder r = new Recorder();
        ClickCounter counter = new ClickCounter(r);
        long now = System.nanoTime();
        r.click(0, true, false, now, now);
        r.click(0, false, false, now, now);
        r.click(1, true, false, now, now);
        r.click(0, true, true, now, now);
        counter.advance();
        assertEquals(1, counter.left());
        assertEquals(1, counter.right());
        counter.advance();
        assertEquals(0, counter.left());
    }
}
