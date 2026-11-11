package com.netfieldpro.core.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonObject;
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
import com.netfieldpro.core.model.serial.AccessPointMarkerSerializer;
import com.netfieldpro.core.model.serial.ApGroupSerializer;
import com.netfieldpro.core.model.serial.AssessmentSerializer;
import com.netfieldpro.core.model.serial.ConnectionEventSerializer;
import com.netfieldpro.core.model.serial.DeviceInfoSerializer;
import com.netfieldpro.core.model.serial.FloorBoundarySerializer;
import com.netfieldpro.core.model.serial.FloorSerializer;
import com.netfieldpro.core.model.serial.LatencyResultSerializer;
import com.netfieldpro.core.model.serial.LinkSampleSerializer;
import com.netfieldpro.core.model.serial.MeasurementSerializer;
import com.netfieldpro.core.model.serial.PathSegmentSerializer;
import com.netfieldpro.core.model.serial.ProjectSerializer;
import com.netfieldpro.core.model.serial.RawSampleSerializer;
import com.netfieldpro.core.model.serial.RequirementProfileSerializer;
import com.netfieldpro.core.model.serial.ScanSerializer;
import com.netfieldpro.core.model.serial.SurveyPointSerializer;
import com.netfieldpro.core.model.serial.SurveySessionSerializer;
import com.netfieldpro.core.model.serial.TargetNetworkSerializer;
import com.netfieldpro.core.model.serial.WallMaterialSerializer;
import com.netfieldpro.core.model.serial.WallSerializer;

public final class EntityRegistry {

    private static final String ID_A = "AAAAAAAAAAAAAAAAAAAAAA";
    private static final String ID_B = "BBBBBBBBBBBBBBBBBBBBBB";
    private static final String ID_C = "CCCCCCCCCCCCCCCCCCCCCC";
    private static final String ID_D = "DDDDDDDDDDDDDDDDDDDDDD";
    private static final String ID_E = "EEEEEEEEEEEEEEEEEEEEEE";
    private static final String DUMMY_PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String DUMMY_SIG = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    public static final EntitySpec<Project> PROJECT = new EntitySpec<>(
            "Project",
            Project.class,
            () -> new Project(ID_A, "Demo Project", "Client A", "Site B", 1700000000000L),
            ProjectSerializer::toJson,
            Comparator.comparing(Project::id)
    );

    public static final EntitySpec<Floor> FLOOR = new EntitySpec<>(
            "Floor",
            Floor.class,
            () -> new Floor(ID_A, ID_B, 1L, "abc", 1000, 1000, 0.05, 0.0, ScaleMethod.LINE, 5.0, true, 0.0, 0.0, 0.0, new Floor.Point(0, 0), new Floor.Point(10, 10)),
            FloorSerializer::toJson,
            Comparator.comparingLong(Floor::levelIndex).thenComparing(Floor::id)
    );

    public static final EntitySpec<SurveySession> SURVEY_SESSION = new EntitySpec<>(
            "SurveySession",
            SurveySession.class,
            () -> new SurveySession(ID_A, ID_B, "Session 1", SessionState.DRAFT, 1700000000000L, 1000000000L, 1, "1.0", "1.0", false, TimingQuality.GOOD, EvidenceGrade.VERDICT, SessionType.STANDARD, ID_C, 0L, 0.0, 1000L, CadenceFlag.DEFAULT, ProbeMethod.ICMP, List.of(2412L, 5180L), "builtin", ID_D, new JsonObject(), null, null, null),
            SurveySessionSerializer::toJson,
            Comparator.comparing(SurveySession::id)
    );

    public static final EntitySpec<DeviceInfo> DEVICE_INFO = new EntitySpec<>(
            "DeviceInfo",
            DeviceInfo.class,
            () -> new DeviceInfo(ID_A, "Pixel 7", "Google", 34, "WiFi 6", 1000L, "fingerprint"),
            DeviceInfoSerializer::toJson,
            Comparator.comparing(DeviceInfo::id)
    );

