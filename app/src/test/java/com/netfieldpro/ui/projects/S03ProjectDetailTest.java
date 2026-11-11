package com.netfieldpro.ui.projects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.NoAuthRule;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.data.db.BatchedWriter;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.repository.FloorRepository;
import com.netfieldpro.data.repository.PlanFileStore;
import com.netfieldpro.data.repository.ProjectRepository;
import com.netfieldpro.data.repository.QuotaGuard;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class S03ProjectDetailTest {

    @Rule
    public NoAuthRule noAuthRule = new NoAuthRule();

    private static final String PROJ_ID = "AAAAAAAAAAAAAAAAAAAA01";
    private static final String FLOOR_1_ID = "AAAAAAAAAAAAAAAAAAAA11";
    private static final String FLOOR_2_ID = "AAAAAAAAAAAAAAAAAAAA12";
    private static final String FLOOR_3_ID = "AAAAAAAAAAAAAAAAAAAA13";

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
        planFileStore = new PlanFileStore(new File(ApplicationProvider.getApplicationContext().getFilesDir(), "test_plans_s03"));
        fakeTier = new AtomicReference<>(Tier.FREE);
        quotaGuard = new QuotaGuard(dao, fakeTier::get);
        projectRepo = new ProjectRepository(db, writer, planFileStore, quotaGuard);
        floorRepo = new FloorRepository(db, writer, planFileStore, quotaGuard);

        NetFieldApp.setProjectRepositoryForTest(projectRepo);
        NetFieldApp.setFloorRepositoryForTest(floorRepo);
        NetFieldApp.setQuotaGuardForTest(quotaGuard);
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        NetFieldApp.onboardingStore().markDone();
    }

    @After
    public void tearDown() {
        NetFieldApp.setProjectRepositoryForTest(null);
        NetFieldApp.setFloorRepositoryForTest(null);
        NetFieldApp.setQuotaGuardForTest(null);
        writer.close();
        db.close();
    }

    private void seedProject(String id, String name, String site) {
        com.netfieldpro.data.db.entity.ProjectRow p = new com.netfieldpro.data.db.entity.ProjectRow();
        p.id = id;
        p.name = name;
        p.site = site;
        p.createdAtMs = System.currentTimeMillis();
        dao.insertProject(p);
    }

    private void seedFloor(String id, String projectId, long level, double mpp, double uncertainty) {
        FloorRow f = new FloorRow();
        f.id = id;
        f.projectId = projectId;
        f.levelIndex = level;
        f.planSha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        f.planWpx = 2000;
        f.planHpx = 1000;
        f.metersPerPixel = mpp;
        f.scaleMethod = "LINE";
        f.scaleUncertaintyPct = uncertainty;
        dao.insertFloor(f);
    }

    private NavController navigateToS03(MainActivity activity, String projId) {
        NavHostFragment navHost =
                (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assertNotNull(navHost);
        navHost.getChildFragmentManager().executePendingTransactions();

        NavController nav = navHost.getNavController();
        Bundle args = new Bundle();
        args.putString("projectId", projId);
        nav.navigate(R.id.action_s02_to_s03, args);
        navHost.getChildFragmentManager().executePendingTransactions();
        return nav;
    }

    @Test
    public void testP03_1_floorsRenderInLevelOrder() {
        seedProject(PROJ_ID, "Dental Office", "HQ");
        seedFloor(FLOOR_1_ID, PROJ_ID, 2, 0.02, 5.0);
        seedFloor(FLOOR_2_ID, PROJ_ID, -1, 0.02, 5.0);
        seedFloor(FLOOR_3_ID, PROJ_ID, 1, 0.02, 5.0);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS03(activity, PROJ_ID);

                TextView header = activity.findViewById(R.id.text_project_header);
                assertNotNull(header);
                assertEquals("Dental Office · HQ", header.getText().toString());

                RecyclerView recycler = activity.findViewById(R.id.recycler_floors);
                assertEquals(View.VISIBLE, recycler.getVisibility());
                assertNotNull(recycler.getAdapter());
                assertEquals(3, recycler.getAdapter().getItemCount());

                recycler.measure(
                        View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)
                );
                recycler.layout(0, 0, 1000, 2000);

                // Adapter items should be sorted by level: -1, 1, 2
                RecyclerView.ViewHolder vh0 = recycler.findViewHolderForAdapterPosition(0);
                assertNotNull(vh0);
                TextView level0 = vh0.itemView.findViewById(R.id.text_floor_level);
                assertEquals("Level -1", level0.getText().toString());

                RecyclerView.ViewHolder vh1 = recycler.findViewHolderForAdapterPosition(1);
                assertNotNull(vh1);
                TextView level1 = vh1.itemView.findViewById(R.id.text_floor_level);
                assertEquals("Level 1", level1.getText().toString());

                RecyclerView.ViewHolder vh2 = recycler.findViewHolderForAdapterPosition(2);
                assertNotNull(vh2);
                TextView level2 = vh2.itemView.findViewById(R.id.text_floor_level);
                assertEquals("Level 2", level2.getText().toString());
            });
        }
    }

    @Test
    public void testP03_2_scaleSetAndMissingChips() {
        seedProject(PROJ_ID, "Clinic", null);
        seedFloor(FLOOR_1_ID, PROJ_ID, 1, 0.016641, 5.588);
        seedFloor(FLOOR_2_ID, PROJ_ID, 2, 0.0, 0.0);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS03(activity, PROJ_ID);

                RecyclerView recycler = activity.findViewById(R.id.recycler_floors);
                recycler.measure(
                        View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)
                );
                recycler.layout(0, 0, 1000, 2000);

                RecyclerView.ViewHolder vh0 = recycler.findViewHolderForAdapterPosition(0);
                assertNotNull(vh0);
                TextView chip0 = vh0.itemView.findViewById(R.id.chip_scale);
                assertEquals("Scale set · ±5.6%", chip0.getText().toString());

                RecyclerView.ViewHolder vh1 = recycler.findViewHolderForAdapterPosition(1);
                assertNotNull(vh1);
                TextView chip1 = vh1.itemView.findViewById(R.id.chip_scale);
                assertEquals("Scale not set", chip1.getText().toString());
            });
        }
    }

    @Test
    public void testP03_3_setScaleOpensS05WithFloorId() {
        seedProject(PROJ_ID, "Warehouse", null);
        seedFloor(FLOOR_1_ID, PROJ_ID, 0, 0.0, 0.0);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavController nav = navigateToS03(activity, PROJ_ID);

                RecyclerView recycler = activity.findViewById(R.id.recycler_floors);
                recycler.measure(
                        View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)
                );
                recycler.layout(0, 0, 1000, 2000);

                RecyclerView.ViewHolder vh0 = recycler.findViewHolderForAdapterPosition(0);
                assertNotNull(vh0);
                Button btnSetScale = vh0.itemView.findViewById(R.id.btn_set_scale);
                btnSetScale.performClick();

                assertEquals(R.id.nav_s05, nav.getCurrentDestination().getId());
                assertNotNull(nav.getCurrentBackStackEntry());
                assertNotNull(nav.getCurrentBackStackEntry().getArguments());
                assertEquals(FLOOR_1_ID, nav.getCurrentBackStackEntry().getArguments().getString("floorId"));
            });
        }
    }

    @Test
    public void testP03_4_startSurveyVisibility() {
        seedProject(PROJ_ID, "Office", null);
        seedFloor(FLOOR_1_ID, PROJ_ID, 1, 0.02, 5.0);
        seedFloor(FLOOR_2_ID, PROJ_ID, 2, 0.0, 0.0);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavController nav = navigateToS03(activity, PROJ_ID);

                RecyclerView recycler = activity.findViewById(R.id.recycler_floors);
                recycler.measure(
                        View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)
                );
                recycler.layout(0, 0, 1000, 2000);

                // Floor 1 has scale -> Start survey visible
                RecyclerView.ViewHolder vh0 = recycler.findViewHolderForAdapterPosition(0);
                assertNotNull(vh0);
                Button btnStart0 = vh0.itemView.findViewById(R.id.btn_start_survey);
                assertEquals(View.VISIBLE, btnStart0.getVisibility());

                // Click start survey -> navigates to S06 with floorId
                btnStart0.performClick();
                assertEquals(R.id.nav_s06, nav.getCurrentDestination().getId());
                assertNotNull(nav.getCurrentBackStackEntry());
                assertEquals(FLOOR_1_ID, nav.getCurrentBackStackEntry().getArguments().getString("floorId"));

                // Navigate back to S03
                nav.popBackStack();
                NavHostFragment navHost =
                        (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                navHost.getChildFragmentManager().executePendingTransactions();

                // Floor 2 does not have scale -> Start survey GONE
                RecyclerView.ViewHolder vh1 = recycler.findViewHolderForAdapterPosition(1);
                assertNotNull(vh1);
                Button btnStart1 = vh1.itemView.findViewById(R.id.btn_start_survey);
                assertEquals(View.GONE, btnStart1.getVisibility());
            });
        }
    }

    @Test
    public void testP03_5_freeTierSecondFloorNavigatesToS18WithQuotaFloor() {
        seedProject(PROJ_ID, "Tower", null);
        // Free tier floor limit is 1. Seed 1 floor to consume quota.
        seedFloor(FLOOR_1_ID, PROJ_ID, 1, 0.02, 5.0);
        fakeTier.set(Tier.FREE);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavController nav = navigateToS03(activity, PROJ_ID);

                Button btnAddFloor = activity.findViewById(R.id.btn_add_floor);
                btnAddFloor.performClick();

                assertEquals(R.id.nav_s18, nav.getCurrentDestination().getId());
                assertNotNull(nav.getCurrentBackStackEntry());
                assertNotNull(nav.getCurrentBackStackEntry().getArguments());
                assertEquals("FLOOR", nav.getCurrentBackStackEntry().getArguments().getString("quota"));
            });
        }
    }

    @Test
    public void testP03_6_emptyProjectShowsHintAndAddFloorButton() {
        seedProject(PROJ_ID, "Empty Space", null);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS03(activity, PROJ_ID);

                View layoutEmpty = activity.findViewById(R.id.layout_empty);
                assertEquals(View.VISIBLE, layoutEmpty.getVisibility());

                TextView hint = activity.findViewById(R.id.text_empty_hint);
                assertTrue(hint.getText().toString().contains("floor plan"));

                Button btnAddFloor = activity.findViewById(R.id.btn_add_floor);
                assertEquals(View.VISIBLE, btnAddFloor.getVisibility());

                RecyclerView recycler = activity.findViewById(R.id.recycler_floors);
                assertEquals(View.GONE, recycler.getVisibility());
            });
        }
    }
}
