package com.craftwar.launcher.manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.craftwar.guardian.protocol.Ec;
import com.craftwar.guardian.protocol.Keys;

public class ManifestSourceTest {

    private static final String KEY = Keys.publicToBase64(Ec.newKeyPair().getPublic());
    private static final String SERVER = "{\"id\":\"pvp\",\"name\":\"PvP\",\"host\":\"h\",\"port\":25565,\"publicKey\":\"" + KEY + "\"}";
    private static final String PROFILE = "{\"id\":\"1.8.9\",\"mainClass\":\"M\",\"files\":[{\"path\":\"mods/a.jar\",\"kind\":\"mod\"}]}";

    private static Manifest parse(String json) throws IOException {
        return ManifestSource.parse(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String manifest(String servers, String profiles) {
        return "{\"version\":2,\"servers\":[" + servers + "],\"profiles\":[" + profiles + "]}";
    }

    private static void rejects(String json, String expected) {
        try {
            parse(json);
            fail("accepted: " + json);
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expected));
        }
    }

    @Test
    public void acceptsValidV2() throws IOException {
        Manifest m = parse(manifest(SERVER, PROFILE));
        assertEquals("pvp", m.server("missing").id);
        assertEquals("1.8.9", m.profile("missing").id);
    }

    @Test
    public void rejectsV1() {
        rejects("{\"version\":1,\"mainClass\":\"M\",\"files\":[]}", "format");
    }

    @Test
    public void rejectsWithoutProfiles() {
        rejects(manifest(SERVER, ""), "no profiles");
    }

    @Test
    public void rejectsServerWithoutKey() {
        rejects(manifest("{\"id\":\"pvp\",\"host\":\"h\",\"port\":1}", PROFILE), "public key");
    }

    @Test
    public void rejectsServerWithBadKey() {
        rejects(manifest("{\"id\":\"pvp\",\"host\":\"h\",\"port\":1,\"publicKey\":\"AAAA\"}", PROFILE), "invalid public key");
    }

    @Test
    public void rejectsUnsafePath() {
        rejects(manifest(SERVER, "{\"id\":\"x\",\"mainClass\":\"M\",\"files\":[{\"path\":\"../evil.jar\"}]}"), "unsafe path");
    }

    @Test
    public void rejectsDuplicateProfile() {
        rejects(manifest(SERVER, PROFILE + "," + PROFILE), "invalid profile id");
    }

    @Test
    public void rejectsProfileIdThatEscapesGameDir() {
        rejects(manifest(SERVER, "{\"id\":\"../x\",\"mainClass\":\"M\",\"files\":[]}"), "invalid profile id");
    }
}
