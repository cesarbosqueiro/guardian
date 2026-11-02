package com.craftwar.launcher.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class Rules {

    static final String[] OPERATING_SYSTEMS = {"windows", "linux", "osx"};

    private Rules() {
    }

    static boolean allows(JsonObject library, String os) {
        if (!library.has("rules")) return true;
        boolean allowed = false;
        JsonArray rules = library.getAsJsonArray("rules");
        for (JsonElement e : rules) {
            JsonObject rule = e.getAsJsonObject();
            if (!appliesTo(rule, os)) continue;
            allowed = "allow".equals(VersionChain.string(rule, "action"));
        }
        return allowed;
    }

    private static boolean appliesTo(JsonObject rule, String os) {
        if (!rule.has("os")) return true;
        return os.equals(VersionChain.string(rule.getAsJsonObject("os"), "name"));
    }
}
