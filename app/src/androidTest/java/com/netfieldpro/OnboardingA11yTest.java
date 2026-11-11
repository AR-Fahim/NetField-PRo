package com.netfieldpro;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OnboardingA11yTest {

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    @AfterClass
    public static void tearDownClass() {
        NetFieldApp.onboardingStore().markDone();
    }

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        TestSetup.resetAppState();
    }

    @Test
    public void testOnboardingAccessibilityAfterReset() {
        onView(withId(R.id.chip_dev)).perform(click());
        onView(withId(R.id.row_debug_reset_onboarding)).perform(scrollTo(), click());

        onView(withId(R.id.text_onboarding_title)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_onboarding_primary)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.btn_onboarding_skip)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.btn_onboarding_verify)).perform(scrollTo()).check(matches(isDisplayed()));
    }
}
