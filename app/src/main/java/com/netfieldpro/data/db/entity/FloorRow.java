package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "floor",
        foreignKeys = @ForeignKey(
                entity = ProjectRow.class,
                parentColumns = "id",
                childColumns = "project_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("project_id")
)
public class FloorRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "project_id")
    public String projectId = "";

    @ColumnInfo(name = "level_index")
    public long levelIndex;

    @NonNull
    @ColumnInfo(name = "plan_sha256")
    public String planSha256 = "";

    @ColumnInfo(name = "plan_wpx")
    public int planWpx;

    @ColumnInfo(name = "plan_hpx")
    public int planHpx;

    @ColumnInfo(name = "meters_per_pixel")
    public double metersPerPixel;

    @ColumnInfo(name = "north_offset_deg")
    public Double northOffsetDeg;

    @NonNull
    @ColumnInfo(name = "scale_method")
    public String scaleMethod = "";

    @ColumnInfo(name = "scale_uncertainty_pct")
    public double scaleUncertaintyPct;

    @ColumnInfo(name = "scale_checked")
    public boolean scaleChecked;

    @ColumnInfo(name = "align_x")
    public Double alignX;

    @ColumnInfo(name = "align_y")
    public Double alignY;

    @ColumnInfo(name = "align_rot_deg")
    public Double alignRotDeg;

    @ColumnInfo(name = "scale_p1_x")
    public Double scaleP1X;

    @ColumnInfo(name = "scale_p1_y")
    public Double scaleP1Y;

    @ColumnInfo(name = "scale_p2_x")
    public Double scaleP2X;

    @ColumnInfo(name = "scale_p2_y")
    public Double scaleP2Y;

    public FloorRow() {
    }
}

