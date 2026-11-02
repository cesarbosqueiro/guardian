package com.craftwar.guardian.agent;

import com.craftwar.guardian.agent.session.Session;

public final class Bridge {

    private static volatile Session session;

    private Bridge() {
    }

    static void install(Session s) {
        session = s;
    }

    public static boolean active() {
        return session != null;
    }

    public static void connected() {
        Session s = session;
        if (s != null) s.connected();
    }

    public static void disconnected() {
        Session s = session;
        if (s != null) s.disconnected();
    }

    public static void received(byte[] frame) {
        Session s = session;
        if (s != null) s.received(frame);
    }

    public static byte[] nextOutgoing() {
        Session s = session;
        return s == null ? null : s.nextOutgoing();
    }

    public static void gameState(float sensitivity, boolean smoothCamera, float gamma, int fps, int flags) {
        GameState.update(sensitivity, smoothCamera, gamma, fps, flags);
    }

    public static void environment(String gpu, int width, int height, String[] mods, String[] resourcePacks) {
        Environment.publish(gpu, width, height, mods, resourcePacks);
    }

    public static String notice() {
        return Notice.current();
    }
}
