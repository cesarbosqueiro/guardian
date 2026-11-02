package com.craftwar.guardian.protocol;

import java.security.KeyPair;
import java.security.PublicKey;

public final class ClientHandshake {

    private final PublicKey serverIdentity;
    private SecureChannel channel;

    public ClientHandshake(PublicKey serverIdentity) {
        this.serverIdentity = serverIdentity;
    }

    public byte[] reply(byte[] helloFrame) {
        Reader r = new Reader(Frame.plainPayload(helloFrame, MessageType.HELLO));
        byte[] nonce = r.bytes(Transcript.NONCE);
        byte[] serverPoint = r.bytes(Ec.POINT);
        byte[] signature = r.blob();
        r.requireEnd();
        if (!Ec.verify(serverIdentity, Transcript.hello(nonce, serverPoint), signature)) {
            throw new ProtocolException("server signature rejected");
        }
        KeyPair ephemeral = Ec.newKeyPair();
        byte[] clientPoint = Ec.encodePoint(ephemeral.getPublic());
        byte[] shared = Ec.agree(ephemeral.getPrivate(), Ec.decodePoint(serverPoint));
        SessionKeys keys = SessionKeys.derive(shared, nonce, serverPoint, clientPoint);
        channel = new SecureChannel(keys.clientToServer, keys.serverToClient);
        byte[] tag = keys.confirmTag(nonce, serverPoint, clientPoint);
        return Frame.plain(MessageType.REPLY, new Writer(Ec.POINT + Wire.TAG).bytes(clientPoint).bytes(tag).toArray());
    }

    public SecureChannel channel() {
        if (channel == null) throw new IllegalStateException("handshake not completed");
        return channel;
    }
}
