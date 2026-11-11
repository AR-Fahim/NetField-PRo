package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "raw_sample",
        foreignKeys = @ForeignKey(
                entity = ScanRow.class,
                parentColumns = "id",
                childColumns = "scan_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("scan_id")
)
public class RawSampleRow {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "row_id")
    public long rowId;

    @NonNull
    @ColumnInfo(name = "scan_id")
    public String scanId = "";

    @NonNull
    @ColumnInfo(name = "bssid")
    public String bssid = "";

    @NonNull
    @ColumnInfo(name = "ssid")
    public String ssid = "";

    @ColumnInfo(name = "freq_mhz")
    public int freqMHz;

    @ColumnInfo(name = "rssi")
    public int rssi;

    @ColumnInfo(name = "scan_result_ts_ns")
    public Long scanResultTsNs;

    public RawSampleRow() {
    }
}

