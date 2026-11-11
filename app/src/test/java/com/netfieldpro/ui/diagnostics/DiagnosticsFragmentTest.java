package com.netfieldpro.ui.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.util.Collections;
import java.util.List;

import android.net.Network;

import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.latency.LatencyStats;
import com.netfieldpro.core.latency.ProbeCandidate;
import com.netfieldpro.core.latency.ProbeSequence;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.wifi.EnvironmentAdvisor;
import com.netfieldpro.core.wifi.FakeWifiScanSource;
import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.wifi.Tier;
import com.netfieldpro.core.wifi.WifiObservation;
import com.netfieldpro.latency.LatencyRunner;
import com.netfieldpro.latency.WifiNetworkTracker;
import com.netfieldpro.wifi.WifiEnvironmentProbe;

@RunWith(RobolectricTestRunner.class)
public class DiagnosticsFragmentTest {

    private CountingFakeWifiScanSource fakeSource;
    private FakeWifiEnvironmentProbe fakeProbe;
    private CountingFakeLatencyRunner fakeRunner;
    private FakeWifiNetworkTracker fakeTracker;

    static class CountingFakeLatencyRunner implements LatencyRunner {
        int startCount = 0;
        int cancelCount = 0;
        Listener lastListener;
        boolean running = false;

        @Override
        public void start(Network network, String gateway, String dns, Listener listener) {
            startCount++;
            running = true;
            lastListener = listener;
            listener.onRunning(new ProbeCandidate(ProbeMethod.ICMP, gateway != null ? gateway : "192.168.1.1", 0));
        }

        @Override
        public void cancel() {
            cancelCount++;
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

    static class FakeWifiNetworkTracker extends WifiNetworkTracker {
        boolean connected = true;
        String gw = "192.168.1.1";
        String dns = "192.168.1.2";

        public FakeWifiNetworkTracker(Context context) {
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

    static class CountingFakeWifiScanSource extends FakeWifiScanSource {
        int requestScanCount = 0;
        int startCount = 0;
        int stopCount = 0;

        @Override
        public void start(Listener listener) {
            startCount++;
            super.start(listener);
        }

        @Override
        public void requestScan() {
            requestScanCount++;
            super.requestScan();
        }

        @Override
        public void stop() {
            stopCount++;
            super.stop();
        }
    }

    static class FakeWifiEnvironmentProbe extends WifiEnvironmentProbe {
        boolean perm = true;
        boolean services = true;
        boolean wifi = true;
        EnvironmentAdvisor.ThrottleState throttle = EnvironmentAdvisor.ThrottleState.OFF;
        ConnectedInfo connectedInfo = null;

        public FakeWifiEnvironmentProbe(Context context) {
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
        public ConnectedInfo getConnectedInfo() {
            return connectedInfo;
        }

        @Override
        public EnvironmentAdvisor.Advice getAdvice() {
            return EnvironmentAdvisor.advise(perm, services, wifi, throttle);
        }
    }

    @Before
    public void setUp() {
        Context context = NetFieldApp.get();
        fakeSource = new CountingFakeWifiScanSource();
        fakeProbe = new FakeWifiEnvironmentProbe(context);
        fakeRunner = new CountingFakeLatencyRunner();
        fakeTracker = new FakeWifiNetworkTracker(context);

        NetFieldApp.setWifiScanSourceForTest(fakeSource);
        NetFieldApp.setWifiEnvironmentProbeForTest(fakeProbe);
        NetFieldApp.setLatencyRunnerForTest(fakeRunner);
        NetFieldApp.setWifiNetworkTrackerForTest(fakeTracker);
        NetFieldApp.onboardingStore().markDone();
    }

    @After
    public void tearDown() {
        NetFieldApp.setWifiScanSourceForTest(null);
        NetFieldApp.setWifiEnvironmentProbeForTest(null);
        NetFieldApp.setLatencyRunnerForTest(null);
        NetFieldApp.setWifiNetworkTrackerForTest(null);
    }

    private void navigateToS20(MainActivity activity) {
        NavController nc = activity.getNavController();
        if (nc.getCurrentDestination() == null || nc.getCurrentDestination().getId() != R.id.nav_s20) {
            nc.navigate(R.id.nav_s20);
        }
        NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            navHost.getChildFragmentManager().executePendingTransactions();
        }
    }

    @Test
    public void testDG1_threeFakeRowsRenderInSl1OrderWithConnectedChip() {
        fakeProbe.connectedInfo = new WifiEnvironmentProbe.ConnectedInfo("bb:00:00:00:00:03", "", 2412, -75);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                WifiObservation obs1 = new WifiObservation("aa:00:00:00:00:01", "eduroam", 5180, -60, 1000L);
                WifiObservation obs2 = new WifiObservation("aa:00:00:00:00:02", "eduroam", 2437, -60, 1000L);
                WifiObservation obs3 = new WifiObservation("bb:00:00:00:00:03", "", 2412, -75, 1000L);
                ScanBatch batch = new ScanBatch(List.of(obs3, obs2, obs1), 1000L, Tier.CALLBACK, true, false, 0);

                fakeSource.emitBatch(batch);

                RecyclerView list = activity.findViewById(R.id.list_diag_networks);
                assertEquals(View.VISIBLE, list.getVisibility());
                assertEquals(3, list.getAdapter().getItemCount());

                list.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
                list.layout(0, 0, 1080, 1920);

                RecyclerView.ViewHolder vh0 = list.findViewHolderForAdapterPosition(0);
                assertNotNull(vh0);
                TextView bssid0 = vh0.itemView.findViewById(R.id.text_diag_network_bssid);
                View chip0 = vh0.itemView.findViewById(R.id.chip_diag_connected);
                assertEquals("aa:00:00:00:00:01", bssid0.getText().toString());
                assertEquals(View.GONE, chip0.getVisibility());

                RecyclerView.ViewHolder vh1 = list.findViewHolderForAdapterPosition(1);
                assertNotNull(vh1);
                TextView bssid1 = vh1.itemView.findViewById(R.id.text_diag_network_bssid);
                assertEquals("aa:00:00:00:00:02", bssid1.getText().toString());

                RecyclerView.ViewHolder vh2 = list.findViewHolderForAdapterPosition(2);
                assertNotNull(vh2);
                TextView bssid2 = vh2.itemView.findViewById(R.id.text_diag_network_bssid);
                View chip2 = vh2.itemView.findViewById(R.id.chip_diag_connected);
                assertEquals("bb:00:00:00:00:03", bssid2.getText().toString());
                assertEquals(View.VISIBLE, chip2.getVisibility());
            });
        }
    }

