package com.netfieldpro;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.model.enums.ScaleMethod;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.repository.FloorRepository;
import com.netfieldpro.data.repository.PlanFileStore;
import com.netfieldpro.data.repository.ProjectRepository;

@RunWith(AndroidJUnit4.class)
public class ResetAppStateDeviceTest {

    @Before
    public void setUp() {
        TestSetup.resetAppState();
    }

    @Test
    public void testRS1_seedAndResetClearsAll() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        NetFieldDatabase db = NetFieldDatabase.get(context);
        ProjectRepository projectRepo = NetFieldApp.projectRepository();
        FloorRepository floorRepo = NetFieldApp.floorRepository();
        PlanFileStore planStore = NetFieldApp.planFileStore();

        // Allow multiple projects/floors
        NetFieldApp.entitlements().setDebugTier(Tier.ENTERPRISE);

        // Project 1 with floor + plan file
        Project p1 = projectRepo.createProject("Project Alpha", "Client A", "Site 1");
        String f1Id = IdGenerator.generate();
        File f1File = planStore.savePlanBytes(f1Id, "png", new byte[]{1, 2, 3});
        Floor fl1 = new Floor(f1Id, p1.id(), 1L, "sha1", 2000, 1000, 0.05, null, ScaleMethod.LINE, 5.0, false, null, null, null, null, null);
        floorRepo.createFloor(fl1, f1File);

        // Project 2 with floor + plan file
        Project p2 = projectRepo.createProject("Project Beta", "Client B", "Site 2");
        String f2Id = IdGenerator.generate();
        File f2File = planStore.savePlanBytes(f2Id, "png", new byte[]{4, 5, 6});
        Floor fl2 = new Floor(f2Id, p2.id(), 1L, "sha2", 2000, 1000, 0.05, null, ScaleMethod.LINE, 5.0, false, null, null, null, null, null);
        floorRepo.createFloor(fl2, f2File);

        // Project 3 without floor
        Project p3 = projectRepo.createProject("Project Gamma", "Client C", "Site 3");

        assertEquals(3, db.netFieldDao().countProjects());
        assertEquals(2, db.netFieldDao().countFloors());
        File plansDir = new File(context.getFilesDir(), "plans");
        File[] filesBefore = plansDir.listFiles();
        assertTrue("Must have at least 2 plan files", filesBefore != null && filesBefore.length >= 2);

        // Call reset
        TestSetup.resetAppState();

        // Verify: projects 0, floors 0, plan files 0, wall materials 5
        assertEquals("Projects must be 0", 0, db.netFieldDao().countProjects());
        assertEquals("Floors must be 0", 0, db.netFieldDao().countFloors());
        File[] filesAfter = plansDir.listFiles();
        assertEquals("Plan files must be 0", 0, filesAfter != null ? filesAfter.length : 0);
        assertEquals("Wall materials must be 5", 5, db.netFieldDao().countWallMaterials());
    }

    @Test
    public void testRS2_resetTwiceNoException() {
        TestSetup.resetAppState();
        TestSetup.resetAppState();
    }

    @Test
    public void testRS3_createProjectWorksAndShowsCard() {
        TestSetup.resetAppState();
        TestSetup.skipOnboarding();

        Project p = NetFieldApp.projectRepository().createProject("Single Reset Project", null, null);
        assertNotNull(p);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.recycler_projects)).check(matches(isDisplayed()));
            onView(withId(R.id.card_project)).check(matches(isDisplayed()));
            onView(withId(R.id.recycler_projects)).check(matches(hasChildCount(1)));
        }
    }
}
