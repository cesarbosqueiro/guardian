package com.craftwar.guardian.agent;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.craftwar.guardian.protocol.Keys;

public final class AgentConfig {

    private static final String KEY_SEPARATOR = ":";

    private final List<PublicKey> serverKeys;
    private final String mapping;

    private AgentConfig(List<PublicKey> serverKeys, String mapping) {
        this.serverKeys = serverKeys;
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
        String joined = values.containsKey("servers") ? values.get("servers") : values.get("server");
        if (joined == null || joined.isEmpty()) throw new IllegalArgumentException("missing server keys");
        List<PublicKey> keys = new ArrayList<PublicKey>();
        for (String k : joined.split(KEY_SEPARATOR)) {
            if (!k.isEmpty()) keys.add(Keys.publicFromBase64(k));
        }
        String mapping = values.containsKey("mapping") ? values.get("mapping") : "forge-1.8.9";
        return new AgentConfig(Collections.unmodifiableList(keys), mapping);
    }

    public List<PublicKey> serverKeys() {
        return serverKeys;
    }

    public String mapping() {
        return mapping;
    }
}
