package com.netfieldpro;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.ui.plan.FloorImportFragment;

@RunWith(AndroidJUnit4.class)
public class M6AccessibilityDeviceTest {

    private static final String PROJ_ID = "AAAAAAAAAAAAAAAAAAAA01";
    private static final String FLOOR_ID = "AAAAAAAAAAAAAAAAAAAA11";

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        TestSetup.skipOnboarding();
        TestSetup.resetAppState();
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
    }

    private void seedProjectAndFloor() {
        ProjectRow p = new ProjectRow();
        p.id = PROJ_ID;
        p.name = "Accessibility Project";
        p.site = "Lab";
        p.createdAtMs = System.currentTimeMillis();
        NetFieldDatabase.get(ApplicationProvider.getApplicationContext()).netFieldDao().insertProject(p);

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
        NetFieldDatabase.get(ApplicationProvider.getApplicationContext()).netFieldDao().insertFloor(f);
    }

    private void clearDatabase() {
        NetFieldDatabase.get(ApplicationProvider.getApplicationContext()).netFieldDao().deleteProject(PROJ_ID);
    }

    private File createTestImage(String name, int w, int h) throws Exception {
        File cacheDir = ApplicationProvider.getApplicationContext().getCacheDir();
        File file = new File(cacheDir, name);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        }
        bmp.recycle();
        return file;
    }

    @Test
    public void testS02EmptyStateA11y() {
        clearDatabase();
        activityRule.getScenario().onActivity(activity -> {
            activity.getNavController().navigate(R.id.nav_s02);
        });

        onView(allOf(withId(R.id.text_screen_title), withText("Projects"))).check(matches(isDisplayed()));
        onView(withId(R.id.text_empty_title)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_new_project)).check(matches(isDisplayed()));
    }

    @Test
    public void testS02ListStateA11y() {
        NetFieldApp.projectRepository().createProject("Accessibility Project", null, "Lab");
        activityRule.getScenario().onActivity(activity -> {
            activity.getNavController().navigate(R.id.nav_s02);
        });

        onView(allOf(withId(R.id.text_screen_title), withText("Projects"))).check(matches(isDisplayed()));
        onView(withId(R.id.card_project)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_new_project)).check(matches(isDisplayed()));
    }

    @Test
    public void testS03ProjectDetailA11y() {
        seedProjectAndFloor();
        activityRule.getScenario().onActivity(activity -> {
            Bundle args = new Bundle();
            args.putString("projectId", PROJ_ID);
            activity.getNavController().navigate(R.id.nav_s03, args);
        });

        onView(allOf(withId(R.id.text_screen_title), withText("Project"))).check(matches(isDisplayed()));
        onView(withId(R.id.text_project_header)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_add_floor)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_set_scale)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_start_survey)).check(matches(isDisplayed()));
    }

    @Test
    public void testS04FloorImportPickedA11y() throws Exception {
        seedProjectAndFloor();
        File testImg = createTestImage("a11y_picked.png", 600, 400);

        activityRule.getScenario().onActivity(activity -> {
            Bundle args = new Bundle();
            args.putString("projectId", PROJ_ID);
            activity.getNavController().navigate(R.id.nav_s04, args);
        });

        onView(allOf(withId(R.id.text_screen_title), withText("Import Floor Plan"))).check(matches(isDisplayed()));

        activityRule.getScenario().onActivity(activity -> {
            NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            assertNotNull(navHost);
            FloorImportFragment fragment = (FloorImportFragment) navHost.getChildFragmentManager()
                    .getPrimaryNavigationFragment();
            assertNotNull(fragment);
            fragment.onFilePicked(testImg, "image/png");
        });

        onView(allOf(withId(R.id.text_screen_title), withText("Import Floor Plan"))).check(matches(isDisplayed()));
        onView(withId(R.id.edit_floor_name)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.edit_floor_level)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.btn_save)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    public void testS04FloorImportErrorA11y() throws Exception {
        seedProjectAndFloor();
        File dummy = new File(ApplicationProvider.getApplicationContext().getCacheDir(), "dummy.gif");
        dummy.createNewFile();

        activityRule.getScenario().onActivity(activity -> {
            Bundle args = new Bundle();
            args.putString("projectId", PROJ_ID);
            activity.getNavController().navigate(R.id.nav_s04, args);
        });

        onView(allOf(withId(R.id.text_screen_title), withText("Import Floor Plan"))).check(matches(isDisplayed()));

        activityRule.getScenario().onActivity(activity -> {
            NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            assertNotNull(navHost);
            FloorImportFragment fragment = (FloorImportFragment) navHost.getChildFragmentManager()
                    .getPrimaryNavigationFragment();
            assertNotNull(fragment);
            fragment.onFilePicked(dummy, "image/gif");
        });

        onView(allOf(withId(R.id.text_screen_title), withText("Import Floor Plan"))).check(matches(isDisplayed()));
        onView(withId(R.id.text_error)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.btn_pick_file)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    public void testS05ScaleA11y() {
        seedProjectAndFloor();
        activityRule.getScenario().onActivity(activity -> {
            Bundle args = new Bundle();
            args.putString("floorId", FLOOR_ID);
            activity.getNavController().navigate(R.id.nav_s05, args);
        });

        onView(allOf(withId(R.id.text_screen_title), withText("Set scale"))).check(matches(isDisplayed()));
        onView(withId(R.id.plan_view)).check(matches(isDisplayed()));
        onView(withId(R.id.radio_mode_line)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.btn_manual_points)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.edit_distance)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    public void testPlanViewLabA11y() {
        activityRule.getScenario().onActivity(activity -> {
            activity.getNavController().navigate(R.id.nav_debug_plan_lab);
        });

        onView(allOf(withId(R.id.text_screen_title), withText("Plan view lab"))).check(matches(isDisplayed()));
        onView(withId(R.id.plan_view)).check(matches(isDisplayed()));
    }
}
