package com.netfieldpro.core.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class RebootAndRecoveryTest {

    private static final long C_WALL = 1_700_000_000_000L;
    private static final long C_MONO = 5_000_000_000_000L;

    // RB-1: not rebooted
    private static final long NOW_WALL_NR = 1_700_000_600_000L;
    private static final long NOW_MONO_NR = 5_600_000_000_000L;

    // RB-2: rebooted
    private static final long NOW_WALL_R = 1_700_000_600_000L;
    private static final long NOW_MONO_R = 100_000_000_000L;

    @Test
    public void testRB_1() {
        assertFalse(RebootDetector.rebooted(C_WALL, C_MONO, NOW_WALL_NR, NOW_MONO_NR));
    }

    @Test
    public void testRB_2() {
        assertTrue(RebootDetector.rebooted(C_WALL, C_MONO, NOW_WALL_R, NOW_MONO_R));
    }

    @Test
    public void testRB_3() {
        long wall = 1_700_000_660_000L;
        long mono = 5_600_000_000_000L;
        assertFalse(RebootDetector.rebooted(C_WALL, C_MONO, wall, mono));
    }

    @Test
    public void testRB_4() {
        long wall = 1_700_000_660_001L;
        long mono = 5_600_000_000_000L;
        assertTrue(RebootDetector.rebooted(C_WALL, C_MONO, wall, mono));
    }

    @Test
    public void testRC_1() {
        RecoveryPlanner.SessionFacts facts = new RecoveryPlanner.SessionFacts("s1", "WARMING_UP", C_WALL, C_MONO, 1);
        List<RecoveryPlanner.PlannedAction> plan = RecoveryPlanner.plan(List.of(facts), NOW_WALL_NR, NOW_MONO_NR);
        assertEquals(1, plan.size());
        assertEquals("s1", plan.get(0).sessionId());
        assertEquals(RecoveryPlanner.Action.SET_WARMUP_FAILED, plan.get(0).action());
    }

    @Test
    public void testRC_2() {
        RecoveryPlanner.SessionFacts facts = new RecoveryPlanner.SessionFacts("s2", "READY", C_WALL, C_MONO, 3);
        List<RecoveryPlanner.PlannedAction> plan = RecoveryPlanner.plan(List.of(facts), NOW_WALL_NR, NOW_MONO_NR);
        assertTrue(plan.isEmpty());
    }

    @Test
    public void testRC_3() {
        RecoveryPlanner.SessionFacts facts = new RecoveryPlanner.SessionFacts("s3", "ACTIVE", C_WALL, C_MONO, 5);
        List<RecoveryPlanner.PlannedAction> plan = RecoveryPlanner.plan(List.of(facts), NOW_WALL_R, NOW_MONO_R);
        assertEquals(1, plan.size());
        assertEquals("s3", plan.get(0).sessionId());
        assertEquals(RecoveryPlanner.Action.END_REBOOT, plan.get(0).action());
    }

    @Test
    public void testRC_4() {
        RecoveryPlanner.SessionFacts facts1 = new RecoveryPlanner.SessionFacts("s4a", "CREATED", C_WALL, C_MONO, 0);
        RecoveryPlanner.SessionFacts facts2 = new RecoveryPlanner.SessionFacts("s4b", "CREATED", C_WALL, C_MONO, 3);
        List<RecoveryPlanner.PlannedAction> plan = RecoveryPlanner.plan(List.of(facts1, facts2), NOW_WALL_NR, NOW_MONO_NR);
        assertEquals(1, plan.size());
        assertEquals("s4a", plan.get(0).sessionId());
        assertEquals(RecoveryPlanner.Action.DELETE, plan.get(0).action());
    }

    @Test
    public void testRC_5() {
        RecoveryPlanner.SessionFacts facts = new RecoveryPlanner.SessionFacts("s5", "DRAFT", C_WALL, C_MONO, 2);
        List<RecoveryPlanner.PlannedAction> plan = RecoveryPlanner.plan(List.of(facts), NOW_WALL_R, NOW_MONO_R);
        assertTrue(plan.isEmpty());
    }

    @Test
    public void testRC_6() {
        RecoveryPlanner.SessionFacts facts = new RecoveryPlanner.SessionFacts("s6", "WARMING_UP", C_WALL, C_MONO, 1);
        List<RecoveryPlanner.PlannedAction> plan = RecoveryPlanner.plan(List.of(facts), NOW_WALL_R, NOW_MONO_R);
        assertEquals(1, plan.size());
        assertEquals("s6", plan.get(0).sessionId());
        assertEquals(RecoveryPlanner.Action.END_REBOOT, plan.get(0).action());
    }
}
