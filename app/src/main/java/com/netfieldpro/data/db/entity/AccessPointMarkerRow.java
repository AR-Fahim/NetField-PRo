package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "access_point_marker",
        foreignKeys = @ForeignKey(
                entity = FloorRow.class,
                parentColumns = "id",
                childColumns = "floor_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("floor_id")
)
public class AccessPointMarkerRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "floor_id")
    public String floorId = "";

    @ColumnInfo(name = "x")
    public double x;

    @ColumnInfo(name = "y")
    public double y;

    @ColumnInfo(name = "bssid")
    public String bssid;

    @ColumnInfo(name = "tx_power_dbm")
    public Integer txPowerDbm;

    public AccessPointMarkerRow() {
    }
}

