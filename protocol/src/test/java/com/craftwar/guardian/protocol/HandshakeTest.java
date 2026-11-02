package com.craftwar.guardian.protocol;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.security.KeyPair;

import org.junit.Test;

public class HandshakeTest {

    private final KeyPair identity = Ec.newKeyPair();

    private SecureChannel[] connect() {
        ServerHandshake server = new ServerHandshake(identity.getPrivate());
        ClientHandshake client = new ClientHandshake(identity.getPublic());
        byte[] reply = client.reply(server.hello());
        return new SecureChannel[] {server.accept(reply), client.channel()};
    }

    @Test
    public void bothDirectionsRoundTrip() {
        SecureChannel[] c = connect();
        byte[] up = {1, 2, 3};
        Frame f = c[0].open(c[1].seal(MessageType.HEARTBEAT, up));
        assertEquals(MessageType.HEARTBEAT, f.type);
        assertArrayEquals(up, f.payload);
        Frame g = c[1].open(c[0].seal(MessageType.REQUEST, new byte[0]));
        assertEquals(MessageType.REQUEST, g.type);
    }

    @Test(expected = ProtocolException.class)
    public void rejectsForgedServer() {
        ServerHandshake impostor = new ServerHandshake(Ec.newKeyPair().getPrivate());
        new ClientHandshake(identity.getPublic()).reply(impostor.hello());
    }

    @Test(expected = ProtocolException.class)
    public void rejectsTamperedFrame() {
        SecureChannel[] c = connect();
        byte[] frame = c[1].seal(MessageType.HEARTBEAT, new byte[] {9, 9});
        frame[Wire.HEADER] ^= 1;
        c[0].open(frame);
    }

    @Test(expected = ProtocolException.class)
    public void rejectsReplay() {
        SecureChannel[] c = connect();
        byte[] frame = c[1].seal(MessageType.HEARTBEAT, new byte[] {7});
        c[0].open(frame);
        c[0].open(frame);
    }

    @Test(expected = ProtocolException.class)
    public void rejectsReflectedFrame() {
        SecureChannel[] c = connect();
        c[1].open(c[1].seal(MessageType.HEARTBEAT, new byte[] {7}));
    }
}
