package com.craftwar.guardian.agent;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import com.craftwar.guardian.protocol.Keys;

public final class AgentConfig {

    private final PublicKey serverKey;
    private final String mapping;

    private AgentConfig(PublicKey serverKey, String mapping) {
        this.serverKey = serverKey;
        this.mapping = mapping;
    }

    public static AgentConfig parse(String args) {
        Map<String, String> values = new HashMap<String, String>();
        if (args != null) {
            for (String pair : args.split(",")) {
                int eq = pair.indexOf('=');
                if (eq > 0) values.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        String key = values.get("server");
        if (key == null) throw new IllegalArgumentException("missing server key");
        String mapping = values.containsKey("mapping") ? values.get("mapping") : "forge-1.8.9";
        return new AgentConfig(Keys.publicFromBase64(key), mapping);
    }

    public PublicKey serverKey() {
        return serverKey;
    }

    public String mapping() {
        return mapping;
    }
}
