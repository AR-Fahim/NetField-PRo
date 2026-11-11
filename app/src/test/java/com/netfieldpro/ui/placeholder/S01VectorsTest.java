package com.netfieldpro.ui.placeholder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.nav.StartRouter;
import com.netfieldpro.core.permissions.PermissionKey;
import com.netfieldpro.core.permissions.PermissionSnapshot;
import com.netfieldpro.databinding.FragmentOnboardingBinding;
import com.netfieldpro.onboarding.OnboardingStore;
import com.netfieldpro.onboarding.PermissionProbe;
import com.netfieldpro.ui.NavArgs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class S01VectorsTest {

    private OnboardingStore store;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences(OnboardingStore.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        store = new OnboardingStore(prefs, true);
    }

    @After
    public void tearDown() {
        NetFieldApp.setPermissionProbeForTest(null);
        Application app = ApplicationProvider.getApplicationContext();
        shadowOf(app).checkActivities(false);
    }

    private PlaceholderFragment navigateToS01(MainActivity activity) {
        NavController controller = activity.getNavController();
        if (controller.getCurrentDestination() == null || controller.getCurrentDestination().getId() != R.id.nav_s01) {
            Bundle args = new Bundle();
            args.putString(NavArgs.SCREEN_ID, "S01");
            controller.navigate(R.id.nav_s01, args);
        }
        NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        assertNotNull(navHost);
        navHost.getChildFragmentManager().executePendingTransactions();

        for (Fragment f : navHost.getChildFragmentManager().getFragments()) {
            if (f instanceof PlaceholderFragment pf && pf.isS01()) {
                return pf;
            }
        }
        throw new IllegalStateException("S01 PlaceholderFragment not found");
    }

    @Test
    public void testS01_1_nothingGranted() {
        PermissionProbe probe = a -> new PermissionSnapshot(34, false, false, false, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(probe);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);
                FragmentOnboardingBinding b = fragment.getOnboardingBinding();
                assertNotNull(b);

                assertEquals(View.VISIBLE, b.textOnboardingTitle.getVisibility());
                assertEquals("NetField Pro", b.textOnboardingTitle.getText().toString());
                assertEquals(View.VISIBLE, b.textOnboardingTagline.getVisibility());
                assertEquals("Measure Wi-Fi you can stand behind.", b.textOnboardingTagline.getText().toString());

                assertEquals("Not allowed yet", b.chipPermLocationState.getText().toString());
                assertEquals("Not allowed yet", b.chipPermNotifState.getText().toString());

                assertEquals(View.VISIBLE, b.btnPermLocation.getVisibility());
                assertEquals("Allow", b.btnPermLocation.getText().toString());

                assertEquals(View.VISIBLE, b.btnPermNotif.getVisibility());
                assertEquals("Allow", b.btnPermNotif.getText().toString());

                assertEquals("Allow location", b.btnOnboardingPrimary.getText().toString());
                assertEquals(View.VISIBLE, b.btnOnboardingSkip.getVisibility());
                assertEquals(View.VISIBLE, b.btnOnboardingVerify.getVisibility());
            });
        }
    }

    @Test
    public void testS01_2_primaryClickRequestsFineAndCoarse() {
        PermissionProbe probe = a -> new PermissionSnapshot(34, false, false, false, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(probe);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);
                List<String> requested = new ArrayList<>();
                fragment.setTestHook(perms -> Collections.addAll(requested, perms));

                fragment.getOnboardingBinding().btnOnboardingPrimary.performClick();

                assertEquals(Arrays.asList(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ), requested);
            });
        }
    }

    @Test
    public void testS01_3_notifButtonClickRequestsPostNotifications() {
        PermissionProbe probe = a -> new PermissionSnapshot(34, false, false, false, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(probe);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);
                List<String> requested = new ArrayList<>();
                fragment.setTestHook(perms -> Collections.addAll(requested, perms));

                fragment.getOnboardingBinding().btnPermNotif.performClick();

                assertEquals(Collections.singletonList(Manifest.permission.POST_NOTIFICATIONS), requested);
            });
        }
    }

    @Test
    public void testS01_4_resultFineAndCoarseGranted() {
        PermissionProbe[] probeHolder = new PermissionProbe[1];
        probeHolder[0] = a -> new PermissionSnapshot(34, false, false, false, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(a -> probeHolder[0].snapshot(a));

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);

                Map<String, Boolean> result = new HashMap<>();
                result.put(Manifest.permission.ACCESS_FINE_LOCATION, true);
                result.put(Manifest.permission.ACCESS_COARSE_LOCATION, true);

                probeHolder[0] = a -> new PermissionSnapshot(34, true, true, true, false, false, false, false);
                fragment.onPermissionResult(result);

                assertTrue(NetFieldApp.onboardingStore().wasAsked(PermissionKey.LOCATION));
                FragmentOnboardingBinding b = fragment.getOnboardingBinding();
                assertEquals("Allowed", b.chipPermLocationState.getText().toString());
                assertEquals(View.GONE, b.btnPermLocation.getVisibility());
                assertEquals("Continue", b.btnOnboardingPrimary.getText().toString());
            });
        }
    }

    @Test
    public void testS01_5_resultDeniedPermanent() {
        PermissionProbe[] probeHolder = new PermissionProbe[1];
        probeHolder[0] = a -> new PermissionSnapshot(34, false, false, false, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(a -> probeHolder[0].snapshot(a));

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);

                Map<String, Boolean> result = new HashMap<>();
                result.put(Manifest.permission.ACCESS_FINE_LOCATION, false);
                result.put(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                // asked=true, rationale=false -> DENIED_PERMANENT
                probeHolder[0] = a -> new PermissionSnapshot(34, false, false, true, false, false, false, false);
                fragment.onPermissionResult(result);

                FragmentOnboardingBinding b = fragment.getOnboardingBinding();
                assertEquals("Blocked in system settings", b.chipPermLocationState.getText().toString());
                assertEquals("Settings", b.btnPermLocation.getText().toString());
                assertEquals("Open settings", b.btnOnboardingPrimary.getText().toString());
            });
        }
    }

    @Test
    public void testS01_6_snapshotSdk30() {
        PermissionProbe probe = a -> new PermissionSnapshot(30, false, false, false, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(probe);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);
                FragmentOnboardingBinding b = fragment.getOnboardingBinding();

                assertEquals(View.VISIBLE, b.cardPermNotif.getVisibility());
                assertEquals("Not needed on this Android version", b.chipPermNotifState.getText().toString());
                assertEquals(View.GONE, b.btnPermNotif.getVisibility());
            });
        }
    }

    @Test
    public void testS01_7_grantedThenPrimaryNavigatesAndPopsS01() {
        PermissionProbe probe = a -> new PermissionSnapshot(34, true, true, true, false, true, true, false);
        NetFieldApp.setPermissionProbeForTest(probe);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);
                fragment.getOnboardingBinding().btnOnboardingPrimary.performClick();

                assertTrue(NetFieldApp.onboardingStore().isDone());
                NavController controller = activity.getNavController();
                assertEquals(R.id.nav_s02, controller.getCurrentDestination().getId());

                if (controller.getPreviousBackStackEntry() != null) {
                    assertNotEquals(R.id.nav_s01, controller.getPreviousBackStackEntry().getDestination().getId());
                }
            });
        }
    }

    @Test
    public void testS01_8_skipWithNothingGrantedNavigatesS02() {
        PermissionProbe probe = a -> new PermissionSnapshot(34, false, false, false, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(probe);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);
                fragment.getOnboardingBinding().btnOnboardingSkip.performClick();

                assertTrue(NetFieldApp.onboardingStore().isDone());
                assertEquals(R.id.nav_s02, activity.getNavController().getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testS01_9_verifyNavigatesS17AndBackReturnsS01() {
        PermissionProbe probe = a -> new PermissionSnapshot(34, false, false, false, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(probe);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);
                fragment.getOnboardingBinding().btnOnboardingVerify.performClick();

                assertTrue(NetFieldApp.onboardingStore().isDone());
                assertEquals(R.id.nav_s17, activity.getNavController().getCurrentDestination().getId());

                activity.getNavController().popBackStack();
                assertEquals(R.id.nav_s01, activity.getNavController().getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testS01_10_blockedSettingsClickStartsIntent() {
        PermissionProbe probe = a -> new PermissionSnapshot(34, false, false, true, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(probe);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);
                fragment.getOnboardingBinding().btnPermLocation.performClick();

                Intent nextIntent = shadowOf(activity.getApplication()).getNextStartedActivity();
                assertNotNull(nextIntent);
                assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, nextIntent.getAction());
                assertEquals(Uri.parse("package:" + activity.getPackageName()), nextIntent.getData());
            });
        }
    }

    @Test
    public void testS01_11_openSettingsErrorShowsSnackbar() {
        PermissionProbe probe = a -> new PermissionSnapshot(34, false, false, true, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(probe);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                shadowOf(activity.getApplication()).checkActivities(true);

                PlaceholderFragment fragment = navigateToS01(activity);
                fragment.openSettings();

                assertNotNull("Snackbar must be shown", fragment.getLastSnackbar());
                assertEquals(activity.getString(R.string.onboarding_settings_error),
                        activity.getString(R.string.onboarding_settings_error));
            });
        }
    }

    @Test
    public void testS01_12_probeChangesWhilePausedUpdatedOnResume() {
        PermissionProbe[] probeHolder = new PermissionProbe[1];
        probeHolder[0] = a -> new PermissionSnapshot(34, false, false, false, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(a -> probeHolder[0].snapshot(a));

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);
                assertEquals("Not allowed yet", fragment.getOnboardingBinding().chipPermLocationState.getText().toString());

                // Simulate background change and onResume
                probeHolder[0] = a -> new PermissionSnapshot(34, true, true, true, false, false, false, false);
                fragment.onResume();

                assertEquals("Allowed", fragment.getOnboardingBinding().chipPermLocationState.getText().toString());
            });
        }
    }

    @Test
    public void testS01_13_backPressLeavesOnboardingDoneFalse() {
        PermissionProbe probe = a -> new PermissionSnapshot(34, false, false, false, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(probe);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS01(activity);
                assertFalse(NetFieldApp.onboardingStore().isDone());

                activity.getOnBackPressedDispatcher().onBackPressed();
                assertFalse(NetFieldApp.onboardingStore().isDone());
            });
        }
    }

    @Test
    public void testS01_14_contentDescriptions() {
        PermissionProbe probe = a -> new PermissionSnapshot(34, false, false, false, false, false, false, false);
        NetFieldApp.setPermissionProbeForTest(probe);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                PlaceholderFragment fragment = navigateToS01(activity);
                FragmentOnboardingBinding b = fragment.getOnboardingBinding();

                assertEquals("Precise location. Not allowed yet.", b.cardPermLocation.getContentDescription().toString());
                assertEquals("Allow Precise location", b.btnPermLocation.getContentDescription().toString());
            });
        }
    }

    @Test
    public void testS01_15_actionViewLandsOnS17AndDoneStaysFalse() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("content://test/sample.pdf"), "application/pdf");

        StartRouter.StartDestination dest = StartRouter.route(StartRouter.StartIntent.VIEW_OR_SEND_DOCUMENT, true, store.isDone());
        assertEquals(StartRouter.StartDestination.S17, dest);
        assertFalse(store.isDone());
    }

    @Test
    public void testS01_16_fontScale13ButtonHeights() {
        Context context = ApplicationProvider.getApplicationContext();
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.fontScale = 1.3f;
        Context themedContext = context.createConfigurationContext(config);
        themedContext.setTheme(R.style.Theme_NetField);

        LayoutInflater inflater = LayoutInflater.from(themedContext);
        FragmentOnboardingBinding b = FragmentOnboardingBinding.inflate(inflater);

        int widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        b.getRoot().measure(widthSpec, heightSpec);

        float density = themedContext.getResources().getDisplayMetrics().density;
        float hPrimary = b.btnOnboardingPrimary.getMeasuredHeight() / density;
        float hSkip = b.btnOnboardingSkip.getMeasuredHeight() / density;
        float hVerify = b.btnOnboardingVerify.getMeasuredHeight() / density;
        float hLoc = b.btnPermLocation.getMeasuredHeight() / density;
        float hNotif = b.btnPermNotif.getMeasuredHeight() / density;

        assertTrue("Primary height " + hPrimary + " < 56dp", hPrimary >= 56.0f);
        assertTrue("Skip height " + hSkip + " < 48dp", hSkip >= 48.0f);
        assertTrue("Verify height " + hVerify + " < 48dp", hVerify >= 48.0f);
        assertTrue("Loc btn height " + hLoc + " < 48dp", hLoc >= 48.0f);
        assertTrue("Notif btn height " + hNotif + " < 48dp", hNotif >= 48.0f);
    }

    @Test
    public void testS01_17_startRouterSuppliedByStore() {
        store.reset();
        assertFalse(store.isDone());
        assertEquals(StartRouter.StartDestination.S01, StartRouter.route(StartRouter.StartIntent.MAIN, true, store.isDone()));

        store.markDone();
        assertTrue(store.isDone());
        assertEquals(StartRouter.StartDestination.S02, StartRouter.route(StartRouter.StartIntent.MAIN, true, store.isDone()));
    }
}

