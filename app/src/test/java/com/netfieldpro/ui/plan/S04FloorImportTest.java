package com.netfieldpro.ui.plan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicReference;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
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
import com.netfieldpro.data.db.BatchedWriter;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.plan.PlanImporter;
import com.netfieldpro.data.repository.FloorRepository;
import com.netfieldpro.data.repository.PlanFileStore;
import com.netfieldpro.data.repository.ProjectRepository;
import com.netfieldpro.data.repository.QuotaGuard;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class S04FloorImportTest {

    @Rule
    public NoAuthRule noAuthRule = new NoAuthRule();

    private static final String PROJ_ID = "AAAAAAAAAAAAAAAAAAAA01";

    private NetFieldDatabase db;
    private NetFieldDao dao;
    private BatchedWriter writer;
    private PlanFileStore planFileStore;
    private AtomicReference<Tier> fakeTier;
    private QuotaGuard quotaGuard;
    private ProjectRepository projectRepo;
    private FloorRepository floorRepo;
    private File testDir;

    @Before
    public void setUp() {
        testDir = new File(ApplicationProvider.getApplicationContext().getFilesDir(), "test_s04");
        testDir.mkdirs();

        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                NetFieldDatabase.class
        ).allowMainThreadQueries().build();
        dao = db.netFieldDao();
        writer = new BatchedWriter(db);
        planFileStore = new PlanFileStore(new File(testDir, "plans"));
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

    private File createTestImage(String name, int width, int height) throws Exception {
        File file = new File(testDir, name);
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        }
        bmp.recycle();
        return file;
    }

    private NavController navigateToS04(MainActivity activity) {
        NavHostFragment navHost =
                (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assertNotNull(navHost);
        navHost.getChildFragmentManager().executePendingTransactions();

        NavController nav = navHost.getNavController();
        Bundle args = new Bundle();
        args.putString("projectId", PROJ_ID);
        nav.navigate(R.id.action_s02_to_s03, args);
        navHost.getChildFragmentManager().executePendingTransactions();

        nav.navigate(R.id.action_s03_to_s04, args);
        navHost.getChildFragmentManager().executePendingTransactions();
        return nav;
    }

    private FloorImportFragment getS04Fragment(MainActivity activity) {
        NavHostFragment navHost =
                (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assertNotNull(navHost);
        return (FloorImportFragment) navHost.getChildFragmentManager().getPrimaryNavigationFragment();
    }

    @Test
    public void testP04_1_initialState() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS04(activity);

                Button btnPick = activity.findViewById(R.id.btn_pick_file);
                assertNotNull(btnPick);
                assertEquals(View.VISIBLE, btnPick.getVisibility());

                TextView hint = activity.findViewById(R.id.text_import_hint);
                assertNotNull(hint);
                assertEquals(View.VISIBLE, hint.getVisibility());

                Button btnSave = activity.findViewById(R.id.btn_save);
                assertNotNull(btnSave);
                assertFalse("Save must be initially disabled", btnSave.isEnabled());

                TextInputEditText editName = activity.findViewById(R.id.edit_floor_name);
                assertNotNull(editName);
                assertFalse("Floor name must be initially disabled", editName.isEnabled());

                TextInputEditText editLevel = activity.findViewById(R.id.edit_floor_level);
                assertNotNull(editLevel);
                assertFalse("Floor level must be initially disabled", editLevel.isEnabled());

                View previewContainer = activity.findViewById(R.id.layout_preview_container);
                assertNotNull(previewContainer);
                assertEquals(View.GONE, previewContainer.getVisibility());
            });
        }
    }

    @Test
    public void testP04_2_validPickShowsPreviewAndEnablesFields() throws Exception {
        File validFile = createTestImage("valid_500x500.png", 500, 500);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS04(activity);
                FloorImportFragment fragment = getS04Fragment(activity);
                assertNotNull(fragment);

                fragment.onFilePicked(validFile, "image/png");

                View previewContainer = activity.findViewById(R.id.layout_preview_container);
                assertEquals(View.VISIBLE, previewContainer.getVisibility());

                TextInputEditText editName = activity.findViewById(R.id.edit_floor_name);
                assertTrue(editName.isEnabled());
                assertEquals("Level 0", editName.getText().toString());

                TextInputEditText editLevel = activity.findViewById(R.id.edit_floor_level);
                assertTrue(editLevel.isEnabled());
                assertEquals("0", editLevel.getText().toString());

                Button btnSave = activity.findViewById(R.id.btn_save);
                assertTrue(btnSave.isEnabled());
            });
        }
    }

    @Test
    public void testP04_3_unsupportedTypeShowsError() throws Exception {
        File dummy = new File(testDir, "test.gif");
        dummy.createNewFile();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS04(activity);
                FloorImportFragment fragment = getS04Fragment(activity);
                assertNotNull(fragment);

                fragment.onFilePicked(dummy, "image/gif");

                TextView error = activity.findViewById(R.id.text_error);
                assertEquals(View.VISIBLE, error.getVisibility());
                assertEquals(activity.getString(R.string.import_err_type), error.getText().toString());

                Button btnSave = activity.findViewById(R.id.btn_save);
                assertFalse(btnSave.isEnabled());
            });
        }
    }

    @Test
    public void testP04_4_tooLargeFileShowsError() throws Exception {
        File largeFile = new File(testDir, "large.png");
        try (RandomAccessFile raf = new RandomAccessFile(largeFile, "rw")) {
            raf.setLength(52428801); // 50MB + 1 byte
        }

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS04(activity);
                FloorImportFragment fragment = getS04Fragment(activity);
                assertNotNull(fragment);

                fragment.onFilePicked(largeFile, "image/png");

                TextView error = activity.findViewById(R.id.text_error);
                assertEquals(View.VISIBLE, error.getVisibility());
                assertEquals(activity.getString(R.string.import_err_size), error.getText().toString());

                Button btnSave = activity.findViewById(R.id.btn_save);
                assertFalse(btnSave.isEnabled());
            });
        }
    }

    @Test
    public void testP04_5_tooSmallImageShowsError() throws Exception {
        File smallFile = createTestImage("small_199x1000.png", 199, 1000);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS04(activity);
                FloorImportFragment fragment = getS04Fragment(activity);
                assertNotNull(fragment);

                fragment.onFilePicked(smallFile, "image/png");

                TextView error = activity.findViewById(R.id.text_error);
                assertEquals(View.VISIBLE, error.getVisibility());
                assertEquals(activity.getString(R.string.import_err_small), error.getText().toString());

                Button btnSave = activity.findViewById(R.id.btn_save);
                assertFalse(btnSave.isEnabled());
            });
        }
    }

    @Test
    public void testP04_6_level100ShowsErrorAndSaveDisabled() throws Exception {
        File validFile = createTestImage("valid.png", 600, 400);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                navigateToS04(activity);
                FloorImportFragment fragment = getS04Fragment(activity);
                fragment.onFilePicked(validFile, "image/png");

                Button btnSave = activity.findViewById(R.id.btn_save);
                assertTrue(btnSave.isEnabled());

                TextInputEditText editLevel = activity.findViewById(R.id.edit_floor_level);
                TextInputLayout layoutLevel = activity.findViewById(R.id.layout_floor_level);

                editLevel.setText("100");

                assertNotNull(layoutLevel.getError());
                assertEquals(activity.getString(R.string.import_err_level), layoutLevel.getError().toString());
                assertFalse("Level 100 must disable Save", btnSave.isEnabled());
            });
        }
    }

    @Test
    public void testP04_7_quotaDenialGoesToS18AndDeletesFile() throws Exception {
        File validFile = createTestImage("quota_test.png", 800, 600);
        fakeTier.set(Tier.FREE);

        // Seed 1 floor to exhaust Free tier (quota is 1)
        FloorRow existing = new FloorRow();
        existing.id = "AAAAAAAAAAAAAAAAAAAAF1";
        existing.projectId = PROJ_ID;
        existing.levelIndex = 1;
        existing.planSha256 = "abc";
        existing.planWpx = 800;
        existing.planHpx = 600;
        dao.insertFloor(existing);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavController nav = navigateToS04(activity);
                FloorImportFragment fragment = getS04Fragment(activity);
                fragment.onFilePicked(validFile, "image/png");

                Button btnSave = activity.findViewById(R.id.btn_save);
                btnSave.performClick();

                assertEquals(R.id.nav_s18, nav.getCurrentDestination().getId());
                assertNotNull(nav.getCurrentBackStackEntry());
                assertEquals("FLOOR", nav.getCurrentBackStackEntry().getArguments().getString("quota"));

                // Verify temp files are deleted
                assertEquals(0, planFileStore.listFileNames().size());
            });
        }
    }

    @Test
    public void testP04_8_saveSuccessCreatesFloorAndOpensS05() throws Exception {
        File validFile = createTestImage("save_success.png", 1000, 800);
        fakeTier.set(Tier.PRO);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavController nav = navigateToS04(activity);
                FloorImportFragment fragment = getS04Fragment(activity);
                fragment.onFilePicked(validFile, "image/png");

                Button btnSave = activity.findViewById(R.id.btn_save);
                btnSave.performClick();

                assertEquals(R.id.nav_s05, nav.getCurrentDestination().getId());
                assertNotNull(nav.getCurrentBackStackEntry());
                String floorId = nav.getCurrentBackStackEntry().getArguments().getString("floorId");
                assertNotNull(floorId);

                // Check Room has the floor row
                FloorRow savedRow = dao.getFloor(floorId);
                assertNotNull(savedRow);
                assertEquals(PROJ_ID, savedRow.projectId);
                assertEquals(1000, savedRow.planWpx);
                assertEquals(800, savedRow.planHpx);

                // Verify file exists and its hash equals planSha256
                File savedFile = planFileStore.findPlanFile(floorId);
                assertNotNull(savedFile);
                assertTrue(savedFile.exists());
                try {
                    String actualHash = PlanImporter.computeSha256Hex(savedFile);
                    assertEquals(savedRow.planSha256, actualHash);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
