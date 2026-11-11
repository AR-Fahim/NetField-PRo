package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.SurveyPoint;
import com.netfieldpro.core.model.enums.PointQuality;
import com.netfieldpro.core.model.enums.PointSource;
import com.netfieldpro.data.db.entity.SurveyPointRow;

public final class SurveyPointMapper {

    private SurveyPointMapper() {
    }

    public static SurveyPointRow toRow(SurveyPoint p) {
        SurveyPointRow row = new SurveyPointRow();
        row.id = p.id();
        row.sessionId = p.sessionId();
        row.x = p.x();
        row.y = p.y();
        row.source = p.source().name();
        row.quality = p.quality().name();
        row.tMonoNs = p.tMonoNs();
        row.tWallMs = p.tWallMs();
        row.nScans = p.nScans();
        row.positionUncertaintyM = p.positionUncertaintyM();
        row.segmentId = p.segmentId();
        return row;
    }

    public static SurveyPoint toDomain(SurveyPointRow row) {
        return new SurveyPoint(
                row.id, row.sessionId, row.x, row.y,
                PointSource.valueOf(row.source), PointQuality.valueOf(row.quality),
                row.tMonoNs, row.tWallMs, row.nScans, row.positionUncertaintyM, row.segmentId
        );
    }
}

