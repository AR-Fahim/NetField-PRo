package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.DeviceInfo;
import com.netfieldpro.data.db.entity.DeviceInfoRow;

public final class DeviceInfoMapper {

    private DeviceInfoMapper() {
    }

    public static DeviceInfoRow toRow(DeviceInfo d) {
        DeviceInfoRow row = new DeviceInfoRow();
        row.id = d.id();
        row.model = d.model();
        row.manufacturer = d.manufacturer();
        row.sdk = d.sdk();
        row.wifiStandards = d.wifiStandards();
        row.linkCadenceMs = d.linkCadenceMs();
        row.buildFingerprint = d.buildFingerprint();
        return row;
    }

    public static DeviceInfo toDomain(DeviceInfoRow row) {
        return new DeviceInfo(
                row.id, row.model, row.manufacturer, row.sdk, row.wifiStandards,
                row.linkCadenceMs, row.buildFingerprint
        );
    }
}

