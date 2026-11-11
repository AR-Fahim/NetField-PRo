package com.netfieldpro.core.permissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PermissionPlannerTest {

    @Test
    public void testPM1() {
        PermissionSnapshot s = new PermissionSnapshot(34, false, false, false, false, false, false, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("LOCATION:DENIED_CAN_ASK:REQUEST", plan.location().toString());
        assertEquals("NOTIFICATIONS:DENIED_CAN_ASK:REQUEST", plan.notifications().toString());
        assertFalse(plan.surveyReady());
        assertEquals(PrimaryAction.ALLOW_LOCATION, plan.primary());
    }

    @Test
    public void testPM2() {
        PermissionSnapshot s = new PermissionSnapshot(30, false, false, false, false, false, false, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("LOCATION:DENIED_CAN_ASK:REQUEST", plan.location().toString());
        assertEquals("NOTIFICATIONS:NOT_REQUIRED:NONE", plan.notifications().toString());
        assertFalse(plan.surveyReady());
        assertEquals(PrimaryAction.ALLOW_LOCATION, plan.primary());
    }

    @Test
    public void testPM3() {
        PermissionSnapshot s = new PermissionSnapshot(32, false, false, false, false, false, false, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("NOTIFICATIONS:NOT_REQUIRED:NONE", plan.notifications().toString());
    }

    @Test
    public void testPM4() {
        PermissionSnapshot s = new PermissionSnapshot(33, false, false, false, false, false, false, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("NOTIFICATIONS:DENIED_CAN_ASK:REQUEST", plan.notifications().toString());
    }

    @Test
    public void testPM5() {
        PermissionSnapshot s = new PermissionSnapshot(34, true, true, true, false, true, true, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("LOCATION:GRANTED:NONE", plan.location().toString());
        assertEquals("NOTIFICATIONS:GRANTED:NONE", plan.notifications().toString());
        assertTrue(plan.surveyReady());
        assertEquals(PrimaryAction.CONTINUE, plan.primary());
    }

    @Test
    public void testPM6() {
        PermissionSnapshot s = new PermissionSnapshot(34, true, true, false, false, false, true, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("LOCATION:GRANTED:NONE", plan.location().toString());
        assertEquals("NOTIFICATIONS:DENIED_PERMANENT:OPEN_SETTINGS", plan.notifications().toString());
        assertTrue(plan.surveyReady());
        assertEquals(PrimaryAction.CONTINUE, plan.primary());
    }

    @Test
    public void testPM7() {
        PermissionSnapshot s = new PermissionSnapshot(34, false, true, true, false, false, false, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("LOCATION:APPROXIMATE_ONLY:OPEN_SETTINGS", plan.location().toString());
        assertEquals("NOTIFICATIONS:DENIED_CAN_ASK:REQUEST", plan.notifications().toString());
        assertFalse(plan.surveyReady());
        assertEquals(PrimaryAction.OPEN_SETTINGS, plan.primary());
    }

    @Test
    public void testPM8() {
        PermissionSnapshot s = new PermissionSnapshot(34, false, true, false, false, false, false, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("LOCATION:APPROXIMATE_ONLY:OPEN_SETTINGS", plan.location().toString());
        assertEquals("NOTIFICATIONS:DENIED_CAN_ASK:REQUEST", plan.notifications().toString());
        assertFalse(plan.surveyReady());
        assertEquals(PrimaryAction.OPEN_SETTINGS, plan.primary());
    }

    @Test
    public void testPM9() {
        PermissionSnapshot s = new PermissionSnapshot(34, false, false, true, true, false, false, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("LOCATION:DENIED_CAN_ASK:REQUEST", plan.location().toString());
        assertEquals(PrimaryAction.ALLOW_LOCATION, plan.primary());
    }

    @Test
    public void testPM10() {
        PermissionSnapshot s = new PermissionSnapshot(34, false, false, true, false, false, false, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("LOCATION:DENIED_PERMANENT:OPEN_SETTINGS", plan.location().toString());
        assertEquals(PrimaryAction.OPEN_SETTINGS, plan.primary());
    }

    @Test
    public void testPM11() {
        PermissionSnapshot s = new PermissionSnapshot(34, false, false, false, true, false, false, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("LOCATION:DENIED_CAN_ASK:REQUEST", plan.location().toString());
    }

    @Test
    public void testPM12() {
        PermissionSnapshot s = new PermissionSnapshot(34, true, false, true, false, false, false, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("LOCATION:GRANTED:NONE", plan.location().toString());
        assertEquals(PrimaryAction.CONTINUE, plan.primary());
    }

    @Test
    public void testPM13() {
        PermissionSnapshot s = new PermissionSnapshot(29, false, false, false, false, false, false, false);
        assertThrows(IllegalArgumentException.class, () -> PermissionPlanner.plan(s));
    }

    @Test
    public void testPM14() {
        PermissionSnapshot s = new PermissionSnapshot(36, false, false, true, false, false, false, false);
        PermissionPlan plan = PermissionPlanner.plan(s);
        assertEquals("LOCATION:DENIED_PERMANENT:OPEN_SETTINGS", plan.location().toString());
        assertEquals("NOTIFICATIONS:DENIED_CAN_ASK:REQUEST", plan.notifications().toString());
    }

    @Test
    public void testPM15() {
        assertThrows(NullPointerException.class, () -> PermissionPlanner.plan(null));
    }
}

