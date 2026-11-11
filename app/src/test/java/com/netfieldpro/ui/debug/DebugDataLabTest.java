package com.netfieldpro.ui.debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import android.widget.TextView;

import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import com.netfieldpro.MainActivity;
import com.netfieldpro.R;
import com.netfieldpro.core.json.CanonicalJson;
import com.netfieldpro.core.util.Sha256;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DebugDataLabTest {

    @Test
    public void testDataLabCreateExportDelete() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                // Navigate to Debug Data Lab
                activity.getNavController().navigate(R.id.nav_debug_data_lab);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                if (navHost != null) {
                    navHost.getChildFragmentManager().executePendingTransactions();
                }
                ShadowLooper.idleMainLooper();

                DebugDataLabFragment fragment = (DebugDataLabFragment) navHost.getChildFragmentManager()
                        .getPrimaryNavigationFragment();
                assertNotNull(fragment);

                // Initial counts: project = 0, floor = 0, survey_point = 0
                fragment.refreshCounts();
                ShadowLooper.idleMainLooper();

                // 1. Click Create Demo Project
                activity.findViewById(R.id.btn_create_demo).performClick();
                fragment.getBatchedWriter().flush();
                ShadowLooper.idleMainLooper();
                // Wait for counts query on executor
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
                ShadowLooper.idleMainLooper();

                TextView countProject = activity.findViewById(R.id.count_project);
                TextView countFloor = activity.findViewById(R.id.count_floor);
                TextView countPoint = activity.findViewById(R.id.count_survey_point);

                assertEquals("1", countProject.getText().toString());
                assertEquals("1", countFloor.getText().toString());
                assertEquals("1", countPoint.getText().toString());

                // 2. Click Export Canonical JSON
                activity.findViewById(R.id.btn_export_json).performClick();
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ignored) {}
                ShadowLooper.idleMainLooper();

                File exportFile = new File(activity.getCacheDir(), "datalab-export.json");
                assertTrue("Export file must exist", exportFile.exists());
                assertTrue("Export file must be non-empty", exportFile.length() > 0);

                byte[] bytes;
                try {
                    bytes = Files.readAllBytes(exportFile.toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // Verify canonical JSON formatting
                CanonicalJson.verifyCanonical(bytes);

                // Verify on-screen text contains the length and SHA-256 hash
                String expectedHash = Sha256.hex(Sha256.hash(bytes));
                TextView textExportInfo = activity.findViewById(R.id.text_export_info);
                String infoText = textExportInfo.getText().toString();
                assertTrue(infoText.contains(String.valueOf(bytes.length)));
                assertTrue(infoText.contains(expectedHash));

                // 3. Click Delete Demo Project
                activity.findViewById(R.id.btn_delete_demo).performClick();
                fragment.getBatchedWriter().flush();
                ShadowLooper.idleMainLooper();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
                ShadowLooper.idleMainLooper();

                assertEquals("0", countProject.getText().toString());
                assertEquals("0", countFloor.getText().toString());
                assertEquals("0", countPoint.getText().toString());
            });
        }
    }
}

