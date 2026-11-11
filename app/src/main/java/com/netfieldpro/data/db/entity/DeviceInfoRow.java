package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "device_info")
public class DeviceInfoRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "model")
    public String model = "";

    @NonNull
    @ColumnInfo(name = "manufacturer")
    public String manufacturer = "";

    @ColumnInfo(name = "sdk")
    public int sdk;

    @NonNull
    @ColumnInfo(name = "wifi_standards")
    public String wifiStandards = "";

    @ColumnInfo(name = "link_cadence_ms")
    public Long linkCadenceMs;

    @NonNull
    @ColumnInfo(name = "build_fingerprint")
    public String buildFingerprint = "";

    public DeviceInfoRow() {
    }
}

