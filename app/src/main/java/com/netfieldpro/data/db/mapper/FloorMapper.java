package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.enums.ScaleMethod;
import com.netfieldpro.data.db.entity.FloorRow;

public final class FloorMapper {

    private FloorMapper() {
    }

    public static FloorRow toRow(Floor f) {
        FloorRow row = new FloorRow();
        row.id = f.id();
        row.projectId = f.projectId();
        row.levelIndex = f.levelIndex();
        row.planSha256 = f.planSha256();
        row.planWpx = f.planWpx();
        row.planHpx = f.planHpx();
        row.metersPerPixel = f.metersPerPixel();
        row.northOffsetDeg = f.northOffsetDeg();
        row.scaleMethod = f.scaleMethod().name();
        row.scaleUncertaintyPct = f.scaleUncertaintyPct();
        row.scaleChecked = f.scaleChecked();
        row.alignX = f.alignX();
        row.alignY = f.alignY();
        row.alignRotDeg = f.alignRotDeg();
        if (f.scaleP1() != null) {
            row.scaleP1X = f.scaleP1().x();
            row.scaleP1Y = f.scaleP1().y();
        }
        if (f.scaleP2() != null) {
            row.scaleP2X = f.scaleP2().x();
            row.scaleP2Y = f.scaleP2().y();
        }
        return row;
    }

    public static Floor toDomain(FloorRow row) {
        Floor.Point p1 = null;
        if (row.scaleP1X != null && row.scaleP1Y != null) {
            p1 = new Floor.Point(row.scaleP1X, row.scaleP1Y);
        }
        Floor.Point p2 = null;
        if (row.scaleP2X != null && row.scaleP2Y != null) {
            p2 = new Floor.Point(row.scaleP2X, row.scaleP2Y);
        }
        ScaleMethod method = ScaleMethod.LINE;
        if (row.scaleMethod != null && !row.scaleMethod.isEmpty()) {
            try {
                method = ScaleMethod.valueOf(row.scaleMethod);
            } catch (Exception ignored) {
            }
        }
        double mpp = row.metersPerPixel > 0.0 ? row.metersPerPixel : 1.0;
        return new Floor(
                row.id, row.projectId, row.levelIndex, row.planSha256, row.planWpx, row.planHpx,
                mpp, row.northOffsetDeg, method,
                row.scaleUncertaintyPct, row.scaleChecked, row.alignX, row.alignY, row.alignRotDeg,
                p1, p2
        );
    }
}

