package com.netfieldpro.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.netfieldpro.core.assess.PerPoint;
import com.netfieldpro.core.heat.Polygon;
import com.netfieldpro.core.heat.Sample;
import com.netfieldpro.core.heat.Vertex;
import com.netfieldpro.core.heat.WallChain;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.FloorBoundary;
import com.netfieldpro.core.model.entity.LatencyResult;
import com.netfieldpro.core.model.entity.Measurement;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.model.entity.RequirementProfile;
import com.netfieldpro.core.model.entity.SurveyPoint;
import com.netfieldpro.core.model.entity.SurveySession;
import com.netfieldpro.core.model.entity.TargetNetwork;
import com.netfieldpro.core.model.entity.Wall;
import com.netfieldpro.core.model.entity.WallMaterial;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.AssessmentRow;
import com.netfieldpro.data.db.entity.FloorBoundaryRow;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.LatencyResultRow;
import com.netfieldpro.data.db.entity.MeasurementRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.RequirementProfileRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.data.db.entity.WallMaterialRow;
import com.netfieldpro.data.db.entity.WallRow;
import com.netfieldpro.data.db.mapper.AssessmentMapper;
import com.netfieldpro.data.db.mapper.FloorBoundaryMapper;
import com.netfieldpro.data.db.mapper.FloorMapper;
import com.netfieldpro.data.db.mapper.LatencyResultMapper;
import com.netfieldpro.data.db.mapper.MeasurementMapper;
import com.netfieldpro.data.db.mapper.ProjectMapper;
import com.netfieldpro.data.db.mapper.RequirementProfileMapper;
import com.netfieldpro.data.db.mapper.SurveyPointMapper;
import com.netfieldpro.data.db.mapper.SurveySessionMapper;
import com.netfieldpro.data.db.mapper.TargetNetworkMapper;
import com.netfieldpro.data.db.mapper.WallMapper;
import com.netfieldpro.data.db.mapper.WallMaterialMapper;

public class SurveyReadRepository {

    private final NetFieldDao dao;

    public SurveyReadRepository(NetFieldDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao cannot be null");
    }

    public static SurveyReadRepository create(Context context) {
        NetFieldDatabase db = NetFieldDatabase.get(context);
        return new SurveyReadRepository(db.netFieldDao());
    }

    public static record PointRecord(
            SurveyPoint point,
            List<Measurement> measurements,
            LatencyResult latency
    ) {
    }

