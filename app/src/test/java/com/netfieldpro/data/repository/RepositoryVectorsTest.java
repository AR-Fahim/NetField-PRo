package com.netfieldpro.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.model.enums.CadenceFlag;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.model.enums.ScaleMethod;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.model.enums.SessionType;
import com.netfieldpro.core.model.enums.TimingQuality;
import com.netfieldpro.data.db.BatchedWriter;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class RepositoryVectorsTest {

    private NetFieldDatabase db;
    private NetFieldDao dao;
    private BatchedWriter writer;
    private PlanFileStore planFileStore;
    private AtomicReference<Tier> fakeTier;
    private QuotaGuard quotaGuard;
    private ProjectRepository projectRepo;
    private FloorRepository floorRepo;
    private File plansDir;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                NetFieldDatabase.class
        ).allowMainThreadQueries().build();
        dao = db.netFieldDao();
        writer = new BatchedWriter(db);

        plansDir = new File(ApplicationProvider.getApplicationContext().getFilesDir(), "test_plans");
        if (plansDir.exists()) {
            File[] files = plansDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            plansDir.delete();
        }
        plansDir.mkdirs();
        planFileStore = new PlanFileStore(plansDir);

        fakeTier = new AtomicReference<>(Tier.FREE);
        quotaGuard = new QuotaGuard(dao, fakeTier::get);

        projectRepo = new ProjectRepository(db, writer, planFileStore, quotaGuard);
        floorRepo = new FloorRepository(db, writer, planFileStore, quotaGuard);
    }

    @After
    public void tearDown() {
        writer.close();
        db.close();
        if (plansDir.exists()) {
            File[] files = plansDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            plansDir.delete();
        }
    }

    @Test
    public void testVectorRP1() {
        // Create a project: the row exists, and the list is ordered per SO-A
        // Projects (created 5,"b"), (9,"a"), (9,"c") -> [a, c, b]
        String idB = "AAAAAAAAAAAAAAAAAAAAAb";
        String idA = "AAAAAAAAAAAAAAAAAAAAAa";
        String idC = "AAAAAAAAAAAAAAAAAAAAAc";

        writer.submit(database -> {
            database.netFieldDao().insertProject(new ProjectRow(idB, "B", null, null, 5L));
            database.netFieldDao().insertProject(new ProjectRow(idA, "A", null, null, 9L));
            database.netFieldDao().insertProject(new ProjectRow(idC, "C", null, null, 9L));
        }, null);
        writer.flush();

        ProjectRow rowA = dao.getProject(idA);
        assertNotNull(rowA);
        assertEquals("A", rowA.name);

        List<Project> list = projectRepo.getProjects();
        assertEquals(3, list.size());
        assertEquals(idA, list.get(0).id());
        assertEquals(idC, list.get(1).id());
        assertEquals(idB, list.get(2).id());
    }

    @Test
    public void testVectorRP2() {
        // A Free-tier second project is denied (QuotaGuard). After deleting the first, creating a project is allowed
        fakeTier.set(Tier.FREE);

        Project p1 = projectRepo.createProject("Project 1", null, null);
        assertNotNull(p1);

        try {
            projectRepo.createProject("Project 2", null, null);
            fail("Expected QuotaGuard to deny second project on FREE tier");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("PROJECT quota exceeded"));
        }

        projectRepo.deleteProject(p1.id());
        assertEquals(0, dao.countProjects());

        Project p2 = projectRepo.createProject("Project 2", null, null);
        assertNotNull(p2);
        assertEquals(1, dao.countProjects());
    }

    @Test
    public void testVectorRP3() throws IOException {
        // Delete a project: its floors and plan files disappear
        fakeTier.set(Tier.PRO);
        Project p = projectRepo.createProject("Acme Dental", null, null);

        String floorId = "BBBBBBBBBBBBBBBBBBBBBF";
        File planFile = planFileStore.savePlanBytes(floorId, "png", new byte[]{1, 2, 3});
        assertTrue(planFile.exists());

        Floor floor = new Floor(
                floorId, p.id(), 1L, "sha256dummy", 2000, 1000,
                0.02, null, ScaleMethod.LINE, 5.0, false, null, null, null, null, null
        );
        floorRepo.createFloor(floor, planFile);

        assertNotNull(dao.getFloor(floorId));
        assertTrue(planFile.exists());

        projectRepo.deleteProject(p.id());

        assertNull(dao.getProject(p.id()));
        assertNull(dao.getFloor(floorId));
        assertFalse(planFile.exists());
    }

    @Test
    public void testVectorRP4() throws IOException {
        // A Free-tier second floor is denied. Pro is allowed
        fakeTier.set(Tier.PRO);
        Project p = projectRepo.createProject("Project", null, null);

        fakeTier.set(Tier.FREE);
        String f1Id = "FFFFFFFFFFFFFFFFFFFF01";
        File f1File = planFileStore.savePlanBytes(f1Id, "png", new byte[]{1});
        Floor f1 = new Floor(f1Id, p.id(), 1L, "sha1", 1000, 1000, 0.02, null, ScaleMethod.LINE, 5.0, false, null, null, null, null, null);
        floorRepo.createFloor(f1, f1File);
        assertEquals(1, dao.countFloors());

        String f2Id = "FFFFFFFFFFFFFFFFFFFF02";
        File f2File = planFileStore.savePlanBytes(f2Id, "png", new byte[]{2});
        Floor f2 = new Floor(f2Id, p.id(), 2L, "sha2", 1000, 1000, 0.02, null, ScaleMethod.LINE, 5.0, false, null, null, null, null, null);
        try {
            floorRepo.createFloor(f2, f2File);
            fail("Expected QuotaGuard to deny second floor on FREE tier");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("FLOOR quota exceeded"));
        }
        assertFalse(f2File.exists());
        assertEquals(1, dao.countFloors());

        fakeTier.set(Tier.PRO);
        File f2FileRetry = planFileStore.savePlanBytes(f2Id, "png", new byte[]{2});
        floorRepo.createFloor(f2, f2FileRetry);
        assertEquals(2, dao.countFloors());
        assertTrue(f2FileRetry.exists());
    }

    @Test
    public void testVectorRP5() throws IOException {
        // A failed insert deletes the stored file
        fakeTier.set(Tier.PRO);
        String nonExistentProjId = "NONEXISTENTPROJID00001";
        String floorId = "FFFFFFFFFFFFFFFFFFFF99";
        File planFile = planFileStore.savePlanBytes(floorId, "png", new byte[]{9, 9});
        assertTrue(planFile.exists());

        Floor floor = new Floor(
                floorId, nonExistentProjId, 1L, "sha", 1000, 1000,
                0.02, null, ScaleMethod.LINE, 5.0, false, null, null, null, null, null
        );

        try {
            floorRepo.createFloor(floor, planFile);
            fail("Expected foreign key constraint violation on non-existent project ID");
        } catch (Exception e) {
            // Success: expected failure
        }

        assertFalse(planFile.exists());
        assertNull(planFileStore.findPlanFile(floorId));
    }

    @Test
    public void testVectorRP6() throws IOException {
        // OrphanSweeper runs at S02 start and removes only the orphan
        String floorA = "AAAAAAAAAAAAAAAAAAAAAA";
        String orphanB = "BBBBBBBBBBBBBBBBBBBBBB";
        String orphanC = "CCCCCCCCCCCCCCCCCCCCCC";

        File fileA = planFileStore.savePlanBytes(floorA, "png", new byte[]{1});
        File fileB = planFileStore.savePlanBytes(orphanB, "jpg", new byte[]{2});
        File fileC = new File(plansDir, orphanC + ".tmp");
        assertTrue(fileC.createNewFile());
        File notes = new File(plansDir, "notes.txt");
        assertTrue(notes.createNewFile());

        Set<String> existingFloors = new HashSet<>();
        existingFloors.add(floorA);

        int swept = planFileStore.sweepOrphans(existingFloors);
        assertEquals(2, swept);

        assertTrue(fileA.exists());
        assertTrue(notes.exists());
        assertFalse(fileB.exists());
        assertFalse(fileC.exists());
    }

    @Test
    public void testVectorRP7() {
        // Rename trims per NR-1 and persists
        fakeTier.set(Tier.PRO);
        Project p = projectRepo.createProject("Initial Name", null, null);

        projectRepo.renameProject(p.id(), "   Trimmed Name   ");

        Project updated = projectRepo.getProject(p.id());
        assertNotNull(updated);
        assertEquals("Trimmed Name", updated.name());
    }

    @Test
    public void testVectorRP8() throws IOException {
        // Scale save writes mpp (D6) and the uncertainty (D3), and recalibration is refused when a FINALIZED session is seeded
        fakeTier.set(Tier.PRO);
        Project p = projectRepo.createProject("Dental Project", null, null);
        String floorId = "FFFFFFFFFFFFFFFFFFFF08";
        File planFile = planFileStore.savePlanBytes(floorId, "png", new byte[]{1});
        Floor floor = new Floor(
                floorId, p.id(), 1L, "sha", 2000, 1000,
                0.01, null, ScaleMethod.LINE, 10.0, false, null, null, null, null, null
        );
        floorRepo.createFloor(floor, planFile);

        floorRepo.updateScale(
                floorId,
                0.016641002,
                5.58838,
                ScaleMethod.LINE,
                true,
                new Floor.Point(100.0, 100.0),
                new Floor.Point(700.0, 500.0)
        );

        Floor updated = floorRepo.getFloor(floorId);
        assertNotNull(updated);
        assertEquals(0.016641, updated.metersPerPixel(), 1e-6);
        assertEquals(5.588, updated.scaleUncertaintyPct(), 1e-3);
        assertTrue(updated.scaleChecked());
        assertEquals(100.0, updated.scaleP1().x(), 1e-6);
        assertEquals(100.0, updated.scaleP1().y(), 1e-6);
        assertEquals(700.0, updated.scaleP2().x(), 1e-6);
        assertEquals(500.0, updated.scaleP2().y(), 1e-6);

        // Seed a FINALIZED session
        SurveySessionRow session = new SurveySessionRow();
        session.id = "SSSSSSSSSSSSSSSSSSSS01";
        session.floorId = floorId;
        session.name = "Finalized Session";
        session.state = SessionState.FINALIZED.name();
        session.createdAtMs = System.currentTimeMillis();
        session.monoEpochNs = 1000L;
        session.schemaVersion = 1;
        session.appVersion = "1.0.0";
        session.algoVersion = "1.0.0";
        session.timingQuality = TimingQuality.GOOD.name();
        session.evidenceGrade = EvidenceGrade.VERDICT.name();
        session.sessionType = SessionType.STANDARD.name();
        session.intendedProfileId = "PROF1";
        session.cadenceFlag = CadenceFlag.MEASURED.name();
        session.probeMethod = ProbeMethod.ICMP.name();
        session.receiverId = "REC1";
        session.deviceInfoId = "DEV1";
        session.capabilityReport = "{}";
        session.finalizedAtMs = System.currentTimeMillis();
        session.evidenceSha256 = "sha256";
        session.publicKeyFp = "fp";

        writer.submit(database -> database.netFieldDao().insertSurveySession(session), null);
        writer.flush();

        assertTrue(floorRepo.isRecalibrationBlocked(floorId));

        try {
            floorRepo.updateScale(floorId, 0.02, 15.0, ScaleMethod.ROOM_WIDTH, false, null, null);
            fail("Expected updateScale to fail when floor has a FINALIZED session");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Scale locked"));
        }
    }
}
