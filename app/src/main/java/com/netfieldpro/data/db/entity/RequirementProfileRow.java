package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "requirement_profile")
public class RequirementProfileRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "kind")
    public String kind = "";

    @NonNull
    @ColumnInfo(name = "thresholds")
    public String thresholds = "";

    @NonNull
    @ColumnInfo(name = "band_policy")
    public String bandPolicy = "";

    @ColumnInfo(name = "max_influence_m")
    public double maxInfluenceM;

    @NonNull
    @ColumnInfo(name = "criterion_enabled")
    public String criterionEnabled = "";

    public RequirementProfileRow() {
    }
}

