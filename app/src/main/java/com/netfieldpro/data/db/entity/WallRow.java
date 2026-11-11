package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "wall",
        foreignKeys = @ForeignKey(
                entity = FloorRow.class,
                parentColumns = "id",
                childColumns = "floor_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("floor_id")
)
public class WallRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "floor_id")
    public String floorId = "";

    @NonNull
    @ColumnInfo(name = "material_id")
    public String materialId = "";

    @NonNull
    @ColumnInfo(name = "vertices")
    public String vertices = "";

    public WallRow() {
    }
}

