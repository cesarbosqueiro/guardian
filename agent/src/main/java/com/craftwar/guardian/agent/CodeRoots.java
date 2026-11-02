package com.craftwar.guardian.agent;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CodeRoots {

    private final List<String> roots;

    public CodeRoots(List<String> roots) {
        this.roots = roots;
    }

    public static CodeRoots fromEnvironment() {
        List<String> roots = new ArrayList<String>();
        roots.add(normalize(new File(System.getProperty("java.home")).getAbsolutePath()));
        for (String entry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
            if (!entry.isEmpty()) roots.add(normalize(new File(entry).getAbsolutePath()));
        }
        roots.add(normalize(new File(System.getProperty("user.dir"), "mods").getAbsolutePath()));
        return new CodeRoots(roots);
    }

    public boolean trusted(URL location) {
        String path = pathOf(location);
        if (path == null) return false;
        for (String root : roots) {
            if (path.startsWith(root)) return true;
        }
        return false;
    }

    public static String pathOf(URL location) {
        if (location == null) return null;
        try {
            String s = location.toString();
            if (s.startsWith("jar:")) {
                int bang = s.indexOf("!/");
                s = s.substring(4, bang < 0 ? s.length() : bang);
            }
            if (!s.startsWith("file:")) return null;
            return normalize(new File(new URL(s).toURI()).getAbsolutePath());
        } catch (Exception e) {
            return null;
        }
    }

    static String normalize(String path) {
        String p = path.replace('\\', '/');
        return File.separatorChar == '\\' ? p.toLowerCase(Locale.ROOT) : p;
    }
}
