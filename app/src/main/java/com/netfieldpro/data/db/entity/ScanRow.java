package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "scan",
        foreignKeys = @ForeignKey(
                entity = SurveySessionRow.class,
                parentColumns = "id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("session_id")
)
public class ScanRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId = "";

    @ColumnInfo(name = "point_id")
    public String pointId;

    @NonNull
    @ColumnInfo(name = "sweep_class")
    public String sweepClass = "";

    @ColumnInfo(name = "start_ns")
    public long startNs;

    @ColumnInfo(name = "end_ns")
    public long endNs;

    @ColumnInfo(name = "arrival_ns")
    public long arrivalNs;

    @ColumnInfo(name = "ts_plausible")
    public boolean tsPlausible;

    @ColumnInfo(name = "throttle_off_at_scan")
    public boolean throttleOffAtScan;

    public ScanRow() {
    }
}

