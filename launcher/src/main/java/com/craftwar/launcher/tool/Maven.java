package com.craftwar.launcher.tool;

final class Maven {

    private Maven() {
    }

    static String path(String coordinates, String classifier) {
        String[] c = coordinates.split(":");
        if (c.length < 3) throw new IllegalArgumentException("bad maven coordinates: " + coordinates);
        String group = c[0].replace('.', '/');
        String file = c[1] + "-" + c[2] + (classifier == null ? "" : "-" + classifier) + ".jar";
        return group + "/" + c[1] + "/" + c[2] + "/" + file;
    }
}
