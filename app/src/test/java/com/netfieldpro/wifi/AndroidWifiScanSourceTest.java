package com.netfieldpro.wifi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.netfieldpro.core.time.FakeClock;

@RunWith(AndroidJUnit4.class)
public class AndroidWifiScanSourceTest {

    @Test
    public void testStopTwiceDoesNotCrash() {
        Context context = ApplicationProvider.getApplicationContext();
        FakeClock clock = new FakeClock(1000000000L, 1700000000000L);
        AndroidWifiScanSource source = new AndroidWifiScanSource(context, clock);

        source.start(batch -> {});
        source.stop();
        source.stop(); // Unregister-twice must not crash
    }

    @Test
    public void testInitialTierIsCallback() {
        Context context = ApplicationProvider.getApplicationContext();
        FakeClock clock = new FakeClock(1000000000L, 1700000000000L);
        AndroidWifiScanSource source = new AndroidWifiScanSource(context, clock);

        assertNotNull(source.getTier());
    }

    @Test
    public void testDBG1_debugLogNonEmptyAfterStartAndStartsWithRegister() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        FakeClock clock = new FakeClock(1000000000L, 1700000000000L);
        AndroidWifiScanSource source = new AndroidWifiScanSource(context, clock);

        source.start(batch -> {});
        try {
            for (int i = 0; i < 50 && source.debugLog().isEmpty(); i++) {
                new CountDownLatch(1).await(20, TimeUnit.MILLISECONDS);
            }
            List<String> log = source.debugLog();
            assertNotNull(log);
            assertFalse("debugLog must be non-empty after start()", log.isEmpty());
            assertTrue("First line must start with REGISTER, but was: " + log.get(0),
                    log.get(0).startsWith("REGISTER"));
        } finally {
            source.stop();
        }
    }

    @Test
    public void testForcePollPrefStartsAtPollTier() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("nf_debug", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("scan_force_poll", true)
                .commit();
        try {
            FakeClock clock = new FakeClock(1000000000L, 1700000000000L);
            AndroidWifiScanSource source = new AndroidWifiScanSource(context, clock);
            source.start(batch -> {});
            try {
                org.junit.Assert.assertEquals(com.netfieldpro.core.wifi.Tier.POLL, source.getTier());
                assertTrue(source.getTierSelector().isConfirmed());
            } finally {
                source.stop();
            }
        } finally {
            context.getSharedPreferences("nf_debug", Context.MODE_PRIVATE)
                    .edit()
                    .remove("scan_force_poll")
                    .commit();
        }
    }
}
