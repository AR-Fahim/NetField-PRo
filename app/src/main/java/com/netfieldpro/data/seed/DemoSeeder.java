package com.netfieldpro.data.seed;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.model.entity.DeviceInfo;
import com.netfieldpro.core.model.entity.FloorBoundary;
import com.netfieldpro.core.model.entity.LatencyResult;
import com.netfieldpro.core.model.entity.Measurement;
import com.netfieldpro.core.model.entity.SurveyPoint;
import com.netfieldpro.core.model.entity.SurveySession;
import com.netfieldpro.core.model.entity.TargetNetwork;
import com.netfieldpro.core.model.entity.Wall;
import com.netfieldpro.core.model.enums.CadenceFlag;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.LatencyMethod;
import com.netfieldpro.core.model.enums.LatencyScope;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.core.model.enums.MeasurementSource;
import com.netfieldpro.core.model.enums.PointQuality;
import com.netfieldpro.core.model.enums.PointSource;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.model.enums.Protocol;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.model.enums.SessionType;
import com.netfieldpro.core.model.enums.TimingQuality;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.core.util.Sha256;
import com.netfieldpro.core.verdict.PresetProfiles;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.WallMaterialRow;
import com.netfieldpro.data.db.mapper.DeviceInfoMapper;
import com.netfieldpro.data.db.mapper.FloorBoundaryMapper;
import com.netfieldpro.data.db.mapper.LatencyResultMapper;
import com.netfieldpro.data.db.mapper.MeasurementMapper;
import com.netfieldpro.data.db.mapper.SurveyPointMapper;
import com.netfieldpro.data.db.mapper.SurveySessionMapper;
import com.netfieldpro.data.db.mapper.TargetNetworkMapper;
import com.netfieldpro.data.db.mapper.WallMapper;
import com.netfieldpro.data.repository.PlanFileStore;

public final class DemoSeeder {

    public static final String DEMO_PROJECT_NAME = "Demo Office";
    public static final String DEMO_PROJECT_SITE = "HQ";
    public static final String DEMO_FLOOR_NAME = "Level 1";

    private DemoSeeder() {
    }

    public static String seedPass(Context context) {
        return seed(context, true);
    }

    public static String seedFail(Context context) {
        return seed(context, false);
    }

    public static String seedOnLatestFloor(Context context, String variant) {
        boolean pass = variant != null && variant.equalsIgnoreCase("PASS");
        return seedOnLatestFloor(context, pass);
    }

