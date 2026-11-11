package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "link_sample",
        foreignKeys = @ForeignKey(
                entity = SurveySessionRow.class,
                parentColumns = "id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("session_id"),
                @Index({"session_id", "observed_at_ns"})
        }
)
public class LinkSampleRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId = "";

    @ColumnInfo(name = "observed_at_ns")
    public long observedAtNs;

    @NonNull
    @ColumnInfo(name = "bssid")
    public String bssid = "";

    @NonNull
    @ColumnInfo(name = "ssid")
    public String ssid = "";

    @ColumnInfo(name = "rssi")
    public int rssi;

    @ColumnInfo(name = "freq_mhz")
    public int freqMHz;

    @ColumnInfo(name = "tx_mbps")
    public Integer txMbps;

    @ColumnInfo(name = "rx_mbps")
    public Integer rxMbps;

    public LinkSampleRow() {
    }
}

