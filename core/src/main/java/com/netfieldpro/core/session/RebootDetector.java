package com.netfieldpro.core.session;

public final class RebootDetector {

    private RebootDetector() {
    }

    public static boolean rebooted(long createdAtMs, long monoEpochNs, long nowWallMs, long nowMonoNs) {
        long bootThen = createdAtMs - monoEpochNs / 1_000_000L;
        long bootNow = nowWallMs - nowMonoNs / 1_000_000L;
        return Math.abs(bootNow - bootThen) > 60_000L;
    }
}
