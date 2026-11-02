package com.craftwar.guardian.protocol.message;

import com.craftwar.guardian.protocol.Reader;
import com.craftwar.guardian.protocol.Writer;

public final class Finding {

    public final int kind;
    public final String subject;
    public final String detail;

    public Finding(int kind, String subject, String detail) {
        this.kind = kind;
        this.subject = subject;
        this.detail = detail;
    }

    public byte[] encode() {
        return new Writer(128).u8(kind).str(subject, 512).str(detail, 2048).toArray();
    }

    public static Finding decode(byte[] payload) {
        Reader r = new Reader(payload);
        Finding f = new Finding(r.u8(), r.str(), r.str());
        r.requireEnd();
        return f;
    }
}
