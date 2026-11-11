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
public class M3StringsTest {

    @Test
    public void testAllM3CanonicalStrings() {
        Context context = ApplicationProvider.getApplicationContext();

        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(R.string.onboarding_title, "NetField Pro");
        expected.put(R.string.onboarding_tagline, "Measure Wi-Fi you can stand behind.");
        expected.put(R.string.onboarding_perm_location_title, "Precise location");
        expected.put(R.string.onboarding_perm_location_why, "Android requires it to read Wi-Fi signal strength. NetField does not record your GPS position.");
        expected.put(R.string.onboarding_perm_notif_title, "Notifications");
        expected.put(R.string.onboarding_perm_notif_why, "Shows a status while a survey is running. Optional.");
        expected.put(R.string.perm_state_granted, "Allowed");
        expected.put(R.string.perm_state_can_ask, "Not allowed yet");
        expected.put(R.string.perm_state_permanent, "Blocked in system settings");
        expected.put(R.string.perm_state_approx, "Precise location needed");
        expected.put(R.string.perm_state_not_required, "Not needed on this Android version");
        expected.put(R.string.perm_btn_allow, "Allow");
        expected.put(R.string.perm_btn_settings, "Settings");
        expected.put(R.string.perm_btn_allow_desc, "Allow %1$s");
        expected.put(R.string.perm_btn_settings_desc, "Open settings for %1$s");
        expected.put(R.string.onboarding_row_desc, "%1$s. %2$s.");
        expected.put(R.string.onboarding_primary_continue, "Continue");
        expected.put(R.string.onboarding_primary_allow, "Allow location");
        expected.put(R.string.onboarding_primary_settings, "Open settings");
        expected.put(R.string.onboarding_skip, "Skip for now");
        expected.put(R.string.onboarding_verify, "Just verify a report");
        expected.put(R.string.onboarding_privacy, "Survey data stays on this phone.");
        expected.put(R.string.onboarding_settings_error, "Couldn't open settings");
        expected.put(R.string.debug_reset_onboarding, "Reset onboarding");

        for (Map.Entry<Integer, String> entry : expected.entrySet()) {
            String actual = context.getString(entry.getKey());
            assertNotNull("String resource missing for " + entry.getKey(), actual);
            assertEquals(entry.getValue(), actual);
        }
    }
}

