package com.netfieldpro.ui.debug;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.netfieldpro.R;
import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.model.EntityRegistry;
import com.netfieldpro.core.model.entity.ConnectionEvent;
import com.netfieldpro.core.model.entity.DeviceInfo;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.LatencyResult;
import com.netfieldpro.core.model.entity.LinkSample;
import com.netfieldpro.core.model.entity.Measurement;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.model.entity.RawSample;
import com.netfieldpro.core.model.entity.Scan;
import com.netfieldpro.core.model.entity.SurveyPoint;
import com.netfieldpro.core.model.entity.SurveySession;
import com.netfieldpro.core.model.entity.Wall;
import com.netfieldpro.core.model.enums.CadenceFlag;
import com.netfieldpro.core.model.enums.ConnectionEventType;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.LatencyMethod;
import com.netfieldpro.core.model.enums.LatencyScope;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.core.model.enums.MeasurementSource;
import com.netfieldpro.core.model.enums.PointQuality;
import com.netfieldpro.core.model.enums.PointSource;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.model.enums.Protocol;
import com.netfieldpro.core.model.enums.ScaleMethod;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.model.enums.SessionType;
import com.netfieldpro.core.model.enums.SweepClass;
import com.netfieldpro.core.model.enums.TimingQuality;
import com.netfieldpro.core.model.serial.ConnectionEventSerializer;
import com.netfieldpro.core.model.serial.DeviceInfoSerializer;
import com.netfieldpro.core.model.serial.FloorSerializer;
import com.netfieldpro.core.model.serial.LatencyResultSerializer;
import com.netfieldpro.core.model.serial.LinkSampleSerializer;
import com.netfieldpro.core.model.serial.MeasurementSerializer;
import com.netfieldpro.core.model.serial.ProjectSerializer;
import com.netfieldpro.core.model.serial.RawSampleSerializer;
import com.netfieldpro.core.model.serial.ScanSerializer;
import com.netfieldpro.core.model.serial.SurveyPointSerializer;
import com.netfieldpro.core.model.serial.SurveySessionSerializer;
import com.netfieldpro.core.model.serial.WallSerializer;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.core.util.Sha256;
import com.netfieldpro.data.db.BatchedWriter;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.WriteCallback;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.ConnectionEventRow;
import com.netfieldpro.data.db.entity.DeviceInfoRow;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.LatencyResultRow;
import com.netfieldpro.data.db.entity.LinkSampleRow;
import com.netfieldpro.data.db.entity.MeasurementRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.RawSampleRow;
import com.netfieldpro.data.db.entity.ScanRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.data.db.entity.WallRow;
import com.netfieldpro.data.db.mapper.ConnectionEventMapper;
import com.netfieldpro.data.db.mapper.DeviceInfoMapper;
import com.netfieldpro.data.db.mapper.FloorMapper;
import com.netfieldpro.data.db.mapper.LatencyResultMapper;
import com.netfieldpro.data.db.mapper.LinkSampleMapper;
import com.netfieldpro.data.db.mapper.MeasurementMapper;
import com.netfieldpro.data.db.mapper.ProjectMapper;
import com.netfieldpro.data.db.mapper.RawSampleMapper;
import com.netfieldpro.data.db.mapper.ScanMapper;
import com.netfieldpro.data.db.mapper.SurveyPointMapper;
import com.netfieldpro.data.db.mapper.SurveySessionMapper;
import com.netfieldpro.data.db.mapper.WallMapper;
import com.netfieldpro.databinding.FragmentDebugDataLabBinding;

public class DebugDataLabFragment extends Fragment {

    private FragmentDebugDataLabBinding binding;
    private NetFieldDatabase db;
    private BatchedWriter writer;
    private final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();

