package com.netfieldpro.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.netfieldpro.core.assess.PerPoint;
import com.netfieldpro.core.heat.Sample;
import com.netfieldpro.core.model.entity.FloorBoundary;
import com.netfieldpro.core.model.entity.LatencyResult;
import com.netfieldpro.core.model.entity.Measurement;
import com.netfieldpro.core.model.entity.Wall;
import com.netfieldpro.core.model.enums.LatencyMethod;
import com.netfieldpro.core.model.enums.LatencyScope;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.core.model.enums.MeasurementSource;
import com.netfieldpro.core.model.enums.Protocol;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.LatencyResultRow;
import com.netfieldpro.data.db.entity.MeasurementRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.data.db.entity.WallMaterialRow;
import com.netfieldpro.data.db.mapper.FloorBoundaryMapper;
import com.netfieldpro.data.db.mapper.LatencyResultMapper;
import com.netfieldpro.data.db.mapper.MeasurementMapper;
import com.netfieldpro.data.db.mapper.WallMapper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SurveyReadRepositoryTest {

    private NetFieldDatabase db;
    private NetFieldDao dao;
    private SurveyReadRepository repo;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                NetFieldDatabase.class
        ).allowMainThreadQueries().build();
        dao = db.netFieldDao();
        repo = new SurveyReadRepository(dao);
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void testNonExistentSessionReturnsNull() {
        assertNull(repo.loadSessionGraph(com.netfieldpro.core.util.IdGenerator.generate()));
    }

    @Test
    public void testLoadSessionGraph() {
        String projId = com.netfieldpro.core.util.IdGenerator.generate();
        String floorId = com.netfieldpro.core.util.IdGenerator.generate();
        String sessId = com.netfieldpro.core.util.IdGenerator.generate();
        String devId = com.netfieldpro.core.util.IdGenerator.generate();
        String profId = com.netfieldpro.core.util.IdGenerator.generate();
        String tnId = com.netfieldpro.core.util.IdGenerator.generate();
        String ptId = com.netfieldpro.core.util.IdGenerator.generate();
        String mId = com.netfieldpro.core.util.IdGenerator.generate();
        String latId = com.netfieldpro.core.util.IdGenerator.generate();
        String wallId = com.netfieldpro.core.util.IdGenerator.generate();

        // Project
        ProjectRow pRow = new ProjectRow();
        pRow.id = projId;
        pRow.name = "Demo Office";
        pRow.site = "HQ";
        pRow.createdAtMs = 1000L;
        dao.insertProject(pRow);

        // Floor
        FloorRow fRow = new FloorRow();
        fRow.id = floorId;
        fRow.projectId = projId;
        fRow.levelIndex = 1L;
        fRow.planSha256 = "sha256";
        fRow.planWpx = 1600;
        fRow.planHpx = 1000;
        fRow.metersPerPixel = 0.025;
        fRow.scaleMethod = "LINE";
        fRow.scaleUncertaintyPct = 0.1;
        fRow.scaleChecked = true;
        dao.insertFloor(fRow);

        // Boundary
        FloorBoundary boundary = new FloorBoundary(floorId, List.of(
                new FloorBoundary.Vertex(2.0, 2.0),
                new FloorBoundary.Vertex(38.0, 2.0),
                new FloorBoundary.Vertex(38.0, 23.0),
                new FloorBoundary.Vertex(2.0, 23.0)
        ));
        dao.insertFloorBoundary(FloorBoundaryMapper.toRow(boundary));

        // Wall
        Wall wall = new Wall(wallId, floorId, MaterialId.DRYWALL_LIGHT, List.of(
                new Wall.Vertex(20.0, 0.0),
                new Wall.Vertex(20.0, 25.0)
        ));
        dao.insertWall(WallMapper.toRow(wall));

        // WallMaterial
        WallMaterialRow wmRow = new WallMaterialRow();
        wmRow.materialId = "DRYWALL_LIGHT";
        wmRow.penaltyM = 3.0;
        wmRow.lossDb24 = 3.0;
        wmRow.lossDb5 = 4.0;
        dao.insertWallMaterial(wmRow);

        // Session
        SurveySessionRow sRow = new SurveySessionRow();
        sRow.id = sessId;
        sRow.floorId = floorId;
        sRow.name = "Survey 1";
        sRow.state = "DRAFT";
        sRow.createdAtMs = 1000L;
        sRow.monoEpochNs = 1000L;
        sRow.schemaVersion = 1;
        sRow.appVersion = "1.0";
        sRow.algoVersion = "1.0";
        sRow.throttleAtStart = false;
        sRow.timingQuality = "GOOD";
        sRow.evidenceGrade = "VERDICT";
        sRow.sessionType = "STANDARD";
        sRow.intendedProfileId = profId;
        sRow.intendedProfileChanges = 0;
        sRow.rssiOffsetDb = 0.0;
        sRow.linkCadenceMs = 3000L;
        sRow.cadenceFlag = "DEFAULT";
        sRow.probeMethod = "ICMP";
        sRow.referenceFreqsMhz = "[]";
        sRow.receiverId = "builtin-wifi";
        sRow.deviceInfoId = devId;
        sRow.capabilityReport = "{}";
        dao.insertSurveySession(sRow);

        // Target Network
        TargetNetworkRow tnRow = new TargetNetworkRow();
        tnRow.id = tnId;
        tnRow.sessionId = sessId;
        tnRow.ssid = "OfficeNet";
        dao.insertTargetNetwork(tnRow);

        // Survey Point
        SurveyPointRow ptRow = new SurveyPointRow();
        ptRow.id = ptId;
        ptRow.sessionId = sessId;
        ptRow.x = 4.0;
        ptRow.y = 3.0;
        ptRow.source = "TAP";
        ptRow.quality = "OK";
        ptRow.tMonoNs = 1000L;
        ptRow.tWallMs = 1000L;
        ptRow.nScans = 2;
        ptRow.positionUncertaintyM = 0.5;
        dao.insertSurveyPoint(ptRow);

        // Measurement
        Measurement m1 = new Measurement(
                mId, ptId, Protocol.WIFI, "02:aa:00:00:00:01", "OfficeNet",
                2437, 6, 20, MeasurementSource.SCAN, "builtin-wifi",
                2, -50.0, -52, -48, null, null
        );
        dao.insertMeasurement(MeasurementMapper.toRow(m1));

        // Latency
        LatencyResult lr = new LatencyResult(
                latId, ptId, LatencyMethod.ICMP, "192.168.1.1", "wlan0",
                LatencyScope.GATEWAY, 5, 5, 10.0, 12.0, 14.0, 1.5,
                0, false, "02:aa:00:00:00:01", "OfficeNet", List.of(10.0, 11.0, 12.0, 13.0, 14.0)
        );
        dao.insertLatencyResult(LatencyResultMapper.toRow(lr));

        // Now load graph
        SurveyReadRepository.SessionGraph graph = repo.loadSessionGraph(sessId);
        assertNotNull(graph);
        assertEquals(sessId, graph.session().id());
        assertEquals(floorId, graph.floor().id());
        assertEquals(projId, graph.project().id());
        assertTrue(graph.hasBoundary());
        assertEquals(4, graph.boundary().vertices().size());
        assertEquals(1, graph.walls().size());
        assertEquals(1, graph.wallMaterials().size());
        assertEquals(1, graph.targetNetworks().size());
        assertEquals(1, graph.points().size());

        // Test toPerPoints
        PerPoint[] perPoints = graph.toPerPoints();
        assertEquals(1, perPoints.length);
        assertEquals(4.0, perPoints[0].x(), 1e-9);
        assertEquals(3.0, perPoints[0].y(), 1e-9);
        assertEquals(-500, perPoints[0].coverageTenths());
        assertNotNull(perPoints[0].latency());
        assertEquals(5, perPoints[0].latency().sent());
        assertEquals(0, perPoints[0].latency().lost());

        // Test samples
        List<Sample> covSamples = graph.samplesForCoverage();
        assertEquals(1, covSamples.size());
        assertEquals(-50.0, covSamples.get(0).dbm(), 1e-9);

        assertTrue(graph.hasBandData(2400, 2500));
        assertFalse(graph.hasBandData(5000, 6000));
        assertTrue(graph.hasLatencyData());

        assertEquals(1, repo.countPointsForSession(sessId));
        assertEquals(1, repo.getSessionsForFloor(floorId).size());
    }
}
