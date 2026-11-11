package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.Measurement;
import com.netfieldpro.core.model.enums.MeasurementSource;
import com.netfieldpro.core.model.enums.Protocol;
import com.netfieldpro.data.db.entity.MeasurementRow;

public final class MeasurementMapper {

    private MeasurementMapper() {
    }

    public static MeasurementRow toRow(Measurement m) {
        MeasurementRow row = new MeasurementRow();
        row.id = m.id();
        row.pointId = m.pointId();
        row.protocol = m.protocol().name();
        row.bssidOrId = m.bssidOrId();
        row.ssid = m.ssid();
        row.freqMHz = m.freqMHz();
        row.channel = m.channel();
        row.widthMHz = m.widthMHz();
        row.source = m.source().name();
        row.receiverId = m.receiverId();
        row.n = m.n();
        row.rssiMedian = m.rssiMedian();
        row.rssiMin = m.rssiMin();
        row.rssiMax = m.rssiMax();
        row.lqi = m.lqi();
        row.panId = m.panId();
        return row;
    }

    public static Measurement toDomain(MeasurementRow row) {
        return new Measurement(
                row.id, row.pointId, Protocol.valueOf(row.protocol),
                row.bssidOrId, row.ssid, row.freqMHz, row.channel, row.widthMHz,
                MeasurementSource.valueOf(row.source), row.receiverId, row.n,
                row.rssiMedian, row.rssiMin, row.rssiMax, row.lqi, row.panId
        );
    }
}

