package com.craftwar.guardian.protocol;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.craftwar.guardian.protocol.blackbox.BlackBox;
import com.craftwar.guardian.protocol.blackbox.BlackBoxCodec;
import com.craftwar.guardian.protocol.message.Chunk;

public class TransferTest {

    @Test
    public void chunksReassembleOutOfOrder() {
        byte[] data = Ec.random(Wire.MAX_CHUNK * 3 + 17);
        List<byte[]> parts = Chunker.split(42, RequestKind.GAME_CAPTURE, data);
        assertEquals(4, parts.size());
        for (byte[] p : parts) assertTrue(p.length + Wire.HEADER + Wire.TAG <= Wire.MAX_MESSAGE);
        Collections.reverse(parts);
        Assembler a = new Assembler(1 << 20, 10000);
        Assembler.Transfer done = null;
        for (int i = 0; i < parts.size(); i++) {
            done = a.accept(Chunk.decode(parts.get(i)), 0);
            if (i < parts.size() - 1) assertNull(done);
        }
        assertEquals(42, done.requestId);
        assertArrayEquals(data, done.data);
    }

    @Test(expected = ProtocolException.class)
    public void rejectsOversizedTransfer() {
        new Assembler(1000, 10000).accept(new Chunk(1, 1, 0, 1, 5000, new byte[5000]), 0);
    }

    @Test
    public void blackBoxRoundTrip() {
        BlackBox b = new BlackBox(2, 1, 1);
        b.sensitivity = 0.5f;
        b.endWallMillis = 123456789L;
        b.tickCount = 2;
        b.tickYaw[1] = -1234.5f;
        b.tickDx[1] = -7;
        b.tickFrames[1] = 15;
        b.clickCount = 1;
        b.clickCode[0] = BlackBox.clickCode(0, true, false);
        b.frameCount = 1;
        b.frameDelta[0] = BlackBox.packDelta(-3, 250);
        BlackBox d = BlackBoxCodec.decode(BlackBoxCodec.encode(b));
        assertEquals(-1234.5f, d.tickYaw[1], 0f);
        assertEquals(-7, d.tickDx[1]);
        assertEquals(15, d.tickFrames[1]);
        assertEquals(1, d.clickCode[0]);
        assertEquals(-3, BlackBox.deltaX(d.frameDelta[0]));
        assertEquals(250, BlackBox.deltaY(d.frameDelta[0]));
    }
}
