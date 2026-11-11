package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.AccessPointMarker;
import com.netfieldpro.data.db.entity.AccessPointMarkerRow;

public final class AccessPointMarkerMapper {

    private AccessPointMarkerMapper() {
    }

    public static AccessPointMarkerRow toRow(AccessPointMarker a) {
        AccessPointMarkerRow row = new AccessPointMarkerRow();
        row.id = a.id();
        row.floorId = a.floorId();
        row.x = a.x();
        row.y = a.y();
        row.bssid = a.bssid();
        row.txPowerDbm = a.txPowerDbm();
        return row;
    }

    public static AccessPointMarker toDomain(AccessPointMarkerRow row) {
        return new AccessPointMarker(
                row.id, row.floorId, row.x, row.y, row.bssid, row.txPowerDbm
        );
    }
}

