package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "path_segment",
        foreignKeys = @ForeignKey(
                entity = SurveySessionRow.class,
                parentColumns = "id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("session_id")
)
public class PathSegmentRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId = "";

    @NonNull
    @ColumnInfo(name = "state")
    public String state = "";

    @ColumnInfo(name = "raw_start_ns")
    public long rawStartNs;

    @ColumnInfo(name = "raw_end_ns")
    public long rawEndNs;

    @ColumnInfo(name = "trim_start_ns")
    public long trimStartNs;

    @ColumnInfo(name = "trim_end_ns")
    public long trimEndNs;

    @ColumnInfo(name = "speed_mps")
    public double speedMps;

    @NonNull
    @ColumnInfo(name = "waypoints")
    public String waypoints = "";

    @NonNull
    @ColumnInfo(name = "anchors")
    public String anchors = "";

    @NonNull
    @ColumnInfo(name = "mode")
    public String mode = "";

    public PathSegmentRow() {
    }
}

