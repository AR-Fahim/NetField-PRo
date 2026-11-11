package com.netfieldpro.core.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class FakeClockTest {

    @Test
    public void testVectorsFK1AndFK2() {
        FakeClock clock = new FakeClock(5_000_000_000L, 1_700_000_000_000L);
        clock.advanceMs(1500);
        assertEquals(6_500_000_000L, clock.monoNs());
        assertEquals(1_700_000_001_500L, clock.wallMs());

        clock.jumpWallMs(-3_600_000L);
        assertEquals(1_699_996_401_500L, clock.wallMs());
        assertEquals(6_500_000_000L, clock.monoNs());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVectorFK3() {
        FakeClock clock = new FakeClock(0L, 0L);
        clock.advanceMs(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeAdvanceNsThrows() {
        FakeClock clock = new FakeClock(0L, 0L);
        clock.advanceNs(-1);
    }

    @Test
    public void testAdvanceNs() {
        FakeClock clock = new FakeClock(100L, 200L);
        clock.advanceNs(50L);
        assertEquals(150L, clock.monoNs());
        assertEquals(200L, clock.wallMs());
    }
}

