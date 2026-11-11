package com.netfieldpro.data.db.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.netfieldpro.data.db.entity.AccessPointMarkerRow;
import com.netfieldpro.data.db.entity.ApGroupRow;
import com.netfieldpro.data.db.entity.AssessmentRow;
import com.netfieldpro.data.db.entity.ConnectionEventRow;
import com.netfieldpro.data.db.entity.DeviceInfoRow;
import com.netfieldpro.data.db.entity.FloorBoundaryRow;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.LatencyResultRow;
import com.netfieldpro.data.db.entity.LinkSampleRow;
import com.netfieldpro.data.db.entity.MeasurementRow;
import com.netfieldpro.data.db.entity.PathSegmentRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.RawSampleRow;
import com.netfieldpro.data.db.entity.RequirementProfileRow;
import com.netfieldpro.data.db.entity.ScanRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.data.db.entity.WallMaterialRow;
import com.netfieldpro.data.db.entity.WallRow;

@Dao
public interface NetFieldDao {

    // Project
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProject(ProjectRow row);

    @Query("DELETE FROM project WHERE id = :id")
    void deleteProject(String id);

    @Query("SELECT COUNT(*) FROM project")
    int countProjects();

    @Query("SELECT * FROM project WHERE id = :id")
    ProjectRow getProject(String id);

    @Query("SELECT * FROM project")
    List<ProjectRow> getAllProjects();

    @Query("UPDATE project SET name = :name WHERE id = :id")
    void updateProjectName(String id, String name);

    // Floor
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFloor(FloorRow row);

    @Query("DELETE FROM floor WHERE id = :id")
    void deleteFloor(String id);

    @Query("UPDATE floor SET meters_per_pixel = :mpp, scale_uncertainty_pct = :uncertainty, scale_method = :method, scale_checked = :checked, scale_p1_x = :p1x, scale_p1_y = :p1y, scale_p2_x = :p2x, scale_p2_y = :p2y WHERE id = :id")
    void updateFloorScale(String id, double mpp, double uncertainty, String method, boolean checked, Double p1x, Double p1y, Double p2x, Double p2y);

    @Query("SELECT COUNT(*) FROM floor")
    int countFloors();

    @Query("SELECT * FROM floor WHERE project_id = :projectId")
    List<FloorRow> getFloorsForProject(String projectId);

    @Query("SELECT * FROM floor WHERE id = :id")
    FloorRow getFloor(String id);

    @Query("SELECT * FROM floor WHERE meters_per_pixel > 0.0 ORDER BY rowid DESC")
    List<FloorRow> getScaledFloorsDesc();

    // DeviceInfo
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDeviceInfo(DeviceInfoRow row);

    @Query("SELECT COUNT(*) FROM device_info")
    int countDeviceInfo();

    @Query("SELECT * FROM device_info WHERE id = :id")
    DeviceInfoRow getDeviceInfo(String id);

    // SurveySession
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSurveySession(SurveySessionRow row);

    @Update
    void updateSurveySession(SurveySessionRow row);

    @Query("SELECT COUNT(*) FROM survey_session")
    int countSurveySessions();

    @Query("SELECT * FROM survey_session WHERE id = :id")
    SurveySessionRow getSurveySession(String id);

    @Query("SELECT * FROM survey_session WHERE floor_id = :floorId")
    List<SurveySessionRow> getSessionsForFloor(String floorId);

    @Query("SELECT COUNT(*) FROM survey_session WHERE floor_id = :floorId AND state = 'FINALIZED'")
    int countFinalizedSessionsForFloor(String floorId);

    @Query("DELETE FROM survey_session WHERE id = :id")
    void deleteSurveySession(String id);

    @Query("SELECT * FROM survey_session WHERE state = 'DRAFT'")
    List<SurveySessionRow> getDraftSessions();

    @Query("UPDATE survey_session SET evidence_grade = :grade WHERE id = :id")
    void updateSessionEvidenceGrade(String id, String grade);

    @Query("UPDATE survey_session SET evidence_grade = :grade, timing_quality = :timingQuality WHERE id = :id")
    void updateSessionDegradedTiming(String id, String grade, String timingQuality);

    // Scan
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertScan(ScanRow row);

    @Query("SELECT COUNT(*) FROM scan")
    int countScans();

    @Query("SELECT * FROM scan WHERE session_id = :sessionId")
    List<ScanRow> getScansForSession(String sessionId);

    // RawSample
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertRawSample(RawSampleRow row);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRawSamples(List<RawSampleRow> rows);

    @Query("SELECT COUNT(*) FROM raw_sample")
    int countRawSamples();

    @Query("SELECT * FROM raw_sample WHERE scan_id = :scanId ORDER BY row_id ASC")
    List<RawSampleRow> getRawSamplesForScan(String scanId);

    // SurveyPoint
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSurveyPoint(SurveyPointRow row);

    @Query("SELECT COUNT(*) FROM survey_point")
    int countSurveyPoints();

    @Query("SELECT * FROM survey_point WHERE session_id = :sessionId")
    List<SurveyPointRow> getSurveyPointsForSession(String sessionId);

    @Query("SELECT * FROM survey_point WHERE id = :id")
    SurveyPointRow getSurveyPoint(String id);

    @Query("DELETE FROM survey_point WHERE id = :id")
    void deleteSurveyPoint(String id);

