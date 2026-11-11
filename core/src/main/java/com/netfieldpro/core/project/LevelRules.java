package com.netfieldpro.core.project;

import java.util.Collection;

public final class LevelRules {

    public static final long MIN_LEVEL = -5L;
    public static final long MAX_LEVEL = 99L;

    private LevelRules() {
    }

    public static boolean isValid(long level) {
        return level >= MIN_LEVEL && level <= MAX_LEVEL;
    }

    public static long defaultNextLevel(Collection<Long> existingLevels) {
        if (existingLevels == null || existingLevels.isEmpty()) {
            return 0L;
        }
        long max = Long.MIN_VALUE;
        for (Long lvl : existingLevels) {
            if (lvl != null && lvl > max) {
                max = lvl;
            }
        }
        if (max == Long.MIN_VALUE) {
            return 0L;
        }
        long next = max + 1L;
        if (next < MIN_LEVEL) {
            return MIN_LEVEL;
        }
        if (next > MAX_LEVEL) {
            return MAX_LEVEL;
        }
        return next;
    }
}
