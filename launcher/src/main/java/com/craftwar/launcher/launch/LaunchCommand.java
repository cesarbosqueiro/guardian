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
import com.craftwar.launcher.sync.Natives;

public final class LaunchCommand {

    private LaunchCommand() {
    }

    public static UUID offlineUuid(String nick) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8));
    }

    public static List<String> build(Manifest m, File java, File gameDir, String os, String nick, int memoryMb) throws IOException {
        List<String> cmd = new ArrayList<String>();
        cmd.add(java.getPath());
        cmd.add("-Xms" + memoryMb + "M");
        cmd.add("-Xmx" + memoryMb + "M");
        cmd.addAll(m.java.flags);
        cmd.add("-XX:+DisableAttachMechanism");
        cmd.add("-Djava.library.path=" + new File(gameDir, Natives.DIR).getAbsolutePath());
        cmd.add("-javaagent:" + agent(m, gameDir, os).getAbsolutePath() + "=server=" + m.server.publicKey + ",mapping=" + m.agentMapping);
        cmd.add("-cp");
        cmd.add(classpath(m, gameDir, os));
        cmd.add(m.mainClass);
        Map<String, String> vars = variables(m, gameDir, nick);
        for (String arg : m.gameArguments) cmd.add(substitute(arg, vars));
        if (m.server.host != null) {
            cmd.add("--server");
            cmd.add(m.server.host);
            cmd.add("--port");
            cmd.add(Integer.toString(m.server.port));
        }
        return cmd;
    }

    private static File agent(Manifest m, File gameDir, String os) throws IOException {
        for (ManifestFile f : m.files) {
            if (ManifestFile.AGENT.equals(f.kind) && f.forOs(os)) return Paths.resolve(gameDir, f.path);
        }
        throw new IOException("manifest has no agent");
    }

    private static String classpath(Manifest m, File gameDir, String os) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (ManifestFile f : m.files) {
            if (!f.onClasspath() || !f.forOs(os)) continue;
            if (sb.length() > 0) sb.append(File.pathSeparatorChar);
            sb.append(Paths.resolve(gameDir, f.path).getAbsolutePath());
        }
        return sb.toString();
    }

    private static Map<String, String> variables(Manifest m, File gameDir, String nick) {
        Map<String, String> v = new HashMap<String, String>();
        v.put("auth_player_name", nick);
        v.put("auth_uuid", offlineUuid(nick).toString().replace("-", ""));
        v.put("auth_access_token", "0");
        v.put("user_properties", "{}");
        v.put("user_type", "legacy");
        v.put("version_name", m.name == null ? "CraftWar" : m.name);
        v.put("game_directory", gameDir.getAbsolutePath());
        v.put("assets_root", new File(gameDir, "assets").getAbsolutePath());
        v.put("assets_index_name", m.assetIndex == null ? "" : m.assetIndex.id);
        return v;
    }

    static String substitute(String arg, Map<String, String> vars) {
        if (!arg.startsWith("${") || !arg.endsWith("}")) return arg;
        String v = vars.get(arg.substring(2, arg.length() - 1));
        return v == null ? arg : v;
    }
}
