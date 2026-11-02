package com.craftwar.guardian.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.craftwar.guardian.protocol.message.Chunk;

public final class Chunker {

    private Chunker() {
    }

    public static List<byte[]> split(int requestId, int kind, byte[] data) {
        int count = Math.max(1, (data.length + Wire.MAX_CHUNK - 1) / Wire.MAX_CHUNK);
        if (count > 0xFFFF) throw new IllegalArgumentException("transfer too large: " + data.length);
        List<byte[]> out = new ArrayList<byte[]>(count);
        for (int i = 0, off = 0; i < count; i++, off += Wire.MAX_CHUNK) {
            byte[] part = Arrays.copyOfRange(data, off, Math.min(data.length, off + Wire.MAX_CHUNK));
            out.add(new Chunk(requestId, kind, i, count, data.length, part).encode());
        }
        return out;
    }
}
