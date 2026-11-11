package com.netfieldpro.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.netfieldpro.core.session.SessionEngine;
import com.netfieldpro.core.session.SessionEvent;
import com.netfieldpro.core.session.Sm1State;
import com.netfieldpro.core.time.FakeClock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@RunWith(RobolectricTestRunner.class)
public class SessionExecutorTest {

    private static final long T0 = 1_000_000_000_000L;

    @Before
    public void setUp() {
        SurveyRuntime.get().resetForTest();
    }

    @After
    public void tearDown() {
        SurveyRuntime.get().resetForTest();
    }

    private static class RecordingSink implements EffectSink {
        final List<String> recorded = new ArrayList<>();
        long scheduledAtNs = -1L;
        long scheduledDelayMs = -1L;
        Runnable scheduledRunnable = null;
        boolean idleCancelled = false;
        boolean throwOnPersist = false;
        boolean throwSecurityOnRuntimeStart = false;

        @Override
        public void onPersistState(Sm1State state) throws Exception {
            recorded.add("PERSIST_STATE(" + state + ")");
            if (throwOnPersist) {
                throw new TimeoutException("PERSIST_TIMEOUT");
            }
        }

        @Override
        public void onRuntimeStart() throws Exception {
            recorded.add("RUNTIME_START");
            if (throwSecurityOnRuntimeStart) {
                throw new SecurityException("Foreground service location blocked");
            }
        }

        @Override
        public void onRuntimeStop() throws Exception {
            recorded.add("RUNTIME_STOP");
        }

        @Override
        public void onScreenFlag(boolean keepScreenOn) throws Exception {
            recorded.add("SCREEN_FLAG(" + keepScreenOn + ")");
        }

        @Override
        public void onScheduleIdle(long atNs, Runnable tickRunnable, long delayMs) throws Exception {
            recorded.add("SCHEDULE_IDLE@" + atNs);
            this.scheduledAtNs = atNs;
            this.scheduledDelayMs = delayMs;
            this.scheduledRunnable = tickRunnable;
            this.idleCancelled = false;
        }

        @Override
        public void onCancelIdle() throws Exception {
            recorded.add("CANCEL_IDLE");
            this.idleCancelled = true;
            this.scheduledRunnable = null;
        }

        @Override
        public void onLogGap(String gapKind, long atNs) throws Exception {
            recorded.add("LOG_GAP(" + gapKind + ")@" + atNs);
        }

        @Override
        public void onMarkIndicative() throws Exception {
            recorded.add("MARK_INDICATIVE");
        }
    }

    @Test
    public void testEX1_effectOrderPreserved() throws Exception {
        FakeClock clock = new FakeClock(T0, 1_700_000_000_000L);
        SessionEngine engine = new SessionEngine(Sm1State.CREATED, 0L);
        RecordingSink sink = new RecordingSink();
        SessionExecutor executor = new SessionExecutor(engine, sink, clock, Executors.newSingleThreadExecutor());

        executor.post(new SessionEvent(SessionEvent.Type.START_WARMUP, T0));
        executor.awaitIdle(2000);

        assertEquals(List.of(
                "PERSIST_STATE(WARMING_UP)",
                "RUNTIME_START",
                "SCREEN_FLAG(true)"
        ), sink.recorded);
    }

    @Test
    public void testEX2_idleScheduleAfterWarmupDoneHasDelay600000msAtFakeClockT() throws Exception {
        FakeClock clock = new FakeClock(T0, 1_700_000_000_000L);
        SessionEngine engine = new SessionEngine(Sm1State.WARMING_UP, 0L);
        RecordingSink sink = new RecordingSink();
        SessionExecutor executor = new SessionExecutor(engine, sink, clock, Executors.newSingleThreadExecutor());

        executor.post(new SessionEvent(SessionEvent.Type.WARMUP_DONE, T0));
        executor.awaitIdle(2000);

        assertEquals(600_000L, sink.scheduledDelayMs);
        assertEquals(T0 + 600_000_000_000L, sink.scheduledAtNs);
    }

    @Test
    public void testEX3_cancelIdleRemovesPendingTick() throws Exception {
        FakeClock clock = new FakeClock(T0, 1_700_000_000_000L);
        SessionEngine engine = new SessionEngine(Sm1State.WARMUP_FAILED, T0);
        RecordingSink sink = new RecordingSink();
        sink.scheduledAtNs = T0 + 600_000_000_000L;
        sink.scheduledRunnable = () -> {};
        SessionExecutor executor = new SessionExecutor(engine, sink, clock, Executors.newSingleThreadExecutor());

        executor.post(new SessionEvent(SessionEvent.Type.RETRY_WARMUP, T0 + 10_000_000_000L));
        executor.awaitIdle(2000);

        assertTrue(sink.idleCancelled);
        assertTrue(sink.recorded.contains("CANCEL_IDLE"));
    }

    @Test
    public void testEX4_persistTimeoutPathSetsLastError() throws Exception {
        FakeClock clock = new FakeClock(T0, 1_700_000_000_000L);
        SessionEngine engine = new SessionEngine(Sm1State.CREATED, 0L);
        RecordingSink sink = new RecordingSink();
        sink.throwOnPersist = true;
        SessionExecutor executor = new SessionExecutor(engine, sink, clock, Executors.newSingleThreadExecutor());

        executor.post(new SessionEvent(SessionEvent.Type.START_WARMUP, T0));
        executor.awaitIdle(2000);

        assertEquals("PERSIST_TIMEOUT", executor.getLastError());
    }

    @Test
    public void testEX5_d9SinkThrowingSecurityExceptionSetsBlockedNoCrash() throws Exception {
        FakeClock clock = new FakeClock(T0, 1_700_000_000_000L);
        SessionEngine engine = new SessionEngine(Sm1State.CREATED, 0L);
        RecordingSink sink = new RecordingSink();
        sink.throwSecurityOnRuntimeStart = true;
        SessionExecutor executor = new SessionExecutor(engine, sink, clock, Executors.newSingleThreadExecutor());

        executor.post(new SessionEvent(SessionEvent.Type.START_WARMUP, T0));
        executor.awaitIdle(2000);

        SurveyRuntime.RuntimeStatus status = SurveyRuntime.get().getRuntimeStatus();
        assertEquals(SurveyRuntime.RuntimeState.BLOCKED, status.state());
        assertNotNull(status.reason());
        assertTrue(status.reason().contains("SecurityException"));
    }
}
