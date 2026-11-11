# M7a Inventory

## 1. Tags
- `git tag -l` output:
  - `m0-done`
  - `m1-done`
  - `m2-0-done`
  - `m2-done`
  - `m3-done`
  - `m4-auto`
  - `m4-done`
  - `m5-auto`
  - `m5-done`
  - `m6-auto`
  - `m6fix-auto`
- Verification: Required precondition tags `m4-auto` and `m6fix-auto` are present.

---

## 2. SurveySession Fields and Entity State Enum
- File: `core/src/main/java/com/netfieldpro/core/model/entity/SurveySession.java`
- Fields:
  - `String id`
  - `String floorId`
  - `String name`
  - `SessionState state`
  - `long createdAtMs`
  - `long monoEpochNs`
  - `int schemaVersion`
  - `String appVersion`
  - `String algoVersion`
  - `boolean throttleAtStart`
  - `TimingQuality timingQuality`
  - `EvidenceGrade evidenceGrade`
  - `SessionType sessionType`
  - `String intendedProfileId`
  - `long intendedProfileChanges`
  - `double rssiOffsetDb`
  - `long linkCadenceMs`
  - `CadenceFlag cadenceFlag`
  - `ProbeMethod probeMethod`
  - `List<Long> referenceFreqsMHz`
  - `String receiverId`
  - `String deviceInfoId`
  - `JsonObject capabilityReport`
  - `Long finalizedAtMs`
  - `String evidenceSha256`
  - `String publicKeyFp`
- Entity state enum: `com.netfieldpro.core.model.enums.SessionState`
  - ALL values:
    - `DRAFT`
    - `FINALIZED`
  - State enum lacks: `CREATED`, `WARMING_UP`, `WARMUP_FAILED`, `READY`, `ACTIVE`, `ENDED`.

---

## 3. ConnectionEvent Fields and Type Enum Values
- File: `core/src/main/java/com/netfieldpro/core/model/entity/ConnectionEvent.java`
- Fields:
  - `String id`
  - `String sessionId`
  - `ConnectionEventType type`
  - `long atNs`
  - `String fromBssid`
  - `String toBssid`
  - `String pointId`
- Type enum: `com.netfieldpro.core.model.enums.ConnectionEventType`
  - ALL values:
    - `CONNECT`
    - `DISCONNECT`
    - `ROAM`
  - Type enum lacks both: `SCREEN_OFF` and `SCREEN_ON`.

---

## 4. Existing Session DAO/Queries and BatchedWriter API
- File: `app/src/main/java/com/netfieldpro/data/db/dao/NetFieldDao.java`
  - SurveySession DAO methods & queries:
    - `@Insert(onConflict = OnConflictStrategy.REPLACE) void insertSurveySession(SurveySessionRow row);`
    - `@Query("SELECT COUNT(*) FROM survey_session") int countSurveySessions();`
    - `@Query("SELECT * FROM survey_session WHERE id = :id") SurveySessionRow getSurveySession(String id);`
    - `@Query("SELECT * FROM survey_session WHERE floor_id = :floorId") List<SurveySessionRow> getSessionsForFloor(String floorId);`
    - `@Query("SELECT COUNT(*) FROM survey_session WHERE floor_id = :floorId AND state = 'FINALIZED'") int countFinalizedSessionsForFloor(String floorId);`
  - ConnectionEvent DAO methods & queries:
    - `@Insert(onConflict = OnConflictStrategy.REPLACE) void insertConnectionEvent(ConnectionEventRow row);`
    - `@Query("SELECT COUNT(*) FROM connection_event") int countConnectionEvents();`
    - `@Query("SELECT * FROM connection_event WHERE session_id = :sessionId") List<ConnectionEventRow> getConnectionEventsForSession(String sessionId);`
- File: `app/src/main/java/com/netfieldpro/data/db/BatchedWriter.java`
  - Transaction & completion API:
    - `public void submit(WriteBatch b, WriteCallback cb)`: Submits write task to single-threaded `nf-db-writer` executor; runs `b.run(db)` within `db.runInTransaction(...)`; invokes `cb.onSuccess()` or `cb.onError(cause)`.
    - `public void flush()`: Submits an empty task to the writer executor and waits synchronously on `future.get()`.
    - `public void close()`: Flushes and shuts down the executor.

---

## 5. Storage of Scan and ConnectionEvent Timestamps
- Scan timestamp storage:
  - File: `app/src/main/java/com/netfieldpro/data/db/mapper/ScanMapper.java`
  - Mapper lines (lines 18-20):
    ```java
    row.startNs = s.startNs();
    row.endNs = s.endNs();
    row.arrivalNs = s.arrivalNs();
    ```
  - Nature: Absolute monotonic nanoseconds (not offset from `session.monoEpochNs`).
