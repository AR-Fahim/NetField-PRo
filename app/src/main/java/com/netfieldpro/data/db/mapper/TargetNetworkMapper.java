package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.TargetNetwork;
import com.netfieldpro.data.db.entity.TargetNetworkRow;

public final class TargetNetworkMapper {

    private TargetNetworkMapper() {
    }

    public static TargetNetworkRow toRow(TargetNetwork t) {
        TargetNetworkRow row = new TargetNetworkRow();
        row.id = t.id();
        row.sessionId = t.sessionId();
        row.ssid = t.ssid();
        return row;
    }

    public static TargetNetwork toDomain(TargetNetworkRow row) {
        return new TargetNetwork(row.id, row.sessionId, row.ssid);
    }
}

