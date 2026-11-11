package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "latency_result",
        foreignKeys = @ForeignKey(
                entity = SurveyPointRow.class,
                parentColumns = "id",
                childColumns = "point_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("point_id")
)
public class LatencyResultRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "point_id")
    public String pointId = "";

    @NonNull
    @ColumnInfo(name = "method")
    public String method = "";

    @NonNull
    @ColumnInfo(name = "target")
    public String target = "";

    @NonNull
    @ColumnInfo(name = "bound_iface")
    public String boundIface = "";

    @NonNull
    @ColumnInfo(name = "scope")
    public String scope = "";

    @ColumnInfo(name = "sent")
    public int sent;

    @ColumnInfo(name = "recv")
    public int recv;

    @ColumnInfo(name = "min_ms")
    public Double minMs;

    @ColumnInfo(name = "avg_ms")
    public Double avgMs;

    @ColumnInfo(name = "max_ms")
    public Double maxMs;

    @ColumnInfo(name = "jitter_ms")
    public Double jitterMs;

    @ColumnInfo(name = "loss_count")
    public int lossCount;

    @ColumnInfo(name = "during_scan")
    public boolean duringScan;

    @NonNull
    @ColumnInfo(name = "connected_bssid")
    public String connectedBssid = "";

    @NonNull
    @ColumnInfo(name = "ssid")
    public String ssid = "";

    @NonNull
    @ColumnInfo(name = "rtts")
    public String rtts = "";

    public LatencyResultRow() {
    }
}

