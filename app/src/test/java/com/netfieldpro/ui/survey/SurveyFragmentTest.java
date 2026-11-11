package com.netfieldpro.ui.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.session.Sm1State;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.entity.DeviceInfoRow;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.MeasurementRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.session.SessionRuntimeStore;
import com.netfieldpro.ui.plan.PlanView;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;

import com.netfieldpro.NoAuthRule;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SurveyFragmentTest {

    @Rule
    public NoAuthRule noAuthRule = new NoAuthRule();

    private String testProjectId;
    private String testFloorId;
    private String testDeviceId;
    private String testProfileId;
    private String testSessionId;
    private String testPointId1;
    private String testPointId2;

    @Before
    public void setUp() {
        NetFieldApp.onboardingStore().markDone();
        SurveyFragment.setBackgroundExecutorForTest(Runnable::run);
        PointDetailSheetFragment.setBackgroundExecutorForTest(Runnable::run);

        testProjectId = IdGenerator.generate();
        testFloorId = IdGenerator.generate();
        testDeviceId = IdGenerator.generate();
        testProfileId = IdGenerator.generate();
        testSessionId = IdGenerator.generate();
        testPointId1 = IdGenerator.generate();
        testPointId2 = IdGenerator.generate();

        NetFieldDatabase db = NetFieldDatabase.get(ApplicationProvider.getApplicationContext());

        DeviceInfoRow dev = new DeviceInfoRow();
        dev.id = testDeviceId;
        dev.manufacturer = "Google";
        dev.model = "Pixel 8";
        dev.sdk = 34;
        dev.wifiStandards = "[]";
        dev.buildFingerprint = "fingerprint";
        db.netFieldDao().insertDeviceInfo(dev);

        ProjectRow p = new ProjectRow();
        p.id = testProjectId;
        p.name = "Test Project";
        p.site = "Test Site";
        p.createdAtMs = 1700000000000L;
        db.netFieldDao().insertProject(p);

        FloorRow f = new FloorRow();
        f.id = testFloorId;
        f.projectId = testProjectId;
        f.levelIndex = 0;
        f.planSha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        f.planWpx = 1000;
        f.planHpx = 1000;
        f.metersPerPixel = 0.05;
        f.scaleMethod = "LINE";
        f.scaleUncertaintyPct = 1.0;
        db.netFieldDao().insertFloor(f);

        SurveySessionRow session = new SurveySessionRow();
        session.id = testSessionId;
        session.floorId = testFloorId;
        session.name = "Survey 14:05";
        session.state = "DRAFT";
        session.evidenceGrade = "VERDICT";
        session.sessionType = "STANDARD";
        session.timingQuality = "GOOD";
        session.schemaVersion = 1;
        session.appVersion = "1.0";
        session.algoVersion = "1.0";
        session.throttleAtStart = false;
        session.probeMethod = "NONE";
        session.referenceFreqsMhz = "[2412,5180]";
        session.receiverId = "builtin-wifi";
        session.deviceInfoId = testDeviceId;
        session.monoEpochNs = 1000000000L;
        session.createdAtMs = 1700000000000L;
        session.intendedProfileChanges = 0;
        session.rssiOffsetDb = 0.0;
        session.linkCadenceMs = 3000;
        session.cadenceFlag = "DEFAULT";
        session.intendedProfileId = testProfileId;
        session.capabilityReport = "{}";
        db.netFieldDao().insertSurveySession(session);

        TargetNetworkRow target = new TargetNetworkRow();
        target.id = IdGenerator.generate();
        target.sessionId = testSessionId;
        target.ssid = "TestNet";
        db.netFieldDao().insertTargetNetwork(target);

        SurveyPointRow pt1 = new SurveyPointRow();
        pt1.id = testPointId1;
        pt1.sessionId = testSessionId;
        pt1.x = 10.0;
        pt1.y = 20.0;
        pt1.tMonoNs = 2000000000L;
        pt1.source = "TAP";
        pt1.quality = "OK";
        db.netFieldDao().insertSurveyPoint(pt1);

        SurveyPointRow pt2 = new SurveyPointRow();
        pt2.id = testPointId2;
        pt2.sessionId = testSessionId;
        pt2.x = 15.0;
        pt2.y = 25.0;
        pt2.tMonoNs = 3000000000L;
        pt2.source = "TAP";
        pt2.quality = "LOW_CONFIDENCE";
        db.netFieldDao().insertSurveyPoint(pt2);

        MeasurementRow m1 = new MeasurementRow();
        m1.id = IdGenerator.generate();
        m1.pointId = testPointId1;
        m1.protocol = "WIFI";
        m1.bssidOrId = "00:11:22:33:44:55";
        m1.ssid = "TestNet";
        m1.freqMHz = 2412;
        m1.channel = 1;
        m1.rssiMedian = -55.0;
        m1.n = 2;
        m1.rssiMin = -56;
        m1.rssiMax = -54;
        m1.source = "SCAN";
        m1.receiverId = "builtin-wifi";
        db.netFieldDao().insertMeasurement(m1);

        SessionRuntimeStore.create(ApplicationProvider.getApplicationContext()).putState(testSessionId, Sm1State.READY);
    }

    @After
    public void tearDown() {
        SurveyFragment.setBackgroundExecutorForTest(null);
        PointDetailSheetFragment.setBackgroundExecutorForTest(null);

        NetFieldDatabase db = NetFieldDatabase.get(ApplicationProvider.getApplicationContext());
        if (testPointId1 != null) {
            db.netFieldDao().deleteSurveyPoint(testPointId1);
        }
        if (testPointId2 != null) {
            db.netFieldDao().deleteSurveyPoint(testPointId2);
        }
        if (testSessionId != null) {
            db.netFieldDao().deleteSurveySession(testSessionId);
        }
        if (testFloorId != null) {
            db.netFieldDao().deleteFloor(testFloorId);
        }
        if (testProjectId != null) {
            db.netFieldDao().deleteProject(testProjectId);
        }
    }

    @Test
    public void testS07Smoke_allUiIdsExistAndAreConfigured() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavController nav = activity.getNavController();
                Bundle args = new Bundle();
                args.putString("sessionId", testSessionId);
                nav.navigate(R.id.nav_s07, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();
                ShadowLooper.idleMainLooper();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                assertTrue(current instanceof SurveyFragment);

                View root = current.getView();
                assertNotNull(root);

                // M8a-4 required UI ids: plan_view, text_pin_count, sheet_capture, text_capture_step, btn_capture_cancel, btn_view_heatmap, action_end_survey
                PlanView planView = root.findViewById(R.id.plan_view);
                assertNotNull("plan_view must exist", planView);

                TextView textPinCount = root.findViewById(R.id.text_pin_count);
                assertNotNull("text_pin_count must exist", textPinCount);
                assertTrue(textPinCount.getText().toString().contains("2"));

                MaterialCardView sheetCapture = root.findViewById(R.id.sheet_capture);
                assertNotNull("sheet_capture must exist", sheetCapture);
                assertEquals(View.GONE, sheetCapture.getVisibility());

                TextView textCaptureStep = root.findViewById(R.id.text_capture_step);
                assertNotNull("text_capture_step must exist", textCaptureStep);

                MaterialButton btnCaptureCancel = root.findViewById(R.id.btn_capture_cancel);
                assertNotNull("btn_capture_cancel must exist", btnCaptureCancel);

                MaterialButton btnViewHeatmap = root.findViewById(R.id.btn_view_heatmap);
                assertNotNull("btn_view_heatmap must exist", btnViewHeatmap);

                View actionEndSurvey = root.findViewById(R.id.action_end_survey);
                assertNotNull("action_end_survey must exist", actionEndSurvey);
            });
        }
    }

    @Test
    public void testS07_viewHeatmap_navigatesToS09() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavController nav = activity.getNavController();
                Bundle args = new Bundle();
                args.putString("sessionId", testSessionId);
                nav.navigate(R.id.nav_s07, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();
                ShadowLooper.idleMainLooper();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                View root = current.getView();
                assertNotNull(root);

                MaterialButton btnViewHeatmap = root.findViewById(R.id.btn_view_heatmap);
                btnViewHeatmap.performClick();
                navHost.getChildFragmentManager().executePendingTransactions();
                ShadowLooper.idleMainLooper();

                assertEquals(R.id.nav_s09, nav.getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testS07_endSurvey_confirmEndsSessionAndNavigatesBack() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavController nav = activity.getNavController();

                // Setup back stack: S02 -> S03 -> S06 -> S07
                Bundle pArgs = new Bundle();
                pArgs.putString("projectId", testProjectId);
                nav.navigate(R.id.action_s02_to_s03, pArgs);

                Bundle args = new Bundle();
                args.putString("sessionId", testSessionId);
                nav.navigate(R.id.action_s03_to_s06);
                nav.navigate(R.id.action_s06_to_s07, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();
                ShadowLooper.idleMainLooper();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                View root = current.getView();
                assertNotNull(root);

                View actionEndSurvey = root.findViewById(R.id.action_end_survey);
                actionEndSurvey.performClick();
                ShadowLooper.idleMainLooper();

                AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
                assertNotNull("Confirm dialog must be shown", dialog);

                dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                navHost.getChildFragmentManager().executePendingTransactions();
                ShadowLooper.idleMainLooper();

                // Verify runtime store updated to ENDED
                Sm1State state = SessionRuntimeStore.create(ApplicationProvider.getApplicationContext()).getState(testSessionId);
                assertEquals(Sm1State.ENDED, state);

                // Verify navigated back to S03
                assertEquals(R.id.nav_s03, nav.getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testS08PointSheetSmoke_allUiIdsExistAndChipQualityIsWord() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PointDetailSheetFragment sheet = new PointDetailSheetFragment();
                Bundle args = new Bundle();
                args.putString("pointId", testPointId1);
                sheet.setArguments(args);
                sheet.show(activity.getSupportFragmentManager(), "point_sheet");

                activity.getSupportFragmentManager().executePendingTransactions();
                ShadowLooper.idleMainLooper();

                View root = sheet.getView();
                assertNotNull(root);

                // S08 UI ids: sheet_point, text_point_title, chip_quality, list_point_bssids, btn_point_delete
                View sheetPoint = root.findViewById(R.id.sheet_point);
                assertNotNull("sheet_point must exist", sheetPoint);

                TextView textPointTitle = root.findViewById(R.id.text_point_title);
                assertNotNull("text_point_title must exist", textPointTitle);

                Chip chipQuality = root.findViewById(R.id.chip_quality);
                assertNotNull("chip_quality must exist", chipQuality);
                assertEquals("OK", chipQuality.getText().toString());

                RecyclerView listPointBssids = root.findViewById(R.id.list_point_bssids);
                assertNotNull("list_point_bssids must exist", listPointBssids);

                MaterialButton btnPointDelete = root.findViewById(R.id.btn_point_delete);
                assertNotNull("btn_point_delete must exist", btnPointDelete);
                assertEquals(View.VISIBLE, btnPointDelete.getVisibility());
            });
        }
    }

    @Test
    public void testS08PointSheet_qualityChipsAreWords() {
        // Test LOW_CONFIDENCE -> "Low confidence"
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PointDetailSheetFragment sheet = new PointDetailSheetFragment();
                Bundle args = new Bundle();
                args.putString("pointId", testPointId2);
                sheet.setArguments(args);
                sheet.show(activity.getSupportFragmentManager(), "point_sheet_2");

                activity.getSupportFragmentManager().executePendingTransactions();
                ShadowLooper.idleMainLooper();

                Chip chipQuality = sheet.getView().findViewById(R.id.chip_quality);
                assertNotNull(chipQuality);
                assertEquals("Low confidence", chipQuality.getText().toString());
            });
        }

        // Test MOVED -> "Moved"
        String movedPointId = IdGenerator.generate();
        NetFieldDatabase db = NetFieldDatabase.get(ApplicationProvider.getApplicationContext());
        SurveyPointRow ptMoved = new SurveyPointRow();
        ptMoved.id = movedPointId;
        ptMoved.sessionId = testSessionId;
        ptMoved.x = 5.0;
        ptMoved.y = 5.0;
        ptMoved.tMonoNs = 4000000000L;
        ptMoved.source = "TAP";
        ptMoved.quality = "MOVED";
        db.netFieldDao().insertSurveyPoint(ptMoved);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PointDetailSheetFragment sheet = new PointDetailSheetFragment();
                Bundle args = new Bundle();
                args.putString("pointId", movedPointId);
                sheet.setArguments(args);
                sheet.show(activity.getSupportFragmentManager(), "point_sheet_3");

                activity.getSupportFragmentManager().executePendingTransactions();
                ShadowLooper.idleMainLooper();

                Chip chipQuality = sheet.getView().findViewById(R.id.chip_quality);
                assertNotNull(chipQuality);
                assertEquals("Moved", chipQuality.getText().toString());
            });
        }
    }

    @Test
    public void testS08PointSheet_deletePointConfirmed() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PointDetailSheetFragment sheet = new PointDetailSheetFragment();
                Bundle args = new Bundle();
                args.putString("pointId", testPointId1);
                sheet.setArguments(args);
                sheet.show(activity.getSupportFragmentManager(), "point_sheet_del");

                activity.getSupportFragmentManager().executePendingTransactions();
                ShadowLooper.idleMainLooper();

                MaterialButton btnPointDelete = sheet.getView().findViewById(R.id.btn_point_delete);
                btnPointDelete.performClick();
                ShadowLooper.idleMainLooper();

                AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
                assertNotNull("Delete confirmation dialog must be shown", dialog);

                dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                ShadowLooper.idleMainLooper();

                // Verify point deleted from database
                NetFieldDatabase db = NetFieldDatabase.get(ApplicationProvider.getApplicationContext());
                SurveyPointRow point = db.netFieldDao().getSurveyPoint(testPointId1);
                assertTrue("Survey point must be deleted", point == null);
            });
        }
    }

    @Test
    public void testS08PointSheet_readOnlyWhenFinalized() {
        NetFieldDatabase db = NetFieldDatabase.get(ApplicationProvider.getApplicationContext());
        SurveySessionRow session = db.netFieldDao().getSurveySession(testSessionId);
        session.state = "FINALIZED";
        db.netFieldDao().updateSurveySession(session);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PointDetailSheetFragment sheet = new PointDetailSheetFragment();
                Bundle args = new Bundle();
                args.putString("pointId", testPointId2);
                sheet.setArguments(args);
                sheet.show(activity.getSupportFragmentManager(), "point_sheet_final");

                activity.getSupportFragmentManager().executePendingTransactions();
                ShadowLooper.idleMainLooper();

                MaterialButton btnPointDelete = sheet.getView().findViewById(R.id.btn_point_delete);
                assertEquals(View.GONE, btnPointDelete.getVisibility());
            });
        }
    }

    @Test
    public void testS07BatteryBanner_transitionsAndConfirmDialog() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavController nav = activity.getNavController();
                Bundle args = new Bundle();
                args.putString("sessionId", testSessionId);
                nav.navigate(R.id.nav_s07, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();
                ShadowLooper.idleMainLooper();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                View root = current.getView();
                assertNotNull(root);

                View bannerBattery = root.findViewById(R.id.banner_battery);
                TextView textBatteryTitle = root.findViewById(R.id.text_battery_title);
                MaterialButton btnBatteryEnd = root.findViewById(R.id.btn_battery_end);
                assertNotNull(bannerBattery);
                assertNotNull(textBatteryTitle);
                assertNotNull(btnBatteryEnd);

                // Initial state: banner is gone
                assertEquals(View.GONE, bannerBattery.getVisibility());

                // With setForTest(WARN, 18) the banner is visible and title contains "Battery low" and "18%"
                NetFieldApp.batteryAlerts().setForTest(com.netfieldpro.core.power.BatteryAdvisor.Level.WARN, 18);
                ShadowLooper.idleMainLooper();

                assertEquals(View.VISIBLE, bannerBattery.getVisibility());
                String warnTitle = textBatteryTitle.getText().toString();
                assertTrue("Title should contain 'Battery low', was: " + warnTitle, warnTitle.contains("Battery low"));
                assertTrue("Title should contain '18%', was: " + warnTitle, warnTitle.contains("18%"));

                // With CRITICAL it contains "Battery critical"
                NetFieldApp.batteryAlerts().setForTest(com.netfieldpro.core.power.BatteryAdvisor.Level.CRITICAL, 9);
                ShadowLooper.idleMainLooper();

                assertEquals(View.VISIBLE, bannerBattery.getVisibility());
                String critTitle = textBatteryTitle.getText().toString();
                assertTrue("Title should contain 'Battery critical', was: " + critTitle, critTitle.contains("Battery critical"));

                // With NONE the banner is gone
                NetFieldApp.batteryAlerts().setForTest(com.netfieldpro.core.power.BatteryAdvisor.Level.NONE, 25);
                ShadowLooper.idleMainLooper();

                assertEquals(View.GONE, bannerBattery.getVisibility());

                // Set back to WARN and test clicking btn_battery_end shows the confirm dialog
                NetFieldApp.batteryAlerts().setForTest(com.netfieldpro.core.power.BatteryAdvisor.Level.WARN, 18);
                ShadowLooper.idleMainLooper();
                assertEquals(View.VISIBLE, bannerBattery.getVisibility());

                btnBatteryEnd.performClick();
                ShadowLooper.idleMainLooper();

                AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
                assertNotNull("Clicking btn_battery_end must show confirm dialog", dialog);
                assertTrue(dialog.isShowing());

                // Test airplane banner
                TextView bannerAirplane = root.findViewById(R.id.banner_airplane);
                assertNotNull("banner_airplane must exist", bannerAirplane);
                assertEquals(View.GONE, bannerAirplane.getVisibility());

                NetFieldApp.batteryAlerts().setAirplaneForTest(true);
                ShadowLooper.idleMainLooper();
                assertEquals(View.VISIBLE, bannerAirplane.getVisibility());
                assertEquals(activity.getString(R.string.banner_airplane), bannerAirplane.getText().toString());

                NetFieldApp.batteryAlerts().setAirplaneForTest(false);
                ShadowLooper.idleMainLooper();
                assertEquals(View.GONE, bannerAirplane.getVisibility());
            });
        }
    }
}