    public static String deterministicId(int counter) {
        byte[] bytes = new byte[16];
        bytes[15] = (byte) counter;
        bytes[14] = (byte) (counter >> 8);
        bytes[13] = (byte) (counter >> 16);
        bytes[12] = (byte) (counter >> 24);
        return IdGenerator.fromBytes(bytes);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDebugDataLabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = NetFieldDatabase.get(requireContext());
        writer = new BatchedWriter(db);

        binding.btnCreateDemo.setOnClickListener(v -> createDemoProject());
        binding.btnSeedDemoPass.setOnClickListener(v -> {
            try {
                com.netfieldpro.data.seed.DemoSeeder.seedPass(requireContext());
                refreshCounts();
                com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), R.string.seed_success, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            } catch (Exception e) {
                com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), getString(R.string.seed_failed, e.getMessage()), com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            }
        });
        binding.btnSeedDemoFail.setOnClickListener(v -> {
            try {
                com.netfieldpro.data.seed.DemoSeeder.seedFail(requireContext());
                refreshCounts();
                com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), R.string.seed_success, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            } catch (Exception e) {
                com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), getString(R.string.seed_failed, e.getMessage()), com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            }
        });
        binding.btnSeedLatestPass.setOnClickListener(v -> {
            try {
                String sid = com.netfieldpro.data.seed.DemoSeeder.seedOnLatestFloor(requireContext(), true);
                if (sid != null) {
                    refreshCounts();
                    com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), R.string.seed_success, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), getString(R.string.seed_failed, e.getMessage()), com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            }
        });
        binding.btnSeedLatestFail.setOnClickListener(v -> {
            try {
                String sid = com.netfieldpro.data.seed.DemoSeeder.seedOnLatestFloor(requireContext(), false);
                if (sid != null) {
                    refreshCounts();
                    com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), R.string.seed_success, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), getString(R.string.seed_failed, e.getMessage()), com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            }
        });
        binding.btnDeleteDemo.setOnClickListener(v -> deleteDemoProject());
        binding.btnExportJson.setOnClickListener(v -> exportCanonicalJson());

        refreshCounts();
    }

    private void createDemoProject() {
        String pId = deterministicId(1);
        String fId = deterministicId(2);
        String dId = deterministicId(3);
        String sId = deterministicId(4);
        String scanId = deterministicId(5);
        String ptId = deterministicId(6);
        String m1Id = deterministicId(7);
        String m2Id = deterministicId(8);
        String latId = deterministicId(9);
        String lsId = deterministicId(10);
        String ceId = deterministicId(11);
        String wId = deterministicId(12);

        Project project = new Project(pId, "Demo Project", "Demo Client", "Demo Site", 1700000000000L);
        Floor floor = new Floor(fId, pId, 1L, "47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU", 1000, 1000, 0.05, 0.0,
                ScaleMethod.LINE, 5.0, true, 0.0, 0.0, 0.0, new Floor.Point(0, 0), new Floor.Point(10, 10));
        DeviceInfo deviceInfo = new DeviceInfo(dId, "Pixel 7", "Google", 34, "Wi-Fi 6", 1000L, "fingerprint");
        SurveySession session = new SurveySession(sId, fId, "Demo Session", SessionState.DRAFT, 1700000000000L, 1000000000L, 1,
                "1.0", "1.0", false, TimingQuality.GOOD, EvidenceGrade.VERDICT, SessionType.STANDARD, deterministicId(99),
                0L, 0.0, 1000L, CadenceFlag.DEFAULT, ProbeMethod.ICMP, List.of(2412L, 5180L), "wifi-builtin", dId,
                new JsonObject(), null, null, null);
        Scan scan = new Scan(scanId, sId, ptId, SweepClass.FULL, 1000000000L, 1200000000L, 1250000000L, true, true);
        RawSample raw1 = new RawSample(1L, scanId, "aa:bb:cc:dd:ee:01", "DemoCorp", 5180, -60, 1100000000L);
        RawSample raw2 = new RawSample(2L, scanId, "aa:bb:cc:dd:ee:02", "DemoGuest", 2412, -70, 1150000000L);
        SurveyPoint point = new SurveyPoint(ptId, sId, 10.0, 15.0, PointSource.TAP, PointQuality.OK, 1000000000L, 1700000000000L, 1, 0.2, null);
        Measurement m1 = new Measurement(m1Id, ptId, Protocol.WIFI, "aa:bb:cc:dd:ee:01", "DemoCorp", 5180, 36, 80,
                MeasurementSource.SCAN, "wifi-builtin", 1, -60.0, -60, -60, null, null);
        Measurement m2 = new Measurement(m2Id, ptId, Protocol.WIFI, "aa:bb:cc:dd:ee:02", "DemoGuest", 2412, 1, 20,
                MeasurementSource.SCAN, "wifi-builtin", 1, -70.0, -70, -70, null, null);
        LatencyResult latency = new LatencyResult(latId, ptId, LatencyMethod.ICMP, "8.8.8.8", "wlan0", LatencyScope.GATEWAY,
                5, 5, 2.0, 3.0, 4.0, 0.5, 0, false, "aa:bb:cc:dd:ee:01", "DemoCorp", List.of(2.0, 2.5, 3.0, 3.5, 4.0));
        LinkSample link = new LinkSample(lsId, sId, 1050000000L, "aa:bb:cc:dd:ee:01", "DemoCorp", -60, 5180, 866, 433);
        ConnectionEvent event = new ConnectionEvent(ceId, sId, ConnectionEventType.CONNECT, 1000000000L, null, "aa:bb:cc:dd:ee:01", ptId);
        Wall wall = new Wall(wId, fId, MaterialId.DRYWALL_LIGHT, List.of(new Wall.Vertex(0, 0), new Wall.Vertex(10, 0)));

        writer.submit(database -> {
            NetFieldDao dao = database.netFieldDao();
            dao.insertProject(ProjectMapper.toRow(project));
            dao.insertFloor(FloorMapper.toRow(floor));
            dao.insertDeviceInfo(DeviceInfoMapper.toRow(deviceInfo));
            dao.insertSurveySession(SurveySessionMapper.toRow(session));
            dao.insertScan(ScanMapper.toRow(scan));
            dao.insertRawSample(RawSampleMapper.toRow(raw1));
            dao.insertRawSample(RawSampleMapper.toRow(raw2));
            dao.insertSurveyPoint(SurveyPointMapper.toRow(point));
            dao.insertMeasurement(MeasurementMapper.toRow(m1));
            dao.insertMeasurement(MeasurementMapper.toRow(m2));
            dao.insertLatencyResult(LatencyResultMapper.toRow(latency));
            dao.insertLinkSample(LinkSampleMapper.toRow(link));
            dao.insertConnectionEvent(ConnectionEventMapper.toRow(event));
            dao.insertWall(WallMapper.toRow(wall));
        }, new WriteCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> refreshCounts());
                }
            }

            @Override
            public void onError(Throwable t) {
            }
        });
    }

    private void deleteDemoProject() {
        String pId = deterministicId(1);
        writer.submit(database -> {
            database.netFieldDao().deleteProject(pId);
        }, new WriteCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> refreshCounts());
                }
            }

            @Override
            public void onError(Throwable t) {
            }
        });
    }

    public JsonObject forSession(String sessionId) {
        return forSession(db.netFieldDao(), sessionId);
    }

    public static JsonObject forSession(NetFieldDao dao, String sessionId) {
        SurveySessionRow sRow = dao.getSurveySession(sessionId);
        if (sRow == null) {
            return null;
        }
        SurveySession session = SurveySessionMapper.toDomain(sRow);

        FloorRow fRow = dao.getFloor(session.floorId());
        Floor floor = fRow == null ? null : FloorMapper.toDomain(fRow);

        ProjectRow pRow = floor == null ? null : dao.getProject(floor.projectId());
        Project project = pRow == null ? null : ProjectMapper.toDomain(pRow);

        DeviceInfoRow dRow = session.deviceInfoId() == null ? null : dao.getDeviceInfo(session.deviceInfoId());
        DeviceInfo deviceInfo = dRow == null ? null : DeviceInfoMapper.toDomain(dRow);

        List<TargetNetworkRow> targetRows = dao.getTargetNetworksForSession(sessionId);
        Set<String> targetSsids = new HashSet<>();
        for (TargetNetworkRow tr : targetRows) {
            targetSsids.add(tr.ssid);
        }

        List<ScanRow> scanRows = dao.getScansForSession(sessionId);
        List<Scan> scans = new ArrayList<>();
        List<RawSample> targetSamples = new ArrayList<>();
        for (ScanRow r : scanRows) {
            scans.add(ScanMapper.toDomain(r));
            List<RawSampleRow> rRows = dao.getRawSamplesForScan(r.id);
            for (RawSampleRow rawRow : rRows) {
                RawSample sample = RawSampleMapper.toDomain(rawRow);
                if (targetSsids.contains(sample.ssid())) {
                    targetSamples.add(sample);
                }
            }
        }
        scans.sort(EntityRegistry.SCAN.sortComparator());
        targetSamples.sort(EntityRegistry.RAW_SAMPLE.sortComparator());

        List<SurveyPointRow> ptRows = dao.getSurveyPointsForSession(sessionId);
        List<SurveyPoint> surveyPoints = new ArrayList<>();
        List<Measurement> measurements = new ArrayList<>();
        List<LatencyResult> latencyResults = new ArrayList<>();
        for (SurveyPointRow r : ptRows) {
            surveyPoints.add(SurveyPointMapper.toDomain(r));
            List<MeasurementRow> mRows = dao.getMeasurementsForPoint(r.id);
            for (MeasurementRow mr : mRows) {
                measurements.add(MeasurementMapper.toDomain(mr));
            }
            List<LatencyResultRow> latRows = dao.getLatencyResultsForPoint(r.id);
            for (LatencyResultRow lr : latRows) {
                latencyResults.add(LatencyResultMapper.toDomain(lr));
            }
        }
        surveyPoints.sort(EntityRegistry.SURVEY_POINT.sortComparator());
        measurements.sort(EntityRegistry.MEASUREMENT.sortComparator());
        latencyResults.sort(EntityRegistry.LATENCY_RESULT.sortComparator());

        List<LinkSampleRow> lsRows = dao.getLinkSamplesForSession(sessionId);
        List<LinkSample> linkSamples = new ArrayList<>();
        for (LinkSampleRow r : lsRows) {
            linkSamples.add(LinkSampleMapper.toDomain(r));
        }
        linkSamples.sort(EntityRegistry.LINK_SAMPLE.sortComparator());

        List<ConnectionEventRow> ceRows = dao.getConnectionEventsForSession(sessionId);
        List<ConnectionEvent> connectionEvents = new ArrayList<>();
        for (ConnectionEventRow r : ceRows) {
            connectionEvents.add(ConnectionEventMapper.toDomain(r));
        }
        connectionEvents.sort(EntityRegistry.CONNECTION_EVENT.sortComparator());

        List<Wall> walls = new ArrayList<>();
        if (floor != null) {
            List<WallRow> wallRows = dao.getWallsForFloor(floor.id());
            for (WallRow r : wallRows) {
                walls.add(WallMapper.toDomain(r));
            }
        }
        walls.sort(EntityRegistry.WALL.sortComparator());

        JsonObject root = new JsonObject();
        if (project != null) root.put("project", ProjectSerializer.toJson(project));
        if (floor != null) root.put("floor", FloorSerializer.toJson(floor));
        if (deviceInfo != null) root.put("deviceInfo", DeviceInfoSerializer.toJson(deviceInfo));
        root.put("surveySession", SurveySessionSerializer.toJson(session));

        JsonArray scanArr = new JsonArray();
        for (Scan s : scans) scanArr.add(ScanSerializer.toJson(s));
        root.put("scans", scanArr);

        JsonArray rawArr = new JsonArray();
        for (RawSample r : targetSamples) rawArr.add(RawSampleSerializer.toJson(r));
        root.put("rawSamples", rawArr);

        JsonArray ptArr = new JsonArray();
        for (SurveyPoint pt : surveyPoints) ptArr.add(SurveyPointSerializer.toJson(pt));
        root.put("surveyPoints", ptArr);

        JsonArray mArr = new JsonArray();
        for (Measurement m : measurements) mArr.add(MeasurementSerializer.toJson(m));
        root.put("measurements", mArr);

        JsonArray latArr = new JsonArray();
        for (LatencyResult l : latencyResults) latArr.add(LatencyResultSerializer.toJson(l));
        root.put("latencyResults", latArr);

        JsonArray lsArr = new JsonArray();
        for (LinkSample l : linkSamples) lsArr.add(LinkSampleSerializer.toJson(l));
        root.put("linkSamples", lsArr);

        JsonArray ceArr = new JsonArray();
        for (ConnectionEvent c : connectionEvents) ceArr.add(ConnectionEventSerializer.toJson(c));
        root.put("connectionEvents", ceArr);

        JsonArray wArr = new JsonArray();
        for (Wall w : walls) wArr.add(WallSerializer.toJson(w));
        root.put("walls", wArr);

        return root;
    }

    public static List<RawSample> neighborSamplesForSession(NetFieldDao dao, String sessionId) {
        List<TargetNetworkRow> targetRows = dao.getTargetNetworksForSession(sessionId);
        Set<String> targetSsids = new HashSet<>();
        for (TargetNetworkRow tr : targetRows) {
            targetSsids.add(tr.ssid);
        }

        List<ScanRow> scanRows = dao.getScansForSession(sessionId);
        List<RawSample> nonTargetSamples = new ArrayList<>();
        for (ScanRow r : scanRows) {
            List<RawSampleRow> rRows = dao.getRawSamplesForScan(r.id);
            for (RawSampleRow rawRow : rRows) {
                RawSample sample = RawSampleMapper.toDomain(rawRow);
                if (!targetSsids.contains(sample.ssid())) {
                    nonTargetSamples.add(sample);
                }
            }
        }
        return nonTargetSamples;
    }

    private void exportCanonicalJson() {
        queryExecutor.submit(() -> {
            try {
                NetFieldDao dao = db.netFieldDao();
                String pId = deterministicId(1);
                ProjectRow pRow = dao.getProject(pId);
                if (pRow == null) {
                    return;
                }
                Project project = ProjectMapper.toDomain(pRow);

                List<FloorRow> fRows = dao.getFloorsForProject(pId);
                Floor floor = fRows.isEmpty() ? null : FloorMapper.toDomain(fRows.get(0));

                String sId = deterministicId(4);
                SurveySessionRow sRow = dao.getSurveySession(sId);
                SurveySession session = sRow == null ? null : SurveySessionMapper.toDomain(sRow);

                String dId = deterministicId(3);
                DeviceInfoRow dRow = dao.getDeviceInfo(dId);
                DeviceInfo deviceInfo = dRow == null ? null : DeviceInfoMapper.toDomain(dRow);

                List<ScanRow> scanRows = dao.getScansForSession(sId);
                List<Scan> scans = new ArrayList<>();
                for (ScanRow r : scanRows) {
                    scans.add(ScanMapper.toDomain(r));
                }
                scans.sort(EntityRegistry.SCAN.sortComparator());

                String scanId = deterministicId(5);
                List<RawSampleRow> rawRows = dao.getRawSamplesForScan(scanId);
                List<RawSample> rawSamples = new ArrayList<>();
                for (RawSampleRow r : rawRows) {
                    rawSamples.add(RawSampleMapper.toDomain(r));
                }
                rawSamples.sort(EntityRegistry.RAW_SAMPLE.sortComparator());

                List<SurveyPointRow> ptRows = dao.getSurveyPointsForSession(sId);
                List<SurveyPoint> surveyPoints = new ArrayList<>();
                for (SurveyPointRow r : ptRows) {
                    surveyPoints.add(SurveyPointMapper.toDomain(r));
                }
                surveyPoints.sort(EntityRegistry.SURVEY_POINT.sortComparator());

                String ptId = deterministicId(6);
                List<MeasurementRow> mRows = dao.getMeasurementsForPoint(ptId);
                List<Measurement> measurements = new ArrayList<>();
                for (MeasurementRow r : mRows) {
                    measurements.add(MeasurementMapper.toDomain(r));
                }
                measurements.sort(EntityRegistry.MEASUREMENT.sortComparator());

                List<LatencyResultRow> latRows = dao.getLatencyResultsForPoint(ptId);
                List<LatencyResult> latencyResults = new ArrayList<>();
                for (LatencyResultRow r : latRows) {
                    latencyResults.add(LatencyResultMapper.toDomain(r));
                }
                latencyResults.sort(EntityRegistry.LATENCY_RESULT.sortComparator());

                List<LinkSampleRow> lsRows = dao.getLinkSamplesForSession(sId);
                List<LinkSample> linkSamples = new ArrayList<>();
                for (LinkSampleRow r : lsRows) {
                    linkSamples.add(LinkSampleMapper.toDomain(r));
                }
                linkSamples.sort(EntityRegistry.LINK_SAMPLE.sortComparator());

                List<ConnectionEventRow> ceRows = dao.getConnectionEventsForSession(sId);
                List<ConnectionEvent> connectionEvents = new ArrayList<>();
                for (ConnectionEventRow r : ceRows) {
                    connectionEvents.add(ConnectionEventMapper.toDomain(r));
                }
                connectionEvents.sort(EntityRegistry.CONNECTION_EVENT.sortComparator());

                String fId = deterministicId(2);
                List<WallRow> wallRows = dao.getWallsForFloor(fId);
                List<Wall> walls = new ArrayList<>();
                for (WallRow r : wallRows) {
                    walls.add(WallMapper.toDomain(r));
                }
                walls.sort(EntityRegistry.WALL.sortComparator());

                JsonObject root = new JsonObject();
                root.put("project", ProjectSerializer.toJson(project));
                if (floor != null) root.put("floor", FloorSerializer.toJson(floor));
                if (deviceInfo != null) root.put("deviceInfo", DeviceInfoSerializer.toJson(deviceInfo));
                if (session != null) root.put("surveySession", SurveySessionSerializer.toJson(session));

                JsonArray scanArr = new JsonArray();
                for (Scan s : scans) scanArr.add(ScanSerializer.toJson(s));
                root.put("scans", scanArr);

                JsonArray rawArr = new JsonArray();
                for (RawSample r : rawSamples) rawArr.add(RawSampleSerializer.toJson(r));
                root.put("rawSamples", rawArr);

                JsonArray ptArr = new JsonArray();
                for (SurveyPoint pt : surveyPoints) ptArr.add(SurveyPointSerializer.toJson(pt));
                root.put("surveyPoints", ptArr);

                JsonArray mArr = new JsonArray();
                for (Measurement m : measurements) mArr.add(MeasurementSerializer.toJson(m));
                root.put("measurements", mArr);

                JsonArray latArr = new JsonArray();
                for (LatencyResult l : latencyResults) latArr.add(LatencyResultSerializer.toJson(l));
                root.put("latencyResults", latArr);

                JsonArray lsArr = new JsonArray();
                for (LinkSample l : linkSamples) lsArr.add(LinkSampleSerializer.toJson(l));
                root.put("linkSamples", lsArr);

                JsonArray ceArr = new JsonArray();
                for (ConnectionEvent c : connectionEvents) ceArr.add(ConnectionEventSerializer.toJson(c));
                root.put("connectionEvents", ceArr);

                JsonArray wArr = new JsonArray();
                for (Wall w : walls) wArr.add(WallSerializer.toJson(w));
                root.put("walls", wArr);

                File cacheDir = requireContext().getCacheDir();
                File exportFile = new File(cacheDir, "datalab-export.json");
                try (FileOutputStream fos = new FileOutputStream(exportFile)) {
                    CanonicalJsonWriter.write(root, fos);
                }

                long len = exportFile.length();
                byte[] hashBytes;
                try (InputStream fis = Files.newInputStream(exportFile.toPath())) {
                    hashBytes = Sha256.hash(fis);
                }
                String hashHex = Sha256.hex(hashBytes);

                String info = len + " bytes\n" + hashHex;
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.textExportInfo.setText(info);
                    });
                }
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.textExportInfo.setText("Export error: " + e.getMessage());
                    });
                }
            }
        });
    }

    public void refreshCounts() {
        queryExecutor.submit(() -> {
            NetFieldDao dao = db.netFieldDao();
            int cProject = dao.countProjects();
            int cFloor = dao.countFloors();
            int cSession = dao.countSurveySessions();
            int cDevice = dao.countDeviceInfo();
            int cScan = dao.countScans();
            int cRaw = dao.countRawSamples();
            int cPoint = dao.countSurveyPoints();
            int cMeasurement = dao.countMeasurements();
            int cLatency = dao.countLatencyResults();
            int cLink = dao.countLinkSamples();
            int cConn = dao.countConnectionEvents();
            int cPath = dao.countPathSegments();
            int cApm = dao.countAccessPointMarkers();
            int cTarget = dao.countTargetNetworks();
            int cGroup = dao.countApGroups();
            int cBoundary = dao.countFloorBoundaries();
            int cWall = dao.countWalls();
            int cMaterial = dao.countWallMaterials();
            int cProfile = dao.countRequirementProfiles();
            int cAssessment = dao.countAssessments();

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.countProject.setText(String.valueOf(cProject));
                        binding.countFloor.setText(String.valueOf(cFloor));
                        binding.countSurveySession.setText(String.valueOf(cSession));
                        binding.countDeviceInfo.setText(String.valueOf(cDevice));
                        binding.countScan.setText(String.valueOf(cScan));
                        binding.countRawSample.setText(String.valueOf(cRaw));
                        binding.countSurveyPoint.setText(String.valueOf(cPoint));
                        binding.countMeasurement.setText(String.valueOf(cMeasurement));
                        binding.countLatencyResult.setText(String.valueOf(cLatency));
                        binding.countLinkSample.setText(String.valueOf(cLink));
                        binding.countConnectionEvent.setText(String.valueOf(cConn));
                        binding.countPathSegment.setText(String.valueOf(cPath));
                        binding.countAccessPointMarker.setText(String.valueOf(cApm));
                        binding.countTargetNetwork.setText(String.valueOf(cTarget));
                        binding.countApGroup.setText(String.valueOf(cGroup));
                        binding.countFloorBoundary.setText(String.valueOf(cBoundary));
                        binding.countWall.setText(String.valueOf(cWall));
                        binding.countWallMaterial.setText(String.valueOf(cMaterial));
                        binding.countRequirementProfile.setText(String.valueOf(cProfile));
                        binding.countAssessment.setText(String.valueOf(cAssessment));
                    }
                });
            }
        });
    }

    public BatchedWriter getBatchedWriter() {
        return writer;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (writer != null) {
            writer.close();
        }
        queryExecutor.shutdown();
        binding = null;
    }
}
