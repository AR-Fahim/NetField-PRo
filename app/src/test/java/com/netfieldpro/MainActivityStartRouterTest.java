package com.netfieldpro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.onboarding.OnboardingStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MainActivityStartRouterTest {

    @Rule
    public NoAuthRule noAuthRule = new NoAuthRule();

    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences(OnboardingStore.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @After
    public void tearDown() {
        prefs.edit().clear().commit();
    }

    @Test
    public void testMainIntent_onboardingNotDone_routesToS01() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.getNavController());
                assertEquals(R.id.nav_s01, activity.getNavController().getCurrentDestination().getId());
                assertFalse(NetFieldApp.onboardingStore().isDone());
            });
        }
    }

    @Test
    public void testMainIntent_onboardingDone_routesToS02() {
        NetFieldApp.onboardingStore().markDone();
        assertTrue(NetFieldApp.onboardingStore().isDone());

        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.getNavController());
                assertEquals(R.id.nav_s02, activity.getNavController().getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testActionView_onboardingNotDone_routesToS17_doneRemainsFalse() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("content://media/test.pdf"), "application/pdf");

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.getNavController());
                assertEquals(R.id.nav_s17, activity.getNavController().getCurrentDestination().getId());
                assertFalse(NetFieldApp.onboardingStore().isDone());
            });
        }
    }

    @Test
    public void testActionView_onboardingDone_routesToS17() {
        NetFieldApp.onboardingStore().markDone();

        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("content://media/test.pdf"), "application/pdf");

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.getNavController());
                assertEquals(R.id.nav_s17, activity.getNavController().getCurrentDestination().getId());
                assertTrue(NetFieldApp.onboardingStore().isDone());
            });
        }
    }

    @Test
    public void testActionSend_onboardingNotDone_routesToS17() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://media/test.pdf"));

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.getNavController());
                assertEquals(R.id.nav_s17, activity.getNavController().getCurrentDestination().getId());
                assertFalse(NetFieldApp.onboardingStore().isDone());
            });
        }
    }
}