    public static String seedOnLatestFloor(Context context, boolean pass) {
        NetFieldDao dao = NetFieldDatabase.get(context).netFieldDao();
        List<FloorRow> scaledFloors = dao.getScaledFloorsDesc();
        if (scaledFloors == null || scaledFloors.isEmpty()) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                    android.widget.Toast.makeText(context, "Set a scale first", android.widget.Toast.LENGTH_SHORT).show()
            );
            return null;
        }

        FloorRow floor = scaledFloors.get(0);
        double mpp = floor.metersPerPixel;
        double W = floor.planWpx * mpp;
        double H = floor.planHpx * mpp;

        ensureWallMaterials(dao);
        ProfileSeeder.seed(dao);

        String floorId = floor.id;

        // DeviceInfo
        String devId = IdGenerator.generate();
        DeviceInfo dev = new DeviceInfo(
                devId,
                "Pixel 8",
                "Google",
                34,
                "802.11ax",
                3000L,
                "google/shiba/shiba:14/UD1A.230805.019/10632420:user/release-keys"
        );
        dao.insertDeviceInfo(DeviceInfoMapper.toRow(dev));

        // SurveySession
        String sessId = IdGenerator.generate();
        String intendedProfileId = PresetProfiles.idFor(PresetProfiles.OFFICE_STANDARD.key());
        SurveySession session = new SurveySession(
                sessId,
                floorId,
                "Demo survey (simulated)",
                SessionState.DRAFT,
                1700000000000L,
                1000000000L,
                1,
                "1.0",
                "1.0",
                false,
                TimingQuality.GOOD,
                EvidenceGrade.VERDICT,
                SessionType.STANDARD,
                intendedProfileId,
                0L,
                0.0,
                3000L,
                CadenceFlag.DEFAULT,
                ProbeMethod.ICMP,
                List.of(2437L, 5180L),
                "builtin-wifi",
                devId,
                new JsonObject(),
                null,
                null,
                null
        );
        dao.insertSurveySession(SurveySessionMapper.toRow(session));

        // TargetNetwork
        String tnId = IdGenerator.generate();
        TargetNetwork tn = new TargetNetwork(tnId, sessId, "DemoNet");
        dao.insertTargetNetwork(TargetNetworkMapper.toRow(tn));

        // Geometry:
        // Boundary rectangle (0.05W,0.05H)-(0.95W,0.95H)
        dao.deleteFloorBoundary(floorId);
        FloorBoundary boundary = new FloorBoundary(floorId, List.of(
                new FloorBoundary.Vertex(0.05 * W, 0.05 * H),
                new FloorBoundary.Vertex(0.95 * W, 0.05 * H),
                new FloorBoundary.Vertex(0.95 * W, 0.95 * H),
                new FloorBoundary.Vertex(0.05 * W, 0.95 * H)
        ));
        dao.insertFloorBoundary(FloorBoundaryMapper.toRow(boundary));

        // One DRYWALL wall (0.5W,0)-(0.5W,H)
        dao.deleteWallsForFloor(floorId);
        Wall wall = new Wall(
                IdGenerator.generate(),
                floorId,
                MaterialId.DRYWALL_LIGHT,
                List.of(
                        new Wall.Vertex(0.5 * W, 0.0),
                        new Wall.Vertex(0.5 * W, H)
                )
        );
        dao.insertWall(WallMapper.toRow(wall));

        // Lattice 6x4: x = W*(0.10 + 0.16*i) for i=0..5, y = H*(0.15 + 0.23*j) for j=0..3
        for (int i = 0; i <= 5; i++) {
            for (int j = 0; j <= 3; j++) {
                double x = W * (0.10 + 0.16 * i);
                double y = H * (0.15 + 0.23 * j);
                String ptId = IdGenerator.generate();

                SurveyPoint pt = new SurveyPoint(
                        ptId,
                        sessId,
                        x,
                        y,
                        PointSource.TAP,
                        PointQuality.OK,
                        1000000000L + (i * 4 + j) * 1000000000L,
                        1700000000000L + (i * 4 + j) * 1000L,
                        2,
                        0.2,
                        null
                );
                dao.insertSurveyPoint(SurveyPointMapper.toRow(pt));

                // AP1 at (0.2W, 0.25H) freq 2437
                double d1 = Math.hypot(x - 0.2 * W, y - 0.25 * H);
                double rssi1 = Math.round((-40.0 - 20.0 * Math.log10(Math.max(d1, 1.0))) * 10.0) / 10.0;
                int min1 = (int) Math.floor(rssi1);
                int max1 = (int) Math.ceil(rssi1);
                Measurement m1 = new Measurement(
                        IdGenerator.generate(),
                        ptId,
                        Protocol.WIFI,
                        "02:aa:00:00:00:01",
                        "DemoNet",
                        2437,
                        6,
                        20,
                        MeasurementSource.SCAN,
                        "builtin-wifi",
                        2,
                        rssi1,
                        min1,
                        max1,
                        null,
                        null
                );
                dao.insertMeasurement(MeasurementMapper.toRow(m1));

                // AP2 at (0.75W, 0.7H) freq 5180 in PASS variant only
                if (pass) {
                    double d2 = Math.hypot(x - 0.75 * W, y - 0.7 * H);
                    double rssi2 = Math.round((-40.0 - 20.0 * Math.log10(Math.max(d2, 1.0)) - 6.0) * 10.0) / 10.0;
                    int min2 = (int) Math.floor(rssi2);
                    int max2 = (int) Math.ceil(rssi2);
                    Measurement m2 = new Measurement(
                            IdGenerator.generate(),
                            ptId,
                            Protocol.WIFI,
                            "02:aa:00:00:00:02",
                            "DemoNet",
                            5180,
                            36,
                            80,
                            MeasurementSource.SCAN,
                            "builtin-wifi",
                            2,
                            rssi2,
                            min2,
                            max2,
                            null,
                            null
                    );
                    dao.insertMeasurement(MeasurementMapper.toRow(m2));
                }

                // Latency 5 probes of 12 000 us (12.0 ms)
                boolean isFailingPoint = !pass && (i == 5 && j == 0);
                int sent = 5;
                int lost = isFailingPoint ? 2 : 0;
                int recv = sent - lost;
                List<Double> rtts = new ArrayList<>();
                for (int k = 0; k < recv; k++) {
                    rtts.add(12.0);
                }
                Double jitter = (recv >= 2) ? 0.0 : null;

                LatencyResult lr = new LatencyResult(
                        IdGenerator.generate(),
                        ptId,
                        LatencyMethod.ICMP,
                        "192.168.1.1",
                        "wlan0",
                        LatencyScope.GATEWAY,
                        sent,
                        recv,
                        12.0,
                        12.0,
                        12.0,
                        jitter,
                        lost,
                        false,
                        "02:aa:00:00:00:01",
                        "DemoNet",
                        rtts
                );
                dao.insertLatencyResult(LatencyResultMapper.toRow(lr));
            }
        }

        return sessId;
    }

    public static String seed(Context context, boolean pass) {
        PlanFileStore planFileStore = NetFieldApp.planFileStore();
        NetFieldDao dao = NetFieldDatabase.get(context).netFieldDao();

        // Ensure wall materials exist
        ensureWallMaterials(dao);

        // Delete previous Demo Office project if exists
        List<ProjectRow> existingProjects = dao.getAllProjects();
        if (existingProjects != null) {
            for (ProjectRow p : existingProjects) {
                if (DEMO_PROJECT_NAME.equals(p.name)) {
                    List<FloorRow> floors = dao.getFloorsForProject(p.id);
                    if (floors != null) {
                        for (FloorRow f : floors) {
                            planFileStore.deletePlanFile(f.id);
                        }
                    }
                    dao.deleteProject(p.id);
                }
            }
        }

        // Seed preset requirement profiles
        ProfileSeeder.seed(dao);

        // 1. Create project "Demo Office" (site "HQ")
        String projId = IdGenerator.generate();
        ProjectRow projectRow = new ProjectRow(
                projId,
                DEMO_PROJECT_NAME,
                "Demo Client",
                DEMO_PROJECT_SITE,
                1700000000000L
        );
        dao.insertProject(projectRow);

        // 2. Generate 1600x1000 PNG plan
        int planW = 1600;
        int planH = 1000;
        Bitmap bmp = Bitmap.createBitmap(planW, planH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.0f);
        canvas.drawRect(50, 50, 1550, 950, paint);
        canvas.drawRect(50, 50, 800, 500, paint);
        canvas.drawRect(800, 50, 1550, 500, paint);
        canvas.drawRect(50, 500, 800, 950, paint);
        canvas.drawRect(800, 500, 1550, 950, paint);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] pngBytes = baos.toByteArray();
        String planSha256 = Sha256.hex(Sha256.hash(pngBytes));

        String floorId = IdGenerator.generate();
        try {
            planFileStore.savePlanBytes(floorId, "png", pngBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save plan file", e);
        }

        // Scale is LINE calibration so that image width equals 40m (mpp 0.025)
        FloorRow floorRow = new FloorRow();
        floorRow.id = floorId;
        floorRow.projectId = projId;
        floorRow.levelIndex = 1L;
        floorRow.planSha256 = planSha256;
        floorRow.planWpx = planW;
        floorRow.planHpx = planH;
        floorRow.metersPerPixel = 0.025;
        floorRow.scaleMethod = "LINE";
        floorRow.scaleUncertaintyPct = 0.1;
        floorRow.scaleChecked = true;
        floorRow.scaleP1X = 0.0;
        floorRow.scaleP1Y = 500.0;
        floorRow.scaleP2X = 1600.0;
        floorRow.scaleP2Y = 500.0;
        dao.insertFloor(floorRow);

        // 3. DeviceInfo
        String devId = IdGenerator.generate();
        DeviceInfo dev = new DeviceInfo(
                devId,
                "Pixel 8",
                "Google",
                34,
                "802.11ax",
                3000L,
                "google/shiba/shiba:14/UD1A.230805.019/10632420:user/release-keys"
        );
        dao.insertDeviceInfo(DeviceInfoMapper.toRow(dev));

        // 4. SurveySession
        String sessId = IdGenerator.generate();
        String intendedProfileId = PresetProfiles.idFor(PresetProfiles.OFFICE_STANDARD.key());
        SurveySession session = new SurveySession(
                sessId,
                floorId,
                "Survey Level 1",
                SessionState.DRAFT,
                1700000000000L,
                1000000000L,
                1,
                "1.0",
                "1.0",
                false,
                TimingQuality.GOOD,
                EvidenceGrade.VERDICT,
                SessionType.STANDARD,
                intendedProfileId,
                0L,
                0.0,
                3000L,
                CadenceFlag.DEFAULT,
                ProbeMethod.ICMP,
                List.of(2437L, 5180L),
                "builtin-wifi",
                devId,
                new JsonObject(),
                null,
                null,
                null
        );
        dao.insertSurveySession(SurveySessionMapper.toRow(session));

        // 5. TargetNetwork
        String tnId = IdGenerator.generate();
        TargetNetwork tn = new TargetNetwork(tnId, sessId, "DemoNet");
        dao.insertTargetNetwork(TargetNetworkMapper.toRow(tn));

        // 6. Boundary rectangle (2,2)-(38,23) & One DRYWALL wall chain (20,0)-(20,25)
        FloorBoundary boundary = new FloorBoundary(floorId, List.of(
                new FloorBoundary.Vertex(2.0, 2.0),
                new FloorBoundary.Vertex(38.0, 2.0),
                new FloorBoundary.Vertex(38.0, 23.0),
                new FloorBoundary.Vertex(2.0, 23.0)
        ));
        dao.insertFloorBoundary(FloorBoundaryMapper.toRow(boundary));

        Wall wall = new Wall(
                IdGenerator.generate(),
                floorId,
                MaterialId.DRYWALL_LIGHT,
                List.of(
                        new Wall.Vertex(20.0, 0.0),
                        new Wall.Vertex(20.0, 25.0)
                )
        );
        dao.insertWall(WallMapper.toRow(wall));

        // 7. 24 points on a 6x4 lattice at x = 4+6.4*i (i=0..5), y = 3+5.5*j (j=0..3)
        for (int i = 0; i <= 5; i++) {
            for (int j = 0; j <= 3; j++) {
                double x = 4.0 + 6.4 * i;
                double y = 3.0 + 5.5 * j;
                String ptId = IdGenerator.generate();

                SurveyPoint pt = new SurveyPoint(
                        ptId,
                        sessId,
                        x,
                        y,
                        PointSource.TAP,
                        PointQuality.OK,
                        1000000000L + (i * 4 + j) * 1000000000L,
                        1700000000000L + (i * 4 + j) * 1000L,
                        2,
                        0.2,
                        null
                );
                dao.insertSurveyPoint(SurveyPointMapper.toRow(pt));

                // AP1 at (8,6) freq 2437
                double d1 = Math.hypot(x - 8.0, y - 6.0);
                double rssi1 = Math.round((-40.0 - 20.0 * Math.log10(Math.max(d1, 1.0))) * 10.0) / 10.0;
                int min1 = (int) Math.floor(rssi1);
                int max1 = (int) Math.ceil(rssi1);
                Measurement m1 = new Measurement(
                        IdGenerator.generate(),
                        ptId,
                        Protocol.WIFI,
                        "02:aa:00:00:00:01",
                        "DemoNet",
                        2437,
                        6,
                        20,
                        MeasurementSource.SCAN,
                        "builtin-wifi",
                        2,
                        rssi1,
                        min1,
                        max1,
                        null,
                        null
                );
                dao.insertMeasurement(MeasurementMapper.toRow(m1));

                // AP2 at (30,16) freq 5180 in PASS variant only
                if (pass) {
                    double d2 = Math.hypot(x - 30.0, y - 16.0);
                    double rssi2 = Math.round((-40.0 - 20.0 * Math.log10(Math.max(d2, 1.0)) - 6.0) * 10.0) / 10.0;
                    int min2 = (int) Math.floor(rssi2);
                    int max2 = (int) Math.ceil(rssi2);
                    Measurement m2 = new Measurement(
                            IdGenerator.generate(),
                            ptId,
                            Protocol.WIFI,
                            "02:aa:00:00:00:02",
                            "DemoNet",
                            5180,
                            36,
                            80,
                            MeasurementSource.SCAN,
                            "builtin-wifi",
                            2,
                            rssi2,
                            min2,
                            max2,
                            null,
                            null
                    );
                    dao.insertMeasurement(MeasurementMapper.toRow(m2));
                }

                // Latency
                boolean isFailingPoint = !pass && (i == 5 && j == 0);
                int sent = 5;
                int lost = isFailingPoint ? 2 : 0;
                int recv = sent - lost;
                List<Double> rtts = new ArrayList<>();
                for (int k = 0; k < recv; k++) {
                    rtts.add(12.0);
                }
                Double jitter = (recv >= 2) ? 0.0 : null;

                LatencyResult lr = new LatencyResult(
                        IdGenerator.generate(),
                        ptId,
                        LatencyMethod.ICMP,
                        "192.168.1.1",
                        "wlan0",
                        LatencyScope.GATEWAY,
                        sent,
                        recv,
                        12.0,
                        12.0,
                        12.0,
                        jitter,
                        lost,
                        false,
                        "02:aa:00:00:00:01",
                        "DemoNet",
                        rtts
                );
                dao.insertLatencyResult(LatencyResultMapper.toRow(lr));
            }
        }

        return sessId;
    }

    private static void ensureWallMaterials(NetFieldDao dao) {
        if (dao.countWallMaterials() == 0) {
            List<WallMaterialRow> mats = new ArrayList<>();
            mats.add(makeMat("DRYWALL_LIGHT", 3.0, 3.0, 4.0));
            mats.add(makeMat("GLASS_PARTITION", 2.5, 2.0, 3.0));
            mats.add(makeMat("BRICK_MEDIUM", 8.0, 6.0, 10.0));
            mats.add(makeMat("CONCRETE_HEAVY", 15.0, 12.0, 20.0));
            mats.add(makeMat("METAL_ELEVATOR", 25.0, 20.0, 30.0));
            dao.insertWallMaterials(mats);
        }
    }

    private static WallMaterialRow makeMat(String id, double penalty, double l24, double l5) {
        WallMaterialRow r = new WallMaterialRow();
        r.materialId = id;
        r.penaltyM = penalty;
        r.lossDb24 = l24;
        r.lossDb5 = l5;
        return r;
    }
}
