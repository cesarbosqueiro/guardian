package com.craftwar.guardian.plugin.cases;

public final class ServerFinding {

    public final long millis;
    public final String kind;
    public final Source source;
    public final double weight;
    public final String subject;
    public final String detail;

    public ServerFinding(long millis, String kind, Source source, double weight, String subject, String detail) {
        this.millis = millis;
        this.kind = kind;
        this.source = source;
        this.weight = weight;
        this.subject = subject;
        this.detail = detail;
    }

    public String line() {
        return millis + "\t" + kind + "\t" + source + "\t" + weight + "\t" + clean(subject) + "\t" + clean(detail);
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
