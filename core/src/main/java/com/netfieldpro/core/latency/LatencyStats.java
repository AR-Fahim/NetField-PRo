package com.netfieldpro.core.latency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.netfieldpro.core.util.FixedDecimal;

public final class LatencyStats {

    public static final long LOST = -1L;

    public record Summary(
            Double avgMs,
            Double minMs,
            Double maxMs,
            Double jitterMs,
            int sent,
            int lost
    ) {
        public int recv() {
            return sent - lost;
        }

        public String avgMsFormatted() {
            return avgMs != null ? FixedDecimal.format(avgMs, 3) : null;
        }

        public String minMsFormatted() {
            return minMs != null ? FixedDecimal.format(minMs, 3) : null;
        }

        public String maxMsFormatted() {
            return maxMs != null ? FixedDecimal.format(maxMs, 3) : null;
        }

        public String jitterMsFormatted() {
            return jitterMs != null ? FixedDecimal.format(jitterMs, 3) : null;
        }
    }

    private LatencyStats() {
    }

    public static Summary summarize(long[] rttUsOrLost) {
        if (rttUsOrLost == null || rttUsOrLost.length == 0) {
            return new Summary(null, null, null, null, 0, 0);
        }

        int sent = rttUsOrLost.length;
        int lost = 0;
        List<Long> received = new ArrayList<>();
        long sumUs = 0;
        long minUs = Long.MAX_VALUE;
        long maxUs = Long.MIN_VALUE;

        for (long rtt : rttUsOrLost) {
            if (rtt == LOST || rtt < 0) {
                lost++;
            } else {
                received.add(rtt);
                sumUs += rtt;
                if (rtt < minUs) {
                    minUs = rtt;
                }
                if (rtt > maxUs) {
                    maxUs = rtt;
                }
            }
        }

        int recv = received.size();
        Double avgMs = null;
        Double minMs = null;
        Double maxMs = null;
        Double jitterMs = null;

        if (recv > 0) {
            double avgMsVal = ((double) sumUs / (double) recv) / 1000.0;
            avgMs = Double.valueOf(FixedDecimal.format(avgMsVal, 3));

            double minMsVal = (double) minUs / 1000.0;
            minMs = Double.valueOf(FixedDecimal.format(minMsVal, 3));

            double maxMsVal = (double) maxUs / 1000.0;
            maxMs = Double.valueOf(FixedDecimal.format(maxMsVal, 3));

            if (recv >= 2) {
                long sumDiffUs = 0;
                for (int i = 1; i < recv; i++) {
                    sumDiffUs += Math.abs(received.get(i) - received.get(i - 1));
                }
                double meanJitterMs = ((double) sumDiffUs / (double) (recv - 1)) / 1000.0;
                jitterMs = Double.valueOf(FixedDecimal.format(meanJitterMs, 3));
            }
        }

        return new Summary(avgMs, minMs, maxMs, jitterMs, sent, lost);
    }

    public static long p95(long[] usValues) {
        if (usValues == null || usValues.length == 0) {
            throw new IllegalArgumentException("usValues must not be empty");
        }
        long[] sorted = usValues.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        int rank = (95 * n + 99) / 100;
        int index = rank - 1;
        if (index >= n) {
            index = n - 1;
        }
        return sorted[index];
    }

    public static boolean lossAtMost(int lost, int sent, int limitPermille) {
        if (sent == 0) {
            throw new IllegalArgumentException("sent must not be 0");
        }
        if (sent < 0 || lost < 0 || limitPermille < 0) {
            throw new IllegalArgumentException("arguments must be non-negative");
        }
        return (long) lost * 1000L <= (long) limitPermille * (long) sent;
    }
}
