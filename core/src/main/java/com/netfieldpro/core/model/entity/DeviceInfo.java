package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;

public record DeviceInfo(
        String id,
        String model,
        String manufacturer,
        int sdk,
        String wifiStandards,
        Long linkCadenceMs,
        String buildFingerprint
) {
    public DeviceInfo {
        ModelValidator.checkId("DeviceInfo", "id", id);
        ModelValidator.checkRequired("DeviceInfo", "model", model);
        ModelValidator.checkString("DeviceInfo", "model", model, 32);
        ModelValidator.checkRequired("DeviceInfo", "manufacturer", manufacturer);
        ModelValidator.checkString("DeviceInfo", "manufacturer", manufacturer, 1024);
        ModelValidator.checkNonNegative("DeviceInfo", "sdk", sdk);
        ModelValidator.checkRequired("DeviceInfo", "wifiStandards", wifiStandards);
        ModelValidator.checkString("DeviceInfo", "wifiStandards", wifiStandards, 1024);
        if (linkCadenceMs != null) {
            ModelValidator.checkPositive("DeviceInfo", "linkCadenceMs", linkCadenceMs);
        }
        ModelValidator.checkRequired("DeviceInfo", "buildFingerprint", buildFingerprint);
        ModelValidator.checkString("DeviceInfo", "buildFingerprint", buildFingerprint, 1024);
    }
}

