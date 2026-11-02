package com.craftwar.guardian.protocol;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Reader {

    private final byte[] buf;
    private final int end;
    private int pos;

    public Reader(byte[] buf) {
        this(buf, 0, buf.length);
    }

    public Reader(byte[] buf, int off, int len) {
        if (off < 0 || len < 0 || off + len > buf.length) throw new IllegalArgumentException("invalid range");
        this.buf = buf;
        this.pos = off;
        this.end = off + len;
    }

    private void require(int n) {
        if (n < 0 || end - pos < n) throw new ProtocolException("truncated: need " + n + ", have " + (end - pos));
    }

    public int u8() {
        require(1);
        return buf[pos++] & 0xFF;
    }

    public int u16() {
        require(2);
        int v = (buf[pos] & 0xFF) << 8 | buf[pos + 1] & 0xFF;
        pos += 2;
        return v;
    }

    public int i32() {
        require(4);
        int v = buf[pos] << 24 | (buf[pos + 1] & 0xFF) << 16 | (buf[pos + 2] & 0xFF) << 8 | buf[pos + 3] & 0xFF;
        pos += 4;
        return v;
    }

    public long i64() {
        return (long) i32() << 32 | i32() & 0xFFFFFFFFL;
    }

    public float f32() {
        return Float.intBitsToFloat(i32());
    }

    public boolean bool() {
        return u8() != 0;
    }

    public byte[] bytes(int n) {
        require(n);
        byte[] b = Arrays.copyOfRange(buf, pos, pos + n);
        pos += n;
        return b;
    }

    public byte[] blob() {
        return bytes(u16());
    }

    public String str() {
        int n = u16();
        require(n);
        String s = new String(buf, pos, n, StandardCharsets.UTF_8);
        pos += n;
        return s;
    }

    public List<String> strings() {
        int n = u16();
        List<String> out = new ArrayList<String>(Math.min(n, 1024));
        for (int i = 0; i < n; i++) out.add(str());
        return out;
    }

    public int remaining() {
        return end - pos;
    }

    public byte[] rest() {
        return bytes(remaining());
    }

    public void requireEnd() {
        if (pos != end) throw new ProtocolException("trailing bytes: " + (end - pos));
    }
}
