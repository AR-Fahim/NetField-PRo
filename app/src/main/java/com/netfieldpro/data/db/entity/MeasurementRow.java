package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "measurement",
        foreignKeys = @ForeignKey(
                entity = SurveyPointRow.class,
                parentColumns = "id",
                childColumns = "point_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("point_id")
)
public class MeasurementRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "point_id")
    public String pointId = "";

    @NonNull
    @ColumnInfo(name = "protocol")
    public String protocol = "";

    @NonNull
    @ColumnInfo(name = "bssid_or_id")
    public String bssidOrId = "";

    @ColumnInfo(name = "ssid")
    public String ssid;

    @ColumnInfo(name = "freq_mhz")
    public int freqMHz;

    @ColumnInfo(name = "channel")
    public int channel;

    @ColumnInfo(name = "width_mhz")
    public Integer widthMHz;

    @NonNull
    @ColumnInfo(name = "source")
    public String source = "";

    @NonNull
    @ColumnInfo(name = "receiver_id")
    public String receiverId = "";

    @ColumnInfo(name = "n")
    public int n;

    @ColumnInfo(name = "rssi_median")
    public double rssiMedian;

    @ColumnInfo(name = "rssi_min")
    public int rssiMin;

    @ColumnInfo(name = "rssi_max")
    public int rssiMax;

    @ColumnInfo(name = "lqi")
    public Integer lqi;

    @ColumnInfo(name = "pan_id")
    public String panId;

    public MeasurementRow() {
    }
}

