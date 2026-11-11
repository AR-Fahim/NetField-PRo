package com.netfieldpro.ui.plan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.google.android.material.textfield.TextInputEditText;
import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.NoAuthRule;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.data.db.BatchedWriter;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.repository.FloorRepository;
import com.netfieldpro.data.repository.PlanFileStore;
import com.netfieldpro.data.repository.ProjectRepository;
import com.netfieldpro.data.repository.QuotaGuard;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class S05ScaleTest {

    @Rule
    public NoAuthRule noAuthRule = new NoAuthRule();

    private static final String PROJ_ID = "AAAAAAAAAAAAAAAAAAAA01";
    private static final String FLOOR_ID = "AAAAAAAAAAAAAAAAAAAA11";

    private NetFieldDatabase db;
    private NetFieldDao dao;
    private BatchedWriter writer;
    private PlanFileStore planFileStore;
    private AtomicReference<Tier> fakeTier;
    private QuotaGuard quotaGuard;
    private ProjectRepository projectRepo;
    private FloorRepository floorRepo;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                NetFieldDatabase.class
        ).allowMainThreadQueries().build();
        dao = db.netFieldDao();
        writer = new BatchedWriter(db);
        planFileStore = new PlanFileStore(new File(ApplicationProvider.getApplicationContext().getFilesDir(), "test_plans_s05"));
        fakeTier = new AtomicReference<>(Tier.PRO);
        quotaGuard = new QuotaGuard(dao, fakeTier::get);
        projectRepo = new ProjectRepository(db, writer, planFileStore, quotaGuard);
        floorRepo = new FloorRepository(db, writer, planFileStore, quotaGuard);

        NetFieldApp.setProjectRepositoryForTest(projectRepo);
        NetFieldApp.setFloorRepositoryForTest(floorRepo);
        NetFieldApp.setQuotaGuardForTest(quotaGuard);
        NetFieldApp.setPlanFileStoreForTest(planFileStore);
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
        NetFieldApp.onboardingStore().markDone();

        ProjectRow p = new ProjectRow();
        p.id = PROJ_ID;
        p.name = "Test Project";
        p.createdAtMs = System.currentTimeMillis();
        dao.insertProject(p);

        FloorRow f = new FloorRow();
        f.id = FLOOR_ID;
        f.projectId = PROJ_ID;
        f.levelIndex = 1;
        f.planSha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        f.planWpx = 2000;
        f.planHpx = 1000;
        f.metersPerPixel = 0.0;
        f.scaleMethod = "LINE";
        f.scaleUncertaintyPct = 0.0;
        dao.insertFloor(f);
    }

    @After
    public void tearDown() {
        NetFieldApp.setProjectRepositoryForTest(null);
        NetFieldApp.setFloorRepositoryForTest(null);
        NetFieldApp.setQuotaGuardForTest(null);
        NetFieldApp.setPlanFileStoreForTest(null);
        writer.close();
        db.close();
    }

    private NavController navigateToS05(MainActivity activity) {
        NavHostFragment navHost =
                (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assertNotNull(navHost);
        navHost.getChildFragmentManager().executePendingTransactions();

        NavController nav = navHost.getNavController();
        Bundle args = new Bundle();
        args.putString("projectId", PROJ_ID);
        nav.navigate(R.id.action_s02_to_s03, args);
        navHost.getChildFragmentManager().executePendingTransactions();

        Bundle s05Args = new Bundle();
        s05Args.putString("floorId", FLOOR_ID);
        nav.navigate(R.id.nav_s05, s05Args);
        navHost.getChildFragmentManager().executePendingTransactions();
        return nav;
    }

    private ScaleFragment getS05Fragment(MainActivity activity) {
        NavHostFragment navHost =
                (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assertNotNull(navHost);
        return (ScaleFragment) navHost.getChildFragmentManager().getPrimaryNavigationFragment();
    }

    @Test
    public void testP05_1_initialStateHandlesAndSaveDisabled() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS05(activity);

                Button btnSave = activity.findViewById(R.id.btn_save_scale);
                assertNotNull(btnSave);
                assertFalse("Save button must be disabled initially", btnSave.isEnabled());

                PlanView planView = activity.findViewById(R.id.plan_view);
                assertNotNull(planView);

                // Handles must be at 25% and 75% of width, centered vertically:
                // Image is 2000x1000 -> (500, 500) and (1500, 500)
                assertEquals(500.0, planView.getHandle1X(), 1e-6);
                assertEquals(500.0, planView.getHandle1Y(), 1e-6);
                assertEquals(1500.0, planView.getHandle2X(), 1e-6);
                assertEquals(500.0, planView.getHandle2Y(), 1e-6);
            });
        }
    }

    @Test
    public void testP05_2_draggingHandlesAndSC1GeometryShowsReadout() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS05(activity);

                PlanView planView = activity.findViewById(R.id.plan_view);
                // Set SC-1 geometry: (100, 100) and (700, 500)
                planView.setHandles(100.0, 100.0, 700.0, 500.0);

                TextInputEditText editDistance = activity.findViewById(R.id.edit_distance);
                editDistance.setText("12.0");

                TextView readout = activity.findViewById(R.id.text_scale_readout);
                assertEquals(View.VISIBLE, readout.getVisibility());
                assertEquals("1 px = 0.016641 m · ±5.588%", readout.getText().toString());

                Button btnSave = activity.findViewById(R.id.btn_save_scale);
                assertTrue("Save must be enabled with valid readout", btnSave.isEnabled());
            });
        }
    }

    @Test
    public void testP05_3_tooShortSpanShowsError() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS05(activity);

                PlanView planView = activity.findViewById(R.id.plan_view);
                // Image 2000x1000. 15% of 2000 is 300 px. Span of 299 px is too short.
                planView.setHandles(0.0, 0.0, 299.0, 0.0);

                TextInputEditText editDistance = activity.findViewById(R.id.edit_distance);
                editDistance.setText("12.0");

                TextView error = activity.findViewById(R.id.text_scale_error);
                assertEquals(View.VISIBLE, error.getVisibility());
                assertEquals(activity.getString(R.string.scale_err_short), error.getText().toString());

                Button btnSave = activity.findViewById(R.id.btn_save_scale);
                assertFalse("Save must be disabled on too short span", btnSave.isEnabled());
            });
        }
    }

    @Test
    public void testP05_4_distance04ShowsError() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS05(activity);

                PlanView planView = activity.findViewById(R.id.plan_view);
                planView.setHandles(500.0, 500.0, 1500.0, 500.0);

                TextInputEditText editDistance = activity.findViewById(R.id.edit_distance);
                editDistance.setText("0.4");

                TextView error = activity.findViewById(R.id.text_scale_error);
                assertEquals(View.VISIBLE, error.getVisibility());
                assertEquals(activity.getString(R.string.scale_err_distance), error.getText().toString());

                Button btnSave = activity.findViewById(R.id.btn_save_scale);
                assertFalse("Save must be disabled on invalid distance", btnSave.isEnabled());
            });
        }
    }

    @Test
    public void testP05_5_roomWidthModeShowsSC2Readout() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS05(activity);

                RadioButton radioWidth = activity.findViewById(R.id.radio_mode_width);
                radioWidth.setChecked(true);

                TextInputEditText editDistance = activity.findViewById(R.id.edit_distance);
                editDistance.setText("40");

                TextView readout = activity.findViewById(R.id.text_scale_readout);
                assertEquals(View.VISIBLE, readout.getVisibility());
                assertEquals("1 px = 0.020000 m · ±15.212%", readout.getText().toString());

                Button btnSave = activity.findViewById(R.id.btn_save_scale);
                assertTrue(btnSave.isEnabled());
            });
        }
    }

    @Test
    public void testP05_6_savePersistsScaleAndS03ShowsChip() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavController nav = navigateToS05(activity);

                PlanView planView = activity.findViewById(R.id.plan_view);
                planView.setHandles(100.0, 100.0, 700.0, 500.0);

                TextInputEditText editDistance = activity.findViewById(R.id.edit_distance);
                editDistance.setText("12.0");

                Button btnSave = activity.findViewById(R.id.btn_save_scale);
                btnSave.performClick();

                NavHostFragment navHost =
                        (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                navHost.getChildFragmentManager().executePendingTransactions();

                // Now back on S03 Project Detail
                assertEquals(R.id.nav_s03, nav.getCurrentDestination().getId());

                // Check DB
                FloorRow updatedRow = dao.getFloor(FLOOR_ID);
                assertNotNull(updatedRow);
                assertEquals(0.016641, updatedRow.metersPerPixel, 1e-6);
                assertEquals(5.588, updatedRow.scaleUncertaintyPct, 1e-3);

                // Check S03 UI shows scale chip
                RecyclerView recycler = activity.findViewById(R.id.recycler_floors);
                recycler.measure(
                        View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)
                );
                recycler.layout(0, 0, 1000, 2000);

                RecyclerView.ViewHolder vh = recycler.findViewHolderForAdapterPosition(0);
                assertNotNull(vh);
                TextView chip = vh.itemView.findViewById(R.id.chip_scale);
                assertEquals("Scale set · ±5.6%", chip.getText().toString());
            });
        }
    }

    @Test
    public void testP05_7_finalizedSessionLocksRecalibration() {
        // Seed a FINALIZED session for FLOOR_ID
        SurveySessionRow session = new SurveySessionRow();
        session.id = "AAAAAAAAAAAAAAAAAAAAS1";
        session.floorId = FLOOR_ID;
        session.name = "Finalized Survey";
        session.state = "FINALIZED";
        session.createdAtMs = System.currentTimeMillis();
        dao.insertSurveySession(session);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS05(activity);

                TextView textLocked = activity.findViewById(R.id.text_scale_locked);
                assertEquals(View.VISIBLE, textLocked.getVisibility());
                assertEquals(activity.getString(R.string.scale_locked), textLocked.getText().toString());

                TextInputEditText editDistance = activity.findViewById(R.id.edit_distance);
                editDistance.setText("12.0");

                Button btnSave = activity.findViewById(R.id.btn_save_scale);
                assertFalse("Save button must stay disabled when recalibration is locked", btnSave.isEnabled());
            });
        }
    }
}
