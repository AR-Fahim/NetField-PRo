package com.netfieldpro.core.power;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.netfieldpro.core.power.BatteryAdvisor.Level;

public class BatteryAlertPolicyTest {

    @Test
    public void testBP1_allNinePairs() {
        assertEquals(NotifyAction.NONE, BatteryAlertPolicy.decide(Level.NONE, Level.NONE));
        assertEquals(NotifyAction.POST_ALERT, BatteryAlertPolicy.decide(Level.NONE, Level.WARN));
        assertEquals(NotifyAction.POST_ALERT, BatteryAlertPolicy.decide(Level.NONE, Level.CRITICAL));

        assertEquals(NotifyAction.CANCEL, BatteryAlertPolicy.decide(Level.WARN, Level.NONE));
        assertEquals(NotifyAction.NONE, BatteryAlertPolicy.decide(Level.WARN, Level.WARN));
        assertEquals(NotifyAction.POST_ALERT, BatteryAlertPolicy.decide(Level.WARN, Level.CRITICAL));

        assertEquals(NotifyAction.CANCEL, BatteryAlertPolicy.decide(Level.CRITICAL, Level.NONE));
        assertEquals(NotifyAction.UPDATE_QUIET, BatteryAlertPolicy.decide(Level.CRITICAL, Level.WARN));
        assertEquals(NotifyAction.NONE, BatteryAlertPolicy.decide(Level.CRITICAL, Level.CRITICAL));
    }
}

