package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.ConnectionEvent;
import com.netfieldpro.core.model.enums.ConnectionEventType;
import com.netfieldpro.data.db.entity.ConnectionEventRow;

public final class ConnectionEventMapper {

    private ConnectionEventMapper() {
    }

    public static ConnectionEventRow toRow(ConnectionEvent c) {
        ConnectionEventRow row = new ConnectionEventRow();
        row.id = c.id();
        row.sessionId = c.sessionId();
        row.type = c.type().name();
        row.atNs = c.atNs();
        row.fromBssid = c.fromBssid();
        row.toBssid = c.toBssid();
        row.pointId = c.pointId();
        return row;
    }

    public static ConnectionEvent toDomain(ConnectionEventRow row) {
        return new ConnectionEvent(
                row.id, row.sessionId, ConnectionEventType.valueOf(row.type),
                row.atNs, row.fromBssid, row.toBssid, row.pointId
        );
    }
}

