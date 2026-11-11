package com.netfieldpro;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DebugAndStyleGuideTest {

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        TestSetup.resetAppState();
        TestSetup.skipOnboarding();
        activityRule.getScenario().onActivity(activity -> {
            if (activity.getNavController().getCurrentDestination() != null
                    && activity.getNavController().getCurrentDestination().getId() != R.id.nav_s02) {
                activity.getNavController().navigate(R.id.nav_s02);
            }
        });
    }

    @Test
    public void testOpenDebugMenuAndSwitchToDark() {
        onView(withId(R.id.chip_dev)).perform(click());
        onView(allOf(withId(R.id.text_build_info))).perform(scrollTo()).check(matches(isDisplayed()));

        onView(withId(R.id.radio_theme_dark)).perform(scrollTo(), click());
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode());
    }

    @Test
    public void testOpenStyleGuideAndVerifyContents() {
        onView(withId(R.id.chip_dev)).perform(click());
        onView(withId(R.id.btn_style_guide)).perform(click());

        // 4 verdict chips
        onView(withText("✓ PASS")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("✕ FAIL")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("◐ INCOMPLETE")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("⊘ NOT ELIGIBLE")).perform(scrollTo()).check(matches(isDisplayed()));

        // 2 lock badges
        onView(withText("🔒 Pro")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("🔒 Enterprise")).perform(scrollTo()).check(matches(isDisplayed()));

        // Monospace BSSID sample
        onView(withText("aa:bb:cc:dd:ee:01")).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    public void testPlaceholderAccessibility() {
        // S02 placeholder
        onView(allOf(withId(R.id.text_screen_title), withText("Projects"))).check(matches(isDisplayed()));
        onView(withId(R.id.chip_dev)).check(matches(isDisplayed()));
    }

    @Test
    public void testAccessibilityS18Locked() {
        NetFieldApp.entitlements().setDebugTier(com.netfieldpro.core.entitlement.Tier.FREE);
        activityRule.getScenario().onActivity(activity -> {
            android.os.Bundle args = new android.os.Bundle();
            args.putString(com.netfieldpro.ui.NavArgs.ARG_FEATURE, com.netfieldpro.core.entitlement.Feature.FINALIZE_SIGN.name());
            activity.getNavController().navigate(R.id.nav_s18, args);
        });
        onView(withId(R.id.text_lock_badge)).check(matches(isDisplayed()));
    }

    @Test
    public void testAccessibilityS18ChoosePlan() {
        NetFieldApp.entitlements().setDebugTier(com.netfieldpro.core.entitlement.Tier.FREE);
        activityRule.getScenario().onActivity(activity -> {
            activity.getNavController().navigate(R.id.nav_s18, null);
        });
        onView(allOf(withId(R.id.text_screen_title), withText("Choose a plan"))).check(matches(isDisplayed()));
    }

    @Test
    public void testAccessibilityS18Stale() {
        NetFieldApp.entitlements().setDebugTier(com.netfieldpro.core.entitlement.Tier.PRO);
        activityRule.getScenario().onActivity(activity -> {
            android.os.Bundle args = new android.os.Bundle();
            args.putString(com.netfieldpro.ui.NavArgs.ARG_FEATURE, com.netfieldpro.core.entitlement.Feature.FINALIZE_SIGN.name());
            activity.getNavController().navigate(R.id.nav_s18, args);
        });
        onView(withId(R.id.btn_continue)).check(matches(isDisplayed()));
    }

    @Test
    public void testAccessibilityS19Settings() {
        NetFieldApp.entitlements().setDebugTier(com.netfieldpro.core.entitlement.Tier.FREE);
        activityRule.getScenario().onActivity(activity -> {
            activity.getNavController().navigate(R.id.nav_s19, null);
        });
        onView(allOf(withId(R.id.text_screen_title), withText("Settings"))).check(matches(isDisplayed()));
    }

    @Test
    public void testAccessibilityGateLab() {
        NetFieldApp.entitlements().setDebugTier(com.netfieldpro.core.entitlement.Tier.FREE);
        activityRule.getScenario().onActivity(activity -> {
            activity.getNavController().navigate(R.id.nav_debug_gate_lab, null);
        });
        onView(withId(R.id.text_gatelab_title)).check(matches(isDisplayed()));
    }
}

