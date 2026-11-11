package com.netfieldpro.ui.debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;

import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;

import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
public class DebugMenuTest {

    @Before
    public void setUp() {
        NetFieldApp.onboardingStore().markDone();
        assertTrue(NetFieldApp.onboardingStore().isDone());
    }

    @After
    public void tearDown() {
        NetFieldApp.onboardingStore().reset();
    }

    @Test
    public void testDBG1_resetOnboardingClearsDoneAndRestartsMainActivity() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getNavController().navigate(R.id.nav_debug_menu);
                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                if (navHost != null) {
                    navHost.getChildFragmentManager().executePendingTransactions();
                }

                int resetId = activity.getResources().getIdentifier(
                        "row_debug_reset_onboarding", "id", activity.getPackageName()
                );
                assertTrue("row_debug_reset_onboarding should exist in debug build", resetId != 0);

                View resetBtn = activity.findViewById(resetId);
                assertNotNull("reset button should be found in debug menu", resetBtn);

                resetBtn.performClick();

                assertFalse("onboarding_done should be false after click", NetFieldApp.onboardingStore().isDone());

                ShadowActivity shadowActivity = Shadows.shadowOf(activity);
                Intent nextIntent = shadowActivity.getNextStartedActivity();
                assertNotNull("Next started activity should not be null", nextIntent);
                assertEquals(MainActivity.class.getName(), nextIntent.getComponent().getClassName());
                assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK, nextIntent.getFlags());
            });
        }
    }
}
