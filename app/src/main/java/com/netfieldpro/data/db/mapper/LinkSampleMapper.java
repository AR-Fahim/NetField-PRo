package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.LinkSample;
import com.netfieldpro.data.db.entity.LinkSampleRow;

public final class LinkSampleMapper {

    private LinkSampleMapper() {
    }

    public static LinkSampleRow toRow(LinkSample l) {
        LinkSampleRow row = new LinkSampleRow();
        row.id = l.id();
        row.sessionId = l.sessionId();
        row.observedAtNs = l.observedAtNs();
        row.bssid = l.bssid();
        row.ssid = l.ssid();
        row.rssi = l.rssi();
        row.freqMHz = l.freqMHz();
        row.txMbps = l.txMbps();
        row.rxMbps = l.rxMbps();
        return row;
    }

    public static LinkSample toDomain(LinkSampleRow row) {
        return new LinkSample(
                row.id, row.sessionId, row.observedAtNs, row.bssid, row.ssid,
                row.rssi, row.freqMHz, row.txMbps, row.rxMbps
        );
    }
}

