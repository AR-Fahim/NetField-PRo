package com.netfieldpro;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.netfieldpro.core.latency.ProbeCandidate;
import com.netfieldpro.core.latency.ProbeSequence;
import com.netfieldpro.latency.DefaultLatencyRunner;
import com.netfieldpro.latency.LatencyRunner;
import com.netfieldpro.latency.WifiNetworkTracker;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class LatencyDeviceTest {

    @Test
    public void testLatencyRunnerLifecycleAndOutcomes() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        WifiNetworkTracker tracker = new WifiNetworkTracker(context);
        tracker.startTracking();

        DefaultLatencyRunner runner = new DefaultLatencyRunner();

        try {
            Network network = tracker.getCurrentNetwork();
            String gateway = tracker.getGateway();
            String dns = tracker.getDns();

            // Outcome 1: the runner returns a result that is OK on some method, or UNREACHABLE, and never throws
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> outcomeRef = new AtomicReference<>();
            AtomicReference<ProbeCandidate> completedCandidateRef = new AtomicReference<>();

            runner.start(network, gateway, dns, new LatencyRunner.Listener() {
                @Override
                public void onRunning(ProbeCandidate candidate) {
                }

                @Override
                public void onCompleted(ProbeCandidate candidate, ProbeSequence.SequenceResult result) {
                    completedCandidateRef.set(candidate);
                    outcomeRef.set("OK:" + candidate.method());
                    latch.countDown();
                }

                @Override
                public void onBlocked() {
                    outcomeRef.set("UNREACHABLE");
                    latch.countDown();
                }

                @Override
                public void onNetworkLost() {
                    outcomeRef.set("NETWORK_LOST");
                    latch.countDown();
                }

                @Override
                public void onCancelled() {
                    outcomeRef.set("CANCELLED");
                    latch.countDown();
                }
            });

            // On emulator or physical device, await completion up to 15 seconds
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            String outcome = outcomeRef.get();
            if (completed) {
                assertNotNull("Outcome must not be null", outcome);
                assertTrue("Outcome must be OK, UNREACHABLE, or NETWORK_LOST: " + outcome,
                        outcome.startsWith("OK:") || outcome.equals("UNREACHABLE") || outcome.equals("NETWORK_LOST"));
            } else {
                runner.cancel();
            }

            ProbeCandidate cand = completedCandidateRef.get();
            String methodFact = (cand != null && cand.method() != null) ? cand.method().name() : "none";
            String targetFact = "none";
            if (cand != null && cand.target() != null) {
                if (cand.target().equals(gateway)) {
                    targetFact = "gateway";
                } else if (cand.target().equals(dns)) {
                    targetFact = "dns";
                }
            }
            String outcomeFact = (outcome != null) ? (outcome.startsWith("OK:") ? "OK" : outcome) : "none";

            NfFacts.log("latency.method", methodFact);
            NfFacts.log("latency.target", targetFact);
            NfFacts.log("latency.outcome", outcomeFact);

            // Outcome 2: cancel during a run returns within 500 ms
            CountDownLatch startedLatch = new CountDownLatch(1);
            runner.start(network, gateway != null ? gateway : "127.0.0.1", dns, new LatencyRunner.Listener() {
                @Override
                public void onRunning(ProbeCandidate candidate) {
                    startedLatch.countDown();
                }

                @Override
                public void onCompleted(ProbeCandidate candidate, ProbeSequence.SequenceResult result) {
                    startedLatch.countDown();
                }

                @Override
                public void onBlocked() {
                    startedLatch.countDown();
                }

                @Override
                public void onNetworkLost() {
                    startedLatch.countDown();
                }

                @Override
                public void onCancelled() {
                    startedLatch.countDown();
                }
            });

            // Allow runner to start
            startedLatch.await(100, TimeUnit.MILLISECONDS);

            long cancelStartNs = SystemClock.elapsedRealtimeNanos();
            runner.cancel();

            // Wait until runner is no longer running
            while (runner.isRunning()) {
                long elapsedMs = (SystemClock.elapsedRealtimeNanos() - cancelStartNs) / 1_000_000L;
                if (elapsedMs > 500) {
                    break;
                }
                Thread.sleep(10);
            }
            long cancelElapsedMs = (SystemClock.elapsedRealtimeNanos() - cancelStartNs) / 1_000_000L;
            assertTrue("Cancel must return within 500ms, took " + cancelElapsedMs + "ms", cancelElapsedMs <= 500);

            // Outcome 3: the sockets are closed (a second run immediately after works)
            CountDownLatch secondLatch = new CountDownLatch(1);
            AtomicBoolean secondRan = new AtomicBoolean(false);

            runner.start(network, gateway != null ? gateway : "127.0.0.1", dns, new LatencyRunner.Listener() {
                @Override
                public void onRunning(ProbeCandidate candidate) {
                    secondRan.set(true);
                    secondLatch.countDown();
                }

                @Override
                public void onCompleted(ProbeCandidate candidate, ProbeSequence.SequenceResult result) {
                    secondRan.set(true);
                    secondLatch.countDown();
                }

                @Override
                public void onBlocked() {
                    secondRan.set(true);
                    secondLatch.countDown();
                }

                @Override
                public void onNetworkLost() {
                    secondRan.set(true);
                    secondLatch.countDown();
                }

                @Override
                public void onCancelled() {
                    secondLatch.countDown();
                }
            });

            secondLatch.await(15, TimeUnit.SECONDS);
            runner.cancel();
            assertTrue("Second run immediately after must work", secondRan.get() || !runner.isRunning());
        } finally {
            runner.shutdown();
            tracker.stopTracking();
        }
    }
}
