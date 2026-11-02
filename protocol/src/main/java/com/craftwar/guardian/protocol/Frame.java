package com.craftwar.guardian.protocol;

public final class Frame {

    public final int type;
    public final byte[] payload;

    public Frame(int type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }

    public static int typeOf(byte[] frame) {
        if (frame.length < Wire.HEADER) throw new ProtocolException("short frame");
        if ((frame[0] & 0xFF) != Wire.VERSION) throw new ProtocolException("version " + (frame[0] & 0xFF));
        return frame[1] & 0xFF;
    }

    public static byte[] plain(int type, byte[] payload) {
        return new Writer(Wire.HEADER + payload.length).u8(Wire.VERSION).u8(type).i32(0).bytes(payload).toArray();
    }

    public static byte[] plainPayload(byte[] frame, int expectedType) {
        if (typeOf(frame) != expectedType) throw new ProtocolException("expected type " + expectedType);
        return java.util.Arrays.copyOfRange(frame, Wire.HEADER, frame.length);
    }
}
