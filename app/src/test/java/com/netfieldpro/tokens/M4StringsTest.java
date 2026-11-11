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
public class M4StringsTest {

    @Test
    public void testAllM4CanonicalStrings() {
        Context context = ApplicationProvider.getApplicationContext();

        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(R.string.diag_title, "Diagnostics");
        expected.put(R.string.diag_env_title, "Environment");
        expected.put(R.string.diag_env_permission, "Location permission");
        expected.put(R.string.diag_env_services, "Location services");
        expected.put(R.string.diag_env_wifi, "Wi-Fi");
        expected.put(R.string.diag_env_throttle, "Scan throttling");
        expected.put(R.string.diag_env_method, "Scan method");
        expected.put(R.string.diag_on, "On");
        expected.put(R.string.diag_off, "Off");
        expected.put(R.string.diag_allowed, "Allowed");
        expected.put(R.string.diag_not_allowed, "Not allowed");
        expected.put(R.string.diag_unknown, "Unknown");
        expected.put(R.string.diag_method_callback, "Callback");
        expected.put(R.string.diag_method_receiver, "Broadcast");
        expected.put(R.string.diag_method_poll, "Polling");
        expected.put(R.string.diag_scan_now, "Scan now");
        expected.put(R.string.diag_scanning, "Scanning…");
        expected.put(R.string.diag_last_scan, "Last scan %1$s · %2$d networks");
        expected.put(R.string.diag_ago_seconds, "%1$d s ago");
        expected.put(R.string.diag_networks_title, "Networks");
        expected.put(R.string.diag_hidden_ssid, "(hidden network)");
        expected.put(R.string.diag_connected, "Connected");
        expected.put(R.string.diag_empty, "No networks found");
        expected.put(R.string.diag_throttled, "Android did not allow a scan right now. Try again in a few seconds.");
        expected.put(R.string.diag_fix_permission, "Open settings");
        expected.put(R.string.diag_fix_services, "Location settings");
        expected.put(R.string.diag_issue_permission, "Location permission is needed to read Wi-Fi signal strength.");
        expected.put(R.string.diag_issue_services, "Turn on Location services. Android hides Wi-Fi results without it.");
        expected.put(R.string.diag_issue_wifi, "Turn on Wi-Fi.");
        expected.put(R.string.diag_issue_throttle_on, "Scan throttling is on. Surveys will be marked indicative. Turn it off in Developer options.");
        expected.put(R.string.diag_issue_throttle_unknown, "Could not read the scan throttling setting. Surveys will be marked indicative.");
        expected.put(R.string.diag_row_desc, "%1$s, %2$d dBm, %3$s, channel %4$d");
        expected.put(R.string.diag_band_24, "2.4 GHz");
        expected.put(R.string.diag_band_5, "5 GHz");
        expected.put(R.string.diag_band_6, "6 GHz");
        expected.put(R.string.diag_band_unknown, "Unknown band");
        expected.put(R.string.diag_dev_record, "[DEV] Record trace");

        for (Map.Entry<Integer, String> entry : expected.entrySet()) {
            String actual = context.getString(entry.getKey());
            assertNotNull("String resource missing for " + entry.getKey(), actual);
            assertEquals(entry.getValue(), actual);
        }
    }
}
