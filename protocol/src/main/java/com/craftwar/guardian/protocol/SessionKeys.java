package com.craftwar.guardian.protocol;

import java.util.Arrays;

final class SessionKeys {

    private static final byte[] KEY_INFO = Transcript.ascii("CWG1 keys");
    private static final byte[] CONFIRM_LABEL = Transcript.ascii("CWG1 confirm");

    final byte[] confirm;
    final byte[] clientToServer;
    final byte[] serverToClient;

    private SessionKeys(byte[] okm) {
        confirm = Arrays.copyOfRange(okm, 0, 32);
        clientToServer = Arrays.copyOfRange(okm, 32, 64);
        serverToClient = Arrays.copyOfRange(okm, 64, 96);
    }

    static SessionKeys derive(byte[] shared, byte[] nonce, byte[] serverPoint, byte[] clientPoint) {
        byte[] info = Transcript.concat(KEY_INFO, serverPoint, clientPoint);
        return new SessionKeys(Hmac.hkdf(nonce, shared, info, 96));
    }

    byte[] confirmTag(byte[] nonce, byte[] serverPoint, byte[] clientPoint) {
        return Arrays.copyOf(Hmac.of(confirm, CONFIRM_LABEL, nonce, serverPoint, clientPoint), Wire.TAG);
    }
}