- ConnectionEvent timestamp storage:
  - File: `app/src/main/java/com/netfieldpro/data/db/mapper/ConnectionEventMapper.java`
  - Mapper line (line 17):
    ```java
    row.atNs = c.atNs();
    ```
  - Nature: Absolute monotonic nanoseconds (not offset from `session.monoEpochNs`).

---

## 6. Data Lab Demo Floor Creation
- Class: `com.netfieldpro.ui.debug.DebugDataLabFragment`
- Method: `private void seedDemo(int seedIndex)` (lines 145-160)
  ```java
  Floor floor = new Floor(fId, pId, 1L, "47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU", 1000, 1000, 0.05, 0.0,
          ScaleMethod.LINE, 5.0, true, 0.0, 0.0, 0.0, new Floor.Point(0, 0), new Floor.Point(10, 10));
  ```
- Access modifier: `private` inside `DebugDataLabFragment`.

---

## 7. MainActivity Window Flags Structure
- File: `app/src/main/java/com/netfieldpro/MainActivity.java`
- Current window setup (lines 27, 32-38):
  ```java
  WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
  ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> { ... });
  ```
- Window flag operations: No `addFlags` or `clearFlags` (e.g. `FLAG_KEEP_SCREEN_ON`) are currently set or observed on `getWindow()`.

---

## 8. Debug Menu Button Pattern from M6
- Layout XML (`app/src/debug/res/layout/fragment_debug_menu.xml` & `app/src/main/res/layout/fragment_debug_menu.xml`):
  ```xml
  <com.google.android.material.button.MaterialButton
      android:id="@+id/btn_plan_lab"
      style="@style/Widget.Material3.Button.OutlinedButton"
      android:layout_width="match_parent"
      android:layout_height="@dimen/touch_primary"
      android:layout_marginTop="@dimen/space_1"
      android:text="@string/debug_plan_lab_title" />
  ```
- Wiring in `app/src/main/java/com/netfieldpro/ui/debug/DebugMenuFragment.java`:
  ```java
  binding.btnPlanLab.setOnClickListener(v ->
          NavHostFragment.findNavController(this).navigate(R.id.nav_debug_plan_lab)
  );
  ```

---

## 9. QuotaGuard.check Signature
- File: `app/src/main/java/com/netfieldpro/data/repository/QuotaGuard.java`
- Signature (line 34):
  ```java
  public QuotaDecision check(Resource resource)
  ```
  where `resource` is of type `com.netfieldpro.core.entitlement.Resource` (`PROJECT`, `FLOOR`, `SESSION`).

---

## 10. Current Manifest Permissions
- Declared in `app/src/main/AndroidManifest.xml`:
  1. `android.permission.ACCESS_FINE_LOCATION`
  2. `android.permission.ACCESS_COARSE_LOCATION`
  3. `android.permission.POST_NOTIFICATIONS`
  4. `android.permission.ACCESS_WIFI_STATE`
  5. `android.permission.CHANGE_WIFI_STATE`
  6. `android.permission.ACCESS_NETWORK_STATE`
  7. `android.permission.INTERNET`
- In `docs/permissions.golden.txt`:
  The 7 permissions above plus `?com.netfieldpro.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.

---

## Conditionals Evaluation

1. **State enum check:**
   - Specification: `state enum lacks any of CREATED, WARMING_UP, WARMUP_FAILED, READY, ACTIVE, ENDED → AUDIT-NOTE, STOP.`
   - Fact: `com.netfieldpro.core.model.enums.SessionState` has only `DRAFT` and `FINALIZED`. It lacks `CREATED`, `WARMING_UP`, `WARMUP_FAILED`, `READY`, `ACTIVE`, and `ENDED`.
   - Result: **TRIGGERED → AUDIT-NOTE, STOP.**

2. **ConnectionEvent type enum check:**
   - Specification: `ConnectionEvent type enum lacks both SCREEN_OFF and SCREEN_ON → AUDIT-NOTE, STOP.`
   - Fact: `com.netfieldpro.core.model.enums.ConnectionEventType` has only `CONNECT`, `DISCONNECT`, and `ROAM`. It lacks both `SCREEN_OFF` and `SCREEN_ON`.
   - Result: **TRIGGERED → AUDIT-NOTE, STOP.**

3. **Data Lab floor creation access:**
   - Specification: `Data Lab's floor creation is private → extract it into a DemoSeeder (debug source set), behaviour unchanged, record in the report.`
   - Fact: `seedDemo` in `DebugDataLabFragment` is private. Written fallback exists.

