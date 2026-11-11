package com.netfieldpro.core.assess;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.heat.HeatPipeline;
import com.netfieldpro.core.latency.LatencyStats;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.verdict.Inputs;
import com.netfieldpro.core.verdict.ProfileSpec;

public final class SessionMetrics {

    private SessionMetrics() {
    }

    public static Inputs compute(
            EvidenceGrade grade,
            long cellsInside,
            long cellsMeeting,
            long cellsMasked,
            Long secCellsMeeting,
            int covDbm,
            PerPoint[] points,
            Integer stickyCount
    ) {
        EvidenceGrade actualGrade = (grade != null) ? grade : EvidenceGrade.VERDICT;

        long tapPins = (points != null) ? points.length : 0L;
        long tapPinsMeeting = 0L;
        List<PerPoint> latencyPoints = new ArrayList<>();

        if (points != null) {
            int thresholdTenths = covDbm * 10;
            for (PerPoint p : points) {
                if (p.coverageTenths() >= thresholdTenths) {
                    tapPinsMeeting++;
                }
                if (p.latency() != null && p.latency().sent() > 0) {
                    latencyPoints.add(p);
                }
            }
        }

        Long p95RttUs = null;
        Long meanJitterMilli = null;
        Long lostSum = null;
        Long sentSum = null;
        Long worstLost = null;
        Long worstSent = null;

        if (!latencyPoints.isEmpty()) {
            long totalLost = 0L;
            long totalSent = 0L;
            PerPoint worstPoint = null;
            List<Long> pooledRtts = new ArrayList<>();
            BigDecimal jitterSum = BigDecimal.ZERO;
            int jitterCount = 0;

            for (PerPoint p : latencyPoints) {
                PerPoint.Latency lat = p.latency();
                totalLost += lat.lost();
                totalSent += lat.sent();

                if (worstPoint == null) {
                    worstPoint = p;
                } else {
                    long pLost = lat.lost();
                    long pSent = lat.sent();
                    long wLost = worstPoint.latency().lost();
                    long wSent = worstPoint.latency().sent();
                    if (pLost * wSent > wLost * pSent) {
                        worstPoint = p;
                    }
                }

                if (lat.rttsUs() != null) {
                    for (long rtt : lat.rttsUs()) {
                        if (rtt >= 0) {
                            pooledRtts.add(rtt);
                        }
                    }
                }

                if (lat.jitterMilli() != null) {
                    jitterSum = jitterSum.add(BigDecimal.valueOf(lat.jitterMilli()));
                    jitterCount++;
                }
            }

            lostSum = totalLost;
            sentSum = totalSent;
            if (worstPoint != null) {
                worstLost = (long) worstPoint.latency().lost();
                worstSent = (long) worstPoint.latency().sent();
            }

            if (!pooledRtts.isEmpty()) {
                long[] rtts = new long[pooledRtts.size()];
                for (int i = 0; i < pooledRtts.size(); i++) {
                    rtts[i] = pooledRtts.get(i);
                }
                p95RttUs = LatencyStats.p95(rtts);
            }

            if (jitterCount > 0) {
                meanJitterMilli = jitterSum.divide(BigDecimal.valueOf(jitterCount), 0, RoundingMode.HALF_EVEN).longValueExact();
            }
        }

        return new Inputs(
                actualGrade,
                cellsInside,
                cellsMeeting,
                cellsMasked,
                secCellsMeeting,
                tapPins,
                tapPinsMeeting,
                p95RttUs,
                meanJitterMilli,
                lostSum,
                sentSum,
                worstLost,
                worstSent,
                stickyCount
        );
    }

    public static Inputs compute(
            EvidenceGrade grade,
            long cellsInside,
            long cellsMeeting,
            long cellsMasked,
            Long secCellsMeeting,
            int covDbm,
            PerPoint[] points
    ) {
        return compute(grade, cellsInside, cellsMeeting, cellsMasked, secCellsMeeting, covDbm, points, null);
    }

    public static Inputs compute(
            EvidenceGrade grade,
            HeatPipeline.Result heatResult,
            HeatPipeline.Result secHeatResult,
            ProfileSpec profile,
            PerPoint[] points,
            Integer stickyCount
    ) {
        int covDbm = (profile != null) ? profile.covDbm() : -67;
        long cellsInside = (heatResult != null) ? heatResult.inside() : 0L;
        long cellsMeeting = (heatResult != null) ? heatResult.meeting(covDbm) : 0L;
        long cellsMasked = (heatResult != null) ? heatResult.masked() : 0L;
        Long secCellsMeeting = null;
        if (secHeatResult != null && profile != null && profile.secDbm() != null) {
            secCellsMeeting = (long) secHeatResult.meeting(profile.secDbm());
        }
        return compute(grade, cellsInside, cellsMeeting, cellsMasked, secCellsMeeting, covDbm, points, stickyCount);
    }

    public static Inputs compute(
            EvidenceGrade grade,
            HeatPipeline.Result heatResult,
            HeatPipeline.Result secHeatResult,
            ProfileSpec profile,
            PerPoint[] points
    ) {
        return compute(grade, heatResult, secHeatResult, profile, points, null);
    }

    public static Inputs compute(
            EvidenceGrade grade,
            HeatPipeline.Result heatResult,
            ProfileSpec profile,
            PerPoint[] points
    ) {
        return compute(grade, heatResult, null, profile, points, null);
    }

    public static Inputs compute(
            PerPoint[] points,
            EvidenceGrade grade,
            ProfileSpec profile,
            HeatPipeline.Result heatResult
    ) {
        return compute(grade, heatResult, null, profile, points, null);
    }

    public static Inputs compute(
            PerPoint[] points,
            EvidenceGrade grade,
            int covDbm
    ) {
        return compute(grade, 0L, 0L, 0L, null, covDbm, points, null);
    }

    public static Inputs compute(PerPoint[] points) {
        return compute(EvidenceGrade.VERDICT, 0L, 0L, 0L, null, -67, points, null);
    }
}

