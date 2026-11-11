package com.netfieldpro.core.capture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class CaptureEngineTest {

    @Test
    public void testCE1_tapStartIdle() {
        CaptureEngine engine = new CaptureEngine(true);
        List<String> effects = engine.onEvent(CaptureEvent.tapStart(0L, 1.0, 2.0));
        assertEquals(List.of("START_SCANNING", "WAKE_AT@15000000000", "PIN_AT(1.000,2.000)"), effects);
        assertEquals(CaptureEngine.State.AWAIT_SCANS, engine.getState());
        assertEquals(1.0, engine.getX(), 1e-6);
        assertEquals(2.0, engine.getY(), 1e-6);
    }

    @Test
    public void testCE2_twoFreshFullScansWithProbe() {
        CaptureEngine engine = new CaptureEngine(true);
        engine.onEvent(CaptureEvent.tapStart(0L, 1.0, 2.0));

        List<String> e1 = engine.onEvent(CaptureEvent.scan(1_000_000_000L, true, true));
        assertTrue(e1.isEmpty());
        assertEquals(CaptureEngine.State.AWAIT_SCANS, engine.getState());
        assertEquals(1, engine.getNFull());

        List<String> e2 = engine.onEvent(CaptureEvent.scan(5_000_000_000L, true, true));
        assertEquals(List.of("STOP_SCANNING", "CANCEL_WAKE", "START_PROBE"), e2);
        assertEquals(CaptureEngine.State.PROBING, engine.getState());
        assertEquals(2, engine.getNFull());

        List<String> e3 = engine.onEvent(CaptureEvent.probeDone(6_000_000_000L));
        assertEquals(List.of("COMMIT(OK,nFull=2)"), e3);
        assertEquals(CaptureEngine.State.COMMITTING, engine.getState());

        List<String> e4 = engine.onEvent(CaptureEvent.commitDone(7_000_000_000L));
        assertTrue(e4.isEmpty());
        assertEquals(CaptureEngine.State.IDLE, engine.getState());
    }

    @Test
    public void testCE3_twoFreshFullScansNoProbe() {
        CaptureEngine engine = new CaptureEngine(false);
        engine.onEvent(CaptureEvent.tapStart(0L, 1.0, 2.0));

        engine.onEvent(CaptureEvent.scan(1_000_000_000L, true, true));
        List<String> e2 = engine.onEvent(CaptureEvent.scan(5_000_000_000L, true, true));
        assertEquals(List.of("STOP_SCANNING", "CANCEL_WAKE", "COMMIT(OK,nFull=2)"), e2);
        assertEquals(CaptureEngine.State.COMMITTING, engine.getState());
    }

    @Test
    public void testCE4_timeoutWithOneScanAndProbe() {
        CaptureEngine engine = new CaptureEngine(true);
        engine.onEvent(CaptureEvent.tapStart(0L, 1.0, 2.0));

        engine.onEvent(CaptureEvent.scan(1_000_000_000L, true, true));
        List<String> nonFresh = engine.onEvent(CaptureEvent.scan(2_000_000_000L, false, true));
        assertTrue(nonFresh.isEmpty());
        assertEquals(1, engine.getNFull());

        List<String> timeoutEffects = engine.onEvent(CaptureEvent.timeout(15_000_000_000L));
        assertEquals(List.of("STOP_SCANNING", "START_PROBE"), timeoutEffects);
        assertEquals(CaptureEngine.State.PROBING, engine.getState());

        List<String> probeEffects = engine.onEvent(CaptureEvent.probeDone(16_000_000_000L));
        assertEquals(List.of("COMMIT(LOW_CONFIDENCE,nFull=1)"), probeEffects);
        assertEquals(CaptureEngine.State.COMMITTING, engine.getState());
    }

    @Test
    public void testCE5_timeoutNoScans() {
        CaptureEngine engine = new CaptureEngine(true);
        engine.onEvent(CaptureEvent.tapStart(0L, 1.0, 2.0));

        List<String> effects = engine.onEvent(CaptureEvent.timeout(15_000_000_000L));
        assertEquals(List.of("STOP_SCANNING", "DISCARD(NO_SCANS)"), effects);
        assertEquals(CaptureEngine.State.IDLE, engine.getState());
    }

    @Test
    public void testCE6_movedDuringAwaitTwoScansNoProbe() {
        CaptureEngine engine = new CaptureEngine(false);
        engine.onEvent(CaptureEvent.tapStart(0L, 1.0, 2.0));

        engine.onEvent(CaptureEvent.scan(1_000_000_000L, true, true));
        List<String> motionEffects = engine.onEvent(CaptureEvent.motion(2_000_000_000L, true));
        assertTrue(motionEffects.isEmpty());
        assertTrue(engine.isMoved());

        List<String> e2 = engine.onEvent(CaptureEvent.scan(5_000_000_000L, true, true));
        assertEquals(List.of("STOP_SCANNING", "CANCEL_WAKE", "COMMIT(MOVED,nFull=2)"), e2);
        assertEquals(CaptureEngine.State.COMMITTING, engine.getState());
    }

    @Test
    public void testCE7_staleTimeoutIgnored() {
        CaptureEngine engine = new CaptureEngine(true);
        engine.onEvent(CaptureEvent.tapStart(0L, 1.0, 2.0));

        List<String> effects = engine.onEvent(CaptureEvent.timeout(14_000_000_000L));
        assertTrue(effects.isEmpty());
        assertEquals(CaptureEngine.State.AWAIT_SCANS, engine.getState());
    }

    @Test
    public void testCE8_cancelInAwaitAndProbing() {
        // Cancel in AWAIT_SCANS
        CaptureEngine engine1 = new CaptureEngine(true);
        engine1.onEvent(CaptureEvent.tapStart(0L, 1.0, 2.0));
        List<String> e1 = engine1.onEvent(CaptureEvent.cancel(1_000_000_000L));
        assertEquals(List.of("STOP_SCANNING", "CANCEL_WAKE", "DISCARD(CANCELLED)"), e1);
        assertEquals(CaptureEngine.State.IDLE, engine1.getState());

        // Cancel in PROBING
        CaptureEngine engine2 = new CaptureEngine(true);
        engine2.onEvent(CaptureEvent.tapStart(0L, 1.0, 2.0));
        engine2.onEvent(CaptureEvent.scan(1_000_000_000L, true, true));
        engine2.onEvent(CaptureEvent.scan(2_000_000_000L, true, true));
        assertEquals(CaptureEngine.State.PROBING, engine2.getState());

        List<String> e2 = engine2.onEvent(CaptureEvent.cancel(3_000_000_000L));
        assertEquals(List.of("ABORT_PROBE", "DISCARD(CANCELLED)"), e2);
        assertEquals(CaptureEngine.State.IDLE, engine2.getState());
    }

    @Test
    public void testCE9_pinDragInAwaitAndCommitting() {
        CaptureEngine engine = new CaptureEngine(false);
        engine.onEvent(CaptureEvent.tapStart(0L, 1.0, 2.0));

        List<String> e1 = engine.onEvent(CaptureEvent.pinDrag(1_000_000_000L, 3.0, 4.0));
        assertEquals(List.of("PIN_AT(3.000,4.000)"), e1);
        assertEquals(3.0, engine.getX(), 1e-6);
        assertEquals(4.0, engine.getY(), 1e-6);

        engine.onEvent(CaptureEvent.scan(2_000_000_000L, true, true));
        engine.onEvent(CaptureEvent.scan(3_000_000_000L, true, true));
        assertEquals(CaptureEngine.State.COMMITTING, engine.getState());

        List<String> e2 = engine.onEvent(CaptureEvent.pinDrag(4_000_000_000L, 5.0, 6.0));
        assertTrue(e2.isEmpty());
        assertEquals(3.0, engine.getX(), 1e-6);
        assertEquals(4.0, engine.getY(), 1e-6);
    }

    @Test
    public void testCE10_tapStartWhileInAwaitIgnored() {
        CaptureEngine engine = new CaptureEngine(true);
        engine.onEvent(CaptureEvent.tapStart(0L, 1.0, 2.0));

        List<String> e = engine.onEvent(CaptureEvent.tapStart(1_000_000_000L, 5.0, 6.0));
        assertTrue(e.isEmpty());
        assertEquals(1.0, engine.getX(), 1e-6);
        assertEquals(2.0, engine.getY(), 1e-6);
        assertEquals(CaptureEngine.State.AWAIT_SCANS, engine.getState());
    }
}
