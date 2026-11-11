package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;

public record TargetNetwork(
        String id,
        String sessionId,
        String ssid
) {
    public TargetNetwork {
        ModelValidator.checkId("TargetNetwork", "id", id);
        ModelValidator.checkId("TargetNetwork", "sessionId", sessionId);
        ModelValidator.checkRequired("TargetNetwork", "ssid", ssid);
        ModelValidator.checkString("TargetNetwork", "ssid", ssid, 1024);
    }
}

