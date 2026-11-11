package com.netfieldpro.data.db.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.EntityRegistry;
import com.netfieldpro.core.model.WallMaterials;
import com.netfieldpro.core.model.entity.AccessPointMarker;
import com.netfieldpro.core.model.entity.ApGroup;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.entity.ConnectionEvent;
import com.netfieldpro.core.model.entity.DeviceInfo;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.FloorBoundary;
import com.netfieldpro.core.model.entity.LatencyResult;
import com.netfieldpro.core.model.entity.LinkSample;
import com.netfieldpro.core.model.entity.Measurement;
import com.netfieldpro.core.model.entity.PathSegment;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.model.entity.RawSample;
import com.netfieldpro.core.model.entity.RequirementProfile;
import com.netfieldpro.core.model.entity.Scan;
import com.netfieldpro.core.model.entity.SurveyPoint;
import com.netfieldpro.core.model.entity.SurveySession;
import com.netfieldpro.core.model.entity.TargetNetwork;
import com.netfieldpro.core.model.entity.Wall;
import com.netfieldpro.core.model.entity.WallMaterial;
import com.netfieldpro.core.model.enums.ApGroupOrigin;
import com.netfieldpro.core.model.enums.BandPolicy;
import com.netfieldpro.core.model.enums.CadenceFlag;
import com.netfieldpro.core.model.enums.ConnectionEventType;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.LatencyMethod;
import com.netfieldpro.core.model.enums.LatencyScope;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.core.model.enums.MeasurementSource;
import com.netfieldpro.core.model.enums.PathMode;
import com.netfieldpro.core.model.enums.PathState;
import com.netfieldpro.core.model.enums.PointQuality;
import com.netfieldpro.core.model.enums.PointSource;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.model.enums.ProfileKind;
import com.netfieldpro.core.model.enums.Protocol;
import com.netfieldpro.core.model.enums.ScaleMethod;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.model.enums.SessionType;
import com.netfieldpro.core.model.enums.SweepClass;
import com.netfieldpro.core.model.enums.TimingQuality;
import com.netfieldpro.core.model.enums.Verdict;

public class MapperRoundTripTest {

    private static final String ID_A = "AAAAAAAAAAAAAAAAAAAAAA";
    private static final String ID_B = "BBBBBBBBBBBBBBBBBBBBBB";
    private static final String ID_C = "CCCCCCCCCCCCCCCCCCCCCC";
    private static final String ID_D = "DDDDDDDDDDDDDDDDDDDDDD";
    private static final String DUMMY_PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String DUMMY_SIG = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final long BIG_INT64 = 9007199254740993L; // > 2^53

