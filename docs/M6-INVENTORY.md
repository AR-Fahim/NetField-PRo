# M6 Inventory

## 1. Entity Field Names

### 1.1 Project Entity & ProjectRow
- **Location:** `core/src/main/java/com/netfieldpro/core/model/entity/Project.java` and `app/src/main/java/com/netfieldpro/data/db/entity/ProjectRow.java`
- **Fields:**
  - `String id`: 22-char base64url primary key
  - `String name`: Project name (1..60 characters per D4, trimmed)
  - `String client`: Client name (optional, nullable)
  - `String site`: Site name/address (optional, nullable per D4)
  - `long createdAtMs`: Creation timestamp in epoch milliseconds

### 1.2 Floor Entity & FloorRow
- **Location:** `core/src/main/java/com/netfieldpro/core/model/entity/Floor.java` and `app/src/main/java/com/netfieldpro/data/db/entity/FloorRow.java`
- **Fields:**
  - `String id`: 22-char base64url primary key
  - `String projectId`: Foreign key to `project.id` (CASCADE on delete)
  - `long levelIndex`: Floor level integer (-5..99 per D5)
  - `String planSha256`: SHA-256 hash of stored plan image bytes (hex string)
  - `int planWpx`: Width of stored plan in pixels
  - `int planHpx`: Height of stored plan in pixels
  - `double metersPerPixel`: Scale factor in meters per pixel
  - `Double northOffsetDeg`: Optional rotation offset in degrees
  - `ScaleMethod scaleMethod`: Scale calibration method (`LINE` or `ROOM_WIDTH`)
  - `double scaleUncertaintyPct`: Uncertainty percentage (e.g., 5.588 for SC-1)
  - `boolean scaleChecked`: Whether cross-check step was performed
  - `Double alignX`: Optional floor-to-floor alignment X offset
  - `Double alignY`: Optional floor-to-floor alignment Y offset
  - `Double alignRotDeg`: Optional floor-to-floor rotation
  - `Floor.Point scaleP1` / (`Double scaleP1X`, `Double scaleP1Y`): First calibration handle point
  - `Floor.Point scaleP2` / (`Double scaleP2X`, `Double scaleP2Y`): Second calibration handle point

### 1.3 SurveySession Entity & SurveySessionRow
- **Location:** `core/src/main/java/com/netfieldpro/core/model/entity/SurveySession.java` and `app/src/main/java/com/netfieldpro/data/db/entity/SurveySessionRow.java`
- **Fields:**
  - `String id`: 22-char base64url primary key
  - `String floorId`: Foreign key to `floor.id` (CASCADE on delete)
  - `String name`: Session name
  - `SessionState state`: State enum (`DRAFT`, `FINALIZED`)
  - `long createdAtMs`: Creation epoch ms
  - `long monoEpochNs`: Monotonic timestamp epoch ns
  - `int schemaVersion`: Schema version integer
  - `String appVersion`: App version
  - `String algoVersion`: Algorithm version
  - `boolean throttleAtStart`: Scan throttle status flag
  - `TimingQuality timingQuality`: Quality enum
  - `EvidenceGrade evidenceGrade`: Grade enum
  - `SessionType sessionType`: Type enum
  - `String intendedProfileId`: Target requirement profile ID
  - `long intendedProfileChanges`: Count of changes
  - `double rssiOffsetDb`: Device RSSI offset
  - `long linkCadenceMs`: Link sample cadence
  - `CadenceFlag cadenceFlag`: Cadence flag enum
  - `ProbeMethod probeMethod`: Latency probe method enum (`TCP`, `ICMP`, `NONE`)
  - `List<Long> referenceFreqsMHz` / `String referenceFreqsMhz`: Reference frequencies
  - `String receiverId`: Receiver ID
  - `String deviceInfoId`: Foreign key to device info
  - `JsonObject capabilityReport` / `String capabilityReport`: JSON capability report
  - `Long finalizedAtMs`: Epoch ms when finalized (null if DRAFT, required if FINALIZED)
  - `String evidenceSha256`: SHA-256 hash of canonical evidence (null if DRAFT, required if FINALIZED)
  - `String publicKeyFp`: Public key fingerprint (null if DRAFT, required if FINALIZED)

