package com.craftwar.guardian.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.craftwar.guardian.protocol.Ec;
import com.craftwar.guardian.protocol.Keys;

public final class ServerIdentity {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    private ServerIdentity(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public static ServerIdentity loadOrCreate(File dir) throws IOException {
        File priv = new File(dir, "identity.key");
        File pub = new File(dir, "identity.pub");
        if (priv.isFile() && pub.isFile()) {
            return new ServerIdentity(Keys.privateFromDer(Files.readAllBytes(priv.toPath())),
                Keys.publicFromDer(Files.readAllBytes(pub.toPath())));
        }
        KeyPair pair = Ec.newKeyPair();
        Files.createDirectories(dir.toPath());
        Files.write(priv.toPath(), pair.getPrivate().getEncoded());
        Files.write(pub.toPath(), pair.getPublic().getEncoded());
        priv.setReadable(false, false);
        priv.setReadable(true, true);
        return new ServerIdentity(pair.getPrivate(), pair.getPublic());
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

    public String publicKeyBase64() {
        return Keys.publicToBase64(publicKey);
    }
}
