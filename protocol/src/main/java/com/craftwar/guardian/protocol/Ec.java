package com.craftwar.guardian.protocol;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;

import javax.crypto.KeyAgreement;

public final class Ec {

    public static final int POINT = 65;
    private static final int COORD = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile ECParameterSpec curve;

    private Ec() {
    }

    public static byte[] random(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }

    public static KeyPair newKeyPair() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
            return g.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("P-256 unavailable; SunEC missing from the JRE?", e);
        }
    }

    private static ECParameterSpec curve() {
        ECParameterSpec c = curve;
        if (c == null) {
            c = ((ECPublicKey) newKeyPair().getPublic()).getParams();
            curve = c;
        }
        return c;
    }

    public static byte[] encodePoint(PublicKey key) {
        ECPoint w = ((ECPublicKey) key).getW();
        byte[] out = new byte[POINT];
        out[0] = 0x04;
        copyCoordinate(w.getAffineX(), out, 1);
        copyCoordinate(w.getAffineY(), out, 1 + COORD);
        return out;
    }

    private static void copyCoordinate(BigInteger v, byte[] out, int off) {
        byte[] b = v.toByteArray();
        int from = Math.max(0, b.length - COORD);
        int n = b.length - from;
        System.arraycopy(b, from, out, off + COORD - n, n);
    }

    public static PublicKey decodePoint(byte[] p) {
        if (p.length != POINT || p[0] != 0x04) throw new ProtocolException("invalid EC point");
        try {
            BigInteger x = new BigInteger(1, java.util.Arrays.copyOfRange(p, 1, 1 + COORD));
            BigInteger y = new BigInteger(1, java.util.Arrays.copyOfRange(p, 1 + COORD, POINT));
            return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), curve()));
        } catch (GeneralSecurityException e) {
            throw new ProtocolException("EC point rejected", e);
        }
    }

    public static byte[] agree(PrivateKey mine, PublicKey theirs) {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(mine);
            ka.doPhase(theirs, true);
            return ka.generateSecret();
        } catch (GeneralSecurityException e) {
            throw new ProtocolException("ECDH failed", e);
        }
    }

    public static byte[] sign(PrivateKey key, byte[] data) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(key, RANDOM);
            s.update(data);
            return s.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean verify(PublicKey key, byte[] data, byte[] signature) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(key);
            s.update(data);
            return s.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
