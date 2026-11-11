package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wall_material")
public class WallMaterialRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "material_id")
    public String materialId = "";

    @ColumnInfo(name = "penalty_m")
    public double penaltyM;

    @ColumnInfo(name = "loss_db24")
    public double lossDb24;

    @ColumnInfo(name = "loss_db5")
    public double lossDb5;

    public WallMaterialRow() {
    }

    @androidx.room.Ignore
    public WallMaterialRow(@NonNull String materialId, double penaltyM, double lossDb24, double lossDb5) {
        this.materialId = materialId;
        this.penaltyM = penaltyM;
        this.lossDb24 = lossDb24;
        this.lossDb5 = lossDb5;
    }
}
