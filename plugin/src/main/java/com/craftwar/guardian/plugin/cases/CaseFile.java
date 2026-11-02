package com.craftwar.guardian.plugin.cases;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class CaseFile {

    private static final int RECENT = 100;

    private final Score score;
    private final Deque<ServerFinding> recent = new ArrayDeque<ServerFinding>();

    CaseFile(double halfLifeMinutes) {
        score = new Score(halfLifeMinutes);
    }

    double add(ServerFinding f) {
        if (recent.size() == RECENT) recent.removeFirst();
        recent.addLast(f);
        return score.add(f.weight, f.millis);
    }

    public double score(long nowMillis) {
        return score.value(nowMillis);
    }

    public List<ServerFinding> latest(int n) {
        List<ServerFinding> out = new ArrayList<ServerFinding>(Math.min(n, recent.size()));
        for (java.util.Iterator<ServerFinding> it = recent.descendingIterator(); it.hasNext() && out.size() < n; ) out.add(it.next());
        return out;
    }
}
