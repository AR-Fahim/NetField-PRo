package com.netfieldpro.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.Tier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.netfieldpro.NoAuthRule;

@RunWith(RobolectricTestRunner.class)
public class TierBadgeTest {

    @Rule
    public NoAuthRule noAuthRule = new NoAuthRule();

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        NetFieldApp.onboardingStore().markDone();
    }

    @org.junit.After
    public void tearDown() {
        NetFieldApp.onboardingStore().reset();
    }

    @Test
    public void testTB1_textAndContentDescriptionPerTier() {
        TierBadgeView badge = new TierBadgeView(context);

        // Case 1: FREE
        badge.setTier(Tier.FREE);
        assertEquals("Free", badge.getText().toString());
        assertEquals("Plan: Free. Opens plan details.", badge.getContentDescription().toString());
        assertEquals(ContextCompat.getColor(context, R.color.nf_on_surface_variant), badge.getCurrentTextColor());
        assertEquals(Tier.FREE, badge.getTier());

        // Case 2: PRO
        badge.setTier(Tier.PRO);
        assertEquals("Pro", badge.getText().toString());
        assertEquals("Plan: Pro. Opens plan details.", badge.getContentDescription().toString());
        assertEquals(ContextCompat.getColor(context, R.color.nf_primary), badge.getCurrentTextColor());
        assertEquals(Tier.PRO, badge.getTier());

        // Case 3: ENTERPRISE
        badge.setTier(Tier.ENTERPRISE);
        assertEquals("Enterprise", badge.getText().toString());
        assertEquals("Plan: Enterprise. Opens plan details.", badge.getContentDescription().toString());
        assertEquals(ContextCompat.getColor(context, R.color.nf_primary), badge.getCurrentTextColor());
        assertEquals(Tier.ENTERPRISE, badge.getTier());
    }

    @Test
    public void testTB2_badgeVisibilityAcrossDestinations() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                MenuItem item = activity.getBinding().toolbar.getMenu().findItem(R.id.action_tier_badge);
                assertNotNull("Badge menu item should exist", item);
                assertTrue("Badge must be visible on S02", item.isVisible());

                // Navigate to S03
                activity.getNavController().navigate(R.id.action_s02_to_s03);
                assertFalse("Badge must be gone on S03", item.isVisible());

                // Navigate to S19
                activity.getNavController().navigate(R.id.nav_s19);
                assertFalse("Badge must be gone on S19", item.isVisible());

                // Navigate back to S02
                activity.getNavController().navigate(R.id.nav_s02);
                assertTrue("Badge must be visible again on S02", item.isVisible());
            });
        }
    }

    @Test
    public void testTB3_robolectricClickNavigatesToS18() {
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                MenuItem item = activity.getBinding().toolbar.getMenu().findItem(R.id.action_tier_badge);
                assertNotNull(item);
                View actionView = item.getActionView();
                assertNotNull(actionView);
                TierBadgeView badgeView = (actionView instanceof TierBadgeView)
                        ? (TierBadgeView) actionView
                        : actionView.findViewById(R.id.tier_badge_view);
                assertNotNull(badgeView);
                assertEquals("Pro", badgeView.getText().toString());

                // Perform click
                badgeView.performClick();
                assertEquals(R.id.nav_s18, activity.getNavController().getCurrentDestination().getId());
            });
        }
    }
}