    // Measurement
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMeasurement(MeasurementRow row);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMeasurements(List<MeasurementRow> rows);

    @Query("SELECT COUNT(*) FROM measurement")
    int countMeasurements();

    @Query("SELECT * FROM measurement WHERE point_id = :pointId")
    List<MeasurementRow> getMeasurementsForPoint(String pointId);

    // LatencyResult
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLatencyResult(LatencyResultRow row);

    @Query("SELECT COUNT(*) FROM latency_result")
    int countLatencyResults();

    @Query("SELECT * FROM latency_result WHERE point_id = :pointId")
    List<LatencyResultRow> getLatencyResultsForPoint(String pointId);

    // LinkSample
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLinkSample(LinkSampleRow row);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLinkSamples(List<LinkSampleRow> rows);

    @Query("SELECT COUNT(*) FROM link_sample")
    int countLinkSamples();

    @Query("SELECT * FROM link_sample WHERE session_id = :sessionId")
    List<LinkSampleRow> getLinkSamplesForSession(String sessionId);

    // ConnectionEvent
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertConnectionEvent(ConnectionEventRow row);

    @Query("SELECT COUNT(*) FROM connection_event")
    int countConnectionEvents();

    @Query("SELECT * FROM connection_event WHERE session_id = :sessionId")
    List<ConnectionEventRow> getConnectionEventsForSession(String sessionId);

    // PathSegment
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPathSegment(PathSegmentRow row);

    @Query("SELECT COUNT(*) FROM path_segment")
    int countPathSegments();

    @Query("SELECT * FROM path_segment WHERE session_id = :sessionId")
    List<PathSegmentRow> getPathSegmentsForSession(String sessionId);

    // AccessPointMarker
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAccessPointMarker(AccessPointMarkerRow row);

    @Query("SELECT COUNT(*) FROM access_point_marker")
    int countAccessPointMarkers();

    @Query("SELECT * FROM access_point_marker WHERE floor_id = :floorId")
    List<AccessPointMarkerRow> getAccessPointMarkersForFloor(String floorId);

    // TargetNetwork
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTargetNetwork(TargetNetworkRow row);

    @Query("SELECT COUNT(*) FROM target_network")
    int countTargetNetworks();

    @Query("SELECT * FROM target_network WHERE session_id = :sessionId")
    List<TargetNetworkRow> getTargetNetworksForSession(String sessionId);

    // ApGroup
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertApGroup(ApGroupRow row);

    @Query("SELECT COUNT(*) FROM ap_group")
    int countApGroups();

    @Query("SELECT * FROM ap_group WHERE session_id = :sessionId")
    List<ApGroupRow> getApGroupsForSession(String sessionId);

    // FloorBoundary
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFloorBoundary(FloorBoundaryRow row);

    @Query("SELECT COUNT(*) FROM floor_boundary")
    int countFloorBoundaries();

    @Query("SELECT * FROM floor_boundary WHERE floor_id = :floorId")
    FloorBoundaryRow getFloorBoundary(String floorId);

    @Query("DELETE FROM floor_boundary WHERE floor_id = :floorId")
    void deleteFloorBoundary(String floorId);

    // Wall
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWall(WallRow row);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWalls(List<WallRow> rows);

    @Query("SELECT COUNT(*) FROM wall")
    int countWalls();

    @Query("SELECT * FROM wall WHERE floor_id = :floorId")
    List<WallRow> getWallsForFloor(String floorId);

    @Query("DELETE FROM wall WHERE id = :id")
    void deleteWall(String id);

    @Query("DELETE FROM wall WHERE floor_id = :floorId")
    void deleteWallsForFloor(String floorId);

    // WallMaterial
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWallMaterial(WallMaterialRow row);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWallMaterials(List<WallMaterialRow> rows);

    @Query("SELECT COUNT(*) FROM wall_material")
    int countWallMaterials();

    @Query("SELECT * FROM wall_material")
    List<WallMaterialRow> getAllWallMaterials();

    // RequirementProfile
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRequirementProfile(RequirementProfileRow row);

    @Query("SELECT COUNT(*) FROM requirement_profile")
    int countRequirementProfiles();

    @Query("SELECT * FROM requirement_profile WHERE id = :id")
    RequirementProfileRow getRequirementProfile(String id);

    // Assessment
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAssessment(AssessmentRow row);

    @Query("SELECT COUNT(*) FROM assessment")
    int countAssessments();

    @Query("SELECT * FROM assessment WHERE assessment_id = :assessmentId")
    AssessmentRow getAssessment(String assessmentId);

    @Query("SELECT * FROM assessment WHERE session_id = :sessionId LIMIT 1")
    AssessmentRow getAssessmentForSession(String sessionId);

    @Query("SELECT * FROM assessment WHERE evidence_sha256 = :evidenceSha256")
    AssessmentRow getAssessmentByEvidence(String evidenceSha256);

    @Query("SELECT m.* FROM measurement m INNER JOIN survey_point p ON m.point_id = p.id WHERE p.session_id = :sessionId")
    List<MeasurementRow> getMeasurementsForSession(String sessionId);

    @Query("SELECT l.* FROM latency_result l INNER JOIN survey_point p ON l.point_id = p.id WHERE p.session_id = :sessionId")
    List<LatencyResultRow> getLatencyResultsForSession(String sessionId);
}

