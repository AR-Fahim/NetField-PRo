package com.netfieldpro;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.startsWith;

import android.os.Bundle;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.ProjectRow;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WalkEveryEdgeTest {

    private static final String PROJ_ID = "AAAAAAAAAAAAAAAAAAAA01";
    private static final String FLOOR_ID = "AAAAAAAAAAAAAAAAAAAA11";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        TestSetup.skipOnboarding();
        TestSetup.resetAppState();

        activityRule.getScenario().onActivity(activity -> {
            ProjectRow p = new ProjectRow();
            p.id = PROJ_ID;
            p.name = "Walk Project";
            p.createdAtMs = System.currentTimeMillis();
            NetFieldDatabase.get(activity).netFieldDao().insertProject(p);

            FloorRow f = new FloorRow();
            f.id = FLOOR_ID;
            f.projectId = PROJ_ID;
            f.levelIndex = 1;
            f.planSha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
            f.planWpx = 2000;
            f.planHpx = 1000;
            f.metersPerPixel = 0.02;
            f.scaleMethod = "LINE";
            f.scaleUncertaintyPct = 5.0;
            NetFieldDatabase.get(activity).netFieldDao().insertFloor(f);

            if (activity.getNavController().getCurrentDestination() != null
                    && activity.getNavController().getCurrentDestination().getId() != R.id.nav_s02) {
                activity.getNavController().navigate(R.id.nav_s02);
            }
        });
    }

    private void testEdge(String targetId, String targetTitle, String sourceTitle) {
        onView(withText(startsWith("→ " + targetId))).perform(scrollTo(), click());
        onView(allOf(withId(R.id.text_screen_title), withText(targetTitle))).check(matches(isDisplayed()));
        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText(sourceTitle))).check(matches(isDisplayed()));
    }

    private void navigateTo(String targetId, String targetTitle) {
        onView(withText(startsWith("→ " + targetId))).perform(scrollTo(), click());
        onView(allOf(withId(R.id.text_screen_title), withText(targetTitle))).check(matches(isDisplayed()));
    }

    @Test
    public void testWalkEveryEdgeFromS02() {
        // Source: S02 Projects
        onView(allOf(withId(R.id.text_screen_title), withText("Projects"))).check(matches(isDisplayed()));

        // Edge 1: S02 -> S17 (action_s02_to_s17)
        activityRule.getScenario().onActivity(a -> a.getNavController().navigate(R.id.action_s02_to_s17));
        onView(allOf(withId(R.id.text_screen_title), withText("Verify Report"))).check(matches(isDisplayed()));
        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText("Projects"))).check(matches(isDisplayed()));

        // Edge 2: S02 -> S18 (action_s02_to_s18)
        activityRule.getScenario().onActivity(a -> a.getNavController().navigate(R.id.action_s02_to_s18));
        onView(allOf(withId(R.id.text_screen_title), withText("Choose a plan"))).check(matches(isDisplayed()));
        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText("Projects"))).check(matches(isDisplayed()));

        // S02 -> S18 via TierBadge
        onView(withId(R.id.tier_badge_view)).perform(click());
        onView(allOf(withId(R.id.text_screen_title), withText("Choose a plan"))).check(matches(isDisplayed()));
        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText("Projects"))).check(matches(isDisplayed()));

        // Edge 3: S02 -> S19 (action_s02_to_s19)
        activityRule.getScenario().onActivity(a -> a.getNavController().navigate(R.id.action_s02_to_s19));
        onView(allOf(withId(R.id.text_screen_title), withText("Settings"))).check(matches(isDisplayed()));

        // Set debug tier to PRO for S19 edges
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);

        // Edge 25: S19 -> S18 through row_manage_plan
        onView(withId(R.id.row_manage_plan)).perform(scrollTo(), click());
        onView(allOf(withId(R.id.text_screen_title), withText("Choose a plan"))).check(matches(isDisplayed()));
        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText("Settings"))).check(matches(isDisplayed()));

        // Edge 26: S19 -> S20 through row_diagnostics
        onView(withId(R.id.row_diagnostics)).perform(scrollTo(), click());
        onView(allOf(withId(R.id.text_screen_title), withText("Diagnostics"))).check(matches(isDisplayed()));
        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText("Settings"))).check(matches(isDisplayed()));

        // Edge 27: S19 -> S21 through row_custom_profiles
        onView(withId(R.id.row_custom_profiles)).perform(scrollTo(), click());
        onView(allOf(withId(R.id.text_screen_title), withText("Custom Profile"))).check(matches(isDisplayed()));
        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText("Settings"))).check(matches(isDisplayed()));

        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText("Projects"))).check(matches(isDisplayed()));

        // Edge 4: S02 -> S03 (action_s02_to_s03) via project card click
        onView(withId(R.id.card_project)).perform(click());
        onView(allOf(withId(R.id.text_screen_title), withText("Project"))).check(matches(isDisplayed()));

        // Edge 5: S03 -> S04 (action_s03_to_s04) via Add floor button
        onView(withId(R.id.btn_add_floor)).perform(click());
        onView(allOf(withId(R.id.text_screen_title), withText("Import Floor Plan"))).check(matches(isDisplayed()));

        // Edge 9: S04 -> S05 (action_s04_to_s05)
        activityRule.getScenario().onActivity(a -> {
            Bundle b = new Bundle();
            b.putString("floorId", FLOOR_ID);
            a.getNavController().navigate(R.id.action_s04_to_s05, b);
        });
        onView(allOf(withId(R.id.text_screen_title), withText("Set scale"))).check(matches(isDisplayed()));
        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText("Import Floor Plan"))).check(matches(isDisplayed()));
        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText("Project"))).check(matches(isDisplayed()));

        // Edge 8: S03 -> S14 (action_s03_to_s14)
        activityRule.getScenario().onActivity(a -> a.getNavController().navigate(R.id.action_s03_to_s14));
        onView(allOf(withId(R.id.text_screen_title), withText("Assessments"))).check(matches(isDisplayed()));
        // Edge 22: S14 -> S15
        testEdge("S15", "Assessment", "Assessments");
        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText("Project"))).check(matches(isDisplayed()));

        // Edge 7: S03 -> S09 (action_s03_to_s09)
        activityRule.getScenario().onActivity(a -> a.getNavController().navigate(R.id.action_s03_to_s09));
        onView(allOf(withId(R.id.text_screen_title), withText("Heatmap"))).check(matches(isDisplayed()));
        pressBack();
        onView(allOf(withId(R.id.text_screen_title), withText("Project"))).check(matches(isDisplayed()));

        // Edge 6: S03 -> S06 (action_s03_to_s06) via Start survey button on calibrated floor
        onView(withId(R.id.btn_start_survey)).perform(click());
        onView(allOf(withId(R.id.text_screen_title), withText("New Survey"))).check(matches(isDisplayed()));

        // Edge 11: S06 -> S18
        testEdge("S18", "Choose a plan", "New Survey");

        // Edge 10: S06 -> S07
        navigateTo("S07", "Survey");
        // Edge 12: S07 -> S08
        testEdge("S08", "Point Detail", "Survey");

        // Edge 13: S07 -> S09
        navigateTo("S09", "Heatmap");
        // Edge 14: S09 -> S10
        testEdge("S10", "Boundary & Walls", "Heatmap");
        // Edge 15: S09 -> S11
        testEdge("S11", "Access Points", "Heatmap");
        // Edge 16: S09 -> S12
        testEdge("S12", "Roaming Analysis", "Heatmap");
        // Edge 18: S09 -> S18
        testEdge("S18", "Choose a plan", "Heatmap");
        // Edge 19: S09 -> S21
        testEdge("S21", "Custom Profile", "Heatmap");

        // Edge 17: S09 -> S13
        navigateTo("S13", "Finalize");
        // Edge 21: S13 -> S18
        testEdge("S18", "Choose a plan", "Finalize");

        // Edge 20: S13 -> S15
        navigateTo("S15", "Assessment");
        // Edge 23: S15 -> S16
        navigateTo("S16", "Report");
        // Edge 24: S16 -> S18
        testEdge("S18", "Choose a plan", "Report");
        pressBack(); // back to S15
        pressBack(); // back to S13
        pressBack(); // back to S09
        pressBack(); // back to S07
        pressBack(); // back to S06
        pressBack(); // back to S03
        pressBack(); // back to S02

        onView(allOf(withId(R.id.text_screen_title), withText("Projects"))).check(matches(isDisplayed()));
    }

    @Test
    public void testWalkEdgesFromS01() {
        activityRule.getScenario().onActivity(activity -> {
            activity.getNavController().navigate(R.id.nav_s01);
            androidx.navigation.fragment.NavHostFragment navHost =
                    (androidx.navigation.fragment.NavHostFragment) activity.getSupportFragmentManager()
                            .findFragmentById(R.id.nav_host_fragment);
            if (navHost != null) {
                navHost.getChildFragmentManager().executePendingTransactions();
            }
        });
        onView(withId(R.id.text_onboarding_title)).check(matches(isDisplayed()));

        // Edge 28: S01 -> S17
        onView(withId(R.id.btn_onboarding_verify)).perform(scrollTo(), click());
        onView(allOf(withId(R.id.text_screen_title), withText("Verify Report"))).check(matches(isDisplayed()));
        pressBack();
        onView(withId(R.id.text_onboarding_title)).perform(scrollTo()).check(matches(isDisplayed()));

        // Edge 29: S01 -> S02
        onView(withId(R.id.btn_onboarding_skip)).perform(scrollTo(), click());
        onView(allOf(withId(R.id.text_screen_title), withText("Projects"))).check(matches(isDisplayed()));
    }
}