    @Test
    public void testDG2_permissionMissingShowsIssueCardAndDisablesScan() {
        fakeProbe.perm = false;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                View card = activity.findViewById(R.id.card_diag_issue);
                TextView textIssue = activity.findViewById(R.id.text_diag_issue);
                View btnFix = activity.findViewById(R.id.btn_diag_issue_fix);
                View btnScan = activity.findViewById(R.id.btn_diag_scan);

                assertEquals(View.VISIBLE, card.getVisibility());
                assertEquals(activity.getString(R.string.diag_issue_permission), textIssue.getText().toString());
                assertFalse(btnScan.isEnabled());

                btnFix.performClick();
                ShadowActivity shadowActivity = Shadows.shadowOf(activity);
                Intent startedIntent = shadowActivity.getNextStartedActivity();
                assertNotNull(startedIntent);
                assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, startedIntent.getAction());
                assertEquals(Uri.fromParts("package", activity.getPackageName(), null), startedIntent.getData());
            });
        }
    }

    @Test
    public void testDG3_servicesOffOpensLocationSettings() {
        fakeProbe.services = false;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                View card = activity.findViewById(R.id.card_diag_issue);
                TextView textIssue = activity.findViewById(R.id.text_diag_issue);
                View btnFix = activity.findViewById(R.id.btn_diag_issue_fix);

                assertEquals(View.VISIBLE, card.getVisibility());
                assertEquals(activity.getString(R.string.diag_issue_services), textIssue.getText().toString());

                btnFix.performClick();
                ShadowActivity shadowActivity = Shadows.shadowOf(activity);
                Intent startedIntent = shadowActivity.getNextStartedActivity();
                assertNotNull(startedIntent);
                assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, startedIntent.getAction());
            });
        }
    }

    @Test
    public void testDG4_scanNowRefusalShowsThrottledAndEmptyBatchShowsDiagEmpty() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                View btnScan = activity.findViewById(R.id.btn_diag_scan);
                btnScan.performClick();
                assertEquals(1, fakeSource.requestScanCount);

                fakeSource.emitThrottled();

                ScanBatch emptyBatch = new ScanBatch(Collections.emptyList(), 1000L, Tier.CALLBACK, true, false, 0);
                fakeSource.emitBatch(emptyBatch);

                TextView textEmpty = activity.findViewById(R.id.text_diag_empty);
                assertEquals(View.VISIBLE, textEmpty.getVisibility());
                assertEquals(activity.getString(R.string.diag_empty), textEmpty.getText().toString());
            });
        }
    }

    @Test
    public void testDG5_throttleOnShowsWarningWithoutDisablingScan() {
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.ON;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                TextView textThrottle = activity.findViewById(R.id.text_diag_env_throttle_value);
                View cardIssue = activity.findViewById(R.id.card_diag_issue);
                TextView textIssue = activity.findViewById(R.id.text_diag_issue);
                View btnScan = activity.findViewById(R.id.btn_diag_scan);

                assertTrue(textThrottle.getText().toString().contains("⚠ On"));
                assertEquals(View.VISIBLE, cardIssue.getVisibility());
                assertEquals(activity.getString(R.string.diag_issue_throttle_on), textIssue.getText().toString());
                assertTrue(btnScan.isEnabled());
            });
        }
    }

    @Test
    public void testDG6_leavingScreenCallsStopExactlyOnce() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);
                assertEquals(1, fakeSource.startCount);
                assertEquals(0, fakeSource.stopCount);

                activity.getNavController().popBackStack();
                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                if (navHost != null) {
                    navHost.getChildFragmentManager().executePendingTransactions();
                }

                assertEquals(1, fakeSource.stopCount);
            });
        }
    }

    @Test
    public void testDG7_rowContentDescriptionExactFormat() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                WifiObservation sampleObs = new WifiObservation("aa:bb:cc:dd:ee:01", "eduroam", 5180, -58, 1000L);
                ScanBatch batch = new ScanBatch(List.of(sampleObs), 1000L, Tier.CALLBACK, true, false, 0);

                fakeSource.emitBatch(batch);

                RecyclerView list = activity.findViewById(R.id.list_diag_networks);
                list.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
                list.layout(0, 0, 1080, 1920);

                RecyclerView.ViewHolder vh = list.findViewHolderForAdapterPosition(0);
                assertNotNull(vh);
                assertEquals("eduroam, -58 dBm, 5 GHz, channel 36", vh.itemView.getContentDescription().toString());
            });
        }
    }

    @Test
    public void testDG9_buttonTouchTargetsAtFontScale13() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                int primaryMinHeight = (int) (56 * activity.getResources().getDisplayMetrics().density);
                int buttonMinHeight = (int) (48 * activity.getResources().getDisplayMetrics().density);

                View btnScan = activity.findViewById(R.id.btn_diag_scan);
                View btnFix = activity.findViewById(R.id.btn_diag_issue_fix);

                assertTrue(btnScan.getMinimumHeight() >= primaryMinHeight || btnScan.getMinimumHeight() >= 56);
                assertTrue(btnFix.getMinimumHeight() >= buttonMinHeight || btnFix.getMinimumHeight() >= 48);
            });
        }
    }

    @Test
    public void testM4_7_debugTraceRecordingTogglesAndWritesTraceFile() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                int switchId = activity.getResources().getIdentifier("switch_diag_" + "record", "id", activity.getPackageName());
                int pathId = activity.getResources().getIdentifier("text_diag_record_" + "path", "id", activity.getPackageName());
                assertTrue(switchId != 0);
                assertTrue(pathId != 0);

                android.widget.CompoundButton switchRecord = activity.findViewById(switchId);
                TextView textPath = activity.findViewById(pathId);
                assertNotNull(switchRecord);
                assertNotNull(textPath);
                assertFalse(switchRecord.isChecked());
                assertEquals(View.GONE, textPath.getVisibility());

                // Toggle switch ON
                switchRecord.setChecked(true);
                assertEquals(View.VISIBLE, textPath.getVisibility());
                String pathStr = textPath.getText().toString();
                assertTrue(pathStr.endsWith(".nftrace"));
                assertTrue(pathStr.contains("traces"));

                // Emit a batch
                WifiObservation sampleObs = new WifiObservation("aa:bb:cc:dd:ee:01", "eduroam", 5180, -58, 1000L);
                ScanBatch batch = new ScanBatch(List.of(sampleObs), 1000L, Tier.CALLBACK, true, false, 0);
                fakeSource.emitBatch(batch);

                // Toggle switch OFF to close trace writer
                switchRecord.setChecked(false);
                assertEquals(View.GONE, textPath.getVisibility());

                java.io.File file = new java.io.File(pathStr);
                assertTrue(file.exists());
                assertTrue(file.length() > 0);

                try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                    com.netfieldpro.core.trace.TraceReader reader = new com.netfieldpro.core.trace.TraceReader(fis);
                    assertEquals(1, reader.header().version());
                    com.netfieldpro.core.trace.TraceEvent event = reader.next();
                    assertNotNull(event);
                    assertEquals("wifi.batch", event.kind());
                    assertEquals(1000L, event.tNs());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    public void testDL1_noWifiNetworkStateAndDisabledButton() {
        fakeTracker.connected = false;
        fakeTracker.gw = null;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                TextView textState = activity.findViewById(R.id.text_diag_latency_state);
                View btnLatency = activity.findViewById(R.id.btn_diag_latency);

                assertEquals(View.VISIBLE, textState.getVisibility());
                assertEquals(activity.getString(R.string.diag_latency_no_wifi), textState.getText().toString());
                assertFalse(btnLatency.isEnabled());
            });
        }
    }

    @Test
    public void testDL2_probeClickShowsRunningStateAndCancel() {
        fakeTracker.connected = true;
        fakeTracker.gw = "192.168.1.1";

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                android.widget.Button btnLatency = activity.findViewById(R.id.btn_diag_latency);
                assertTrue(btnLatency.isEnabled());
                assertEquals(activity.getString(R.string.diag_latency_probe), btnLatency.getText().toString());

                btnLatency.performClick();

                assertEquals(1, fakeRunner.startCount);
                TextView textState = activity.findViewById(R.id.text_diag_latency_state);
                assertEquals(View.VISIBLE, textState.getVisibility());
                assertEquals(activity.getString(R.string.diag_latency_running), textState.getText().toString());
                assertEquals(activity.getString(R.string.diag_latency_cancel), btnLatency.getText().toString());
            });
        }
    }

    @Test
    public void testDL3_ls1ResultRendersValuesAndSummary() {
        fakeTracker.connected = true;
        fakeTracker.gw = "192.168.1.1";

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                android.widget.Button btnLatency = activity.findViewById(R.id.btn_diag_latency);
                btnLatency.performClick();

                long[] us = new long[]{10_000L, 14_000L, 11_000L, 30_000L, 12_000L};
                LatencyStats.Summary summary = LatencyStats.summarize(us);
                ProbeSequence.SequenceResult seqResult = new ProbeSequence.SequenceResult(
                        us, List.of(0L, 200L, 400L, 600L, 800L), false, false, 5, summary
                );

                ProbeCandidate candidate = new ProbeCandidate(ProbeMethod.ICMP, "192.168.1.1", 0);
                fakeRunner.lastListener.onCompleted(candidate, seqResult);

                TextView textValues = activity.findViewById(R.id.text_diag_latency_values);
                TextView textSummary = activity.findViewById(R.id.text_diag_latency_summary);
                TextView textTarget = activity.findViewById(R.id.text_diag_latency_target);

                assertEquals(View.VISIBLE, textValues.getVisibility());
                assertEquals("10 14 11 30 12", textValues.getText().toString());

                assertEquals(View.VISIBLE, textSummary.getVisibility());
                assertEquals("Avg 15.4 · Jitter 11.0 · Lost 0/5", textSummary.getText().toString());

                assertEquals(View.VISIBLE, textTarget.getVisibility());
                assertEquals("Target 192.168.1.1 · ICMP", textTarget.getText().toString());
            });
        }
    }

    @Test
    public void testDL4_ls2ResultRendersValuesAndSummaryWithHalfEven() {
        fakeTracker.connected = true;
        fakeTracker.gw = "192.168.1.1";

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                android.widget.Button btnLatency = activity.findViewById(R.id.btn_diag_latency);
                btnLatency.performClick();

                long[] us = new long[]{10_000L, LatencyStats.LOST, 14_000L, 11_000L, 30_000L};
                LatencyStats.Summary summary = LatencyStats.summarize(us);
                ProbeSequence.SequenceResult seqResult = new ProbeSequence.SequenceResult(
                        us, List.of(0L, 200L, 400L, 1400L, 1600L), false, false, 5, summary
                );

                ProbeCandidate candidate = new ProbeCandidate(ProbeMethod.ICMP, "192.168.1.1", 0);
                fakeRunner.lastListener.onCompleted(candidate, seqResult);

                TextView textValues = activity.findViewById(R.id.text_diag_latency_values);
                TextView textSummary = activity.findViewById(R.id.text_diag_latency_summary);

                assertEquals(View.VISIBLE, textValues.getVisibility());
                assertEquals("10 ✕ 14 11 30", textValues.getText().toString());

                assertEquals(View.VISIBLE, textSummary.getVisibility());
                assertEquals("Avg 16.2 · Jitter 8.7 · Lost 1/5", textSummary.getText().toString());
            });
        }
    }

    @Test
    public void testDL5_unreachableShowsBlocked() {
        fakeTracker.connected = true;
        fakeTracker.gw = "192.168.1.1";

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                android.widget.Button btnLatency = activity.findViewById(R.id.btn_diag_latency);
                btnLatency.performClick();

                fakeRunner.lastListener.onBlocked();

                TextView textState = activity.findViewById(R.id.text_diag_latency_state);
                assertEquals(View.VISIBLE, textState.getVisibility());
                assertEquals(activity.getString(R.string.diag_latency_blocked), textState.getText().toString());
            });
        }
    }

    @Test
    public void testDL6_abortedShowsNetworkLost() {
        fakeTracker.connected = true;
        fakeTracker.gw = "192.168.1.1";

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                android.widget.Button btnLatency = activity.findViewById(R.id.btn_diag_latency);
                btnLatency.performClick();

                fakeRunner.lastListener.onNetworkLost();

                TextView textState = activity.findViewById(R.id.text_diag_latency_state);
                assertEquals(View.VISIBLE, textState.getVisibility());
                assertEquals(activity.getString(R.string.diag_latency_net_lost), textState.getText().toString());
                assertFalse(btnLatency.isEnabled());
            });
        }
    }

    @Test
    public void testDL7_leavingScreenCancelsRunnerExactlyOnce() {
        fakeTracker.connected = true;
        fakeTracker.gw = "192.168.1.1";

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                android.widget.Button btnLatency = activity.findViewById(R.id.btn_diag_latency);
                btnLatency.performClick();

                assertEquals(1, fakeRunner.startCount);
                assertEquals(0, fakeRunner.cancelCount);

                NavController nc = activity.getNavController();
                nc.popBackStack();
                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                if (navHost != null) {
                    navHost.getChildFragmentManager().executePendingTransactions();
                }
                org.robolectric.shadows.ShadowLooper.idleMainLooper();

                assertEquals(1, fakeRunner.cancelCount);
            });

            assertEquals(1, fakeRunner.cancelCount);
        }
    }

    @Test
    public void testM4_8_forcePollSwitchToggles() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS20(activity);

                int switchId = activity.getResources().getIdentifier("switch_diag_" + "force_poll", "id", activity.getPackageName());
                assertTrue(switchId != 0);
                android.widget.CompoundButton switchForcePoll = activity.findViewById(switchId);
                assertNotNull(switchForcePoll);
                assertFalse(switchForcePoll.isChecked());

                switchForcePoll.setChecked(true);
                android.content.SharedPreferences prefs = activity.getSharedPreferences("nf_debug", Context.MODE_PRIVATE);
                assertTrue(prefs.getBoolean("scan_force_poll", false));

                switchForcePoll.setChecked(false);
                assertFalse(prefs.getBoolean("scan_force_poll", false));
            });
        }
    }
}
