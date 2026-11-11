package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;

public record AccessPointMarker(
        String id,
        String floorId,
        double x,
        double y,
        String bssid,
        Integer txPowerDbm
) {
    public AccessPointMarker {
        ModelValidator.checkId("AccessPointMarker", "id", id);
        ModelValidator.checkId("AccessPointMarker", "floorId", floorId);
        ModelValidator.checkDouble("AccessPointMarker", "x", x);
        ModelValidator.checkDouble("AccessPointMarker", "y", y);
        ModelValidator.checkString("AccessPointMarker", "bssid", bssid, 1024);
    }
}

