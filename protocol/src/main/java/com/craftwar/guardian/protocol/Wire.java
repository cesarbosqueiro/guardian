package com.craftwar.guardian.protocol;

public final class Wire {

    public static final String CHANNEL = "CW|Guard";
    public static final int VERSION = 1;
    public static final int HEADER = 6;
    public static final int TAG = 16;
    public static final int MAX_MESSAGE = 32000;
    public static final int CHUNK_HEADER = 13;
    public static final int MAX_CHUNK = MAX_MESSAGE - HEADER - TAG - CHUNK_HEADER;

    private Wire() {
    }
}
