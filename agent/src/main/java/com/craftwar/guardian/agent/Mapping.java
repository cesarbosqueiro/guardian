package com.craftwar.guardian.agent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Mapping {

    private final String id;
    private final Properties names;

    private Mapping(String id, Properties names) {
        this.id = id;
        this.names = names;
    }

    public static Mapping load(String id) throws IOException {
        if (!id.matches("[a-z0-9.\\-]+")) throw new IOException("invalid mapping id: " + id);
        InputStream in = Mapping.class.getResourceAsStream("/mappings/" + id + ".properties");
        if (in == null) throw new IOException("unknown mapping: " + id);
        try {
            Properties p = new Properties();
            p.load(in);
            return new Mapping(id, p);
        } finally {
            in.close();
        }
    }

    public static Mapping of(String id, Properties names) {
        return new Mapping(id, names);
    }

    public String id() {
        return id;
    }

    public String get(String key) {
        String v = names.getProperty(key);
        if (v == null) throw new IllegalStateException("mapping " + id + " missing " + key);
        return v;
    }
}
