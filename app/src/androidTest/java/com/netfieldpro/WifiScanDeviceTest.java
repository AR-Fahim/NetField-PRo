package com.netfieldpro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.netfieldpro.core.wifi.Band;
import com.netfieldpro.core.wifi.ChannelMath;
import com.netfieldpro.core.wifi.Normalize;
import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.wifi.WifiObservation;
import com.netfieldpro.core.wifi.WifiScanSource;
import com.netfieldpro.wifi.AndroidWifiScanSource;

@RunWith(AndroidJUnit4.class)
public class WifiScanDeviceTest {

    private static final String TAG = "WifiScanDeviceTest";

    public static class GrantPermissionRule implements TestRule {
        private final String[] permissions;

        public static GrantPermissionRule grant(String... permissions) {
            return new GrantPermissionRule(permissions);
        }

        private GrantPermissionRule(String... permissions) {
            this.permissions = permissions;
        }

        @Override
        public Statement apply(Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Context ctx = ApplicationProvider.getApplicationContext();
                    String pkg = ctx.getPackageName();
                    for (String perm : permissions) {
                        try {
                            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(pkg, perm);
                        } catch (Throwable t) {
                            try {
                                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("pm grant " + pkg + " " + perm);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    base.evaluate();
                }
            };
        }
    }

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    );

    @Before
    public void setUp() {
        TestSetup.skipOnboarding();
        TestSetup.resetAppState();
    }

    private static boolean isEmulator() {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getMethod = systemProperties.getMethod("get", String.class, String.class);
            String qemu = (String) getMethod.invoke(null, "ro.kernel.qemu", "0");
            if ("1".equals(qemu)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Process process = Runtime.getRuntime().exec("getprop ro.kernel.qemu");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && "1".equals(line.trim())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu");
    }

    @Test
    public void testWifiScanSourceLifecycleAndBatch() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        WifiScanSource source = NetFieldApp.wifiScanSource();

        boolean emulator = isEmulator();
        long waitSec = emulator ? 20L : 60L;
        long startNs = SystemClock.elapsedRealtimeNanos();
        AtomicReference<Long> firstBatchMsRef = new AtomicReference<>(null);

        CountDownLatch batchLatch = new CountDownLatch(1);
        AtomicReference<ScanBatch> batchRef = new AtomicReference<>();

        // starts and stops the source cleanly
        source.start(new WifiScanSource.Listener() {
            @Override
            public void onBatch(ScanBatch batch) {
                long elapsed = (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000L;
                firstBatchMsRef.compareAndSet(null, elapsed);
                batchRef.set(batch);
                batchLatch.countDown();
            }

            @Override
            public void onScanThrottled() {
            }
        });

        // requestScan() does not throw
        try {
            source.requestScan();
        } catch (Throwable t) {
            source.stop();
            throw new AssertionError("requestScan() threw an exception", t);
        }

        // wait window: 60 s on PHYSICAL, 20 s on EMULATOR
        boolean received = batchLatch.await(waitSec, TimeUnit.SECONDS);
        long totalWaitMs = (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000L;
        source.stop();

        ScanBatch batch = batchRef.get();

        // log facts: scan.first_batch_ms, scan.tier, scan.rows, scan.updated, scan.degraded
        if (received && batch != null) {
            Long firstBatchMs = firstBatchMsRef.get();
            NfFacts.log("scan.first_batch_ms", firstBatchMs != null ? String.valueOf(firstBatchMs) : String.valueOf(totalWaitMs));
            NfFacts.log("scan.tier", source.getTier().name());
            NfFacts.log("scan.rows", String.valueOf(batch.rows().size()));
            NfFacts.log("scan.updated", String.valueOf(batch.updated()));
            NfFacts.log("scan.degraded", String.valueOf(batch.degradedTiming()));
        } else {
            NfFacts.log("scan.first_batch_ms", "none");
            NfFacts.log("scan.tier", source.getTier().name());
            NfFacts.log("scan.rows", "0");
            NfFacts.log("scan.updated", "false");
            NfFacts.log("scan.degraded", "false");
        }

        if (received && batch != null && !batch.rows().isEmpty()) {
            // every row must have a normalized BSSID and a band != UNKNOWN
            for (WifiObservation obs : batch.rows()) {
                assertEquals("BSSID must be normalized", Normalize.bssid(obs.bssid()), obs.bssid());
                assertNotEquals("Band must not be UNKNOWN", Band.UNKNOWN, ChannelMath.toBand(obs.freqMHz()));
            }

            // and the tier is reported to logcat and a result file
            Log.i(TAG, "Tier: " + source.getTier() + ", batch rows: " + batch.rows().size());

            File resultFile = new File(context.getFilesDir(), "wifi_scan_device_test.txt");
            try (FileWriter writer = new FileWriter(resultFile)) {
                writer.write("tier=" + source.getTier() + "\ncount=" + batch.rows().size() + "\n");
            }
        } else if (emulator) {
            // If no batch arrives on an emulator (ro.kernel.qemu=1) the test passes and logs NO_BATCH_ON_EMULATOR.
            Log.i(TAG, "NO_BATCH_ON_EMULATOR");
            if (batch != null) {
                Log.i(TAG, "Tier: " + source.getTier() + ", batch rows: 0");
            }
        } else {
            // On a physical device:
            if (received && batch != null && batch.rows().isEmpty()) {
                fail("NO_ROWS_ON_PHYSICAL");
            }
            List<String> debugLines = Collections.emptyList();
            if (source instanceof AndroidWifiScanSource) {
                debugLines = ((AndroidWifiScanSource) source).debugLog();
            }
            int startIdx = Math.max(0, debugLines.size() - 10);
            List<String> last10 = debugLines.subList(startIdx, debugLines.size());
            String joined = String.join(" | ", last10);
            fail("NO_BATCH_ON_PHYSICAL after " + totalWaitMs + "; log=" + joined);
        }
    }
}
