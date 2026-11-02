package com.craftwar.guardian.plugin.cases;

public final class Score {

    private final double decayPerMilli;
    private double value;
    private long updatedMillis;

    public Score(double halfLifeMinutes) {
        decayPerMilli = Math.log(2) / (halfLifeMinutes * 60_000.0);
    }

    public double add(double weight, long nowMillis) {
        double v = value(nowMillis) + weight;
        value = v;
        return v;
    }

    public double value(long nowMillis) {
        if (value != 0 && nowMillis > updatedMillis) value *= Math.exp(-decayPerMilli * (nowMillis - updatedMillis));
        updatedMillis = nowMillis;
        return value;
    }
}
