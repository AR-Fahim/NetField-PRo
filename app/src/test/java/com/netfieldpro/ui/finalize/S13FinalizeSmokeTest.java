package com.netfieldpro.ui.finalize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.button.MaterialButton;
import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.data.seed.DemoSeeder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class S13FinalizeSmokeTest {

    private String sessionId;

    @Before
    public void setUp() {
        NetFieldApp.onboardingStore().markDone();
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
        sessionId = DemoSeeder.seedPass(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void testS13SmokeTest_launchesAndDisplaysChecklist() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("sessionId", sessionId);
                activity.getNavController().navigate(R.id.nav_s13, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                assertTrue(current instanceof FinalizeFragment);

                View root = current.getView();
                assertNotNull(root);

                TextView screenTitle = root.findViewById(R.id.text_screen_title);
                assertNotNull(screenTitle);
                assertEquals(activity.getString(R.string.screen_s13_title), screenTitle.getText().toString());

                TextView textHeader = root.findViewById(R.id.text_finalize_header);
                assertNotNull(textHeader);

                TextView textBoundary = root.findViewById(R.id.text_check_boundary);
                assertNotNull(textBoundary);

                TextView textPoints = root.findViewById(R.id.text_check_points);
                assertNotNull(textPoints);

                TextView textTier = root.findViewById(R.id.text_check_tier);
                assertNotNull(textTier);

                MaterialButton btnHold = root.findViewById(R.id.btn_hold_finalize);
                assertNotNull(btnHold);
                assertTrue(btnHold.isEnabled());
            });
        }
    }
}
