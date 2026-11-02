package com.craftwar.guardian.protocol;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

public final class ClientHandshake {

    private final List<PublicKey> trustedServers;
    private SecureChannel channel;
    private PublicKey matchedServer;

    public ClientHandshake(PublicKey serverIdentity) {
        this(Collections.singletonList(serverIdentity));
    }

    public ClientHandshake(List<PublicKey> trustedServers) {
        if (trustedServers.isEmpty()) throw new IllegalArgumentException("no trusted server keys");
        this.trustedServers = trustedServers;
    }

    public byte[] reply(byte[] helloFrame) {
        Reader r = new Reader(Frame.plainPayload(helloFrame, MessageType.HELLO));
        byte[] nonce = r.bytes(Transcript.NONCE);
        byte[] serverPoint = r.bytes(Ec.POINT);
        byte[] signature = r.blob();
        r.requireEnd();
        matchedServer = signer(Transcript.hello(nonce, serverPoint), signature);
        if (matchedServer == null) throw new ProtocolException("server signature rejected");
        KeyPair ephemeral = Ec.newKeyPair();
        byte[] clientPoint = Ec.encodePoint(ephemeral.getPublic());
        byte[] shared = Ec.agree(ephemeral.getPrivate(), Ec.decodePoint(serverPoint));
        SessionKeys keys = SessionKeys.derive(shared, nonce, serverPoint, clientPoint);
        channel = new SecureChannel(keys.clientToServer, keys.serverToClient);
        byte[] tag = keys.confirmTag(nonce, serverPoint, clientPoint);
        return Frame.plain(MessageType.REPLY, new Writer(Ec.POINT + Wire.TAG).bytes(clientPoint).bytes(tag).toArray());
    }

    private PublicKey signer(byte[] transcript, byte[] signature) {
        for (PublicKey k : trustedServers) {
            if (Ec.verify(k, transcript, signature)) return k;
        }
        return null;
    }

    public PublicKey matchedServer() {
        return matchedServer;
    }

    public SecureChannel channel() {
        if (channel == null) throw new IllegalStateException("handshake not completed");
        return channel;
    }
}
