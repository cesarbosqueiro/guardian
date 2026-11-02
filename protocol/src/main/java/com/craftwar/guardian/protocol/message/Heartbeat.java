package com.craftwar.guardian.protocol.message;

import com.craftwar.guardian.protocol.Reader;
import com.craftwar.guardian.protocol.Writer;

public final class Heartbeat {

    public final int fps;
    public final int leftClicks;
    public final int rightClicks;
    public final int flags;
    public final int agentState;
    public final int loadedClasses;

    public Heartbeat(int fps, int leftClicks, int rightClicks, int flags, int agentState, int loadedClasses) {
        this.fps = fps;
        this.leftClicks = leftClicks;
        this.rightClicks = rightClicks;
        this.flags = flags;
        this.agentState = agentState;
        this.loadedClasses = loadedClasses;
    }

    public byte[] encode() {
        return new Writer(12).u16(Math.min(fps, 0xFFFF)).u16(Math.min(leftClicks, 0xFFFF)).u16(Math.min(rightClicks, 0xFFFF))
            .u8(flags).u8(agentState).i32(loadedClasses).toArray();
    }

    public static Heartbeat decode(byte[] payload) {
        Reader r = new Reader(payload);
        Heartbeat h = new Heartbeat(r.u16(), r.u16(), r.u16(), r.u8(), r.u8(), r.i32());
        r.requireEnd();
        return h;
    }
}
