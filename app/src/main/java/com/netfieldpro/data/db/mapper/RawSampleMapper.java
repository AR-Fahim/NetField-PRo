package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.RawSample;
import com.netfieldpro.data.db.entity.RawSampleRow;

public final class RawSampleMapper {

    private RawSampleMapper() {
    }

    public static RawSampleRow toRow(RawSample r) {
        RawSampleRow row = new RawSampleRow();
        row.rowId = r.rowId();
        row.scanId = r.scanId();
        row.bssid = r.bssid();
        row.ssid = r.ssid();
        row.freqMHz = r.freqMHz();
        row.rssi = r.rssi();
        row.scanResultTsNs = r.scanResultTsNs();
        return row;
    }

    public static RawSample toDomain(RawSampleRow row) {
        return new RawSample(
                row.rowId, row.scanId, row.bssid, row.ssid,
                row.freqMHz, row.rssi, row.scanResultTsNs
        );
    }
}

