package com.craftwar.launcher.launch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.craftwar.launcher.manifest.Manifest;
import com.craftwar.launcher.manifest.ManifestFile;
import com.craftwar.launcher.manifest.Paths;
import com.craftwar.launcher.manifest.Profile;
import com.craftwar.launcher.manifest.ServerEntry;
import com.craftwar.launcher.sync.Assets;
import com.craftwar.launcher.sync.JarMod;
import com.craftwar.launcher.sync.Natives;

public final class LaunchCommand {

    public static final String KEY_SEPARATOR = ":";

    private LaunchCommand() {
    }

    public static UUID offlineUuid(String nick) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8));
    }

    public static List<String> build(Manifest m, Profile p, LaunchTarget t) throws IOException {
        List<String> cmd = new ArrayList<String>();
        cmd.add(t.java.getPath());
        cmd.add("-Xms" + t.memoryMb + "M");
        cmd.add("-Xmx" + t.memoryMb + "M");
        cmd.addAll(m.java.flags);
        cmd.add("-XX:+DisableAttachMechanism");
        cmd.add("-Djava.library.path=" + new File(t.profileDir, Natives.DIR).getAbsolutePath());
        cmd.add("-javaagent:" + agent(p, t).getAbsolutePath() + "=servers=" + serverKeys(m) + ",mapping=" + p.agentMapping);
        cmd.add("-cp");
        cmd.add(classpath(p, t));
        cmd.add(p.mainClass);
        Map<String, String> vars = variables(m, p, t);
        for (String arg : p.gameArguments) cmd.add(substitute(arg, vars));
        return cmd;
    }

    static String serverKeys(Manifest m) {
        StringBuilder sb = new StringBuilder();
        for (ServerEntry s : m.servers) {
            if (sb.length() > 0) sb.append(KEY_SEPARATOR);
            sb.append(s.publicKey);
        }
        return sb.toString();
    }

    private static File agent(Profile p, LaunchTarget t) throws IOException {
        for (ManifestFile f : p.files) {
            if (f.is(ManifestFile.AGENT) && f.forOs(t.os)) return Paths.resolve(t.profileDir, f.path);
        }
        throw new IOException("profile " + p.id + " has no agent");
    }

    private static String classpath(Profile p, LaunchTarget t) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (ManifestFile f : p.files) {
            if (!f.forOs(t.os) || !(f.is(ManifestFile.LIBRARY) || f.is(ManifestFile.GAME))) continue;
            String path = f.is(ManifestFile.GAME) && p.patchedJar ? JarMod.patchedPath(f) : f.path;
            if (sb.length() > 0) sb.append(File.pathSeparatorChar);
            sb.append(Paths.resolve(t.profileDir, path).getAbsolutePath());
        }
        return sb.toString();
    }

    private static Map<String, String> variables(Manifest m, Profile p, LaunchTarget t) {
        String indexId = p.assetIndex == null ? "" : p.assetIndex.id;
        Map<String, String> v = new HashMap<String, String>();
        v.put("auth_player_name", t.nick);
        v.put("auth_uuid", offlineUuid(t.nick).toString().replace("-", ""));
        v.put("auth_access_token", "0");
        v.put("auth_session", "0");
        v.put("user_properties", "{}");
        v.put("user_type", "legacy");
        v.put("version_name", m.name == null ? p.id : m.name + " " + p.id);
        v.put("game_directory", t.profileDir.getAbsolutePath());
        v.put("assets_root", t.assetsDir.getAbsolutePath());
        v.put("assets_index_name", indexId);
        v.put("game_assets", Assets.virtualDir(t.assetsDir, indexId).getAbsolutePath());
        return v;
    }

    static String substitute(String arg, Map<String, String> vars) {
        if (!arg.startsWith("${") || !arg.endsWith("}")) return arg;
        String v = vars.get(arg.substring(2, arg.length() - 1));
        return v == null ? arg : v;
    }
}