    public static final EntitySpec<Scan> SCAN = new EntitySpec<>(
            "Scan",
            Scan.class,
            () -> new Scan(ID_A, ID_B, null, SweepClass.FULL, 1000000L, 2000000L, 2100000L, true, true),
            ScanSerializer::toJson,
            Comparator.comparingLong(Scan::startNs).thenComparing(Scan::id)
    );

    public static final EntitySpec<RawSample> RAW_SAMPLE = new EntitySpec<>(
            "RawSample",
            RawSample.class,
            () -> new RawSample(1L, ID_A, "aa:bb:cc:dd:ee:01", "SSID", 2412, -60, null),
            RawSampleSerializer::toJson,
            Comparator.comparingLong(RawSample::rowId)
    );

    public static final EntitySpec<SurveyPoint> SURVEY_POINT = new EntitySpec<>(
            "SurveyPoint",
            SurveyPoint.class,
            () -> new SurveyPoint(ID_A, ID_B, 10.0, 20.0, PointSource.TAP, PointQuality.OK, 1000000L, 1700000000000L, 2, 0.1, null),
            SurveyPointSerializer::toJson,
            Comparator.comparingLong(SurveyPoint::tMonoNs).thenComparing(SurveyPoint::id)
    );

    public static final EntitySpec<Measurement> MEASUREMENT = new EntitySpec<>(
            "Measurement",
            Measurement.class,
            () -> new Measurement(ID_A, ID_B, Protocol.WIFI, "aa:bb:cc:dd:ee:01", "SSID", 5180, 36, 80, MeasurementSource.SCAN, "builtin", 4, -61.5, -65, -60, null, null),
            MeasurementSerializer::toJson,
            Comparator.comparing(Measurement::pointId)
                    .thenComparing(Measurement::bssidOrId)
                    .thenComparing(m -> m.source().name())
                    .thenComparing(Measurement::receiverId)
                    .thenComparing(Measurement::id)
    );

    public static final EntitySpec<LatencyResult> LATENCY_RESULT = new EntitySpec<>(
            "LatencyResult",
            LatencyResult.class,
            () -> new LatencyResult(ID_A, ID_B, LatencyMethod.ICMP, "192.168.1.1", "wlan0", LatencyScope.GATEWAY, 5, 5, 10.0, 12.0, 15.0, 2.0, 0, false, "aa:bb:cc:dd:ee:01", "SSID", List.of(10.0, 11.0, 12.0, 13.0, 15.0)),
            LatencyResultSerializer::toJson,
            Comparator.comparing(LatencyResult::pointId).thenComparing(LatencyResult::id)
    );

    public static final EntitySpec<LinkSample> LINK_SAMPLE = new EntitySpec<>(
            "LinkSample",
            LinkSample.class,
            () -> new LinkSample(ID_A, ID_B, 1500000000L, "aa:bb:cc:dd:ee:01", "SSID", -65, 5180, 300, 433),
            LinkSampleSerializer::toJson,
            Comparator.comparingLong(LinkSample::observedAtNs).thenComparing(LinkSample::id)
    );

    public static final EntitySpec<ConnectionEvent> CONNECTION_EVENT = new EntitySpec<>(
            "ConnectionEvent",
            ConnectionEvent.class,
            () -> new ConnectionEvent(ID_A, ID_B, ConnectionEventType.CONNECT, 1000000000L, null, "aa:bb:cc:dd:ee:01", null),
            ConnectionEventSerializer::toJson,
            Comparator.comparingLong(ConnectionEvent::atNs).thenComparing(ConnectionEvent::id)
    );

    public static final EntitySpec<PathSegment> PATH_SEGMENT = new EntitySpec<>(
            "PathSegment",
            PathSegment.class,
            () -> new PathSegment(ID_A, ID_B, PathState.DONE, 0L, 30000000000L, 0L, 30000000000L, 0.5, List.of(new PathSegment.Waypoint(0, 0, 0L), new PathSegment.Waypoint(10, 0, 20000000000L)), List.of(new PathSegment.Anchor(0, 0L)), PathMode.ROUTE),
            PathSegmentSerializer::toJson,
            Comparator.comparingLong(PathSegment::rawStartNs).thenComparing(PathSegment::id)
    );

