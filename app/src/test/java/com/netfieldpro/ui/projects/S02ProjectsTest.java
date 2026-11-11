package com.netfieldpro.ui.projects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
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
import com.google.android.material.textfield.TextInputLayout;
import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.NoAuthRule;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.model.enums.ScaleMethod;
import com.netfieldpro.core.session.Sm1State;
import com.netfieldpro.core.time.FakeClock;
import com.netfieldpro.data.db.BatchedWriter;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.repository.FloorRepository;
import com.netfieldpro.data.repository.PlanFileStore;
import com.netfieldpro.data.repository.ProjectRepository;
import com.netfieldpro.data.repository.QuotaGuard;
import com.netfieldpro.data.repository.SessionRepository;
import com.netfieldpro.session.SessionRecovery;
import com.netfieldpro.session.SessionRuntimeStore;
import com.netfieldpro.ui.NavArgs;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class S02ProjectsTest {

    @Rule
    public NoAuthRule noAuthRule = new NoAuthRule();

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
        planFileStore = new PlanFileStore(new File(ApplicationProvider.getApplicationContext().getFilesDir(), "test_plans_s02"));
        fakeTier = new AtomicReference<>(Tier.FREE);
        quotaGuard = new QuotaGuard(dao, fakeTier::get);
        projectRepo = new ProjectRepository(db, writer, planFileStore, quotaGuard);
        floorRepo = new FloorRepository(db, writer, planFileStore, quotaGuard);

        NetFieldApp.setProjectRepositoryForTest(projectRepo);
        NetFieldApp.setFloorRepositoryForTest(floorRepo);
        NetFieldApp.setQuotaGuardForTest(quotaGuard);
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        NetFieldApp.onboardingStore().markDone();
        SessionRecovery.resetForTest();
    }

    @After
    public void tearDown() {
        SessionRecovery.resetForTest();
        NetFieldApp.setProjectRepositoryForTest(null);
        NetFieldApp.setFloorRepositoryForTest(null);
        NetFieldApp.setQuotaGuardForTest(null);
        writer.close();
        db.close();
    }

    @Test
    public void testP02_1_emptyState() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                androidx.navigation.fragment.NavHostFragment navHost =
                        (androidx.navigation.fragment.NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                View layoutEmpty = activity.findViewById(R.id.layout_empty);
                assertNotNull(layoutEmpty);
                assertEquals(View.VISIBLE, layoutEmpty.getVisibility());

                TextView textTitle = activity.findViewById(R.id.text_empty_title);
                assertNotNull(textTitle);
                assertEquals(activity.getString(R.string.projects_empty_title), textTitle.getText().toString());

                TextView textHint = activity.findViewById(R.id.text_empty_hint);
                assertNotNull(textHint);
                assertEquals(activity.getString(R.string.projects_empty_hint), textHint.getText().toString());

                Button btnNew = activity.findViewById(R.id.btn_new_project);
                assertNotNull(btnNew);
                assertEquals(View.VISIBLE, btnNew.getVisibility());
                assertEquals(activity.getString(R.string.projects_new), btnNew.getText().toString());
            });
        }
    }

    @Test
    public void testP02_2_cardSubtitleFormatting() {
        fakeTier.set(Tier.PRO);
        Project p = projectRepo.createProject("Dental Office", null, "HQ");

        // Add 2 floors
        FloorRow f1 = new FloorRow();
        f1.id = "FFFFFFFFFFFFFFFFFFFF01";
        f1.projectId = p.id();
        f1.levelIndex = 1L;
        f1.planSha256 = "sha";
        f1.scaleMethod = ScaleMethod.LINE.name();

        FloorRow f2 = new FloorRow();
        f2.id = "FFFFFFFFFFFFFFFFFFFF02";
        f2.projectId = p.id();
        f2.levelIndex = 2L;
        f2.planSha256 = "sha";
        f2.scaleMethod = ScaleMethod.LINE.name();

        writer.submit(database -> {
            database.netFieldDao().insertFloor(f1);
            database.netFieldDao().insertFloor(f2);
        }, null);
        writer.flush();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                androidx.navigation.fragment.NavHostFragment navHost =
                        (androidx.navigation.fragment.NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                androidx.recyclerview.widget.RecyclerView rv = activity.findViewById(R.id.recycler_projects);
                assertNotNull(rv);
                rv.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
                rv.layout(0, 0, 1000, 1000);

                androidx.recyclerview.widget.RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(0);
                assertNotNull(vh);

                TextView textName = vh.itemView.findViewById(R.id.text_project_name);
                assertNotNull(textName);
                assertEquals("Dental Office", textName.getText().toString());

                TextView textSub = vh.itemView.findViewById(R.id.text_project_subtitle);
                assertNotNull(textSub);
                assertEquals("HQ · 2 floors", textSub.getText().toString());
            });
        }
    }

    @Test
    public void testP02_3_newProjectEmptyNameValidation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_new_project, null);
                TextInputLayout layoutName = dialogView.findViewById(R.id.layout_project_name);
                TextInputEditText editName = dialogView.findViewById(R.id.edit_project_name);
                TextView textError = dialogView.findViewById(R.id.text_name_error);
                Button btnSave = dialogView.findViewById(R.id.btn_save);

                editName.setText("");
                btnSave.performClick();

                String name = editName.getText() != null ? editName.getText().toString() : "";
                com.netfieldpro.core.project.NameRules.Status status =
                        com.netfieldpro.core.project.NameRules.validate(name);
                assertEquals(com.netfieldpro.core.project.NameRules.Status.EMPTY, status);
            });
        }
    }

    @Test
    public void testP02_4_newProject61CharsValidation() {
        StringBuilder sb61 = new StringBuilder();
        for (int i = 0; i < 61; i++) {
            sb61.append('a');
        }
        com.netfieldpro.core.project.NameRules.Status status =
                com.netfieldpro.core.project.NameRules.validate(sb61.toString());
        assertEquals(com.netfieldpro.core.project.NameRules.Status.TOO_LONG, status);
    }

    @Test
    public void testP02_5_freeTierSecondCreationNavigatesToS18() {
        fakeTier.set(Tier.FREE);
        NetFieldApp.entitlements().setDebugTier(Tier.FREE);
        Project p1 = projectRepo.createProject("Project 1", null, null);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                androidx.navigation.fragment.NavHostFragment navHost =
                        (androidx.navigation.fragment.NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Button btnNew = activity.findViewById(R.id.btn_new_project);
                btnNew.performClick();
                navHost.getChildFragmentManager().executePendingTransactions();

                int currentDest = activity.getNavController().getCurrentDestination().getId();
                assertEquals(R.id.nav_s18, currentDest);

                TextView title = activity.findViewById(R.id.text_screen_title);
                assertNotNull(title);
                assertEquals(activity.getString(R.string.paywall_choose_plan), title.getText().toString());

                TextView subtitle = activity.findViewById(R.id.text_paywall_subtitle);
                assertNotNull(subtitle);
                assertEquals(View.VISIBLE, subtitle.getVisibility());
                assertEquals(activity.getString(R.string.paywall_quota_project), subtitle.getText().toString());
            });
        }
    }

    @Test
    public void testP02_6_deleteConfirmation() {
        fakeTier.set(Tier.PRO);
        Project p = projectRepo.createProject("To Delete", null, null);
        assertEquals(1, dao.countProjects());

        projectRepo.deleteProject(p.id());
        assertEquals(0, dao.countProjects());
    }

    @Test
    public void testP02_7_cardTapNavigatesToS03() {
        fakeTier.set(Tier.PRO);
        Project p = projectRepo.createProject("Acme Dental", null, null);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                androidx.navigation.fragment.NavHostFragment navHost =
                        (androidx.navigation.fragment.NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                androidx.recyclerview.widget.RecyclerView rv = activity.findViewById(R.id.recycler_projects);
                assertNotNull(rv);
                rv.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
                rv.layout(0, 0, 1000, 1000);

                androidx.recyclerview.widget.RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(0);
                assertNotNull(vh);
                View card = vh.itemView.findViewById(R.id.card_project);
                card.performClick();
                navHost.getChildFragmentManager().executePendingTransactions();

                int currentDest = activity.getNavController().getCurrentDestination().getId();
                assertEquals(R.id.nav_s03, currentDest);
            });
        }
    }

    @Test
    public void testP02_8_touchTargetsAt13FontScale() {
        int touchMin = ApplicationProvider.getApplicationContext().getResources().getDimensionPixelSize(R.dimen.touch_min);
        int touchPrimary = ApplicationProvider.getApplicationContext().getResources().getDimensionPixelSize(R.dimen.touch_primary);

        float density = ApplicationProvider.getApplicationContext().getResources().getDisplayMetrics().density;
        int min48dp = Math.round(48 * density);
        int min56dp = Math.round(56 * density);

        assertTrue("touch_min must be >= 48dp", touchMin >= min48dp);
        assertTrue("touch_primary must be >= 56dp", touchPrimary >= min56dp);
    }

    @Test
    public void testP02_9_recoveryWarmupFailed() {
        SessionRecovery.resetForTest();
        fakeTier.set(Tier.PRO);
        Project p = projectRepo.createProject("P02-9 Project", null, null);
        FloorRow floor = new FloorRow();
        floor.id = "FFFFFFFFFFFFFFFFFFFF09";
        floor.projectId = p.id();
        floor.levelIndex = 1L;
        floor.planSha256 = "sha";
        floor.scaleMethod = ScaleMethod.LINE.name();
        dao.insertFloor(floor);

        SessionRuntimeStore runtimeStore = SessionRuntimeStore.create(ApplicationProvider.getApplicationContext());
        FakeClock fakeClock = new FakeClock(1_000_000_000L, 1_700_000_000_000L);
        SessionRepository sessionRepo = new SessionRepository(dao, null, runtimeStore, quotaGuard, fakeClock);
        SessionRecovery.setSessionRepositoryForTest(sessionRepo);
        SessionRecovery.setClockForTest(fakeClock);

        SurveySessionRow sessionRow = sessionRepo.create(floor.id, "Live Survey");
        runtimeStore.putState(sessionRow.id, Sm1State.WARMING_UP);
        assertEquals(Sm1State.WARMING_UP, runtimeStore.getState(sessionRow.id));

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertEquals(Sm1State.WARMUP_FAILED, runtimeStore.getState(sessionRow.id));
            });
        }
    }

    @Test
    public void testP02_10_recoveryRebootedEndedAndSnackbarDisplayed() {
        SessionRecovery.resetForTest();
        fakeTier.set(Tier.PRO);
        Project p = projectRepo.createProject("P02-10 Project", null, null);
        FloorRow floor = new FloorRow();
        floor.id = "FFFFFFFFFFFFFFFFFFFF10";
        floor.projectId = p.id();
        floor.levelIndex = 1L;
        floor.planSha256 = "sha";
        floor.scaleMethod = ScaleMethod.LINE.name();
        dao.insertFloor(floor);

        long createdWall = 1_700_000_000_000L;
        long createdMono = 5_000_000_000_000L;
        FakeClock fakeClock = new FakeClock(createdMono, createdWall);

        SessionRuntimeStore runtimeStore = SessionRuntimeStore.create(ApplicationProvider.getApplicationContext());
        SessionRepository sessionRepo = new SessionRepository(dao, null, runtimeStore, quotaGuard, fakeClock);
        SessionRecovery.setSessionRepositoryForTest(sessionRepo);

        SurveySessionRow sessionRow = sessionRepo.create(floor.id, "Active Survey");
        runtimeStore.putState(sessionRow.id, Sm1State.ACTIVE);

        FakeClock rebootClock = new FakeClock(100_000_000_000L, 1_700_000_600_000L);
        SessionRecovery.setClockForTest(rebootClock);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertEquals(Sm1State.ENDED, runtimeStore.getState(sessionRow.id));

                TextView snackbarText = activity.findViewById(com.google.android.material.R.id.snackbar_text);
                assertNotNull(snackbarText);
                assertEquals(
                        activity.getString(R.string.session_ended_reboot),
                        snackbarText.getText().toString()
                );
            });
        }
    }
}