    @Test
    public void testAllRegistrySamplesRoundTrip() {
        // Test all 20 registry sample entities round trip through their mapper
        Project p = EntityRegistry.PROJECT.sampleFactory().get();
        assertEquals(p, ProjectMapper.toDomain(ProjectMapper.toRow(p)));

        Floor f = EntityRegistry.FLOOR.sampleFactory().get();
        assertEquals(f, FloorMapper.toDomain(FloorMapper.toRow(f)));

        SurveySession s = EntityRegistry.SURVEY_SESSION.sampleFactory().get();
        assertEquals(s, SurveySessionMapper.toDomain(SurveySessionMapper.toRow(s)));

        DeviceInfo d = EntityRegistry.DEVICE_INFO.sampleFactory().get();
        assertEquals(d, DeviceInfoMapper.toDomain(DeviceInfoMapper.toRow(d)));

        Scan scan = EntityRegistry.SCAN.sampleFactory().get();
        assertEquals(scan, ScanMapper.toDomain(ScanMapper.toRow(scan)));

        RawSample raw = EntityRegistry.RAW_SAMPLE.sampleFactory().get();
        assertEquals(raw, RawSampleMapper.toDomain(RawSampleMapper.toRow(raw)));

        SurveyPoint pt = EntityRegistry.SURVEY_POINT.sampleFactory().get();
        assertEquals(pt, SurveyPointMapper.toDomain(SurveyPointMapper.toRow(pt)));

        Measurement m = EntityRegistry.MEASUREMENT.sampleFactory().get();
        assertEquals(m, MeasurementMapper.toDomain(MeasurementMapper.toRow(m)));

        LatencyResult lr = EntityRegistry.LATENCY_RESULT.sampleFactory().get();
        assertEquals(lr, LatencyResultMapper.toDomain(LatencyResultMapper.toRow(lr)));

        LinkSample ls = EntityRegistry.LINK_SAMPLE.sampleFactory().get();
        assertEquals(ls, LinkSampleMapper.toDomain(LinkSampleMapper.toRow(ls)));

        ConnectionEvent ce = EntityRegistry.CONNECTION_EVENT.sampleFactory().get();
        assertEquals(ce, ConnectionEventMapper.toDomain(ConnectionEventMapper.toRow(ce)));

        PathSegment ps = EntityRegistry.PATH_SEGMENT.sampleFactory().get();
        assertEquals(ps, PathSegmentMapper.toDomain(PathSegmentMapper.toRow(ps)));

        AccessPointMarker apm = EntityRegistry.ACCESS_POINT_MARKER.sampleFactory().get();
        assertEquals(apm, AccessPointMarkerMapper.toDomain(AccessPointMarkerMapper.toRow(apm)));

        TargetNetwork tn = EntityRegistry.TARGET_NETWORK.sampleFactory().get();
        assertEquals(tn, TargetNetworkMapper.toDomain(TargetNetworkMapper.toRow(tn)));

        ApGroup apg = EntityRegistry.AP_GROUP.sampleFactory().get();
        assertEquals(apg, ApGroupMapper.toDomain(ApGroupMapper.toRow(apg)));

        FloorBoundary fb = EntityRegistry.FLOOR_BOUNDARY.sampleFactory().get();
        assertEquals(fb, FloorBoundaryMapper.toDomain(FloorBoundaryMapper.toRow(fb)));

        Wall w = EntityRegistry.WALL.sampleFactory().get();
        assertEquals(w, WallMapper.toDomain(WallMapper.toRow(w)));

        WallMaterial wm = EntityRegistry.WALL_MATERIAL.sampleFactory().get();
        assertEquals(wm, WallMaterialMapper.toDomain(WallMaterialMapper.toRow(wm)));

        RequirementProfile rp = EntityRegistry.REQUIREMENT_PROFILE.sampleFactory().get();
        assertEquals(rp, RequirementProfileMapper.toDomain(RequirementProfileMapper.toRow(rp)));

        Assessment as = EntityRegistry.ASSESSMENT.sampleFactory().get();
        assertEquals(as, AssessmentMapper.toDomain(AssessmentMapper.toRow(as)));
        assertEquals(as, AssessmentMapper.toDomain(AssessmentMapper.toRow(as, ID_B)));
    }

