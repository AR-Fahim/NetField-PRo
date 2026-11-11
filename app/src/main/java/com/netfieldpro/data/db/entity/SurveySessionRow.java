package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "survey_session",
        foreignKeys = @ForeignKey(
                entity = FloorRow.class,
                parentColumns = "id",
                childColumns = "floor_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("floor_id")
)
public class SurveySessionRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = "";

    @NonNull
    @ColumnInfo(name = "floor_id")
    public String floorId = "";

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @NonNull
    @ColumnInfo(name = "state")
    public String state = "";

    @ColumnInfo(name = "created_at_ms")
    public long createdAtMs;

    @ColumnInfo(name = "mono_epoch_ns")
    public long monoEpochNs;

    @ColumnInfo(name = "schema_version")
    public int schemaVersion;

    @NonNull
    @ColumnInfo(name = "app_version")
    public String appVersion = "";

    @NonNull
    @ColumnInfo(name = "algo_version")
    public String algoVersion = "";

    @ColumnInfo(name = "throttle_at_start")
    public boolean throttleAtStart;

    @NonNull
    @ColumnInfo(name = "timing_quality")
    public String timingQuality = "";

    @NonNull
    @ColumnInfo(name = "evidence_grade")
    public String evidenceGrade = "";

    @NonNull
    @ColumnInfo(name = "session_type")
    public String sessionType = "";

    @NonNull
    @ColumnInfo(name = "intended_profile_id")
    public String intendedProfileId = "";

    @ColumnInfo(name = "intended_profile_changes")
    public long intendedProfileChanges;

    @ColumnInfo(name = "rssi_offset_db")
    public double rssiOffsetDb;

    @ColumnInfo(name = "link_cadence_ms")
    public long linkCadenceMs;

    @NonNull
    @ColumnInfo(name = "cadence_flag")
    public String cadenceFlag = "";

    @NonNull
    @ColumnInfo(name = "probe_method")
    public String probeMethod = "";

    @NonNull
    @ColumnInfo(name = "reference_freqs_mhz")
    public String referenceFreqsMhz = "";

    @NonNull
    @ColumnInfo(name = "receiver_id")
    public String receiverId = "";

    @NonNull
    @ColumnInfo(name = "device_info_id")
    public String deviceInfoId = "";

    @NonNull
    @ColumnInfo(name = "capability_report")
    public String capabilityReport = "";

    @ColumnInfo(name = "finalized_at_ms")
    public Long finalizedAtMs;

    @ColumnInfo(name = "evidence_sha256")
    public String evidenceSha256;

    @ColumnInfo(name = "public_key_fp")
    public String publicKeyFp;

    public SurveySessionRow() {
    }
}

