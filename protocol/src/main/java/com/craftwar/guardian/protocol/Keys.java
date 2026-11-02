package com.craftwar.guardian.protocol;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class Keys {

    private Keys() {
    }

    public static String publicToBase64(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static PublicKey publicFromBase64(String s) {
        return publicFromDer(Base64.getDecoder().decode(s.trim()));
    }

    public static PublicKey publicFromDer(byte[] der) {
        try {
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("invalid public key", e);
        }
    }

    public static PrivateKey privateFromDer(byte[] der) {
        try {
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("invalid private key", e);
        }
    }
}
