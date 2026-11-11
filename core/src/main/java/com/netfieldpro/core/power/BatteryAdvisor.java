package com.netfieldpro.core.power;

public class BatteryAdvisor {

    public enum Level {
        NONE,
        WARN,
        CRITICAL
    }

    public static final int WARN_PCT = 20;
    public static final int CRIT_PCT = 10;
    public static final int CLEAR_PCT = 25;

    private Level current = Level.NONE;

    public Level onSample(int pct, boolean charging) {
        if (pct < 0 || pct > 100) {
            return current;
        }
        if (charging) {
            current = Level.NONE;
            return current;
        }
        if (pct <= CRIT_PCT) {
            current = Level.CRITICAL;
            return current;
        }
        if (pct <= WARN_PCT) {
            current = Level.WARN;
            return current;
        }
        if (current != Level.NONE && pct < CLEAR_PCT) {
            current = Level.WARN;
            return current;
        }
        current = Level.NONE;
        return current;
    }

    public Level onLowBroadcast() {
        if (current == Level.NONE) {
            current = Level.WARN;
        }
        return current;
    }

    public Level current() {
        return current;
    }

    public void reset() {
        current = Level.NONE;
    }
}

