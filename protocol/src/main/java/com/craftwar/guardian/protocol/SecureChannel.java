package com.craftwar.guardian.protocol;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Mac;

public final class SecureChannel {

    private static final long MAX_SEQ = 0xFFFFFFFFL;

    private final Mac sendMac;
    private final Mac receiveMac;
    private final byte[] sendTag = new byte[Hmac.SIZE];
    private final byte[] receiveTag = new byte[Hmac.SIZE];
    private long sendSeq;
    private long lastReceivedSeq = -1;

    SecureChannel(byte[] sendKey, byte[] receiveKey) {
        sendMac = Hmac.create(sendKey);
        receiveMac = Hmac.create(receiveKey);
    }

    public synchronized byte[] seal(int type, byte[] payload) {
        if (sendSeq > MAX_SEQ) throw new ProtocolException("sequence exhausted");
        int body = Wire.HEADER + payload.length;
        if (body > Wire.MAX_MESSAGE) throw new IllegalArgumentException("message too large: " + body);
        byte[] out = new byte[body + Wire.TAG];
        int seq = (int) sendSeq++;
        out[0] = (byte) Wire.VERSION;
        out[1] = (byte) type;
        out[2] = (byte) (seq >>> 24);
        out[3] = (byte) (seq >>> 16);
        out[4] = (byte) (seq >>> 8);
        out[5] = (byte) seq;
        System.arraycopy(payload, 0, out, Wire.HEADER, payload.length);
        sendMac.update(out, 0, body);
        finish(sendMac, sendTag);
        System.arraycopy(sendTag, 0, out, body, Wire.TAG);
        return out;
    }

    public Frame open(byte[] frame) {
        int body = frame.length - Wire.TAG;
        if (body < Wire.HEADER) throw new ProtocolException("short frame");
        int type = Frame.typeOf(frame);
        long seq = ((frame[2] & 0xFFL) << 24) | (frame[3] & 0xFF) << 16 | (frame[4] & 0xFF) << 8 | frame[5] & 0xFF;
        synchronized (receiveMac) {
            receiveMac.update(frame, 0, body);
            finish(receiveMac, receiveTag);
            if (!Hmac.equalPrefix(receiveTag, 0, frame, body, Wire.TAG)) throw new ProtocolException("bad tag");
            if (seq <= lastReceivedSeq) throw new ProtocolException("replayed or reordered seq " + seq);
            lastReceivedSeq = seq;
        }
        return new Frame(type, Arrays.copyOfRange(frame, Wire.HEADER, body));
    }

    private static void finish(Mac mac, byte[] into) {
        try {
            mac.doFinal(into, 0);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