    public static final EntitySpec<AccessPointMarker> ACCESS_POINT_MARKER = new EntitySpec<>(
            "AccessPointMarker",
            AccessPointMarker.class,
            () -> new AccessPointMarker(ID_A, ID_B, 5.0, 5.0, "aa:bb:cc:dd:ee:01", 20),
            AccessPointMarkerSerializer::toJson,
            Comparator.comparing(AccessPointMarker::id)
    );

    public static final EntitySpec<TargetNetwork> TARGET_NETWORK = new EntitySpec<>(
            "TargetNetwork",
            TargetNetwork.class,
            () -> new TargetNetwork(ID_A, ID_B, "Eduroam"),
            TargetNetworkSerializer::toJson,
            Comparator.comparing(TargetNetwork::ssid).thenComparing(TargetNetwork::id)
    );

    public static final EntitySpec<ApGroup> AP_GROUP = new EntitySpec<>(
            "ApGroup",
            ApGroup.class,
            () -> new ApGroup(ID_A, ID_B, "Group 1", ApGroupOrigin.HEURISTIC, List.of("aa:bb:cc:dd:ee:01", "aa:bb:cc:dd:ee:02")),
            ApGroupSerializer::toJson,
            Comparator.comparing(ApGroup::label).thenComparing(ApGroup::id)
    );

    public static final EntitySpec<FloorBoundary> FLOOR_BOUNDARY = new EntitySpec<>(
            "FloorBoundary",
            FloorBoundary.class,
            () -> new FloorBoundary(ID_A, List.of(new FloorBoundary.Vertex(0, 0), new FloorBoundary.Vertex(10, 0), new FloorBoundary.Vertex(10, 10), new FloorBoundary.Vertex(0, 10))),
            FloorBoundarySerializer::toJson,
            Comparator.comparing(FloorBoundary::floorId)
    );

    public static final EntitySpec<Wall> WALL = new EntitySpec<>(
            "Wall",
            Wall.class,
            () -> new Wall(ID_A, ID_B, MaterialId.DRYWALL_LIGHT, List.of(new Wall.Vertex(0, 0), new Wall.Vertex(10, 0))),
            WallSerializer::toJson,
            Comparator.comparing(Wall::id)
    );

    public static final EntitySpec<WallMaterial> WALL_MATERIAL = new EntitySpec<>(
            "WallMaterial",
            WallMaterial.class,
            () -> WallMaterials.DRYWALL_LIGHT,
            WallMaterialSerializer::toJson,
            Comparator.comparing(w -> w.materialId().name())
    );

    public static final EntitySpec<RequirementProfile> REQUIREMENT_PROFILE = new EntitySpec<>(
            "RequirementProfile",
            RequirementProfile.class,
            () -> new RequirementProfile(ID_A, ProfileKind.PRESET, new JsonObject(), BandPolicy.BEST_ANY, 10.0, Map.of("coverage", true)),
            RequirementProfileSerializer::toJson,
            Comparator.comparing(RequirementProfile::id)
    );

    public static final EntitySpec<Assessment> ASSESSMENT = new EntitySpec<>(
            "Assessment",
            Assessment.class,
            () -> new Assessment(ID_A, "evidenceHash", ID_B, new JsonObject(), ID_C, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 1700000000000L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG),
            AssessmentSerializer::toJson,
            Comparator.comparingLong(Assessment::assessedAtMs).thenComparing(Assessment::assessmentId)
    );

    public static final List<EntitySpec<?>> ALL = List.of(
            PROJECT,
            FLOOR,
            SURVEY_SESSION,
            DEVICE_INFO,
            SCAN,
            RAW_SAMPLE,
            SURVEY_POINT,
            MEASUREMENT,
            LATENCY_RESULT,
            LINK_SAMPLE,
            CONNECTION_EVENT,
            PATH_SEGMENT,
            ACCESS_POINT_MARKER,
            TARGET_NETWORK,
            AP_GROUP,
            FLOOR_BOUNDARY,
            WALL,
            WALL_MATERIAL,
            REQUIREMENT_PROFILE,
            ASSESSMENT
    );

    private EntityRegistry() {
    }
}