---

## 2. Existing DAO Methods

Located at `app/src/main/java/com/netfieldpro/data/db/dao/NetFieldDao.java`:

- **Project:**
  - `void insertProject(ProjectRow row)`
  - `void deleteProject(String id)`
  - `int countProjects()`
  - `ProjectRow getProject(String id)`
  - `List<ProjectRow> getAllProjects()`
- **Floor:**
  - `void insertFloor(FloorRow row)`
  - `int countFloors()`
  - `List<FloorRow> getFloorsForProject(String projectId)`
  - `FloorRow getFloor(String id)`
- **DeviceInfo:**
  - `void insertDeviceInfo(DeviceInfoRow row)`
  - `int countDeviceInfo()`
  - `DeviceInfoRow getDeviceInfo(String id)`
- **SurveySession:**
  - `void insertSurveySession(SurveySessionRow row)`
  - `int countSurveySessions()`
  - `SurveySessionRow getSurveySession(String id)`
  - `List<SurveySessionRow> getSessionsForFloor(String floorId)`
- **Scan:**
  - `void insertScan(ScanRow row)`
  - `int countScans()`
  - `List<ScanRow> getScansForSession(String sessionId)`
- **RawSample:**
  - `long insertRawSample(RawSampleRow row)`
  - `void insertRawSamples(List<RawSampleRow> rows)`
  - `int countRawSamples()`
  - `List<RawSampleRow> getRawSamplesForScan(String scanId)`
- **SurveyPoint, Measurement, LatencyResult, LinkSample, ConnectionEvent, PathSegment, AccessPointMarker, TargetNetwork, ApGroup, FloorBoundary, Wall, WallMaterial, RequirementProfile, Assessment:**
  - Full insert, count, and query methods matching their tables.

*Note on DAO Additions:* Queries may add DAO methods (e.g., project rename update, floor deletion, and counting finalized sessions for a floor to check recalibration lock) without modifying table schemas (`git diff --exit-code app/schemas` remains empty).

---

## 3. BatchedWriter Public API

Located at `app/src/main/java/com/netfieldpro/data/db/BatchedWriter.java`:

- **Constructor:**
  - `public BatchedWriter(NetFieldDatabase db)`
- **Methods:**
  - `public void submit(WriteBatch b, WriteCallback cb)`: Submits batch to single-thread `nf-db-writer` executor within a Room transaction, notifying callback on completion or error.
  - `public void flush()`: Blocks until all currently queued write batches finish executing.
  - `public void close()`: Flushes pending tasks and gracefully shuts down executor (awaiting up to 5s).
- **Associated Interfaces:**
  - `com.netfieldpro.data.db.WriteBatch`: `void run(NetFieldDatabase db) throws Exception;`
  - `com.netfieldpro.data.db.WriteCallback`: `void onSuccess();`, `void onError(Throwable t);`

---

## 4. Nav Destinations, Arguments, and Edges (S02–S05)

Located at `app/src/main/res/navigation/nav_main.xml`:

- **Root / Start Destination:** `@id/nav_s02`
- **S02 (`nav_s02`):**
  - Current Class: `com.netfieldpro.ui.placeholder.PlaceholderFragment` (M6-4 target)
  - Label: `@string/screen_s02_title`
  - Arguments: `screenId` (string, default "S02")
  - Outgoing Edges:
    - `action_s02_to_s03` -> `@id/nav_s03`
    - `action_s02_to_s17` -> `@id/nav_s17`
    - `action_s02_to_s18` -> `@id/nav_s18`
    - `action_s02_to_s19` -> `@id/nav_s19`
