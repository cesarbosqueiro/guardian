package com.craftwar.guardian.protocol;

import java.security.KeyPair;
import java.security.PrivateKey;

public final class ServerHandshake {

    private final PrivateKey identity;
    private final KeyPair ephemeral = Ec.newKeyPair();
    private final byte[] nonce = Ec.random(Transcript.NONCE);
    private final byte[] serverPoint = Ec.encodePoint(ephemeral.getPublic());

    public ServerHandshake(PrivateKey identity) {
        this.identity = identity;
    }

    public byte[] hello() {
        byte[] signature = Ec.sign(identity, Transcript.hello(nonce, serverPoint));
        Writer w = new Writer(Transcript.NONCE + Ec.POINT + 2 + signature.length);
        w.bytes(nonce).bytes(serverPoint).blob(signature);
        return Frame.plain(MessageType.HELLO, w.toArray());
    }

    public SecureChannel accept(byte[] replyFrame) {
        Reader r = new Reader(Frame.plainPayload(replyFrame, MessageType.REPLY));
        byte[] clientPoint = r.bytes(Ec.POINT);
        byte[] tag = r.bytes(Wire.TAG);
        r.requireEnd();
        byte[] shared = Ec.agree(ephemeral.getPrivate(), Ec.decodePoint(clientPoint));
        SessionKeys keys = SessionKeys.derive(shared, nonce, serverPoint, clientPoint);
        byte[] expected = keys.confirmTag(nonce, serverPoint, clientPoint);
        if (!Hmac.equalPrefix(expected, 0, tag, 0, Wire.TAG)) throw new ProtocolException("bad confirmation");
        return new SecureChannel(keys.serverToClient, keys.clientToServer);
    }
}
