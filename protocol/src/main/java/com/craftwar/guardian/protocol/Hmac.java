package com.craftwar.guardian.protocol;

import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Hmac {

    public static final int SIZE = 32;

    private Hmac() {
    }

    public static Mac create(byte[] key) {
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(key, "HmacSHA256"));
            return m;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] of(byte[] key, byte[]... parts) {
        Mac m = create(key);
        for (byte[] p : parts) m.update(p);
        return m.doFinal();
    }

    public static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) {
        Mac expand = create(of(salt, ikm));
        byte[] out = new byte[length];
        byte[] t = new byte[0];
        for (int done = 0, i = 1; done < length; i++) {
            expand.update(t);
            expand.update(info);
            expand.update((byte) i);
            t = expand.doFinal();
            int n = Math.min(SIZE, length - done);
            System.arraycopy(t, 0, out, done, n);
            done += n;
        }
        return out;
    }

    public static boolean equalPrefix(byte[] a, int aOff, byte[] b, int bOff, int len) {
        int diff = 0;
        for (int i = 0; i < len; i++) diff |= a[aOff + i] ^ b[bOff + i];
        return diff == 0;
    }
}
