package com.netfieldpro.ui.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.model.enums.ScaleMethod;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BoundaryWallsFragmentTest {

    private String testFloorId;

    @Before
    public void setUp() {
        Project p = NetFieldApp.projectRepository().createProject("S10 Test Project", "Client", "Site");
        Floor f = new Floor(
                com.netfieldpro.core.util.IdGenerator.generate(),
                p.id(),
                1L,
                "plan_sha_test",
                1600,
                1000,
                0.025,
                null,
                ScaleMethod.LINE,
                1.0,
                true,
                null,
                null,
                null,
                null,
                null
        );
        File fakePlan = new File(ApplicationProvider.getApplicationContext().getFilesDir(), "test_plan.png");
        NetFieldApp.floorRepository().createFloor(f, fakePlan);
        testFloorId = f.id();
    }

    @Test
    public void testS10SmokeTest_launchesAndDoneIsDisabledWithNoBoundary() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s10, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                assertTrue(current instanceof BoundaryWallsFragment);

                View root = current.getView();
                assertNotNull(root);

                TextView screenTitle = root.findViewById(R.id.text_screen_title);
                assertNotNull(screenTitle);
                assertEquals(activity.getString(R.string.screen_s10_title), screenTitle.getText().toString());

                Button btnDone = root.findViewById(R.id.btn_done);
                assertNotNull(btnDone);
                // Done must be disabled with no boundary
                assertFalse("Done must be disabled with no boundary", btnDone.isEnabled());

                assertNotNull(root.findViewById(R.id.toggle_mode));
                assertNotNull(root.findViewById(R.id.btn_mode_boundary));
                assertNotNull(root.findViewById(R.id.btn_mode_walls));
                assertNotNull(root.findViewById(R.id.plan_view));

                View boundaryControls = root.findViewById(R.id.layout_boundary_controls);
                assertNotNull(boundaryControls);
                assertEquals(View.VISIBLE, boundaryControls.getVisibility());

                Button btnFit = root.findViewById(R.id.btn_fit_rectangle);
                assertNotNull(btnFit);
                assertNotNull(root.findViewById(R.id.btn_draw_boundary));
                assertNotNull(root.findViewById(R.id.btn_clear_boundary));

                View wallControls = root.findViewById(R.id.layout_walls_controls);
                assertNotNull(wallControls);
                assertEquals(View.GONE, wallControls.getVisibility());

                assertNotNull(root.findViewById(R.id.chip_group_materials));
                assertNotNull(root.findViewById(R.id.chip_drywall));
                assertNotNull(root.findViewById(R.id.btn_finish_wall));
                assertNotNull(root.findViewById(R.id.btn_undo_wall));

                TextView textStatus = root.findViewById(R.id.text_status);
                assertNotNull(textStatus);
                assertEquals(activity.getString(R.string.status_no_boundary), textStatus.getText().toString());

                // Now tap Fit rectangle -> should create a valid boundary and enable Done
                btnFit.performClick();
                assertTrue("Done should be enabled after fitting rectangle", btnDone.isEnabled());
                assertTrue("Status should indicate Boundary OK", textStatus.getText().toString().startsWith("Boundary OK"));
            });
        }
    }

    @Test
    public void testModeToggleSwitchesControls() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("floorId", testFloorId);
                activity.getNavController().navigate(R.id.nav_s10, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                View root = current.getView();
                assertNotNull(root);

                View boundaryControls = root.findViewById(R.id.layout_boundary_controls);
                View wallControls = root.findViewById(R.id.layout_walls_controls);
                Button btnModeWalls = root.findViewById(R.id.btn_mode_walls);

                btnModeWalls.performClick();
                assertEquals(View.GONE, boundaryControls.getVisibility());
                assertEquals(View.VISIBLE, wallControls.getVisibility());
            });
        }
    }
}

