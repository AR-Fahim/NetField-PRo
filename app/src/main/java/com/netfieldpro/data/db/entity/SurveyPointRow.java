package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "survey_point",
        foreignKeys = @ForeignKey(
                entity = SurveySessionRow.class,
                parentColumns = "id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("session_id"),
                @Index({"session_id", "t_mono_ns"})
        }
)
public class SurveyPointRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId = "";

    @ColumnInfo(name = "x")
    public double x;

    @ColumnInfo(name = "y")
    public double y;

    @NonNull
    @ColumnInfo(name = "source")
    public String source = "";

    @NonNull
    @ColumnInfo(name = "quality")
    public String quality = "";

    @ColumnInfo(name = "t_mono_ns")
    public long tMonoNs;

    @ColumnInfo(name = "t_wall_ms")
    public long tWallMs;

    @ColumnInfo(name = "n_scans")
    public int nScans;

    @ColumnInfo(name = "position_uncertainty_m")
    public double positionUncertaintyM;

    @ColumnInfo(name = "segment_id")
    public String segmentId;

    public SurveyPointRow() {
    }
}

