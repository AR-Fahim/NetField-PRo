package com.netfieldpro.ui.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.QuotaDecision;
import com.netfieldpro.core.entitlement.Resource;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.session.Sm1State;
import com.netfieldpro.core.time.FakeClock;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.core.wifi.EnvironmentAdvisor;
import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.wifi.WifiObservation;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.entity.DeviceInfoRow;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.data.repository.QuotaGuard;
import com.netfieldpro.session.SessionRuntimeStore;
import com.netfieldpro.wifi.WifiEnvironmentProbe;
import android.widget.Spinner;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class NewSurveyFragmentTest {

    private FakeWifiEnvironmentProbe fakeProbe;
    private String testProjectId;
    private String testFloorId;

    static class FakeWifiEnvironmentProbe extends WifiEnvironmentProbe {
        boolean perm = true;
        boolean services = true;
        boolean wifi = true;
        EnvironmentAdvisor.ThrottleState throttle = EnvironmentAdvisor.ThrottleState.OFF;

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
    }

    @Before
    public void setUp() {
        NetFieldApp.onboardingStore().markDone();
        fakeProbe = new FakeWifiEnvironmentProbe(ApplicationProvider.getApplicationContext());
        NetFieldApp.setWifiEnvironmentProbeForTest(fakeProbe);

        testProjectId = IdGenerator.generate();
        testFloorId = IdGenerator.generate();

        NetFieldDatabase db = NetFieldDatabase.get(ApplicationProvider.getApplicationContext());
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
    }

    @After
    public void tearDown() {
        NetFieldApp.setWifiEnvironmentProbeForTest(null);
        if (NetFieldApp.entitlements() != null) {
            NetFieldApp.entitlements().setDebugTier(null);
        }
        NetFieldApp.setQuotaGuardForTest(null);

        NetFieldDatabase db = NetFieldDatabase.get(ApplicationProvider.getApplicationContext());
        if (testFloorId != null) {
            db.netFieldDao().deleteFloor(testFloorId);
        }
        if (testProjectId != null) {
            db.netFieldDao().deleteProject(testProjectId);
        }
    }

    @Test
    public void testS06Smoke_launchesAndNextDisabledWhenPermissionDenied() {
        fakeProbe.perm = false;
        fakeProbe.services = true;
        fakeProbe.wifi = true;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s06, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                assertTrue(current instanceof NewSurveyFragment);

                View root = current.getView();
                assertNotNull(root);

                TextView screenTitle = root.findViewById(R.id.text_screen_title);
                assertNotNull(screenTitle);
                assertEquals(activity.getString(R.string.screen_s06_title), screenTitle.getText().toString());

                TextView stepIndicator = root.findViewById(R.id.text_step_indicator);
                assertNotNull(stepIndicator);
                assertEquals(activity.getString(R.string.s06_step1_title), stepIndicator.getText().toString());

                MaterialButton btnNext = root.findViewById(R.id.btn_next);
                assertNotNull(btnNext);
                assertFalse("Next button should be disabled when permission denied", btnNext.isEnabled());

                MaterialCardView cardIssue = root.findViewById(R.id.card_env_issue);
                assertNotNull(cardIssue);
                assertEquals(View.VISIBLE, cardIssue.getVisibility());

                TextView textIssue = root.findViewById(R.id.text_env_issue);
                assertNotNull(textIssue);
                assertEquals(activity.getString(R.string.diag_issue_permission), textIssue.getText().toString());
            });
        }
    }

    @Test
    public void testS06_nextEnabledWhenAllEnvironmentReady() {
        fakeProbe.perm = true;
        fakeProbe.services = true;
        fakeProbe.wifi = true;
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.OFF;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s06, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                View root = current.getView();
                assertNotNull(root);

                MaterialButton btnNext = root.findViewById(R.id.btn_next);
                assertNotNull(btnNext);
                assertTrue("Next button should be enabled when ready", btnNext.isEnabled());

                MaterialCardView cardIssue = root.findViewById(R.id.card_env_issue);
                assertEquals(View.GONE, cardIssue.getVisibility());

                // Perform click Next -> moves to Step 2
                btnNext.performClick();

                View layoutStep1 = root.findViewById(R.id.layout_step1);
                View layoutStep2 = root.findViewById(R.id.layout_step2);
                assertEquals(View.GONE, layoutStep1.getVisibility());
                assertEquals(View.VISIBLE, layoutStep2.getVisibility());

                TextView stepIndicator = root.findViewById(R.id.text_step_indicator);
                assertEquals(activity.getString(R.string.s06_step2_title), stepIndicator.getText().toString());
            });
        }
    }

    @Test
    public void testS06_throttlingOn_showsWarningAndDevOptions() {
        fakeProbe.perm = true;
        fakeProbe.services = true;
        fakeProbe.wifi = true;
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.ON;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s06, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                View root = current.getView();
                assertNotNull(root);

                MaterialButton btnDev = root.findViewById(R.id.btn_dev_options);
                assertNotNull(btnDev);
                assertEquals(View.VISIBLE, btnDev.getVisibility());

                MaterialButton btnNext = root.findViewById(R.id.btn_next);
                assertTrue("Next should still be enabled even if throttling is on", btnNext.isEnabled());
            });
        }
    }

    @Test
    public void testS06_step2_warmupFailureShowsRetryAndContinueIndicative() {
        fakeProbe.perm = true;
        fakeProbe.services = true;
        fakeProbe.wifi = true;
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.OFF;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s06, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                NewSurveyFragment fragment = (NewSurveyFragment) navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(fragment);
                View root = fragment.getView();
                assertNotNull(root);

                MaterialButton btnNext = root.findViewById(R.id.btn_next);
                btnNext.performClick();

                WarmupCoordinator coordinator = fragment.getWarmupCoordinator();
                assertNotNull(coordinator);

                // Step 2 Next should be disabled initially
                MaterialButton btnStep2Next = root.findViewById(R.id.btn_step2_next);
                assertFalse(btnStep2Next.isEnabled());

                // Trigger failure
                coordinator.triggerFailureForTest();
                coordinator.continueIndicative();

                // Indicative selected -> if SSID selected, next enables
                coordinator.toggleSsid("test_ssid");
                assertTrue(coordinator.isIndicative());
            });
        }
    }

    @Test
    public void testS06_step3_defaultNameAndProfilePicker() {
        fakeProbe.perm = true;
        fakeProbe.services = true;
        fakeProbe.wifi = true;
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.OFF;
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s06, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                NewSurveyFragment fragment = (NewSurveyFragment) navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(fragment);
                View root = fragment.getView();
                assertNotNull(root);

                FakeClock fakeClock = new FakeClock(1_000_000_000L, 1_700_000_000_000L);
                fragment.setClockForTest(fakeClock);

                // Step 1 -> Next
                MaterialButton btnNext = root.findViewById(R.id.btn_next);
                btnNext.performClick();

                // Step 2
                WarmupCoordinator coordinator = fragment.getWarmupCoordinator();
                coordinator.triggerFailureForTest();
                coordinator.continueIndicative();
                coordinator.toggleSsid("OfficeNet");

                MaterialButton btnStep2Next = root.findViewById(R.id.btn_step2_next);
                assertTrue(btnStep2Next.isEnabled());
                btnStep2Next.performClick();

                // Step 3 layout visible
                View layoutStep2 = root.findViewById(R.id.layout_step2);
                View layoutStep3 = root.findViewById(R.id.layout_step3);
                assertEquals(View.GONE, layoutStep2.getVisibility());
                assertEquals(View.VISIBLE, layoutStep3.getVisibility());

                TextView stepIndicator = root.findViewById(R.id.text_step_indicator);
                assertEquals(activity.getString(R.string.s06_step3_title), stepIndicator.getText().toString());

                // Check default name format
                TextInputEditText editName = root.findViewById(R.id.edit_survey_name);
                String name = editName.getText() != null ? editName.getText().toString() : "";
                assertTrue("Survey name should start with 'Survey '", name.startsWith("Survey "));
                assertTrue("Survey name matches format", name.matches("^Survey \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$"));

                // Profile picker has 4 items and shows lock icon for Free tier
                Spinner spinner = root.findViewById(R.id.spinner_profile);
                assertEquals(4, spinner.getAdapter().getCount());
                String item0 = (String) spinner.getAdapter().getItem(0);
                assertTrue("Free tier should show lock on preset profile", item0.contains("🔒"));
            });
        }
    }

    @Test
    public void testS06_step3_nameValidation() {
        fakeProbe.perm = true;
        fakeProbe.services = true;
        fakeProbe.wifi = true;
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.OFF;
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s06, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                NewSurveyFragment fragment = (NewSurveyFragment) navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(fragment);
                View root = fragment.getView();
                assertNotNull(root);

                // Step 1 -> Next
                MaterialButton btnNext = root.findViewById(R.id.btn_next);
                btnNext.performClick();

                // Step 2
                WarmupCoordinator coordinator = fragment.getWarmupCoordinator();
                coordinator.triggerFailureForTest();
                coordinator.continueIndicative();
                coordinator.toggleSsid("OfficeNet");

                MaterialButton btnStep2Next = root.findViewById(R.id.btn_step2_next);
                btnStep2Next.performClick();

                TextInputEditText editName = root.findViewById(R.id.edit_survey_name);
                MaterialButton btnStartSurvey = root.findViewById(R.id.btn_start_survey);

                // Empty name
                editName.setText("");
                btnStartSurvey.performClick();
                assertNotNull("Empty name should set error", editName.getError());
                assertEquals(activity.getString(R.string.s06_error_empty_name), editName.getError().toString());

                // Name too long (>60 code points)
                String longName = "a".repeat(61);
                editName.setText(longName);
                btnStartSurvey.performClick();
                assertNotNull("Long name should set error", editName.getError());
                assertEquals(activity.getString(R.string.s06_error_long_name), editName.getError().toString());
            });
        }
    }

    @Test
    public void testS06_step3_lockedProfile_navigatesToPaywall() {
        fakeProbe.perm = true;
        fakeProbe.services = true;
        fakeProbe.wifi = true;
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.OFF;
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s06, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                NewSurveyFragment fragment = (NewSurveyFragment) navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(fragment);
                View root = fragment.getView();
                assertNotNull(root);

                // Step 1 -> Next
                MaterialButton btnNext = root.findViewById(R.id.btn_next);
                btnNext.performClick();

                // Step 2
                WarmupCoordinator coordinator = fragment.getWarmupCoordinator();
                coordinator.triggerFailureForTest();
                coordinator.continueIndicative();
                coordinator.toggleSsid("OfficeNet");

                MaterialButton btnStep2Next = root.findViewById(R.id.btn_step2_next);
                btnStep2Next.performClick();

                Spinner spinner = root.findViewById(R.id.spinner_profile);
                assertNotNull(spinner.getOnItemSelectedListener());
                // Position 1 is WAREHOUSE_ROBOTICS which is locked on FREE tier
                spinner.getOnItemSelectedListener().onItemSelected(spinner, null, 1, 1);

                navHost.getChildFragmentManager().executePendingTransactions();
                assertNotNull(activity.getNavController().getCurrentDestination());
                assertEquals(R.id.nav_s18, activity.getNavController().getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testS06_step3_quotaDenied_navigatesToPaywall() {
        fakeProbe.perm = true;
        fakeProbe.services = true;
        fakeProbe.wifi = true;
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.OFF;
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);

        QuotaGuard denyingGuard = new QuotaGuard(
                NetFieldDatabase.get(ApplicationProvider.getApplicationContext()).netFieldDao(),
                () -> Tier.FREE
        ) {
            @Override
            public QuotaDecision check(Resource resource) {
                if (resource == Resource.SESSION) {
                    return new QuotaDecision(false, 2, Tier.PRO);
                }
                return super.check(resource);
            }
        };
        NetFieldApp.setQuotaGuardForTest(denyingGuard);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s06, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                NewSurveyFragment fragment = (NewSurveyFragment) navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(fragment);
                View root = fragment.getView();
                assertNotNull(root);

                // Step 1 -> Next
                MaterialButton btnNext = root.findViewById(R.id.btn_next);
                btnNext.performClick();

                // Step 2
                WarmupCoordinator coordinator = fragment.getWarmupCoordinator();
                coordinator.triggerFailureForTest();
                coordinator.continueIndicative();
                coordinator.toggleSsid("OfficeNet");

                MaterialButton btnStep2Next = root.findViewById(R.id.btn_step2_next);
                btnStep2Next.performClick();

                MaterialButton btnStartSurvey = root.findViewById(R.id.btn_start_survey);
                btnStartSurvey.performClick();

                navHost.getChildFragmentManager().executePendingTransactions();
                assertNotNull(activity.getNavController().getCurrentDestination());
                assertEquals(R.id.nav_s18, activity.getNavController().getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testS06_step3_startSurvey_createsBatchAndNavigatesToS07() {
        fakeProbe.perm = true;
        fakeProbe.services = true;
        fakeProbe.wifi = true;
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.OFF;
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
        NetFieldApp.setQuotaGuardForTest(null);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s06, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                NewSurveyFragment fragment = (NewSurveyFragment) navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(fragment);
                View root = fragment.getView();
                assertNotNull(root);

                FakeClock fakeClock = new FakeClock(5_000_000_000L, 1_700_000_000_000L);
                fragment.setClockForTest(fakeClock);

                // Step 1 -> Next
                MaterialButton btnNext = root.findViewById(R.id.btn_next);
                btnNext.performClick();

                // Step 2: Feed 3 scan batches to complete warmup normally
                WarmupCoordinator coordinator = fragment.getWarmupCoordinator();
                for (int i = 0; i < 3; i++) {
                    List<WifiObservation> obs = List.of(
                            new WifiObservation("aa:bb:cc:dd:ee:01", "CorpNet", 2412, -55, fakeClock.monoNs()),
                            new WifiObservation("aa:bb:cc:dd:ee:02", "CorpNet", 5180, -60, fakeClock.monoNs())
                    );
                    ScanBatch batch = new ScanBatch(obs, fakeClock.monoNs(), com.netfieldpro.core.wifi.Tier.CALLBACK, true, false, 0);
                    coordinator.handleBatch(batch);
                }
                assertTrue(coordinator.isCompleted());
                coordinator.toggleSsid("CorpNet");

                MaterialButton btnStep2Next = root.findViewById(R.id.btn_step2_next);
                assertTrue(btnStep2Next.isEnabled());
                btnStep2Next.performClick();

                // In Step 3
                TextInputEditText editName = root.findViewById(R.id.edit_survey_name);
                editName.setText("Floor 42 Verification Survey");

                MaterialButton btnStartSurvey = root.findViewById(R.id.btn_start_survey);
                btnStartSurvey.performClick();

                // Should navigate to S07
                navHost.getChildFragmentManager().executePendingTransactions();
                assertNotNull(activity.getNavController().getCurrentDestination());
                assertEquals(R.id.nav_s07, activity.getNavController().getCurrentDestination().getId());

                // Check session executor current session id
                assertNotNull(NetFieldApp.sessionExecutor());
                String createdSessionId = NetFieldApp.sessionExecutor().getCurrentSessionId();
                assertNotNull(createdSessionId);

                // Verify DB batch creation
                NetFieldDatabase db = NetFieldDatabase.get(activity);
                SurveySessionRow sessionRow = db.netFieldDao().getSurveySession(createdSessionId);
                assertNotNull(sessionRow);
                assertEquals(testFloorId, sessionRow.floorId);
                assertEquals("Floor 42 Verification Survey", sessionRow.name);
                assertEquals("DRAFT", sessionRow.state);
                assertEquals("VERDICT", sessionRow.evidenceGrade);
                assertEquals(false, sessionRow.throttleAtStart);
                assertEquals(3000L, sessionRow.linkCadenceMs);
                assertEquals("DEFAULT", sessionRow.cadenceFlag);
                assertEquals("builtin-wifi", sessionRow.receiverId);
                assertEquals(0, sessionRow.intendedProfileChanges);
                assertEquals(0.0, sessionRow.rssiOffsetDb, 0.001);

                // DeviceInfoRow created
                assertNotNull(sessionRow.deviceInfoId);
                DeviceInfoRow deviceInfo = db.netFieldDao().getDeviceInfo(sessionRow.deviceInfoId);
                assertNotNull(deviceInfo);

                // TargetNetworkRow created
                List<TargetNetworkRow> targetNetworks = db.netFieldDao().getTargetNetworksForSession(createdSessionId);
                assertFalse(targetNetworks.isEmpty());
                assertEquals("CorpNet", targetNetworks.get(0).ssid);

                // Runtime store set to READY
                SessionRuntimeStore store = SessionRuntimeStore.create(activity);
                assertEquals(Sm1State.READY, store.getState(createdSessionId));
            });
        }
    }

    @Test
    public void testS06_step3_indicativeGrade_whenContinueIndicativeUsed() {
        fakeProbe.perm = true;
        fakeProbe.services = true;
        fakeProbe.wifi = true;
        fakeProbe.throttle = EnvironmentAdvisor.ThrottleState.OFF;
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
        NetFieldApp.setQuotaGuardForTest(null);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s06, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                NewSurveyFragment fragment = (NewSurveyFragment) navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(fragment);
                View root = fragment.getView();
                assertNotNull(root);

                // Step 1 -> Next
                MaterialButton btnNext = root.findViewById(R.id.btn_next);
                btnNext.performClick();

                // Step 2: failure + continue indicative
                WarmupCoordinator coordinator = fragment.getWarmupCoordinator();
                coordinator.triggerFailureForTest();
                coordinator.continueIndicative();
                coordinator.toggleSsid("IndicativeNet");

                MaterialButton btnStep2Next = root.findViewById(R.id.btn_step2_next);
                btnStep2Next.performClick();

                // Indicative warning is visible in Step 3
                TextView textIndicative = root.findViewById(R.id.text_indicative_warning);
                assertEquals(View.VISIBLE, textIndicative.getVisibility());

                MaterialButton btnStartSurvey = root.findViewById(R.id.btn_start_survey);
                btnStartSurvey.performClick();

                String createdSessionId = NetFieldApp.sessionExecutor().getCurrentSessionId();
                assertNotNull(createdSessionId);

                NetFieldDatabase db = NetFieldDatabase.get(activity);
                SurveySessionRow sessionRow = db.netFieldDao().getSurveySession(createdSessionId);
                assertNotNull(sessionRow);
                assertEquals("INDICATIVE", sessionRow.evidenceGrade);
            });
        }
    }
}
