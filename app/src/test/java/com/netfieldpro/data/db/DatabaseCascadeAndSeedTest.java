package com.netfieldpro.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.AccessPointMarkerRow;
import com.netfieldpro.data.db.entity.ApGroupRow;
import com.netfieldpro.data.db.entity.AssessmentRow;
import com.netfieldpro.data.db.entity.ConnectionEventRow;
import com.netfieldpro.data.db.entity.FloorBoundaryRow;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.LatencyResultRow;
import com.netfieldpro.data.db.entity.LinkSampleRow;
import com.netfieldpro.data.db.entity.MeasurementRow;
import com.netfieldpro.data.db.entity.PathSegmentRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.RawSampleRow;
import com.netfieldpro.data.db.entity.ScanRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.data.db.entity.WallMaterialRow;
import com.netfieldpro.data.db.entity.WallRow;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DatabaseCascadeAndSeedTest {

    private NetFieldDatabase db;
    private NetFieldDao dao;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                NetFieldDatabase.class
        ).addCallback(NetFieldDatabase.SEED_CALLBACK).allowMainThreadQueries().build();
        dao = db.netFieldDao();
        // Trigger database creation by querying wall materials
        dao.countWallMaterials();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void testDB1_cascadeDeleteProject() {
        String pId = "AAAAAAAAAAAAAAAAAAAAAA";
        String fId = "BBBBBBBBBBBBBBBBBBBBBB";
        String sId = "CCCCCCCCCCCCCCCCCCCCCC";
        String scanId = "DDDDDDDDDDDDDDDDDDDDDD";
        String ptId = "EEEEEEEEEEEEEEEEEEEEEE";

        // Insert project
        ProjectRow p = new ProjectRow();
        p.id = pId;
        p.name = "Project";
        p.createdAtMs = 1000L;
        dao.insertProject(p);

        // Insert floor
        FloorRow f = new FloorRow();
        f.id = fId;
        f.projectId = pId;
        f.levelIndex = 1L;
        f.planSha256 = "sha";
        f.planWpx = 100;
        f.planHpx = 100;
        f.metersPerPixel = 0.05;
        f.scaleMethod = "LINE";
        f.scaleUncertaintyPct = 1.0;
        f.scaleChecked = true;
        dao.insertFloor(f);

        // Floor children: FloorBoundary, Wall, AccessPointMarker
        FloorBoundaryRow fb = new FloorBoundaryRow();
        fb.floorId = fId;
        fb.vertices = "[]";
        dao.insertFloorBoundary(fb);

        WallRow w = new WallRow();
        w.id = "W111111111111111111111";
        w.floorId = fId;
        w.materialId = "DRYWALL_LIGHT";
        w.vertices = "[]";
        dao.insertWall(w);

        AccessPointMarkerRow apm = new AccessPointMarkerRow();
        apm.id = "APM1111111111111111111";
        apm.floorId = fId;
        apm.x = 1.0;
        apm.y = 1.0;
        dao.insertAccessPointMarker(apm);

        // Insert survey session
        SurveySessionRow s = new SurveySessionRow();
        s.id = sId;
        s.floorId = fId;
        s.name = "Session";
        s.state = "DRAFT";
        s.createdAtMs = 1000L;
        s.monoEpochNs = 1000L;
        s.schemaVersion = 1;
        s.appVersion = "1.0";
        s.algoVersion = "1.0";
        s.throttleAtStart = false;
        s.timingQuality = "GOOD";
        s.evidenceGrade = "VERDICT";
        s.sessionType = "STANDARD";
        s.intendedProfileId = "PROF111111111111111111";
        s.intendedProfileChanges = 0L;
        s.rssiOffsetDb = 0.0;
        s.linkCadenceMs = 1000L;
        s.cadenceFlag = "DEFAULT";
        s.probeMethod = "ICMP";
        s.referenceFreqsMhz = "[]";
        s.receiverId = "rx";
        s.deviceInfoId = "DEV1111111111111111111";
        s.capabilityReport = "{}";
        dao.insertSurveySession(s);

        // Session children: Scan, TargetNetwork, ApGroup, LinkSample, ConnectionEvent, PathSegment, Assessment
        ScanRow scan = new ScanRow();
        scan.id = scanId;
        scan.sessionId = sId;
        scan.sweepClass = "FULL";
        scan.startNs = 1000L;
        scan.endNs = 2000L;
        scan.arrivalNs = 2050L;
        scan.tsPlausible = true;
        scan.throttleOffAtScan = true;
        dao.insertScan(scan);

        RawSampleRow r = new RawSampleRow();
        r.scanId = scanId;
        r.bssid = "00:11:22:33:44:55";
        r.ssid = "TestSSID";
        r.freqMHz = 2412;
        r.rssi = -60;
        dao.insertRawSample(r);

        TargetNetworkRow tn = new TargetNetworkRow();
        tn.id = "TN11111111111111111111";
        tn.sessionId = sId;
        tn.ssid = "TestSSID";
        dao.insertTargetNetwork(tn);

        ApGroupRow apg = new ApGroupRow();
        apg.id = "APG1111111111111111111";
        apg.sessionId = sId;
        apg.label = "Group 1";
        apg.origin = "HEURISTIC";
        apg.memberBssids = "[]";
        dao.insertApGroup(apg);

        LinkSampleRow ls = new LinkSampleRow();
        ls.id = "LS11111111111111111111";
        ls.sessionId = sId;
        ls.observedAtNs = 1000L;
        ls.bssid = "00:11:22:33:44:55";
        ls.ssid = "TestSSID";
        ls.rssi = -65;
        ls.freqMHz = 5180;
        dao.insertLinkSample(ls);

        ConnectionEventRow ce = new ConnectionEventRow();
        ce.id = "CE11111111111111111111";
        ce.sessionId = sId;
        ce.type = "CONNECT";
        ce.atNs = 1000L;
        dao.insertConnectionEvent(ce);

        PathSegmentRow ps = new PathSegmentRow();
        ps.id = "PS11111111111111111111";
        ps.sessionId = sId;
        ps.state = "DONE";
        ps.rawStartNs = 1000L;
        ps.rawEndNs = 2000L;
        ps.trimStartNs = 1000L;
        ps.trimEndNs = 2000L;
        ps.speedMps = 1.0;
        ps.waypoints = "[]";
        ps.anchors = "[]";
        ps.mode = "ROUTE";
        dao.insertPathSegment(ps);

        AssessmentRow as = new AssessmentRow();
        as.assessmentId = "AS1111111111111111111";
        as.sessionId = sId;
        as.evidenceSha256 = "sha";
        as.profileId = "PROF111111111111111111";
        as.profileSnapshot = "{}";
        as.intendedProfileId = "PROF111111111111111111";
        as.isReassessment = false;
        as.thresholds = "{}";
        as.computedMetrics = "{}";
        as.criteria = "[]";
        as.verdict = "PASS";
        as.assessedAtMs = 1000L;
        as.algoVersion = "1.0";
        as.gridG = 0.5;
        as.publicKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        as.sig = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        dao.insertAssessment(as);

        // SurveyPoint and its children: Measurement, LatencyResult
        SurveyPointRow pt = new SurveyPointRow();
        pt.id = ptId;
        pt.sessionId = sId;
        pt.x = 5.0;
        pt.y = 5.0;
        pt.source = "TAP";
        pt.quality = "OK";
        pt.tMonoNs = 1000L;
        pt.tWallMs = 1000L;
        pt.nScans = 1;
        pt.positionUncertaintyM = 0.5;
        dao.insertSurveyPoint(pt);

        MeasurementRow m = new MeasurementRow();
        m.id = "M111111111111111111111";
        m.pointId = ptId;
        m.protocol = "WIFI";
        m.bssidOrId = "00:11:22:33:44:55";
        m.freqMHz = 5180;
        m.channel = 36;
        m.source = "SCAN";
        m.receiverId = "builtin";
        m.n = 1;
        m.rssiMedian = -60.0;
        m.rssiMin = -60;
        m.rssiMax = -60;
        dao.insertMeasurement(m);

        LatencyResultRow lr = new LatencyResultRow();
        lr.id = "LR11111111111111111111";
        lr.pointId = ptId;
        lr.method = "ICMP";
        lr.target = "8.8.8.8";
        lr.boundIface = "wlan0";
        lr.scope = "GATEWAY";
        lr.sent = 1;
        lr.recv = 1;
        lr.lossCount = 0;
        lr.duringScan = false;
        lr.connectedBssid = "00:11:22:33:44:55";
        lr.ssid = "TestSSID";
        lr.rtts = "[]";
        dao.insertLatencyResult(lr);

        // Verify all 16 cascaded tables have 1 row
        assertEquals(1, dao.countProjects());
        assertEquals(1, dao.countFloors());
        assertEquals(1, dao.countFloorBoundaries());
        assertEquals(1, dao.countWalls());
        assertEquals(1, dao.countAccessPointMarkers());
        assertEquals(1, dao.countSurveySessions());
        assertEquals(1, dao.countScans());
        assertEquals(1, dao.countRawSamples());
        assertEquals(1, dao.countTargetNetworks());
        assertEquals(1, dao.countApGroups());
        assertEquals(1, dao.countLinkSamples());
        assertEquals(1, dao.countConnectionEvents());
        assertEquals(1, dao.countPathSegments());
        assertEquals(1, dao.countAssessments());
        assertEquals(1, dao.countSurveyPoints());
        assertEquals(1, dao.countMeasurements());
        assertEquals(1, dao.countLatencyResults());

        // Delete project
        dao.deleteProject(pId);

        // Assert all 16 tables cascaded to 0 rows
        assertEquals(0, dao.countProjects());
        assertEquals(0, dao.countFloors());
        assertEquals(0, dao.countFloorBoundaries());
        assertEquals(0, dao.countWalls());
        assertEquals(0, dao.countAccessPointMarkers());
        assertEquals(0, dao.countSurveySessions());
        assertEquals(0, dao.countScans());
        assertEquals(0, dao.countRawSamples());
        assertEquals(0, dao.countTargetNetworks());
        assertEquals(0, dao.countApGroups());
        assertEquals(0, dao.countLinkSamples());
        assertEquals(0, dao.countConnectionEvents());
        assertEquals(0, dao.countPathSegments());
        assertEquals(0, dao.countAssessments());
        assertEquals(0, dao.countSurveyPoints());
        assertEquals(0, dao.countMeasurements());
        assertEquals(0, dao.countLatencyResults());
    }

    @Test
    public void testDB2_seededWallMaterials() {
        List<WallMaterialRow> list = dao.getAllWallMaterials();
        assertEquals(5, list.size());

        Map<String, WallMaterialRow> map = list.stream()
                .collect(Collectors.toMap(r -> r.materialId, r -> r));

        assertMaterial(map.get("DRYWALL_LIGHT"), 3.0, 3.0, 4.5);
        assertMaterial(map.get("GLASS_PARTITION"), 2.5, 2.5, 4.0);
        assertMaterial(map.get("BRICK_MEDIUM"), 8.0, 8.0, 12.0);
        assertMaterial(map.get("CONCRETE_HEAVY"), 15.0, 12.0, 18.0);
        assertMaterial(map.get("METAL_ELEVATOR"), 25.0, 20.0, 28.0);
    }

    private void assertMaterial(WallMaterialRow row, double penaltyM, double lossDb24, double lossDb5) {
        assertNotNull(row);
        assertEquals(penaltyM, row.penaltyM, 1e-6);
        assertEquals(lossDb24, row.lossDb24, 1e-6);
        assertEquals(lossDb5, row.lossDb5, 1e-6);
    }

    @Test
    public void testDB3_schemaJsonAndNoDestructiveMigration() throws IOException {
        // 1. Schema JSON file exists
        File schemaFile = new File("schemas/com.netfieldpro.data.db.NetFieldDatabase/1.json");
        if (!schemaFile.exists()) {
            schemaFile = new File("app/schemas/com.netfieldpro.data.db.NetFieldDatabase/1.json");
        }
        assertTrue("Schema JSON must exist at " + schemaFile.getAbsolutePath(), schemaFile.exists());

        // 2. fallbackToDestructiveMigration must not appear anywhere in app/src/main/java (including comments)
        File mainJavaDir = new File("src/main/java");
        if (!mainJavaDir.exists()) {
            mainJavaDir = new File("app/src/main/java");
        }
        assertTrue("main java dir must exist", mainJavaDir.exists());

        try (var stream = Files.walk(mainJavaDir.toPath())) {
            List<File> files = stream.filter(p -> p.toString().endsWith(".java"))
                    .map(java.nio.file.Path::toFile)
                    .collect(Collectors.toList());

            for (File file : files) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                assertFalse("fallbackToDestructiveMigration forbidden in " + file.getName(),
                        content.contains("fallbackToDestructiveMigration"));
            }
        }
    }
}
