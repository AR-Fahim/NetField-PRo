package com.netfieldpro.core.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MotionVarianceTest {

    @Test
    public void testMV1_alternatingSamplesVariance25Moved() {
        // MV-1: 50 samples alternating 0 and 1 in one window -> variance 0.25 -> moved
        MotionVariance mv = new MotionVariance();
        for (int i = 0; i < 50; i++) {
            long tNs = i * 19_000_000L; // all within 1 second (< 950 ms)
            double mag = (i % 2 == 0) ? 0.0 : 1.0;
            mv.addSample(tNs, mag);
        }
        // Sample at 1s closes the first window
        mv.addSample(1_000_000_000L, 0.0);

        assertEquals(0.25, mv.maxVariance(), 1e-6);
        assertTrue(mv.moved());
        assertTrue(mv.moved(0.05));
    }

    @Test
    public void testMV2_constantSamplesVariance0NotMoved() {
        // MV-2: constant 9.8 -> 0 -> not moved
        MotionVariance mv = new MotionVariance();
        for (int i = 0; i < 50; i++) {
            long tNs = i * 19_000_000L;
            mv.addSample(tNs, 9.8);
        }
        mv.addSample(1_000_000_000L, 9.8);

        assertEquals(0.0, mv.maxVariance(), 1e-6);
        assertFalse(mv.moved());
        assertFalse(mv.moved(0.05));
    }

    @Test
    public void testMultipleWindowsTracksMaxVariance() {
        MotionVariance mv = new MotionVariance();

        // Window 1: constant 0.0
        for (int i = 0; i < 20; i++) {
            mv.addSample(i * 40_000_000L, 0.0);
        }

        // Window 2: alternating 0 and 1
        for (int i = 0; i < 50; i++) {
            long tNs = 1_000_000_000L + i * 18_000_000L;
            mv.addSample(tNs, (i % 2 == 0) ? 0.0 : 1.0);
        }

        // Window 3: constant 5.0
        for (int i = 0; i < 20; i++) {
            long tNs = 2_000_000_000L + i * 40_000_000L;
            mv.addSample(tNs, 5.0);
        }
        mv.closeWindow();

        assertEquals(0.25, mv.maxVariance(), 1e-6);
        assertTrue(mv.moved());
    }

    @Test
    public void test3dCoordinatesMagnitude() {
        MotionVariance mv = new MotionVariance();
        // x=3, y=4, z=0 -> magnitude = 5
        mv.addSample(0L, 3.0, 4.0, 0.0);
        // x=0, y=0, z=5 -> magnitude = 5
        mv.addSample(500_000_000L, 0.0, 0.0, 5.0);
        mv.closeWindow();

        assertEquals(0.0, mv.maxVariance(), 1e-6);
        assertFalse(mv.moved());
    }
}
