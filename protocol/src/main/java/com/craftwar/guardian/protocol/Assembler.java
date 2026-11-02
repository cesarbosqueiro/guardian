package com.craftwar.guardian.protocol;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.craftwar.guardian.protocol.message.Chunk;

public final class Assembler {

    private final int maxBytes;
    private final long timeoutMillis;
    private final Map<Integer, Partial> pending = new HashMap<Integer, Partial>();

    public Assembler(int maxBytes, long timeoutMillis) {
        this.maxBytes = maxBytes;
        this.timeoutMillis = timeoutMillis;
    }

    public Transfer accept(Chunk c, long nowMillis) {
        expire(nowMillis);
        if (c.totalLength < 0 || c.totalLength > maxBytes) throw new ProtocolException("transfer too large: " + c.totalLength);
        Partial p = pending.get(c.requestId);
        if (p == null) {
            if (c.count != (Math.max(1, (c.totalLength + Wire.MAX_CHUNK - 1) / Wire.MAX_CHUNK))) {
                throw new ProtocolException("inconsistent chunk count");
            }
            p = new Partial(c.kind, c.count, c.totalLength, nowMillis);
            pending.put(c.requestId, p);
        }
        if (!p.put(c)) throw new ProtocolException("inconsistent chunk " + c.index);
        if (!p.complete()) return null;
        pending.remove(c.requestId);
        return new Transfer(c.requestId, p.kind, p.data);
    }

    private void expire(long nowMillis) {
        for (Iterator<Partial> it = pending.values().iterator(); it.hasNext(); ) {
            if (nowMillis - it.next().startedMillis > timeoutMillis) it.remove();
        }
    }

    public static final class Transfer {
        public final int requestId;
        public final int kind;
        public final byte[] data;

        Transfer(int requestId, int kind, byte[] data) {
            this.requestId = requestId;
            this.kind = kind;
            this.data = data;
        }
    }

    private static final class Partial {
        final int kind;
        final int count;
        final byte[] data;
        final long startedMillis;
        final boolean[] seen;
        int received;

        Partial(int kind, int count, int totalLength, long startedMillis) {
            this.kind = kind;
            this.count = count;
            this.data = new byte[totalLength];
            this.startedMillis = startedMillis;
            this.seen = new boolean[count];
        }

        boolean put(Chunk c) {
            if (c.kind != kind || c.count != count || c.totalLength != data.length) return false;
            if (c.index >= count || seen[c.index]) return false;
            int off = c.index * Wire.MAX_CHUNK;
            int expected = Math.min(Wire.MAX_CHUNK, data.length - off);
            if (c.data.length != expected) return false;
            System.arraycopy(c.data, 0, data, off, expected);
            seen[c.index] = true;
            received++;
            return true;
        }

        boolean complete() {
            return received == count;
        }
    }
}
