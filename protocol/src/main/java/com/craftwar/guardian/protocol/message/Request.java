package com.craftwar.guardian.protocol.message;

import com.craftwar.guardian.protocol.Reader;
import com.craftwar.guardian.protocol.Writer;

public final class Request {

    public final int id;
    public final int kind;
    public final String notice;

    public Request(int id, int kind, String notice) {
        this.id = id;
        this.kind = kind;
        this.notice = notice;
    }

    public byte[] encode() {
        return new Writer(64).i32(id).u8(kind).str(notice, 256).toArray();
    }

    public static Request decode(byte[] payload) {
        Reader r = new Reader(payload);
        Request q = new Request(r.i32(), r.u8(), r.str());
        r.requireEnd();
        return q;
    }
}
