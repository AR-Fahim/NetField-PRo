package com.netfieldpro;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DebugDataLabEspressoTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        TestSetup.skipOnboarding();
        TestSetup.resetAppState();
    }

    @Test
    public void testDataLabCreateAndDeleteDemo() throws InterruptedException {
        // Open debug menu
        onView(withId(R.id.chip_dev)).perform(click());

        // Open Data Lab
        onView(withId(R.id.btn_data_lab)).perform(click());

        // Create demo project
        onView(withId(R.id.btn_create_demo)).perform(click());
        Thread.sleep(300);

        // Verify counts
        onView(withId(R.id.count_project)).check(matches(withText("1")));
        onView(withId(R.id.count_floor)).check(matches(withText("1")));
        onView(withId(R.id.count_survey_point)).check(matches(withText("1")));

        // Export canonical JSON
        onView(withId(R.id.btn_export_json)).perform(click());
        Thread.sleep(300);

        // Delete demo project
        onView(withId(R.id.btn_delete_demo)).perform(click());
        Thread.sleep(300);

        // Verify counts are 0
        onView(withId(R.id.count_project)).check(matches(withText("0")));
        onView(withId(R.id.count_floor)).check(matches(withText("0")));
        onView(withId(R.id.count_survey_point)).check(matches(withText("0")));
    }
}

