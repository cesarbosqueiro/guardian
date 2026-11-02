package com.craftwar.client.hud;

final class CachedText {

    private final String prefix;
    private final String separator;
    private int first = Integer.MIN_VALUE;
    private int second = Integer.MIN_VALUE;
    private String text = "";

    CachedText(String prefix) {
        this(prefix, "");
    }

    CachedText(String prefix, String separator) {
        this.prefix = prefix;
        this.separator = separator;
    }

    String of(int v) {
        if (v != first) {
            first = v;
            text = prefix + v;
        }
        return text;
    }

    String of(int a, int b) {
        if (a != first || b != second) {
            first = a;
            second = b;
            text = prefix + a + separator + b;
        }
        return text;
    }
}
