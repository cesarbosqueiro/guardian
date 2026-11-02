package com.craftwar.guardian.protocol;

import java.nio.charset.StandardCharsets;

final class Transcript {

    static final int NONCE = 32;
    private static final byte[] HELLO_LABEL = ascii("CWG1 hello");

    private Transcript() {
    }

    static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] concat(byte[]... parts) {
        int n = 0;
        for (byte[] p : parts) n += p.length;
        byte[] out = new byte[n];
        int at = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, at, p.length);
            at += p.length;
        }
        return out;
    }

    static byte[] hello(byte[] nonce, byte[] serverPoint) {
        return concat(HELLO_LABEL, nonce, serverPoint);
    }
}
