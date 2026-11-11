package com.netfieldpro.core.power;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.netfieldpro.core.power.BatteryAdvisor.Level;

public class BatteryAdvisorTest {

    @Test
    public void testBA1_sample50False() {
        BatteryAdvisor advisor = new BatteryAdvisor();
        assertEquals(Level.NONE, advisor.onSample(50, false));
        assertEquals(Level.NONE, advisor.current());
    }

    @Test
    public void testBA2_hysteresis() {
        BatteryAdvisor advisor = new BatteryAdvisor();
        assertEquals(Level.WARN, advisor.onSample(20, false));
        assertEquals(Level.WARN, advisor.onSample(21, false));
        assertEquals(Level.WARN, advisor.onSample(24, false));
        assertEquals(Level.NONE, advisor.onSample(25, false));
    }

    @Test
    public void testBA3_criticalThenWarn() {
        BatteryAdvisor advisor = new BatteryAdvisor();
        assertEquals(Level.CRITICAL, advisor.onSample(10, false));
        assertEquals(Level.WARN, advisor.onSample(11, false));
    }

    @Test
    public void testBA4_chargingClearsEvenFromCritical() {
        BatteryAdvisor advisor = new BatteryAdvisor();
        advisor.onSample(5, false);
        assertEquals(Level.CRITICAL, advisor.current());
        assertEquals(Level.NONE, advisor.onSample(5, true));
        assertEquals(Level.NONE, advisor.current());
    }

    @Test
    public void testBA5_lowBroadcast() {
        BatteryAdvisor advisor = new BatteryAdvisor();
        assertEquals(Level.WARN, advisor.onLowBroadcast());
        advisor.onSample(5, false);
        assertEquals(Level.CRITICAL, advisor.current());
        assertEquals(Level.CRITICAL, advisor.onLowBroadcast());
    }

    @Test
    public void testBA6_sequence() {
        BatteryAdvisor advisor = new BatteryAdvisor();
        assertEquals(Level.NONE, advisor.onSample(30, false));
        assertEquals(Level.NONE, advisor.onSample(22, false));
        assertEquals(Level.WARN, advisor.onSample(19, false));
        assertEquals(Level.WARN, advisor.onSample(23, false));
        assertEquals(Level.NONE, advisor.onSample(26, false));
    }

    @Test
    public void testBA7_outOfRangeLeavesCurrentUnchanged() {
        BatteryAdvisor advisor = new BatteryAdvisor();
        advisor.onSample(15, false);
        assertEquals(Level.WARN, advisor.current());
        assertEquals(Level.WARN, advisor.onSample(-1, false));
        assertEquals(Level.WARN, advisor.current());
        assertEquals(Level.WARN, advisor.onSample(101, false));
        assertEquals(Level.WARN, advisor.current());
    }

    @Test
    public void testBA8_boundariesFromFresh() {
        BatteryAdvisor adv1 = new BatteryAdvisor();
        assertEquals(Level.CRITICAL, adv1.onSample(10, false));

        BatteryAdvisor adv2 = new BatteryAdvisor();
        assertEquals(Level.WARN, adv2.onSample(11, false));

        BatteryAdvisor adv3 = new BatteryAdvisor();
        assertEquals(Level.WARN, adv3.onSample(20, false));

        BatteryAdvisor adv4 = new BatteryAdvisor();
        assertEquals(Level.NONE, adv4.onSample(21, false));
    }
}

