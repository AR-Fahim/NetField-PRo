package com.netfieldpro.ui.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.netfieldpro.core.session.SessionEngine;
import com.netfieldpro.core.session.Sm1State;
import com.netfieldpro.core.time.FakeClock;
import com.netfieldpro.core.wifi.FakeWifiScanSource;
import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.wifi.Tier;
import com.netfieldpro.core.wifi.WifiObservation;
import com.netfieldpro.session.SessionExecutor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class WarmupCoordinatorTest {

    private static final long T0 = 1_000_000_000_000L;

    private FakeClock clock;
    private SessionEngine engine;
    private SessionExecutor executor;
    private FakeWifiScanSource scanSource;
    private TestTimeoutScheduler timeoutScheduler;
    private WarmupCoordinator coordinator;

    static class TestTimeoutScheduler implements WarmupCoordinator.TimeoutScheduler {
        Runnable scheduledRunnable;
        long delayMs;

        @Override
        public void schedule(Runnable runnable, long delayMs) {
            this.scheduledRunnable = runnable;
            this.delayMs = delayMs;
        }

        @Override
        public void cancel() {
            this.scheduledRunnable = null;
        }

        public void trigger() {
            if (scheduledRunnable != null) {
                scheduledRunnable.run();
            }
        }
    }

    @Before
    public void setUp() {
        clock = new FakeClock(T0, 1_700_000_000_000L);
        engine = new SessionEngine(Sm1State.CREATED, 0L);
        executor = new SessionExecutor(engine, null, clock, Executors.newSingleThreadExecutor());
        scanSource = new FakeWifiScanSource();
        timeoutScheduler = new TestTimeoutScheduler();

        coordinator = new WarmupCoordinator(
                scanSource,
                executor,
                clock,
                null,
                timeoutScheduler
        );
    }

    private ScanBatch createBatch(List<Integer> freqs, String ssid, String bssidPrefix) {
        List<WifiObservation> rows = new ArrayList<>();
        int idx = 1;
        for (int f : freqs) {
            String bssid = String.format("%s:%02d", bssidPrefix, idx++);
            rows.add(new WifiObservation(bssid, ssid, f, -60, clock.monoNs()));
        }
        return new ScanBatch(rows, clock.monoNs(), Tier.CALLBACK, true, false, 0);
    }

    @Test
    public void testWU1_threeScansComplete_referenceSetMatchesSW1() throws Exception {
        // Frozen vector SW-1:
        // s1 = {2412, 2437, 2462, 5180, 5200}
        // s2 = {2412, 2437, 5180, 5200, 5220, 5745}
        // s3 = {2412, 2462, 5180, 5745, 5765}
        // ref = [2412, 2437, 2462, 5180, 5200, 5745] (frequencies appearing in >=2 of 3)
        List<Integer> s1 = List.of(2412, 2437, 2462, 5180, 5200);
        List<Integer> s2 = List.of(2412, 2437, 5180, 5200, 5220, 5745);
        List<Integer> s3 = List.of(2412, 2462, 5180, 5745, 5765);
        List<Integer> expectedRef = List.of(2412, 2437, 2462, 5180, 5200, 5745);

        AtomicInteger progressCount = new AtomicInteger(0);
        AtomicBoolean doneCalled = new AtomicBoolean(false);
        List<Integer> resultRef = new ArrayList<>();

        coordinator.setListener(new WarmupCoordinator.Listener() {
            @Override
            public void onProgress(int scansDone, int totalScans) {
                progressCount.set(scansDone);
            }

            @Override
            public void onNetworksUpdated(List<WarmupCoordinator.NetworkGroup> networks, int totalSelectedBssids) {}

            @Override
            public void onWarmupDone(List<Integer> referenceFreqs) {
                doneCalled.set(true);
                resultRef.addAll(referenceFreqs);
            }

            @Override
            public void onWarmupFailed() {}

            @Override
            public void onCapExceeded() {}
        });

        coordinator.start();
        executor.awaitIdle(2000);
        assertEquals(Sm1State.WARMING_UP, engine.getState());
        assertEquals(0, progressCount.get());

        // Emit scan 1
        clock.advanceMs(1000);
        coordinator.handleBatch(createBatch(s1, "eduroam", "00:11:22:33:44"));
        assertEquals(1, progressCount.get());

        // Emit scan 2
        clock.advanceMs(1000);
        coordinator.handleBatch(createBatch(s2, "eduroam", "00:11:22:33:55"));
        assertEquals(2, progressCount.get());

        // Emit scan 3
        clock.advanceMs(1000);
        coordinator.handleBatch(createBatch(s3, "eduroam", "00:11:22:33:66"));
        assertEquals(3, progressCount.get());

        executor.awaitIdle(2000);
        assertTrue("Warmup done should be called", doneCalled.get());
        assertEquals("Reference set should match SW-1", expectedRef, resultRef);
        assertEquals("Engine state should become READY", Sm1State.READY, engine.getState());
        assertTrue(coordinator.isCompleted());
        assertFalse(coordinator.isFailed());
    }

    @Test
    public void testWU2_emptyReferenceSet_failsWarmup() throws Exception {
        // Disjoint freqs across 3 scans -> no frequency appears in >= 2 of 3 -> ref set empty
        List<Integer> s1 = List.of(2412);
        List<Integer> s2 = List.of(2437);
        List<Integer> s3 = List.of(2462);

        AtomicBoolean failedCalled = new AtomicBoolean(false);
        coordinator.setListener(new WarmupCoordinator.Listener() {
            @Override
            public void onProgress(int scansDone, int totalScans) {}
            @Override
            public void onNetworksUpdated(List<WarmupCoordinator.NetworkGroup> networks, int totalSelectedBssids) {}
            @Override
            public void onWarmupDone(List<Integer> referenceFreqs) {}
            @Override
            public void onWarmupFailed() {
                failedCalled.set(true);
            }
            @Override
            public void onCapExceeded() {}
        });

        coordinator.start();
        executor.awaitIdle(2000);

        clock.advanceMs(1000);
        coordinator.handleBatch(createBatch(s1, "NetA", "00:01"));
        clock.advanceMs(1000);
        coordinator.handleBatch(createBatch(s2, "NetA", "00:02"));
        clock.advanceMs(1000);
        coordinator.handleBatch(createBatch(s3, "NetA", "00:03"));

        executor.awaitIdle(2000);
        assertTrue("Warmup failed should be called on empty ref set", failedCalled.get());
        assertEquals(Sm1State.WARMUP_FAILED, engine.getState());
        assertTrue(coordinator.isFailed());
        assertFalse(coordinator.isCompleted());
    }

    @Test
    public void testWU3_timeoutAt20SecondsWithTwoScans_failsWarmup() throws Exception {
        AtomicBoolean failedCalled = new AtomicBoolean(false);
        coordinator.setListener(new WarmupCoordinator.Listener() {
            @Override
            public void onProgress(int scansDone, int totalScans) {}
            @Override
            public void onNetworksUpdated(List<WarmupCoordinator.NetworkGroup> networks, int totalSelectedBssids) {}
            @Override
            public void onWarmupDone(List<Integer> referenceFreqs) {}
            @Override
            public void onWarmupFailed() {
                failedCalled.set(true);
            }
            @Override
            public void onCapExceeded() {}
        });

        coordinator.start();
        executor.awaitIdle(2000);
        assertEquals(Sm1State.WARMING_UP, engine.getState());

        clock.advanceMs(2000);
        coordinator.handleBatch(createBatch(List.of(2412, 5180), "NetA", "00:01"));

        clock.advanceMs(5000);
        coordinator.handleBatch(createBatch(List.of(2412, 5180), "NetA", "00:02"));

        // Advance to 20 s
        clock.advanceMs(15000);
        timeoutScheduler.trigger();

        executor.awaitIdle(2000);
        assertTrue("Failure should trigger at 20 s with only 2 scans", failedCalled.get());
        assertEquals(Sm1State.WARMUP_FAILED, engine.getState());
        assertTrue(coordinator.isFailed());
    }

    @Test
    public void testWU4_retryWorksAndCapSixtyEnforced() throws Exception {
        AtomicBoolean failedCalled = new AtomicBoolean(false);
        AtomicBoolean doneCalled = new AtomicBoolean(false);
        AtomicBoolean capExceeded = new AtomicBoolean(false);

        coordinator.setListener(new WarmupCoordinator.Listener() {
            @Override
            public void onProgress(int scansDone, int totalScans) {}
            @Override
            public void onNetworksUpdated(List<WarmupCoordinator.NetworkGroup> networks, int totalSelectedBssids) {}
            @Override
            public void onWarmupDone(List<Integer> referenceFreqs) {
                doneCalled.set(true);
            }
            @Override
            public void onWarmupFailed() {
                failedCalled.set(true);
            }
            @Override
            public void onCapExceeded() {
                capExceeded.set(true);
            }
        });

        coordinator.start();
        executor.awaitIdle(2000);
        // Fail at 20 s with 0 scans
        clock.advanceMs(20000);
        timeoutScheduler.trigger();
        executor.awaitIdle(2000);
        assertTrue(failedCalled.get());
        assertEquals(Sm1State.WARMUP_FAILED, engine.getState());

        // Test Retry
        coordinator.retry();
        executor.awaitIdle(2000);
        assertEquals(Sm1State.WARMING_UP, engine.getState());
        assertFalse(coordinator.isFailed());

        // Now deliver 3 valid scans
        List<WifiObservation> obs1 = new ArrayList<>();
        // Create 60 APs for "NetBig"
        for (int i = 0; i < 60; i++) {
            obs1.add(new WifiObservation(String.format("00:AA:%02d", i), "NetBig", 2412, -50, clock.monoNs()));
        }
        // Create 2 APs for "NetSmall"
        obs1.add(new WifiObservation("00:BB:01", "NetSmall", 2412, -60, clock.monoNs()));
        obs1.add(new WifiObservation("00:BB:02", "NetSmall", 2412, -60, clock.monoNs()));

        ScanBatch b1 = new ScanBatch(obs1, clock.monoNs(), Tier.CALLBACK, true, false, 0);
        ScanBatch b2 = new ScanBatch(obs1, clock.monoNs(), Tier.CALLBACK, true, false, 0);
        ScanBatch b3 = new ScanBatch(obs1, clock.monoNs(), Tier.CALLBACK, true, false, 0);

        clock.advanceMs(1000);
        coordinator.handleBatch(b1);
        clock.advanceMs(1000);
        coordinator.handleBatch(b2);
        clock.advanceMs(1000);
        coordinator.handleBatch(b3);

        executor.awaitIdle(2000);
        assertTrue("Warmup done after retry", doneCalled.get());
        assertEquals(Sm1State.READY, engine.getState());

        // Test BSSID Selection and Cap 60
        // Select NetBig (60 APs) -> succeeds
        boolean selBig = coordinator.toggleSsid("NetBig");
        assertTrue(selBig);
        assertEquals(60, coordinator.getSelectedBssidCount());

        // Select NetSmall (2 APs) -> 60 + 2 = 62 > 60 -> refused!
        boolean selSmall = coordinator.toggleSsid("NetSmall");
        assertFalse("Should be refused over 60", selSmall);
        assertTrue("Cap exceeded listener should be notified", capExceeded.get());
        assertEquals(60, coordinator.getSelectedBssidCount());

        // Unselect NetBig -> 0 APs
        coordinator.toggleSsid("NetBig");
        assertEquals(0, coordinator.getSelectedBssidCount());

        // Now select NetSmall -> 2 APs succeeds
        boolean selSmall2 = coordinator.toggleSsid("NetSmall");
        assertTrue(selSmall2);
        assertEquals(2, coordinator.getSelectedBssidCount());
    }

    @Test
    public void testContinueIndicative_transitionsToReadyAndSetsIndicative() throws Exception {
        coordinator.start();
        executor.awaitIdle(2000);
        clock.advanceMs(20000);
        timeoutScheduler.trigger();
        executor.awaitIdle(2000);
        assertTrue(coordinator.isFailed());
        assertEquals(Sm1State.WARMUP_FAILED, engine.getState());

        coordinator.continueIndicative();
        executor.awaitIdle(2000);
        assertEquals(Sm1State.READY, engine.getState());
        assertTrue(coordinator.isIndicative());
        assertTrue(coordinator.isCompleted());
    }
}
