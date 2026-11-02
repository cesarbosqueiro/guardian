package com.craftwar.guardian.protocol.message;

import java.util.List;

import com.craftwar.guardian.protocol.Reader;
import com.craftwar.guardian.protocol.Writer;

public final class Policy {

    public final int heartbeatSeconds;
    public final int inventorySeconds;
    public final List<String> ignoredClassPrefixes;

    public Policy(int heartbeatSeconds, int inventorySeconds, List<String> ignoredClassPrefixes) {
        this.heartbeatSeconds = heartbeatSeconds;
        this.inventorySeconds = inventorySeconds;
        this.ignoredClassPrefixes = ignoredClassPrefixes;
    }

    public byte[] encode() {
        return new Writer(256).u16(heartbeatSeconds).u16(inventorySeconds).strings(ignoredClassPrefixes, 256).toArray();
    }

    public static Policy decode(byte[] payload) {
        Reader r = new Reader(payload);
        Policy p = new Policy(r.u16(), r.u16(), r.strings());
        r.requireEnd();
        return p;
    }
}
