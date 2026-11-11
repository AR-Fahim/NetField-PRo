package com.netfieldpro;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.netfieldpro.core.entitlement.Tier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TierBadgeEspressoTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        TestSetup.resetAppState();
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
    }

    @Test
    public void testTB3_espressoBadgeShowsProAndClickLandsOnS18Title() throws InterruptedException {
        Thread.sleep(300);
        onView(withText("Pro")).check(matches(isDisplayed()));
        onView(withText("Pro")).perform(click());
        Thread.sleep(300);
        onView(withText("Upgrade")).check(matches(isDisplayed()));
    }
}

