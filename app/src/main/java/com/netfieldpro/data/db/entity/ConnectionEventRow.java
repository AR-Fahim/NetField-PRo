package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "connection_event",
        foreignKeys = @ForeignKey(
                entity = SurveySessionRow.class,
                parentColumns = "id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("session_id")
)
public class ConnectionEventRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId = "";

    @NonNull
    @ColumnInfo(name = "type")
    public String type = "";

    @ColumnInfo(name = "at_ns")
    public long atNs;

    @ColumnInfo(name = "from_bssid")
    public String fromBssid;

    @ColumnInfo(name = "to_bssid")
    public String toBssid;

    @ColumnInfo(name = "point_id")
    public String pointId;

    public ConnectionEventRow() {
    }
}

