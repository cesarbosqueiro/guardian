package com.craftwar.guardian.protocol;

public final class FindingKind {

    public static final int ORPHAN_CLASS = 1;
    public static final int REDEFINITION = 2;
    public static final int JVM_ARGUMENT = 3;
    public static final int AGENT_DEGRADED = 4;
    public static final int FINDINGS_DROPPED = 5;
    public static final int FOREIGN_SOURCE = 6;

    private FindingKind() {
    }

    public static String name(int kind) {
        switch (kind) {
            case ORPHAN_CLASS: return "ORPHAN_CLASS";
            case REDEFINITION: return "REDEFINITION";
            case JVM_ARGUMENT: return "JVM_ARGUMENT";
            case AGENT_DEGRADED: return "AGENT_DEGRADED";
            case FINDINGS_DROPPED: return "FINDINGS_DROPPED";
            case FOREIGN_SOURCE: return "FOREIGN_SOURCE";
            default: return "UNKNOWN_" + kind;
        }
    }
}
