package com.craftwar.guardian.agent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class Health {

    public static final int OK = 0;
    public static final int DEGRADED = 1;
    public static final int DISABLED = 2;

    private static final int MAX_PROBLEMS = 64;
    private static final AtomicInteger STATE = new AtomicInteger(OK);
    private static final AtomicInteger PROBLEM_COUNT = new AtomicInteger();
    private static final Queue<String> PROBLEMS = new ConcurrentLinkedQueue<String>();
    private static volatile boolean probeFailed;

    private Health() {
    }

    public static int state() {
        return STATE.get();
    }

    public static void degrade(String reason) {
        raise(DEGRADED);
        report(reason);
    }

    public static void disable(String reason) {
        raise(DISABLED);
        report(reason);
    }

    public static void probeFailed(Throwable t) {
        if (probeFailed) return;
        probeFailed = true;
        degrade("probe failed: " + t);
    }

    public static String nextProblem() {
        return PROBLEMS.poll();
    }

    private static void raise(int level) {
        for (int s = STATE.get(); s < level; s = STATE.get()) {
            if (STATE.compareAndSet(s, level)) return;
        }
    }

    private static void report(String reason) {
        Log.warn(reason);
        if (PROBLEM_COUNT.incrementAndGet() <= MAX_PROBLEMS) PROBLEMS.add(reason);
    }
}
