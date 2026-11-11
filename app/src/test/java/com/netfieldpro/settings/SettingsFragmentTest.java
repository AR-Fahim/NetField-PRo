package com.netfieldpro.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;

import com.netfieldpro.BuildConfig;
import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.settings.HeatPalette;
import com.netfieldpro.core.settings.ThemeMode;
import com.netfieldpro.ui.TierBadgeView;
import com.netfieldpro.ui.settings.SettingsExtensionRegistry;
import com.netfieldpro.ui.settings.SettingsRow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
public class SettingsFragmentTest {

    @Before
    public void setUp() {
        SettingsExtensionRegistry.getInstance().clear();
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
    }

    @After
    public void tearDown() {
        SettingsExtensionRegistry.getInstance().clear();
    }

    private void navigateToS19(MainActivity activity) {
        activity.getNavController().navigate(R.id.nav_s19);
        NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            navHost.getChildFragmentManager().executePendingTransactions();
        }
    }

    @Test
    public void testST19_1_planRowReflectsTierAndClickLandsOnS18() {
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS19(activity);

                TierBadgeView badge = activity.findViewById(R.id.tier_badge_view);
                assertNotNull(badge);
                assertEquals(Tier.PRO, badge.getTier());
                assertEquals("Pro", badge.getText().toString());

                View rowManagePlan = activity.findViewById(R.id.row_manage_plan);
                assertNotNull(rowManagePlan);
                rowManagePlan.performClick();

                NavController nc = activity.getNavController();
                assertEquals("Should navigate to S18", R.id.nav_s18, nc.getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testST19_2_themeSelectionPersistsAndUpdatesNightMode() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS19(activity);
                AppSettings settings = AppSettings.get(activity);

                RadioButton rbLight = activity.findViewById(R.id.rb_theme_light);
                RadioButton rbDark = activity.findViewById(R.id.rb_theme_dark);
                RadioButton rbSystem = activity.findViewById(R.id.rb_theme_system);

                // Select Light
                rbLight.performClick();
                assertEquals(ThemeMode.LIGHT, settings.getTheme());
                assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode());

                // Select Dark
                rbDark.performClick();
                assertEquals(ThemeMode.DARK, settings.getTheme());
                assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode());

                // Select System
                rbSystem.performClick();
                assertEquals(ThemeMode.SYSTEM, settings.getTheme());
                assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.getDefaultNightMode());
            });
        }
    }

    @Test
    public void testST19_3_paletteSelectionPersists() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS19(activity);
                AppSettings settings = AppSettings.get(activity);

                RadioButton rbViridis = activity.findViewById(R.id.rb_palette_viridis);
                RadioButton rbTraffic = activity.findViewById(R.id.rb_palette_traffic);

                // Select Viridis
                rbViridis.performClick();
                assertEquals(HeatPalette.VIRIDIS, settings.getHeatPalette());

                // Select Traffic light
                rbTraffic.performClick();
                assertEquals(HeatPalette.TRAFFIC_LIGHT, settings.getHeatPalette());
            });
        }
    }

    @Test
    public void testST19_4_diagnosticsNavigatesToS20() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS19(activity);

                View rowDiag = activity.findViewById(R.id.row_diagnostics);
                assertNotNull(rowDiag);
                rowDiag.performClick();

                assertEquals(R.id.nav_s20, activity.getNavController().getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testST19_5_customProfilesFreeTierShowsLockAndNavigatesToS18() {
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS19(activity);

                TextView lockBadge = activity.findViewById(R.id.badge_custom_profiles_lock);
                assertNotNull(lockBadge);
                assertEquals(View.VISIBLE, lockBadge.getVisibility());
                assertEquals("🔒 Pro", lockBadge.getText().toString());

                View rowCustomProfiles = activity.findViewById(R.id.row_custom_profiles);
                assertNotNull(rowCustomProfiles);
                rowCustomProfiles.performClick();

                NavController nc = activity.getNavController();
                assertEquals("Locked feature should navigate to S18 paywall",
                        R.id.nav_s18, nc.getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testST19_6_customProfilesProTierHidesLockAndNavigatesToS21() {
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS19(activity);

                TextView lockBadge = activity.findViewById(R.id.badge_custom_profiles_lock);
                assertNotNull(lockBadge);
                assertEquals(View.GONE, lockBadge.getVisibility());

                View rowCustomProfiles = activity.findViewById(R.id.row_custom_profiles);
                assertNotNull(rowCustomProfiles);
                rowCustomProfiles.performClick();

                NavController nc = activity.getNavController();
                assertEquals("Allowed feature should navigate to S21",
                        R.id.nav_s21, nc.getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testST19_7_versionTextHasDebugInDebugBuilds() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS19(activity);

                TextView textVersion = activity.findViewById(R.id.text_version);
                assertNotNull(textVersion);
                String ver = textVersion.getText().toString();
                assertTrue(ver.startsWith(BuildConfig.VERSION_NAME));
                if (BuildConfig.DEBUG) {
                    assertTrue("Debug build must have (DEBUG)", ver.contains("(DEBUG)"));
                } else {
                    assertFalse("Release build must not have (DEBUG)", ver.contains("(DEBUG)"));
                }
            });
        }
    }

    @Test
    public void testST19_8_dynamicExtensionsRenderBelowAbout() {
        AtomicBoolean clicked = new AtomicBoolean(false);
        SettingsExtensionRegistry.getInstance().register(
                new SettingsRow("ext_1", "External Plugin", "Plugin description", () -> clicked.set(true), true)
        );

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS19(activity);

                View layoutExt = activity.findViewById(R.id.layout_extensions);
                assertNotNull(layoutExt);
                assertEquals(View.VISIBLE, layoutExt.getVisibility());

                TextView rowTitle = activity.findViewById(R.id.layout_extension_rows).findViewWithTag(null);
                // Find view with text "External Plugin"
                boolean found = false;
                android.view.ViewGroup group = activity.findViewById(R.id.layout_extension_rows);
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (child instanceof android.view.ViewGroup) {
                        android.view.ViewGroup rowVg = (android.view.ViewGroup) child;
                        for (int j = 0; j < rowVg.getChildCount(); j++) {
                            View sub = rowVg.getChildAt(j);
                            if (sub instanceof TextView && "External Plugin".equals(((TextView) sub).getText().toString())) {
                                found = true;
                                child.performClick();
                                break;
                            }
                        }
                    }
                }
                assertTrue("Extension row should be rendered", found);
                assertTrue("Extension row click should execute action", clicked.get());
            });
        }
    }
}