    public static record SessionGraph(
            SurveySession session,
            Floor floor,
            Project project,
            @Nullable FloorBoundary boundary,
            List<Wall> walls,
            List<WallMaterial> wallMaterials,
            List<TargetNetwork> targetNetworks,
            List<PointRecord> points,
            @Nullable RequirementProfile profile,
            @Nullable Assessment assessment
    ) {
        public boolean hasBoundary() {
            return boundary != null && boundary.vertices() != null && boundary.vertices().size() >= 3;
        }

        public Polygon toBoundaryPolygon() {
            if (hasBoundary()) {
                List<Vertex> vList = new ArrayList<>();
                for (FloorBoundary.Vertex v : boundary.vertices()) {
                    vList.add(new Vertex(v.x(), v.y()));
                }
                return new Polygon(vList);
            }
            if (floor != null && floor.metersPerPixel() > 0.0) {
                double w = floor.planWpx() * floor.metersPerPixel();
                double h = floor.planHpx() * floor.metersPerPixel();
                return new Polygon(List.of(
                        new Vertex(0.0, 0.0),
                        new Vertex(w, 0.0),
                        new Vertex(w, h),
                        new Vertex(0.0, h)
                ));
            }
            return new Polygon(List.of(
                    new Vertex(0.0, 0.0),
                    new Vertex(40.0, 0.0),
                    new Vertex(40.0, 25.0),
                    new Vertex(0.0, 25.0)
            ));
        }

        public List<WallChain> toWallChains() {
            if (walls == null || walls.isEmpty()) {
                return Collections.emptyList();
            }
            List<WallChain> chains = new ArrayList<>();
            for (Wall w : walls) {
                if (w.vertices() != null && w.vertices().size() >= 2) {
                    List<Vertex> vList = new ArrayList<>();
                    for (Wall.Vertex v : w.vertices()) {
                        vList.add(new Vertex(v.x(), v.y()));
                    }
                    chains.add(new WallChain(w.id(), w.materialId().name(), vList));
                }
            }
            return chains;
        }

        public Map<String, Double> toWallPenalties() {
            Map<String, Double> map = new HashMap<>();
            if (wallMaterials != null) {
                for (WallMaterial wm : wallMaterials) {
                    map.put(wm.materialId().name(), wm.penaltyM());
                }
            }
            return map;
        }

        public PerPoint[] toPerPoints() {
            if (points == null || points.isEmpty()) {
                return new PerPoint[0];
            }
            Set<String> targetSsids = new HashSet<>();
            if (targetNetworks != null) {
                for (TargetNetwork tn : targetNetworks) {
                    if (tn.ssid() != null && !tn.ssid().isEmpty()) {
                        targetSsids.add(tn.ssid());
                    }
                }
            }

            PerPoint[] result = new PerPoint[points.size()];
            for (int i = 0; i < points.size(); i++) {
                PointRecord pr = points.get(i);
                SurveyPoint pt = pr.point();
                Map<String, Integer> bssidTenths = new LinkedHashMap<>();
                Map<String, Integer> freqMHz = new LinkedHashMap<>();

                if (pr.measurements() != null) {
                    for (Measurement m : pr.measurements()) {
                        if (targetSsids.isEmpty() || targetSsids.contains(m.ssid())) {
                            int tenths = (int) Math.round(m.rssiMedian() * 10.0);
                            bssidTenths.put(m.bssidOrId(), tenths);
                            freqMHz.put(m.bssidOrId(), m.freqMHz());
                        }
                    }
                }

                PerPoint.Latency lat = null;
                if (pr.latency() != null && pr.latency().sent() > 0) {
                    LatencyResult lr = pr.latency();
                    List<Long> rttsUs = new ArrayList<>();
                    if (lr.rtts() != null) {
                        for (Double rttMs : lr.rtts()) {
                            rttsUs.add(Math.round(rttMs * 1000.0));
                        }
                    }
                    Long jitterMilli = lr.jitterMs() != null ? Math.round(lr.jitterMs() * 1000.0) : null;
                    lat = new PerPoint.Latency(rttsUs, lr.lossCount(), lr.sent(), jitterMilli);
                }

                result[i] = new PerPoint(pt.x(), pt.y(), bssidTenths, freqMHz, lat);
            }
            return result;
        }

        public List<Sample> samplesForCoverage() {
            if (points == null || points.isEmpty()) {
                return Collections.emptyList();
            }
            PerPoint[] pp = toPerPoints();
            List<Sample> list = new ArrayList<>();
            for (int i = 0; i < points.size(); i++) {
                PointRecord pr = points.get(i);
                double dbm = pp[i].coverageTenths() / 10.0;
                list.add(new Sample(pr.point().id(), pr.point().x(), pr.point().y(), dbm, 1.0));
            }
            return list;
        }

        public List<Sample> samplesForBand(int minFreq, int maxFreq) {
            if (points == null || points.isEmpty()) {
                return Collections.emptyList();
            }
            Set<String> targetSsids = new HashSet<>();
            if (targetNetworks != null) {
                for (TargetNetwork tn : targetNetworks) {
                    if (tn.ssid() != null && !tn.ssid().isEmpty()) {
                        targetSsids.add(tn.ssid());
                    }
                }
            }

            List<Sample> list = new ArrayList<>();
            for (PointRecord pr : points) {
                double bestDbm = -95.0;
                boolean heard = false;
                if (pr.measurements() != null) {
                    for (Measurement m : pr.measurements()) {
                        if (targetSsids.isEmpty() || targetSsids.contains(m.ssid())) {
                            if (m.freqMHz() >= minFreq && m.freqMHz() <= maxFreq) {
                                if (!heard || m.rssiMedian() > bestDbm) {
                                    bestDbm = m.rssiMedian();
                                    heard = true;
                                }
                            }
                        }
                    }
                }
                list.add(new Sample(pr.point().id(), pr.point().x(), pr.point().y(), bestDbm, 1.0));
            }
            return list;
        }

        public List<Sample> samplesForLatency() {
            if (points == null || points.isEmpty()) {
                return Collections.emptyList();
            }
            List<Sample> list = new ArrayList<>();
            for (PointRecord pr : points) {
                if (pr.latency() != null && pr.latency().avgMs() != null) {
                    list.add(new Sample(pr.point().id(), pr.point().x(), pr.point().y(), pr.latency().avgMs(), 1.0));
                }
            }
            return list;
        }

        public boolean hasBandData(int minFreq, int maxFreq) {
            if (points == null) return false;
            for (PointRecord pr : points) {
                if (pr.measurements() != null) {
                    for (Measurement m : pr.measurements()) {
                        if (m.freqMHz() >= minFreq && m.freqMHz() <= maxFreq) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public boolean hasLatencyData() {
            if (points == null) return false;
            for (PointRecord pr : points) {
                if (pr.latency() != null && pr.latency().avgMs() != null) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nullable
    public SessionGraph loadSessionGraph(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        SurveySessionRow sessionRow = dao.getSurveySession(sessionId);
        if (sessionRow == null) {
            return null;
        }
        SurveySession session = SurveySessionMapper.toDomain(sessionRow);

        FloorRow floorRow = dao.getFloor(session.floorId());
        Floor floor = (floorRow != null) ? FloorMapper.toDomain(floorRow) : null;

        ProjectRow projectRow = (floor != null) ? dao.getProject(floor.projectId()) : null;
        Project project = (projectRow != null) ? ProjectMapper.toDomain(projectRow) : null;

        FloorBoundaryRow boundaryRow = dao.getFloorBoundary(session.floorId());
        FloorBoundary boundary = (boundaryRow != null) ? FloorBoundaryMapper.toDomain(boundaryRow) : null;

        List<WallRow> wallRows = dao.getWallsForFloor(session.floorId());
        List<Wall> walls = new ArrayList<>();
        if (wallRows != null) {
            for (WallRow wr : wallRows) {
                walls.add(WallMapper.toDomain(wr));
            }
        }

        List<WallMaterialRow> matRows = dao.getAllWallMaterials();
        List<WallMaterial> wallMaterials = new ArrayList<>();
        if (matRows != null) {
            for (WallMaterialRow mr : matRows) {
                wallMaterials.add(WallMaterialMapper.toDomain(mr));
            }
        }

        List<TargetNetworkRow> targetRows = dao.getTargetNetworksForSession(sessionId);
        List<TargetNetwork> targets = new ArrayList<>();
        if (targetRows != null) {
            for (TargetNetworkRow tr : targetRows) {
                targets.add(TargetNetworkMapper.toDomain(tr));
            }
        }

        List<SurveyPointRow> pointRows = dao.getSurveyPointsForSession(sessionId);
        List<MeasurementRow> allMeasurements = dao.getMeasurementsForSession(sessionId);
        Map<String, List<Measurement>> measurementsByPoint = new HashMap<>();
        if (allMeasurements != null) {
            for (MeasurementRow mr : allMeasurements) {
                measurementsByPoint.computeIfAbsent(mr.pointId, k -> new ArrayList<>())
                        .add(MeasurementMapper.toDomain(mr));
            }
        }

        List<LatencyResultRow> allLatency = dao.getLatencyResultsForSession(sessionId);
        Map<String, LatencyResult> latencyByPoint = new HashMap<>();
        if (allLatency != null) {
            for (LatencyResultRow lr : allLatency) {
                latencyByPoint.put(lr.pointId, LatencyResultMapper.toDomain(lr));
            }
        }

        List<PointRecord> pointRecords = new ArrayList<>();
        if (pointRows != null) {
            for (SurveyPointRow pr : pointRows) {
                SurveyPoint pt = SurveyPointMapper.toDomain(pr);
                List<Measurement> measurements = measurementsByPoint.getOrDefault(pt.id(), Collections.emptyList());
                LatencyResult latency = latencyByPoint.get(pt.id());
                pointRecords.add(new PointRecord(pt, measurements, latency));
            }
        }

        RequirementProfile profile = null;
        if (session.intendedProfileId() != null && !session.intendedProfileId().isEmpty()) {
            RequirementProfileRow profileRow = dao.getRequirementProfile(session.intendedProfileId());
            if (profileRow != null) {
                profile = RequirementProfileMapper.toDomain(profileRow);
            }
        }

        Assessment assessment = null;
        if (session.evidenceSha256() != null && !session.evidenceSha256().isEmpty()) {
            AssessmentRow assRow = dao.getAssessmentByEvidence(session.evidenceSha256());
            if (assRow != null) {
                assessment = AssessmentMapper.toDomain(assRow);
            }
        }

        return new SessionGraph(
                session,
                floor,
                project,
                boundary,
                walls,
                wallMaterials,
                targets,
                pointRecords,
                profile,
                assessment
        );
    }

    public List<SurveySession> getSessionsForFloor(String floorId) {
        List<SurveySessionRow> rows = dao.getSessionsForFloor(floorId);
        List<SurveySession> result = new ArrayList<>();
        if (rows != null) {
            for (SurveySessionRow r : rows) {
                result.add(SurveySessionMapper.toDomain(r));
            }
        }
        return result;
    }

    public int countPointsForSession(String sessionId) {
        List<SurveyPointRow> rows = dao.getSurveyPointsForSession(sessionId);
        return rows != null ? rows.size() : 0;
    }
}
