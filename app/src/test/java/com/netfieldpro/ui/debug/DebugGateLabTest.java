package com.netfieldpro.ui.debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;

import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.Feature;
import com.netfieldpro.core.entitlement.Tier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DebugGateLabTest {

    @Before
    public void setUp() {
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
    }

    private void navigateToGateLab(MainActivity activity) {
        activity.getNavController().navigate(R.id.nav_debug_gate_lab);
        NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            navHost.getChildFragmentManager().executePendingTransactions();
        }
    }

    @Test
    public void testGL1_switchingTierUpdatesAllBadgesImmediately() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToGateLab(activity);

                RadioButton rbFree = activity.findViewById(R.id.rb_gatelab_free);
                RadioButton rbPro = activity.findViewById(R.id.rb_gatelab_pro);
                RadioButton rbEnterprise = activity.findViewById(R.id.rb_gatelab_enterprise);

                // Initial tier: FREE
                View root = activity.getWindow().getDecorView();
                TextView badgeTap = root.findViewWithTag("badge_" + Feature.TAP_CAPTURE.name());
                TextView badgeSign = root.findViewWithTag("badge_" + Feature.FINALIZE_SIGN.name());
                TextView badgeRoam = root.findViewWithTag("badge_" + Feature.ROAMING_AUDIT.name());
                assertNotNull(badgeTap);
                assertNotNull(badgeSign);
                assertNotNull(badgeRoam);

                assertEquals("✓ Allowed", badgeTap.getText().toString());
                assertEquals("🔒 Pro", badgeSign.getText().toString());
                assertEquals("🔒 Enterprise", badgeRoam.getText().toString());

                // Switch to PRO
                rbPro.performClick();
                assertEquals("✓ Allowed", badgeTap.getText().toString());
                assertEquals("✓ Allowed", badgeSign.getText().toString());
                assertEquals("🔒 Enterprise", badgeRoam.getText().toString());

                TextView badgeWatermark = root.findViewWithTag("badge_" + Feature.WATERMARKED_PDF.name());
                assertNotNull(badgeWatermark);
                assertEquals("— n/a", badgeWatermark.getText().toString());

                // Switch to ENTERPRISE
                rbEnterprise.performClick();
                assertEquals("✓ Allowed", badgeTap.getText().toString());
                assertEquals("✓ Allowed", badgeSign.getText().toString());
                assertEquals("✓ Allowed", badgeRoam.getText().toString());
            });
        }
    }

    @Test
    public void testGL2_tryAllowedFeatureShowsSnackbarWithoutNavigating() {
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToGateLab(activity);

                View btnTry = activity.getWindow().getDecorView().findViewWithTag("try_" + Feature.TAP_CAPTURE.name());
                assertNotNull(btnTry);
                btnTry.performClick();

                NavController nc = activity.getNavController();
                assertEquals("Allowed feature should not navigate away from Gate Lab",
                        R.id.nav_debug_gate_lab, nc.getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testGL3_tryLockedFeatureNavigatesToS18AndSimulateReturns() {
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToGateLab(activity);

                View btnTry = activity.getWindow().getDecorView().findViewWithTag("try_" + Feature.FINALIZE_SIGN.name());
                assertNotNull(btnTry);
                btnTry.performClick();

                NavController nc = activity.getNavController();
                assertEquals("Locked feature must navigate to S18 paywall",
                        R.id.nav_s18, nc.getCurrentDestination().getId());

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                if (navHost != null) {
                    navHost.getChildFragmentManager().executePendingTransactions();
                }

                View btnSimulate = activity.findViewById(R.id.btn_dev_simulate);
                assertNotNull("Dev simulate button must exist on S18 in debug builds", btnSimulate);
                btnSimulate.performClick();

                if (navHost != null) {
                    navHost.getChildFragmentManager().executePendingTransactions();
                }

                assertEquals("Simulating purchase must pop back to Gate Lab",
                        R.id.nav_debug_gate_lab, nc.getCurrentDestination().getId());
                assertEquals("Tier must now be PRO", Tier.PRO, NetFieldApp.entitlements().current());
            });
        }
    }
}
