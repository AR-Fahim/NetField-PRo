package com.netfieldpro.core.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class Sm1EngineTest {

    private static final long T0 = 1_000_000_000_000L;

    @Test
    public void testSM1_1() {
        SessionEngine engine = SessionEngine.at(Sm1State.CREATED, 0L);
        Step step = engine.step(new SessionEvent(SessionEvent.Type.START_WARMUP, T0));
        assertEquals(Sm1State.WARMING_UP, step.state());
        assertEquals("PERSIST_STATE,RUNTIME_START,SCREEN_FLAG(true)", step.effectsText());
    }

    @Test
    public void testSM1_2() {
        SessionEngine engine = SessionEngine.at(Sm1State.WARMING_UP, 0L);
        Step step = engine.step(new SessionEvent(SessionEvent.Type.WARMUP_DONE, 1_020_000_000_000L));
        assertEquals(Sm1State.READY, step.state());
        assertEquals("PERSIST_STATE,SCHEDULE_IDLE@1620000000000", step.effectsText());
    }

    @Test
    public void testSM1_3() {
        SessionEngine engine = SessionEngine.at(Sm1State.READY, 0L);
        Step step = engine.step(new SessionEvent(SessionEvent.Type.SURVEY_OPENED, 1_030_000_000_000L));
        assertEquals(Sm1State.ACTIVE, step.state());
        assertEquals("PERSIST_STATE,SCHEDULE_IDLE@1630000000000", step.effectsText());
    }

    @Test
    public void testSM1_4() {
        SessionEngine engine = SessionEngine.at(Sm1State.ACTIVE, 1_030_000_000_000L);
        Step step = engine.step(new SessionEvent(SessionEvent.Type.IDLE_TICK, 1_629_999_999_999L));
        assertEquals(Sm1State.ACTIVE, step.state());
        assertEquals("SCHEDULE_IDLE@1630000000000", step.effectsText());
    }

    @Test
    public void testSM1_5() {
        SessionEngine engine = SessionEngine.at(Sm1State.ACTIVE, 1_030_000_000_000L);
        Step step = engine.step(new SessionEvent(SessionEvent.Type.IDLE_TICK, 1_630_000_000_000L));
        assertEquals(Sm1State.ENDED, step.state());
        assertEquals(EndReason.IDLE_10MIN, step.reason());
        assertEquals("PERSIST_STATE,CANCEL_IDLE,RUNTIME_STOP,SCREEN_FLAG(false)", step.effectsText());
    }

    @Test
    public void testSM1_6() {
        SessionEngine engine = SessionEngine.at(Sm1State.ACTIVE, 1_030_000_000_000L);
        Step s1 = engine.step(new SessionEvent(SessionEvent.Type.ACTIVITY, 1_100_000_000_000L));
        assertEquals("SCHEDULE_IDLE@1700000000000", s1.effectsText());

        Step s2 = engine.step(new SessionEvent(SessionEvent.Type.IDLE_TICK, 1_630_000_000_000L));
        assertEquals(Sm1State.ACTIVE, s2.state());
        assertEquals("SCHEDULE_IDLE@1700000000000", s2.effectsText());
    }

    @Test
    public void testSM1_7() {
        SessionEngine engine = SessionEngine.at(Sm1State.WARMING_UP, 0L);
        Step s1 = engine.step(new SessionEvent(SessionEvent.Type.WARMUP_FAIL, 1_015_000_000_000L));
        assertEquals(Sm1State.WARMUP_FAILED, s1.state());
        assertEquals("PERSIST_STATE,SCHEDULE_IDLE@1615000000000", s1.effectsText());

        Step s2 = engine.step(new SessionEvent(SessionEvent.Type.CONTINUE_INDICATIVE, 1_020_000_000_000L));
        assertEquals(Sm1State.READY, s2.state());
        assertEquals("PERSIST_STATE,MARK_INDICATIVE,SCHEDULE_IDLE@1620000000000", s2.effectsText());
    }

    @Test
    public void testSM1_8() {
        SessionEngine engine = SessionEngine.at(Sm1State.WARMUP_FAILED, 0L);
        Step step = engine.step(new SessionEvent(SessionEvent.Type.RETRY_WARMUP, 1_030_000_000_000L));
        assertEquals(Sm1State.WARMING_UP, step.state());
        assertEquals("PERSIST_STATE,CANCEL_IDLE", step.effectsText());
    }

    @Test
    public void testSM1_9() {
        SessionEngine engine = SessionEngine.at(Sm1State.READY, 0L);
        Step s1 = engine.step(new SessionEvent(SessionEvent.Type.SCREEN_OFF, 1_500_000_000_000L));
        assertEquals("LOG_GAP(SCREEN_OFF)@1500000000000", s1.effectsText());

        Step s2 = engine.step(new SessionEvent(SessionEvent.Type.SCREEN_ON, 1_510_000_000_000L));
        assertEquals("LOG_GAP(SCREEN_ON)@1510000000000", s2.effectsText());

        Step s3 = engine.step(new SessionEvent(SessionEvent.Type.SCREEN_ON, 1_520_000_000_000L));
        assertEquals("", s3.effectsText());
    }

    @Test
    public void testSM1_10() {
        SessionEngine engine = SessionEngine.at(Sm1State.ENDED, 0L);
        for (SessionEvent.Type type : SessionEvent.Type.values()) {
            Step step = engine.step(new SessionEvent(type, T0));
            assertEquals(Sm1State.ENDED, step.state());
            assertEquals("", step.effectsText());
        }
    }

    @Test
    public void testSM1_11() {
        SessionEngine e1 = SessionEngine.restored(Sm1State.WARMING_UP, 2_000_000_000_000L);
        assertEquals(Sm1State.WARMUP_FAILED, e1.getState());
        assertEquals("PERSIST_STATE", e1.getLastStep().effectsText());

        SessionEngine e2 = SessionEngine.restored(Sm1State.ACTIVE, 2_000_000_000_000L);
        assertEquals(Sm1State.ACTIVE, e2.getState());
        assertEquals("", e2.getLastStep().effectsText());

        Step s2 = e2.step(new SessionEvent(SessionEvent.Type.RESUME, 2_001_000_000_000L));
        assertEquals("RUNTIME_START,SCREEN_FLAG(true),SCHEDULE_IDLE@2601000000000", s2.effectsText());
    }

    @Test
    public void testSM1_12() {
        SessionEngine e1 = SessionEngine.at(Sm1State.CREATED, 0L);
        Step s1 = e1.step(new SessionEvent(SessionEvent.Type.END_SESSION, T0));
        assertEquals(Sm1State.ENDED, s1.state());
        assertEquals(EndReason.END_SESSION, s1.reason());
        assertEquals("PERSIST_STATE", s1.effectsText());

        SessionEngine e2 = SessionEngine.at(Sm1State.READY, 0L);
        Step s2 = e2.step(new SessionEvent(SessionEvent.Type.END_SESSION, 1_040_000_000_000L));
        assertEquals(Sm1State.ENDED, s2.state());
        assertEquals(EndReason.END_SESSION, s2.reason());
        assertEquals("PERSIST_STATE,CANCEL_IDLE,RUNTIME_STOP,SCREEN_FLAG(false)", s2.effectsText());
    }

    @Test
    public void testSM1_13() {
        SessionEngine e1 = SessionEngine.at(Sm1State.CREATED, 0L);
        assertEquals("", e1.step(new SessionEvent(SessionEvent.Type.SURVEY_OPENED, T0)).effectsText());

        SessionEngine e2 = SessionEngine.at(Sm1State.READY, 0L);
        assertEquals("", e2.step(new SessionEvent(SessionEvent.Type.START_WARMUP, T0)).effectsText());

        SessionEngine e3 = SessionEngine.at(Sm1State.ACTIVE, 0L);
        assertEquals("", e3.step(new SessionEvent(SessionEvent.Type.SURVEY_OPENED, T0)).effectsText());

        SessionEngine e4 = SessionEngine.at(Sm1State.WARMING_UP, 0L);
        assertEquals("", e4.step(new SessionEvent(SessionEvent.Type.ACTIVITY, T0)).effectsText());
    }

    @Test
    public void testSM1_14() {
        SessionEngine engine = SessionEngine.at(Sm1State.WARMING_UP, 0L);
        engine.step(new SessionEvent(SessionEvent.Type.WARMUP_FAIL, 1_015_000_000_000L));
        assertEquals(Sm1State.WARMUP_FAILED, engine.getState());

        Step step = engine.step(new SessionEvent(SessionEvent.Type.IDLE_TICK, 1_615_000_000_000L));
        assertEquals(Sm1State.ENDED, step.state());
        assertEquals(EndReason.IDLE_10MIN, step.reason());
    }

    @Test
    public void testSM1_15() {
        SessionEngine engine = SessionEngine.at(Sm1State.READY, 1_020_000_000_000L);
        engine.step(new SessionEvent(SessionEvent.Type.SCREEN_OFF, 1_500_000_000_000L));
        Step step = engine.step(new SessionEvent(SessionEvent.Type.IDLE_TICK, 1_620_000_000_000L));
        assertEquals(Sm1State.ENDED, step.state());
        assertEquals(EndReason.IDLE_10MIN, step.reason());
    }

    @Test
    public void testSM1_16() {
        for (Sm1State state : Sm1State.values()) {
            for (SessionEvent.Type eventType : SessionEvent.Type.values()) {
                try {
                    SessionEngine engine = SessionEngine.at(state, T0);
                    engine.step(new SessionEvent(eventType, T0 + 1000L));
                } catch (Throwable t) {
                    fail("Threw exception on state=" + state + " event=" + eventType + ": " + t);
                }
            }
        }
    }
}
