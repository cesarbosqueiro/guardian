package com.craftwar.guardian.protocol;

public final class MessageType {

    public static final int HELLO = 0x01;
    public static final int REPLY = 0x02;

    public static final int CONTEXT = 0x10;
    public static final int HEARTBEAT = 0x11;
    public static final int FINDING = 0x12;
    public static final int CHUNK = 0x13;
    public static final int ABORT = 0x14;

    public static final int POLICY = 0x20;
    public static final int REQUEST = 0x21;

    private MessageType() {
    }
}
