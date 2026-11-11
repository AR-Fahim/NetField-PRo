package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "floor_boundary",
        foreignKeys = @ForeignKey(
                entity = FloorRow.class,
                parentColumns = "id",
                childColumns = "floor_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("floor_id")
)
public class FloorBoundaryRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "floor_id")
    public String floorId = "";

    @NonNull
    @ColumnInfo(name = "vertices")
    public String vertices = "";

    public FloorBoundaryRow() {
    }
}

