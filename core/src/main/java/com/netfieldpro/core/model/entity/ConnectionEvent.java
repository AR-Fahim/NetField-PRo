package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.ConnectionEventType;

public record ConnectionEvent(
        String id,
        String sessionId,
        ConnectionEventType type,
        long atNs,
        String fromBssid,
        String toBssid,
        String pointId
) {
    public ConnectionEvent {
        ModelValidator.checkId("ConnectionEvent", "id", id);
        ModelValidator.checkId("ConnectionEvent", "sessionId", sessionId);
        ModelValidator.checkRequired("ConnectionEvent", "type", type);
        ModelValidator.checkNonNegative("ConnectionEvent", "atNs", atNs);
        ModelValidator.checkString("ConnectionEvent", "fromBssid", fromBssid, 1024);
        ModelValidator.checkString("ConnectionEvent", "toBssid", toBssid, 1024);
        ModelValidator.checkOptionalId("ConnectionEvent", "pointId", pointId);
    }
}

