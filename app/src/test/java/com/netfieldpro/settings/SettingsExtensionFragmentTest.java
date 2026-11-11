package com.netfieldpro.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import com.netfieldpro.MainActivity;
import com.netfieldpro.R;
import com.netfieldpro.ui.NavArgs;
import com.netfieldpro.ui.settings.SettingsExtensionRegistry;
import com.netfieldpro.ui.settings.SettingsRow;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import com.netfieldpro.NoAuthRule;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingsExtensionFragmentTest {

    @Rule
    public NoAuthRule noAuthRule = new NoAuthRule();

    @Before
    public void setUp() {
        SettingsExtensionRegistry.getInstance().clear();
    }

    @After
    public void tearDown() {
        SettingsExtensionRegistry.getInstance().clear();
    }

    @Test
    public void testExtensionsHiddenWhenEmpty() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString(NavArgs.SCREEN_ID, "S19");
                activity.getNavController().navigate(R.id.nav_s19, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                if (navHost != null) {
                    navHost.getChildFragmentManager().executePendingTransactions();
                }
                ShadowLooper.idleMainLooper();

                View extLayout = activity.findViewById(R.id.layout_extensions);
                assertNotNull(extLayout);
                assertEquals(View.GONE, extLayout.getVisibility());
            });
        }
    }

    @Test
    public void testExtensionsVisibleWithRegisteredRow() {
        SettingsExtensionRegistry.getInstance().register(
                new SettingsRow("custom_tool", "Custom Hardware Probe", "Probe active antenna", null, true)
        );

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString(NavArgs.SCREEN_ID, "S19");
                activity.getNavController().navigate(R.id.nav_s19, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                if (navHost != null) {
                    navHost.getChildFragmentManager().executePendingTransactions();
                }
                ShadowLooper.idleMainLooper();

                View extLayout = activity.findViewById(R.id.layout_extensions);
                assertNotNull(extLayout);
                assertEquals(View.VISIBLE, extLayout.getVisibility());

                ViewGroup rowsContainer = activity.findViewById(R.id.layout_extension_rows);
                assertNotNull(rowsContainer);

                List<String> textFound = new ArrayList<>();
                collectText(rowsContainer, textFound);
                assertTrue("Expected Custom Hardware Probe but found: " + textFound,
                        textFound.contains("Custom Hardware Probe"));
            });
        }
    }

    private void collectText(View view, List<String> list) {
        if (view instanceof TextView) {
            list.add(((TextView) view).getText().toString());
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                collectText(vg.getChildAt(i), list);
            }
        }
    }
}
