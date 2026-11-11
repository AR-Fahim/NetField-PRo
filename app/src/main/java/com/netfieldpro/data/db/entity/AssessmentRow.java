package com.netfieldpro.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "assessment",
        foreignKeys = @ForeignKey(
                entity = SurveySessionRow.class,
                parentColumns = "id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("session_id")
)
public class AssessmentRow {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "assessment_id")
    public String assessmentId = "";

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId = "";

    @NonNull
    @ColumnInfo(name = "evidence_sha256")
    public String evidenceSha256 = "";

    @NonNull
    @ColumnInfo(name = "profile_id")
    public String profileId = "";

    @NonNull
    @ColumnInfo(name = "profile_snapshot")
    public String profileSnapshot = "";

    @NonNull
    @ColumnInfo(name = "intended_profile_id")
    public String intendedProfileId = "";

    @ColumnInfo(name = "is_reassessment")
    public boolean isReassessment;

    @NonNull
    @ColumnInfo(name = "thresholds")
    public String thresholds = "";

    @NonNull
    @ColumnInfo(name = "computed_metrics")
    public String computedMetrics = "";

    @NonNull
    @ColumnInfo(name = "criteria")
    public String criteria = "";

    @NonNull
    @ColumnInfo(name = "verdict")
    public String verdict = "";

    @ColumnInfo(name = "assessed_at_ms")
    public long assessedAtMs;

    @NonNull
    @ColumnInfo(name = "algo_version")
    public String algoVersion = "";

    @ColumnInfo(name = "grid_g")
    public double gridG;

    @NonNull
    @ColumnInfo(name = "public_key")
    public String publicKey = "";

    @NonNull
    @ColumnInfo(name = "sig")
    public String sig = "";

    public AssessmentRow() {
    }
}

