package com.craftwar.guardian.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class Writer {

    private byte[] buf;
    private int pos;

    public Writer() {
        this(256);
    }

    public Writer(int capacity) {
        buf = new byte[Math.max(16, capacity)];
    }

    private void ensure(int n) {
        if (pos + n > buf.length) {
            buf = Arrays.copyOf(buf, Math.max(buf.length << 1, pos + n));
        }
    }

    public Writer u8(int v) {
        ensure(1);
        buf[pos++] = (byte) v;
        return this;
    }

    public Writer u16(int v) {
        if ((v & ~0xFFFF) != 0) throw new IllegalArgumentException("u16 out of range: " + v);
        ensure(2);
        buf[pos] = (byte) (v >>> 8);
        buf[pos + 1] = (byte) v;
        pos += 2;
        return this;
    }

    public Writer i32(int v) {
        ensure(4);
        buf[pos] = (byte) (v >>> 24);
        buf[pos + 1] = (byte) (v >>> 16);
        buf[pos + 2] = (byte) (v >>> 8);
        buf[pos + 3] = (byte) v;
        pos += 4;
        return this;
    }

    public Writer i64(long v) {
        i32((int) (v >>> 32));
        return i32((int) v);
    }

    public Writer f32(float v) {
        return i32(Float.floatToRawIntBits(v));
    }

    public Writer bool(boolean v) {
        return u8(v ? 1 : 0);
    }

    public Writer bytes(byte[] b) {
        return bytes(b, 0, b.length);
    }

    public Writer bytes(byte[] b, int off, int len) {
        ensure(len);
        System.arraycopy(b, off, buf, pos, len);
        pos += len;
        return this;
    }

    public Writer blob(byte[] b) {
        u16(b.length);
        return bytes(b);
    }

    public Writer str(String s, int maxBytes) {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        return blob(b.length > maxBytes ? Arrays.copyOf(b, maxBytes) : b);
    }

    public Writer str(String s) {
        return str(s, 1024);
    }

    public Writer strings(List<String> values, int maxCount) {
        int n = Math.min(values.size(), maxCount);
        u16(n);
        for (int i = 0; i < n; i++) str(values.get(i));
        return this;
    }

    public int size() {
        return pos;
    }

    public byte[] toArray() {
        return Arrays.copyOf(buf, pos);
    }
}
