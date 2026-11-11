package com.netfieldpro;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.net.Network;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import com.netfieldpro.core.latency.LatencyStats;
import com.netfieldpro.core.latency.ProbeCandidate;
import com.netfieldpro.core.latency.ProbeSequence;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.wifi.EnvironmentAdvisor;
import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.wifi.Tier;
import com.netfieldpro.core.wifi.WifiObservation;
import com.netfieldpro.core.wifi.WifiScanSource;
import com.netfieldpro.latency.LatencyRunner;
import com.netfieldpro.latency.WifiNetworkTracker;
import com.netfieldpro.wifi.WifiEnvironmentProbe;

@RunWith(AndroidJUnit4.class)
public class DiagnosticsA11yTest {

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    static class TestScanSource implements WifiScanSource {
        private Listener listener;
        private Tier tier = Tier.CALLBACK;

        @Override
        public void start(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void requestScan() {
        }

        @Override
        public void stop() {
        }

        @Override
        public Tier getTier() {
            return tier;
        }

        public void emitBatch(ScanBatch batch) {
            if (listener != null) {
                listener.onBatch(batch);
            }
        }
    }

    static class TestWifiEnvironmentProbe extends WifiEnvironmentProbe {
        boolean perm = true;
        boolean services = true;
        boolean wifi = true;
        EnvironmentAdvisor.ThrottleState throttle = EnvironmentAdvisor.ThrottleState.OFF;

        public TestWifiEnvironmentProbe(Context context) {
            super(context);
        }

        @Override
        public boolean hasLocationPermission() {
            return perm;
        }

        @Override
        public boolean isLocationServicesEnabled() {
            return services;
        }

        @Override
        public boolean isWifiEnabled() {
            return wifi;
        }

        @Override
        public EnvironmentAdvisor.ThrottleState isScanThrottleEnabled() {
            return throttle;
        }

        @Override
        public EnvironmentAdvisor.Advice getAdvice() {
            return EnvironmentAdvisor.advise(perm, services, wifi, throttle);
        }
    }

    static class TestLatencyRunner implements LatencyRunner {
        Listener lastListener;
        boolean running = false;

        @Override
        public void start(Network network, String gateway, String dns, Listener listener) {
            running = true;
            lastListener = listener;
            listener.onRunning(new ProbeCandidate(ProbeMethod.ICMP, gateway != null ? gateway : "192.168.1.1", 0));
        }

        @Override
        public void cancel() {
            running = false;
            if (lastListener != null) {
                lastListener.onCancelled();
            }
        }

        @Override
        public void onNetworkLost() {
            running = false;
            if (lastListener != null) {
                lastListener.onNetworkLost();
            }
        }

        @Override
        public void shutdown() {
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }
    }

    static class TestWifiNetworkTracker extends WifiNetworkTracker {
        boolean connected = true;
        String gw = "192.168.1.1";
        String dns = "192.168.1.2";

        public TestWifiNetworkTracker(Context context) {
            super(context);
        }

        @Override
        public synchronized boolean isConnected() {
            return connected;
        }

        @Override
        public synchronized String getGateway() {
            return gw;
        }

        @Override
        public synchronized String getDns() {
            return dns;
        }

        @Override
        public synchronized void startTracking() {
        }

        @Override
        public synchronized void stopTracking() {
        }
    }

    private TestScanSource fakeSource;
    private TestWifiEnvironmentProbe fakeProbe;
    private TestLatencyRunner fakeRunner;
    private TestWifiNetworkTracker fakeTracker;

    @Before
    public void setUp() {
        TestSetup.resetAppState();
        Context context = ApplicationProvider.getApplicationContext();
        fakeSource = new TestScanSource();
        fakeProbe = new TestWifiEnvironmentProbe(context);
        fakeRunner = new TestLatencyRunner();
        fakeTracker = new TestWifiNetworkTracker(context);

        NetFieldApp.setWifiScanSourceForTest(fakeSource);
        NetFieldApp.setWifiEnvironmentProbeForTest(fakeProbe);
        NetFieldApp.setLatencyRunnerForTest(fakeRunner);
        NetFieldApp.setWifiNetworkTrackerForTest(fakeTracker);
        TestSetup.skipOnboarding();
    }

    @After
    public void tearDown() {
        NetFieldApp.setWifiScanSourceForTest(null);
        NetFieldApp.setWifiEnvironmentProbeForTest(null);
        NetFieldApp.setLatencyRunnerForTest(null);
        NetFieldApp.setWifiNetworkTrackerForTest(null);
    }

    @Test
    public void testDiagnosticsAccessibilityLoadedState() {
        fakeProbe.perm = true;
        fakeProbe.services = true;
        fakeProbe.wifi = true;
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.OFF;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getNavController().navigate(R.id.nav_s20);
            });

            onView(withId(R.id.btn_diag_scan)).perform(scrollTo()).check(matches(isDisplayed()));

            scenario.onActivity(activity -> {
                WifiObservation obs1 = new WifiObservation("aa:00:00:00:00:01", "eduroam", 5180, -60, 1000L);
                WifiObservation obs2 = new WifiObservation("aa:00:00:00:00:02", "eduroam", 2437, -65, 1000L);
                fakeSource.emitBatch(new ScanBatch(List.of(obs1, obs2), 1000L, Tier.CALLBACK, true, false, 0));
            });

            onView(withId(R.id.list_diag_networks)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testDiagnosticsAccessibilityIssueState() {
        fakeProbe.perm = false;
        fakeProbe.services = true;
        fakeProbe.wifi = true;
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.OFF;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getNavController().navigate(R.id.nav_s20);
            });

            onView(withId(R.id.card_diag_issue)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.btn_diag_issue_fix)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testDiagnosticsAccessibilityLatencyResultState() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getNavController().navigate(R.id.nav_s20);
            });

            onView(withId(R.id.card_diag_latency)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.btn_diag_latency)).perform(scrollTo(), click());

            scenario.onActivity(activity -> {
                long[] us = new long[]{10_000L, 14_000L, 11_000L, 30_000L, 12_000L};
                LatencyStats.Summary summary = LatencyStats.summarize(us);
                ProbeSequence.SequenceResult seqResult = new ProbeSequence.SequenceResult(
                        us, List.of(0L, 200L, 400L, 600L, 800L), false, false, 5, summary
                );
                ProbeCandidate candidate = new ProbeCandidate(ProbeMethod.ICMP, "192.168.1.1", 0);
                fakeRunner.lastListener.onCompleted(candidate, seqResult);
            });

            onView(withId(R.id.text_diag_latency_target)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.text_diag_latency_values)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.text_diag_latency_summary)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testDiagnosticsAccessibilityLatencyBlockedState() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getNavController().navigate(R.id.nav_s20);
            });

            onView(withId(R.id.card_diag_latency)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.btn_diag_latency)).perform(scrollTo(), click());

            scenario.onActivity(activity -> {
                fakeRunner.lastListener.onBlocked();
            });

            onView(withId(R.id.text_diag_latency_state)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.btn_diag_latency)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
}