- **S03 (`nav_s03`):**
  - Current Class: `com.netfieldpro.ui.placeholder.PlaceholderFragment` (M6-5 target)
  - Label: `@string/screen_s03_title`
  - Arguments:
    - `screenId` (string, default "S03")
    - `projectId` (string, nullable=true, defaultValue=@null)
  - Outgoing Edges:
    - `action_s03_to_s04` -> `@id/nav_s04`
    - `action_s03_to_s06` -> `@id/nav_s06` (*Confirmed present per D12*)
    - `action_s03_to_s09` -> `@id/nav_s09`
    - `action_s03_to_s14` -> `@id/nav_s14`
- **S04 (`nav_s04`):**
  - Current Class: `com.netfieldpro.ui.placeholder.PlaceholderFragment` (M6-7 target)
  - Label: `@string/screen_s04_title`
  - Arguments:
    - `screenId` (string, default "S04")
    - `projectId` (string, nullable=true, defaultValue=@null)
  - Outgoing Edges:
    - `action_s04_to_s05` -> `@id/nav_s05`
- **S05 (`nav_s05`):**
  - Current Class: `com.netfieldpro.ui.placeholder.PlaceholderFragment` (M6-8 target)
  - Label: `@string/screen_s05_title`
  - Arguments:
    - `screenId` (string, default "S05")
    - `floorId` (string, nullable=true, defaultValue=@null)
  - Outgoing Edges: None
- **S18 (`nav_s18`):**
  - Class: `com.netfieldpro.paywall.PaywallFragment`
  - Label: `@string/screen_s18_title`
  - Arguments:
    - `screenId` (string, default "S18")
    - `feature` (string, nullable=true, defaultValue=@null)
    - *Note for M6-3:* M6 adds optional string arg `quota` to S18 navigation.

---

## 5. Current S02 Placeholder and Toolbar Badge Wiring

- **In `MainActivity.java`:**
  - `appBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_s02).build();`
  - Toolbar menu hosts `R.id.action_tier_badge` inflating `TierBadgeView` (`@id/tier_badge_view`).
  - `NavController.OnDestinationChangedListener` toggles badge visibility: visible on `R.id.nav_s02`, gone on all other screens.
  - Badge click triggers navigation: `navController.navigate(R.id.action_global_paywall)`.
  - Entitlement changes observe `EntitlementManager.getTier()` and update `badgeView.setTier(tier)`.
- **In `PlaceholderSpec.java`:**
  - `register("S02", R.string.screen_s02_title, "M6", R.id.nav_s02, "S03", "S17", "S18", "S19");`
  - Maps S02 placeholder destination to owner "M6".

---

## 6. Affine3x3 Public API

- **Current Status:** Not present in the codebase prior to M6.
- **Reference in Traceability Matrix (`docs/freeze/PART-B.md §5`):**
  - Scheduled for M6 alongside `ScaleCalculator`.
  - Roles: `fromTwoPoints(...)`, coordinate transforms between view/display and plan pixel space.
- **M6 Implementation Plan:** Define `Affine3x3` in `:core` (`com.netfieldpro.core.scale.Affine3x3`) providing 3x3 affine transformation matrix operations (scale, translate, rotate, inverted, mapping points between coordinate spaces).

---

## 7. Gate Check Conclusion

- All entity fields required for D7 (plan storage policy: `planSha256`, `planWpx`, `planHpx`) and D9 (scale: `metersPerPixel`, `scaleUncertaintyPct`, `scaleMethod`, `scaleChecked`, `scaleP1X`, `scaleP1Y`, `scaleP2X`, `scaleP2Y`) exist on `Floor` and `FloorRow`.
- `SurveySession.state` (`SessionState.FINALIZED`) exists for detecting finalized sessions that lock recalibration.
- No schema migration or new fields are required.
- Gate status: **PASSED**.
