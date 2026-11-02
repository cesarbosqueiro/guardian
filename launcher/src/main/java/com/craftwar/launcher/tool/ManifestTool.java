package com.craftwar.launcher.tool;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.craftwar.guardian.protocol.Ec;
import com.craftwar.guardian.protocol.Keys;
import com.craftwar.launcher.manifest.Manifest;
import com.craftwar.launcher.manifest.ManifestSource;
import com.craftwar.launcher.manifest.Profile;
import com.craftwar.launcher.manifest.Signatures;
import com.craftwar.launcher.sync.Files2;

public final class ManifestTool {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String USAGE = "uso:\n"
        + "  keygen <dir>\n"
        + "  build <template.json> <manifest.key> <saida>";

    private ManifestTool() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 2 && args[0].equals("keygen")) keygen(new File(args[1]));
        else if (args.length == 4 && args[0].equals("build")) build(new File(args[1]), new File(args[2]), new File(args[3]));
        else {
            System.err.println(USAGE);
            System.exit(2);
        }
    }

    private static void keygen(File dir) throws IOException {
        Files2.mkdirs(dir);
        KeyPair pair = Ec.newKeyPair();
        Files.write(new File(dir, "manifest.key").toPath(), pair.getPrivate().getEncoded());
        Files.write(new File(dir, "manifest.pub").toPath(), pair.getPublic().getEncoded());
        System.out.println("manifestKey=" + Keys.publicToBase64(pair.getPublic()));
    }

    private static void build(File templateFile, File keyFile, File out) throws IOException {
        Template template;
        try (Reader r = Files.newBufferedReader(templateFile.toPath(), StandardCharsets.UTF_8)) {
            template = GSON.fromJson(r, Template.class);
        }
        Files2.mkdirs(out);
        Manifest m = new ManifestBuilder(templateFile, template, out).build();
        byte[] json = GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
        ManifestSource.parse(json);
        PrivateKey key = Keys.privateFromDer(Files.readAllBytes(keyFile.toPath()));
        Files.write(new File(out, "manifest.json").toPath(), json);
        Files.write(new File(out, "manifest.json.sig").toPath(), Signatures.sign(key, json).getBytes(StandardCharsets.US_ASCII));
        int files = 0;
        for (Profile p : m.profiles) files += p.files.size();
        System.out.println(m.profiles.size() + " perfis, " + files + " arquivos, " + m.servers.size()
            + " servidores; publique o conteudo de " + out + " (files/ em " + template.cdnBaseUrl + ")");
    }
}
