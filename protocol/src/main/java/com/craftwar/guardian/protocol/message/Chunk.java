package com.craftwar.guardian.protocol.message;

import com.craftwar.guardian.protocol.Reader;
import com.craftwar.guardian.protocol.Wire;
import com.craftwar.guardian.protocol.Writer;

public final class Chunk {

    public final int requestId;
    public final int kind;
    public final int index;
    public final int count;
    public final int totalLength;
    public final byte[] data;

    public Chunk(int requestId, int kind, int index, int count, int totalLength, byte[] data) {
        this.requestId = requestId;
        this.kind = kind;
        this.index = index;
        this.count = count;
        this.totalLength = totalLength;
        this.data = data;
    }

    public byte[] encode() {
        return new Writer(Wire.CHUNK_HEADER + data.length).i32(requestId).u8(kind).u16(index).u16(count)
            .i32(totalLength).bytes(data).toArray();
    }

    public static Chunk decode(byte[] payload) {
        Reader r = new Reader(payload);
        return new Chunk(r.i32(), r.u8(), r.u16(), r.u16(), r.i32(), r.rest());
    }
}
