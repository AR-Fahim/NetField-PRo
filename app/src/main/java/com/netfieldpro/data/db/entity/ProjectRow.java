package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "project")
public class ProjectRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "client")
    public String client;

    @ColumnInfo(name = "site")
    public String site;

    @ColumnInfo(name = "created_at_ms")
    public long createdAtMs;

    public ProjectRow() {
    }

    @androidx.room.Ignore
    public ProjectRow(@NonNull String id, @NonNull String name, String client, String site, long createdAtMs) {
        this.id = id;
        this.name = name;
        this.client = client;
        this.site = site;
        this.createdAtMs = createdAtMs;
    }
}