    @Test
    public void testBigInt64AndNullsRoundTrip() {
        // Project with nulls and big int64
        Project p = new Project(ID_A, "Name", null, null, BIG_INT64);
        assertEquals(p, ProjectMapper.toDomain(ProjectMapper.toRow(p)));

        // Floor with big int64 and null optional fields
        Floor f = new Floor(ID_A, ID_B, BIG_INT64, "imgHash", 1920, 1080, 0.05, null,
                ScaleMethod.LINE, 5.0, false, null, null, null, null, null);
        assertEquals(f, FloorMapper.toDomain(FloorMapper.toRow(f)));

        // SurveySession with big int64 and nulls
        SurveySession s = new SurveySession(ID_A, ID_B, "Session", SessionState.DRAFT,
                BIG_INT64, BIG_INT64 + 1000, 1, "1.0", "1.0", false,
                TimingQuality.GOOD, EvidenceGrade.VERDICT, SessionType.STANDARD, ID_C,
                BIG_INT64 + 2000, 0.0, BIG_INT64 + 3000, CadenceFlag.DEFAULT, ProbeMethod.ICMP,
                List.of(BIG_INT64), "builtin", ID_D, new JsonObject(), null, null, null);
        assertEquals(s, SurveySessionMapper.toDomain(SurveySessionMapper.toRow(s)));

        // DeviceInfo with big int64 and null optional linkCadenceMs
        DeviceInfo d1 = new DeviceInfo(ID_A, "Pixel 7", "Google", 34, "WiFi 6", BIG_INT64, "fp");
        assertEquals(d1, DeviceInfoMapper.toDomain(DeviceInfoMapper.toRow(d1)));

        DeviceInfo d2 = new DeviceInfo(ID_A, "Pixel 7", "Google", 34, "WiFi 6", null, "fp");
        assertEquals(d2, DeviceInfoMapper.toDomain(DeviceInfoMapper.toRow(d2)));

        // Scan with big int64
        Scan scan = new Scan(ID_A, ID_B, null, SweepClass.FULL, BIG_INT64, BIG_INT64 + 500, BIG_INT64 + 600, false, false);
        assertEquals(scan, ScanMapper.toDomain(ScanMapper.toRow(scan)));

        // RawSample with big int64 and nulls
        RawSample raw = new RawSample(BIG_INT64, ID_A, "aa:bb:cc:dd:ee:01", "SSID", 2412, -70, BIG_INT64 + 100);
        assertEquals(raw, RawSampleMapper.toDomain(RawSampleMapper.toRow(raw)));

        // SurveyPoint with big int64 and nulls
        SurveyPoint pt = new SurveyPoint(ID_A, ID_B, 10.0, 20.0, PointSource.TAP, PointQuality.OK,
                BIG_INT64, BIG_INT64 + 1000, 1, 0.1, null);
        assertEquals(pt, SurveyPointMapper.toDomain(SurveyPointMapper.toRow(pt)));

        // Measurement with nulls
        Measurement m = new Measurement(ID_A, ID_B, Protocol.BLE, "beacon", null, 2402, 37, null,
                MeasurementSource.SCAN, "builtin", 1, -80.0, -80, -80, null, null);
        assertEquals(m, MeasurementMapper.toDomain(MeasurementMapper.toRow(m)));

        // LatencyResult with nulls
        LatencyResult lr = new LatencyResult(ID_A, ID_B, LatencyMethod.ICMP, "192.168.1.1", "wlan0",
                LatencyScope.GATEWAY, 5, 0, null, null, null, null, 5, false, "aa:bb:cc:dd:ee:01", "SSID", List.of());
        assertEquals(lr, LatencyResultMapper.toDomain(LatencyResultMapper.toRow(lr)));

        // LinkSample with big int64 and nulls
        LinkSample ls = new LinkSample(ID_A, ID_B, BIG_INT64, "aa:bb:cc:dd:ee:01", "SSID", -65, 5180, null, null);
        assertEquals(ls, LinkSampleMapper.toDomain(LinkSampleMapper.toRow(ls)));

        // ConnectionEvent with big int64 and nulls
        ConnectionEvent ce = new ConnectionEvent(ID_A, ID_B, ConnectionEventType.DISCONNECT, BIG_INT64, null, null, null);
        assertEquals(ce, ConnectionEventMapper.toDomain(ConnectionEventMapper.toRow(ce)));

        // PathSegment with big int64 and nulls
        PathSegment ps = new PathSegment(ID_A, ID_B, PathState.ABORTED, BIG_INT64, BIG_INT64 + 1000,
                BIG_INT64, BIG_INT64 + 1000, 0.0,
                List.of(new PathSegment.Waypoint(0.0, 0.0, BIG_INT64), new PathSegment.Waypoint(1.0, 1.0, BIG_INT64 + 1000)),
                List.of(), PathMode.LIVE_CORNER);
        assertEquals(ps, PathSegmentMapper.toDomain(PathSegmentMapper.toRow(ps)));

        // AccessPointMarker with nulls
        AccessPointMarker apm = new AccessPointMarker(ID_A, ID_B, 1.0, 2.0, null, null);
        assertEquals(apm, AccessPointMarkerMapper.toDomain(AccessPointMarkerMapper.toRow(apm)));

        // Assessment with big int64
        Assessment as = new Assessment(ID_A, "evidenceHash", ID_B, new JsonObject(), ID_C, true,
                new JsonObject(), new JsonObject(), new JsonArray(), Verdict.FAIL, BIG_INT64, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG);
        assertEquals(as, AssessmentMapper.toDomain(AssessmentMapper.toRow(as)));
    }
}
