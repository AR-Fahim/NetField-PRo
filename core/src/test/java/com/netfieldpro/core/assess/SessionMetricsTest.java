package com.netfieldpro.core.assess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.verdict.Inputs;

public class SessionMetricsTest {

    @Test
    public void testSM1_singlePointLatency() {
        // SM-1: one point with latency 5/0, expecting loss 0/5 and worst 0/5
        PerPoint p1 = new PerPoint(
                1.0,
                2.0,
                Map.of("02:00:00:00:00:01", -500),
                Map.of("02:00:00:00:00:01", 2437),
                List.of(10000L, 11000L, 12000L, 13000L, 14000L),
                0, // lost
                5, // sent
                1500L // jitterMilli
        );

        Inputs inputs = SessionMetrics.compute(new PerPoint[]{p1}, EvidenceGrade.VERDICT, -67);

        assertNotNull(inputs);
        assertEquals(EvidenceGrade.VERDICT, inputs.grade());
        assertEquals(1L, inputs.tapPins());
        assertEquals(1L, inputs.tapPinsMeeting());
        assertEquals(Long.valueOf(0L), inputs.lostSum());
        assertEquals(Long.valueOf(5L), inputs.sentSum());
        assertEquals(Long.valueOf(0L), inputs.worstLost());
        assertEquals(Long.valueOf(5L), inputs.worstSent());
        assertEquals(Long.valueOf(14000L), inputs.p95RttUs());
        assertEquals(Long.valueOf(1500L), inputs.meanJitterMilli());
    }

    @Test
    public void testSM2_multiplePointsWorstLoss() {
        // SM-2: points with 1/10 (10%), 2/5 (40%), 0/5 (0%)
        PerPoint p1 = new PerPoint(
                0.0, 0.0,
                Map.of("bssid1", -600),
                Map.of("bssid1", 2412),
                List.of(10000L),
                1, 10, 500L
        );
        PerPoint p2 = new PerPoint(
                1.0, 1.0,
                Map.of("bssid1", -650),
                Map.of("bssid1", 2412),
                List.of(20000L),
                2, 5, 800L
        );
        PerPoint p3 = new PerPoint(
                2.0, 2.0,
                Map.of("bssid1", -700),
                Map.of("bssid1", 2412),
                List.of(15000L),
                0, 5, 300L
        );

        Inputs inputs = SessionMetrics.compute(new PerPoint[]{p1, p2, p3}, EvidenceGrade.VERDICT, -67);

        assertEquals(3L, inputs.tapPins());
        assertEquals(2L, inputs.tapPinsMeeting()); // p1 (-600 >= -670), p2 (-650 >= -670)
        assertEquals(Long.valueOf(3L), inputs.lostSum()); // 1 + 2 + 0
        assertEquals(Long.valueOf(20L), inputs.sentSum()); // 10 + 5 + 5
        assertEquals(Long.valueOf(2L), inputs.worstLost()); // p2 has 40% loss
        assertEquals(Long.valueOf(5L), inputs.worstSent());
    }

    @Test
    public void testSM3_pointsWithoutLatency() {
        // SM-3: Points without latency
        PerPoint p1 = new PerPoint(
                0.0, 0.0,
                Map.of("target1", -600),
                Map.of("target1", 2412)
        );
        PerPoint p2 = new PerPoint(
                1.0, 1.0,
                Map.of(), // no target heard -> -950
                Map.of()
        );

        Inputs inputs = SessionMetrics.compute(new PerPoint[]{p1, p2}, EvidenceGrade.VERDICT, -67);

        assertEquals(2L, inputs.tapPins());
        assertEquals(1L, inputs.tapPinsMeeting()); // only p1 meets -67
        assertNull(inputs.lostSum());
        assertNull(inputs.sentSum());
        assertNull(inputs.worstLost());
        assertNull(inputs.worstSent());
        assertNull(inputs.p95RttUs());
        assertNull(inputs.meanJitterMilli());
    }

    @Test
    public void testSM4_jitterMeanHalfEven() {
        // SM-4: Jitter mean HALF_EVEN
        PerPoint p1 = new PerPoint(
                0.0, 0.0,
                Map.of(), Map.of(),
                List.of(10000L), 0, 1, 1000L
        );
        PerPoint p2 = new PerPoint(
                1.0, 1.0,
                Map.of(), Map.of(),
                List.of(10000L), 0, 1, 2001L
        );
        // (1000 + 2001) / 2 = 1500.5 -> HALF_EVEN to nearest even = 1500
        Inputs inputs = SessionMetrics.compute(new PerPoint[]{p1, p2}, EvidenceGrade.VERDICT, -67);
        assertEquals(Long.valueOf(1500L), inputs.meanJitterMilli());
    }
}

