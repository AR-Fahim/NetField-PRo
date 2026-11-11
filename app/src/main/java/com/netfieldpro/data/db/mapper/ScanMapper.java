package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.Scan;
import com.netfieldpro.core.model.enums.SweepClass;
import com.netfieldpro.data.db.entity.ScanRow;

public final class ScanMapper {

    private ScanMapper() {
    }

    public static ScanRow toRow(Scan s) {
        ScanRow row = new ScanRow();
        row.id = s.id();
        row.sessionId = s.sessionId();
        row.pointId = s.pointId();
        row.sweepClass = s.sweepClass().name();
        row.startNs = s.startNs();
        row.endNs = s.endNs();
        row.arrivalNs = s.arrivalNs();
        row.tsPlausible = s.tsPlausible();
        row.throttleOffAtScan = s.throttleOffAtScan();
        return row;
    }

    public static Scan toDomain(ScanRow row) {
        return new Scan(
                row.id, row.sessionId, row.pointId, SweepClass.valueOf(row.sweepClass),
                row.startNs, row.endNs, row.arrivalNs, row.tsPlausible, row.throttleOffAtScan
        );
    }
}

