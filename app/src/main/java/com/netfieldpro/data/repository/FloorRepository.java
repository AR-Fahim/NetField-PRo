package com.netfieldpro.data.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.netfieldpro.core.entitlement.QuotaDecision;
import com.netfieldpro.core.entitlement.Resource;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.FloorBoundary;
import com.netfieldpro.core.model.entity.Wall;
import com.netfieldpro.core.model.enums.ScaleMethod;
import com.netfieldpro.core.project.FloorSort;
import com.netfieldpro.core.project.LevelRules;
import com.netfieldpro.core.util.FixedDecimal;
import com.netfieldpro.data.db.BatchedWriter;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.WriteCallback;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorBoundaryRow;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.WallRow;
import com.netfieldpro.data.db.mapper.FloorBoundaryMapper;
import com.netfieldpro.data.db.mapper.FloorMapper;
import com.netfieldpro.data.db.mapper.WallMapper;

public class FloorRepository {

    private final NetFieldDatabase db;
    private final NetFieldDao dao;
    private final BatchedWriter writer;
    private final PlanFileStore planFileStore;
    private final QuotaGuard quotaGuard;

    public FloorRepository(
            NetFieldDatabase db,
            BatchedWriter writer,
            PlanFileStore planFileStore,
            QuotaGuard quotaGuard
    ) {
        this.db = Objects.requireNonNull(db, "db cannot be null");
        this.dao = db.netFieldDao();
        this.writer = Objects.requireNonNull(writer, "writer cannot be null");
        this.planFileStore = Objects.requireNonNull(planFileStore, "planFileStore cannot be null");
        this.quotaGuard = Objects.requireNonNull(quotaGuard, "quotaGuard cannot be null");
    }

    public static FloorRepository create(
            android.content.Context context,
            com.netfieldpro.entitlement.EntitlementRepository entitlements
    ) {
        NetFieldDatabase db = NetFieldDatabase.get(context);
        return new FloorRepository(
                db,
                new BatchedWriter(db),
                new PlanFileStore(context),
                new QuotaGuard(db.netFieldDao(), entitlements)
        );
    }

    public List<Floor> getFloorsForProject(String projectId) {
        List<FloorRow> rows = dao.getFloorsForProject(projectId);
        List<Floor> list = new ArrayList<>();
        if (rows != null) {
            for (FloorRow r : rows) {
                list.add(FloorMapper.toDomain(r));
            }
        }
        return FloorSort.sort(list);
    }

    public Floor getFloor(String id) {
        FloorRow row = dao.getFloor(id);
        return row != null ? FloorMapper.toDomain(row) : null;
    }

    public long getNextDefaultLevel(String projectId) {
        List<FloorRow> rows = dao.getFloorsForProject(projectId);
        List<Long> levels = new ArrayList<>();
        if (rows != null) {
            for (FloorRow r : rows) {
                levels.add(r.levelIndex);
            }
        }
        return LevelRules.defaultNextLevel(levels);
    }

    public boolean isRecalibrationBlocked(String floorId) {
        return dao.countFinalizedSessionsForFloor(floorId) > 0;
    }

    public Floor createFloor(Floor floor, File storedPlanFile) {
        QuotaDecision decision = quotaGuard.check(Resource.FLOOR);
        if (!decision.allowed()) {
            if (storedPlanFile != null && storedPlanFile.exists()) {
                storedPlanFile.delete();
            }
            planFileStore.deletePlanFile(floor.id());
            throw new IllegalStateException("FLOOR quota exceeded");
        }

        FloorRow row = FloorMapper.toRow(floor);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        writer.submit(database -> database.netFieldDao().insertFloor(row), new WriteCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(Throwable t) {
                errorRef.set(t);
                if (storedPlanFile != null && storedPlanFile.exists()) {
                    storedPlanFile.delete();
                }
                planFileStore.deletePlanFile(floor.id());
            }
        });
        writer.flush();

        if (errorRef.get() != null) {
            Throwable t = errorRef.get();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException("Insert floor failed", t);
        }

        return floor;
    }

    public void updateScale(
            String floorId,
            double mpp,
            double uncertaintyPct,
            ScaleMethod method,
            boolean checked,
            Floor.Point p1,
            Floor.Point p2
    ) {
        if (isRecalibrationBlocked(floorId)) {
            throw new IllegalStateException("Scale locked: session finalized");
        }

        double roundedMpp = Double.parseDouble(FixedDecimal.format(mpp, 6));
        double roundedUnc = Double.parseDouble(FixedDecimal.format(uncertaintyPct, 3));
        Double p1x = p1 != null ? p1.x() : null;
        Double p1y = p1 != null ? p1.y() : null;
        Double p2x = p2 != null ? p2.x() : null;
        Double p2y = p2 != null ? p2.y() : null;

        writer.submit(database -> database.netFieldDao().updateFloorScale(
                floorId,
                roundedMpp,
                roundedUnc,
                method.name(),
                checked,
                p1x, p1y, p2x, p2y
        ), null);
        writer.flush();
    }

    public void deleteFloor(String floorId) {
        planFileStore.deletePlanFile(floorId);
        writer.submit(database -> database.netFieldDao().deleteFloor(floorId), null);
        writer.flush();
    }

    public FloorBoundary getBoundary(String floorId) {
        FloorBoundaryRow row = dao.getFloorBoundary(floorId);
        return (row != null && row.vertices != null && !row.vertices.isEmpty())
                ? FloorBoundaryMapper.toDomain(row)
                : null;
    }

    public void saveBoundary(FloorBoundary boundary) {
        FloorBoundaryRow row = FloorBoundaryMapper.toRow(boundary);
        writer.submit(database -> database.netFieldDao().insertFloorBoundary(row), null);
        writer.flush();
    }

    public void deleteBoundary(String floorId) {
        writer.submit(database -> database.netFieldDao().deleteFloorBoundary(floorId), null);
        writer.flush();
    }

    public List<Wall> getWalls(String floorId) {
        List<WallRow> rows = dao.getWallsForFloor(floorId);
        List<Wall> list = new ArrayList<>();
        if (rows != null) {
            for (WallRow r : rows) {
                list.add(WallMapper.toDomain(r));
            }
        }
        return list;
    }

    public void saveWalls(String floorId, List<Wall> walls) {
        writer.submit(database -> {
            database.netFieldDao().deleteWallsForFloor(floorId);
            if (walls != null && !walls.isEmpty()) {
                List<WallRow> rows = new ArrayList<>(walls.size());
                for (Wall w : walls) {
                    rows.add(WallMapper.toRow(w));
                }
                database.netFieldDao().insertWalls(rows);
            }
        }, null);
        writer.flush();
    }

    public void deleteWall(String wallId) {
        writer.submit(database -> database.netFieldDao().deleteWall(wallId), null);
        writer.flush();
    }
}
