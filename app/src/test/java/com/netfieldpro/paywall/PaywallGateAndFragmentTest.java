package com.netfieldpro.paywall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;

import com.netfieldpro.BuildConfig;
import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.arch.SourceScan;
import com.netfieldpro.core.entitlement.Feature;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.ui.NavArgs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class PaywallGateAndFragmentTest {

    @Before
    public void setUp() {
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
    }

    @Test
    public void testPG1_evaluateGateOutcomes() {
        assertEquals(PaywallGate.GateOutcome.RUN, PaywallGate.evaluate(Tier.FREE, Feature.TAP_CAPTURE));
        assertEquals(PaywallGate.GateOutcome.SHOW_PAYWALL, PaywallGate.evaluate(Tier.FREE, Feature.FINALIZE_SIGN));
        assertEquals(PaywallGate.GateOutcome.IGNORE, PaywallGate.evaluate(Tier.PRO, Feature.WATERMARKED_PDF));
        assertEquals(PaywallGate.GateOutcome.SHOW_PAYWALL, PaywallGate.evaluate(Tier.PRO, Feature.ROAMING_AUDIT));
        assertEquals(PaywallGate.GateOutcome.RUN, PaywallGate.evaluate(Tier.ENTERPRISE, Feature.PRESET_WAREHOUSE));
    }

    @Test
    public void testPG2_allowedFeatureRunsImmediatelyWithoutNavigation() {
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Fragment host = activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)
                        .getChildFragmentManager().getFragments().get(0);
                assertNotNull(host);
                int initialDestId = activity.getNavController().getCurrentDestination().getId();

                AtomicInteger runs = new AtomicInteger(0);
                boolean allowed = PaywallGate.require(host, Feature.TAP_CAPTURE, runs::incrementAndGet);

                assertTrue("Should be allowed", allowed);
                assertEquals("onAllowed should run once", 1, runs.get());
                assertEquals("Should not have navigated", initialDestId,
                        activity.getNavController().getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testPG3_lockedFeatureNavigatesToS18AndDoesNotRun() {
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Fragment host = activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)
                        .getChildFragmentManager().getFragments().get(0);
                assertNotNull(host);

                AtomicInteger runs = new AtomicInteger(0);
                boolean allowed = PaywallGate.require(host, Feature.FINALIZE_SIGN, runs::incrementAndGet);

                assertFalse("Should not be allowed immediately", allowed);
                assertEquals("onAllowed should not have run", 0, runs.get());
                NavController nc = activity.getNavController();
                assertEquals("Must navigate to S18", R.id.nav_s18, nc.getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testPG4_purchasedResultRunsOnceAndDismissedNeverRuns() {
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Fragment host = activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)
                        .getChildFragmentManager().getFragments().get(0);
                assertNotNull(host);

                AtomicInteger runs = new AtomicInteger(0);
                PaywallGate.require(host, Feature.FINALIZE_SIGN, runs::incrementAndGet);
                assertEquals(0, runs.get());

                // Test DISMISSED: listener does nothing
                Bundle dismissedBundle = new Bundle();
                dismissedBundle.putString(NavArgs.RESULT_VALUE, NavArgs.RESULT_DISMISSED);
                host.getParentFragmentManager().setFragmentResult(NavArgs.RESULT_KEY_PAYWALL, dismissedBundle);
                assertEquals("DISMISSED must never run onAllowed", 0, runs.get());

                // Re-register require to test PURCHASED
                PaywallGate.require(host, Feature.FINALIZE_SIGN, runs::incrementAndGet);
                assertEquals(0, runs.get());

                // Raise tier to PRO and send PURCHASED
                NetFieldApp.entitlements().setDebugTier(Tier.PRO);
                Bundle purchasedBundle = new Bundle();
                purchasedBundle.putString(NavArgs.RESULT_VALUE, NavArgs.RESULT_PURCHASED);
                host.getParentFragmentManager().setFragmentResult(NavArgs.RESULT_KEY_PAYWALL, purchasedBundle);
                assertEquals("PURCHASED with raised tier must run onAllowed once", 1, runs.get());
            });
        }
    }

    @Test
    public void testPG5_s18States() {
        // State 1: Locked shows the badge 🔒 Pro for FINALIZE_SIGN
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle lockedArgs = new Bundle();
                lockedArgs.putString(NavArgs.ARG_FEATURE, Feature.FINALIZE_SIGN.name());
                activity.getNavController().navigate(R.id.nav_s18, lockedArgs);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                TextView badge = activity.findViewById(R.id.text_lock_badge);
                assertNotNull(badge);
                assertEquals(View.VISIBLE, badge.getVisibility());
                assertEquals("🔒 Pro", badge.getText().toString());
            });
        }

        // State 2: Choose-plan shows for "garbage"
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle garbageArgs = new Bundle();
                garbageArgs.putString(NavArgs.ARG_FEATURE, "garbage");
                activity.getNavController().navigate(R.id.nav_s18, garbageArgs);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                TextView badge = activity.findViewById(R.id.text_lock_badge);
                assertEquals(View.GONE, badge.getVisibility());
                TextView title = activity.findViewById(R.id.text_screen_title);
                assertEquals("Choose a plan", title.getText().toString());
            });
        }

        // State 3: Choose-plan shows for null
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getNavController().navigate(R.id.nav_s18, null);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                TextView badge = activity.findViewById(R.id.text_lock_badge);
                assertEquals(View.GONE, badge.getVisibility());
                TextView title = activity.findViewById(R.id.text_screen_title);
                assertEquals("Choose a plan", title.getText().toString());
            });
        }

        // State 4: Stale shows Continue
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle staleArgs = new Bundle();
                staleArgs.putString(NavArgs.ARG_FEATURE, Feature.FINALIZE_SIGN.name());
                activity.getNavController().navigate(R.id.nav_s18, staleArgs);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                View btnContinue = activity.findViewById(R.id.btn_continue);
                assertNotNull(btnContinue);
                assertEquals(View.VISIBLE, btnContinue.getVisibility());
                TextView included = activity.findViewById(R.id.text_paywall_included);
                assertNotNull(included);
                assertEquals(View.VISIBLE, included.getVisibility());
            });
        }
    }

    @Test
    public void testPG6_simulateButtonGuardedByBuildConfigDebug() {
        File current = new File(".").getAbsoluteFile();
        File sourceFile = new File(current, "app/src/main/java/com/netfieldpro/paywall/PaywallFragment.java");
        if (!sourceFile.exists()) {
            sourceFile = new File(current, "src/main/java/com/netfieldpro/paywall/PaywallFragment.java");
        }
        assertTrue("PaywallFragment.java must exist", sourceFile.exists());
        String code = SourceScan.readFile(sourceFile);
        assertTrue("PaywallFragment must guard simulate button with BuildConfig.DEBUG",
                code.contains("BuildConfig.DEBUG"));
    }

    @Test
    public void testPG7_quotaProjectShowsQuotaLine() {
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString(NavArgs.QUOTA, "PROJECT");
                activity.getNavController().navigate(R.id.nav_s18, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                TextView title = activity.findViewById(R.id.text_screen_title);
                assertNotNull(title);
                assertEquals(activity.getString(R.string.paywall_choose_plan), title.getText().toString());

                TextView subtitle = activity.findViewById(R.id.text_paywall_subtitle);
                assertNotNull(subtitle);
                assertEquals(View.VISIBLE, subtitle.getVisibility());
                assertEquals(activity.getString(R.string.paywall_quota_project), subtitle.getText().toString());
            });
        }
    }
}
