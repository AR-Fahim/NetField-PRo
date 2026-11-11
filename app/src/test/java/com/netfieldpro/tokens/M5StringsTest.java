package com.netfieldpro.tokens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class M5StringsTest {

    @Test
    public void testAllM5CanonicalStrings() {
        Context context = ApplicationProvider.getApplicationContext();

        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(R.string.diag_latency_title, "Latency probe");
        expected.put(R.string.diag_latency_target, "Target %1$s · %2$s");
        expected.put(R.string.diag_latency_method_icmp, "ICMP");
        expected.put(R.string.diag_latency_method_tcp, "TCP port %1$d");
        expected.put(R.string.diag_latency_probe, "Probe gateway");
        expected.put(R.string.diag_latency_running, "Probing…");
        expected.put(R.string.diag_latency_cancel, "Cancel");
        expected.put(R.string.diag_latency_summary, "Avg %1$s · Jitter %2$s · Lost %3$d/%4$d");
        expected.put(R.string.diag_latency_unit_ms, "ms");
        expected.put(R.string.diag_latency_lost_mark, "✕");
        expected.put(R.string.diag_latency_no_wifi, "Not connected to Wi-Fi");
        expected.put(R.string.diag_latency_blocked, "This network blocks local probes. Latency will be Not evaluated.");
        expected.put(R.string.diag_latency_net_lost, "Wi-Fi connection was lost during the probe.");
        expected.put(R.string.diag_latency_none, "n/a");

        for (Map.Entry<Integer, String> entry : expected.entrySet()) {
            String actual = context.getString(entry.getKey());
            assertNotNull("String resource missing for " + entry.getKey(), actual);
            assertEquals(entry.getValue(), actual);
        }
    }
}
