package com.craftwar.guardian.protocol.blackbox;

import com.craftwar.guardian.protocol.ProtocolException;
import com.craftwar.guardian.protocol.Reader;
import com.craftwar.guardian.protocol.Writer;

public final class BlackBoxCodec {

    private static final int MAGIC = 0x43574258;
    private static final int VERSION = 1;
    private static final int TICK_BYTES = 23;
    private static final int CLICK_BYTES = 5;
    private static final int FRAME_BYTES = 8;

    private BlackBoxCodec() {
    }

    public static byte[] encode(BlackBox b) {
        Writer w = new Writer(32 + b.tickCount * TICK_BYTES + b.clickCount * CLICK_BYTES + b.frameCount * FRAME_BYTES);
        w.i32(MAGIC).u8(VERSION).f32(b.sensitivity).bool(b.smoothCamera).i64(b.endWallMillis);
        w.u16(b.tickCount);
        for (int i = 0; i < b.tickCount; i++) {
            w.i32(b.tickMicrosBeforeEnd[i]).f32(b.tickYaw[i]).f32(b.tickPitch[i]).i32(b.tickDx[i]).i32(b.tickDy[i])
                .u16(b.tickFrames[i]).u8(b.tickFlags[i]);
        }
        w.u16(b.clickCount);
        for (int i = 0; i < b.clickCount; i++) w.i32(b.clickMicrosBeforeEnd[i]).u8(b.clickCode[i]);
        w.u16(b.frameCount);
        for (int i = 0; i < b.frameCount; i++) w.i32(b.frameMicrosBeforeEnd[i]).i32(b.frameDelta[i]);
        return w.toArray();
    }

    public static BlackBox decode(byte[] data) {
        Reader r = new Reader(data);
        if (r.i32() != MAGIC) throw new ProtocolException("not a black box");
        if (r.u8() != VERSION) throw new ProtocolException("unsupported black box version");
        float sensitivity = r.f32();
        boolean smooth = r.bool();
        long end = r.i64();
        int ticks = bounded(r.u16(), r.remaining(), TICK_BYTES);
        BlackBox b = new BlackBox(ticks, 0, 0);
        b.sensitivity = sensitivity;
        b.smoothCamera = smooth;
        b.endWallMillis = end;
        b.tickCount = ticks;
        for (int i = 0; i < ticks; i++) {
            b.tickMicrosBeforeEnd[i] = r.i32();
            b.tickYaw[i] = r.f32();
            b.tickPitch[i] = r.f32();
            b.tickDx[i] = r.i32();
            b.tickDy[i] = r.i32();
            b.tickFrames[i] = r.u16();
            b.tickFlags[i] = r.u8();
        }
        int clicks = bounded(r.u16(), r.remaining(), CLICK_BYTES);
        b.clickCount = clicks;
        b.clickMicrosBeforeEnd = new int[clicks];
        b.clickCode = new int[clicks];
        for (int i = 0; i < clicks; i++) {
            b.clickMicrosBeforeEnd[i] = r.i32();
            b.clickCode[i] = r.u8();
        }
        int frames = bounded(r.u16(), r.remaining(), FRAME_BYTES);
        b.frameCount = frames;
        b.frameMicrosBeforeEnd = new int[frames];
        b.frameDelta = new int[frames];
        for (int i = 0; i < frames; i++) {
            b.frameMicrosBeforeEnd[i] = r.i32();
            b.frameDelta[i] = r.i32();
        }
        r.requireEnd();
        return b;
    }

    private static int bounded(int count, int remaining, int recordBytes) {
        if ((long) count * recordBytes > remaining) throw new ProtocolException("black box truncated");
        return count;
    }
}
