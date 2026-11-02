package com.craftwar.launcher.manifest;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import com.craftwar.guardian.protocol.Ec;

public final class Signatures {

    private Signatures() {
    }

    public static String sign(PrivateKey key, byte[] data) {
        return Base64.getEncoder().encodeToString(Ec.sign(key, data));
    }

    public static boolean verify(PublicKey key, byte[] data, byte[] signatureFile) {
        try {
            byte[] sig = Base64.getDecoder().decode(new String(signatureFile, StandardCharsets.US_ASCII).trim());
            return Ec.verify(key, data, sig);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
