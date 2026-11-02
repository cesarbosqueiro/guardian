package com.craftwar.guardian.protocol.message;

import com.craftwar.guardian.protocol.Reader;
import com.craftwar.guardian.protocol.Writer;

public final class Abort {

    public final int requestId;
    public final String reason;

    public Abort(int requestId, String reason) {
        this.requestId = requestId;
        this.reason = reason;
    }

    public byte[] encode() {
        return new Writer(64).i32(requestId).str(reason, 256).toArray();
    }

    public static Abort decode(byte[] payload) {
        Reader r = new Reader(payload);
        Abort a = new Abort(r.i32(), r.str());
        r.requireEnd();
        return a;
    }
}
