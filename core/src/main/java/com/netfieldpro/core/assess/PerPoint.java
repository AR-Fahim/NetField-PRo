package com.netfieldpro.core.assess;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record PerPoint(
        double x,
        double y,
        Map<String, Integer> bssidTenths,
        Map<String, Integer> freqMHz,
        Latency latency
) {
    public record Latency(
            List<Long> rttsUs,
            int lost,
            int sent,
            Long jitterMilli
    ) {
        public Latency {
            if (rttsUs != null) {
                rttsUs = Collections.unmodifiableList(rttsUs);
            }
        }
    }

    public PerPoint {
        bssidTenths = (bssidTenths != null) ? Collections.unmodifiableMap(bssidTenths) : Collections.emptyMap();
        freqMHz = (freqMHz != null) ? Collections.unmodifiableMap(freqMHz) : Collections.emptyMap();
    }

    public PerPoint(double x, double y, Map<String, Integer> bssidTenths, Map<String, Integer> freqMHz) {
        this(x, y, bssidTenths, freqMHz, null);
    }

    public PerPoint(
            double x,
            double y,
            Map<String, Integer> bssidTenths,
            Map<String, Integer> freqMHz,
            List<Long> rttsUs,
            int lost,
            int sent,
            Long jitterMilli
    ) {
        this(x, y, bssidTenths, freqMHz, new Latency(rttsUs, lost, sent, jitterMilli));
    }

    public int coverageTenths() {
        if (bssidTenths == null || bssidTenths.isEmpty()) {
            return -950;
        }
        int max = -950;
        for (int t : bssidTenths.values()) {
            if (t > max) {
                max = t;
            }
        }
        return max;
    }
}

