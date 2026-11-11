package com.netfieldpro.core.wifi;

import java.util.List;

public final class TimestampPlausibility {
    private TimestampPlausibility() {}

    public static final long FUTURE_TOLERANCE_NS = 1_000_000_000L;
    public static final long PAST_MAXIMUM_NS = 30_000_000_000L;

    public static boolean isPlausible(long tsNs, long arrivalMonoNs) {
        if (tsNs <= 0) {
            return false;
        }
        return tsNs >= (arrivalMonoNs - PAST_MAXIMUM_NS)
                && tsNs <= (arrivalMonoNs + FUTURE_TOLERANCE_NS);
    }

    public static boolean isBatchPlausible(List<WifiObservation> rows, long arrivalMonoNs) {
        if (rows == null || rows.isEmpty()) {
            return true;
        }
        long maxTs = Long.MIN_VALUE;
        for (WifiObservation obs : rows) {
            if (obs.timestampNs() > maxTs) {
                maxTs = obs.timestampNs();
            }
        }
        return isPlausible(maxTs, arrivalMonoNs);
    }

    public static long effective(Long rowTsNs, long arrivalMonoNs, boolean plausible) {
        if (plausible && rowTsNs != null && rowTsNs > 0) {
            return rowTsNs;
        }
        return arrivalMonoNs;
    }
}
