# M6FIX Inventory

### 1. Files in `app/src/androidTest` referencing `ActivityScenario`, `ActivityScenarioRule`, `launch(`, or `Room`/`NetFieldApp`
Total files matching the criteria: 8 test classes (+ 1 helper class).

- **`app/src/androidTest/java/com/netfieldpro/DebugAndStyleGuideTest.java`**
  - Line 15: `import androidx.test.ext.junit.rules.ActivityScenarioRule;`
  - Line 33-34: `public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);`
  - Line 36-38: `@Before public void setUp() { TestSetup.skipOnboarding(); }`
  - State seeding: Does not seed projects/floors; relies on default empty state and `skipOnboarding()`.

- **`app/src/androidTest/java/com/netfieldpro/DebugDataLabEspressoTest.java`**
  - Line 9: `import androidx.test.ext.junit.rules.ActivityScenarioRule;`
  - Line 20-21: `public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);`
  - Line 23: No `@Before` method.
  - State seeding: Interacts with UI buttons in Debug Data Lab (e.g. `SEED_1`, `SEED_3`, `RESET`) directly during test execution.

- **`app/src/androidTest/java/com/netfieldpro/DiagnosticsA11yTest.java`**
  - Line 13: `import androidx.test.core.app.ActivityScenario;`
  - Line 188-200: `@Before public void setUp() { TestSetup.skipOnboarding(); ... }`
  - Line 218, 242, 254, 280: `try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class))`
  - State seeding: Uses fake scan source / latency runner injected via `NetFieldApp.setWifiScanSourceForTest(...)` etc.; does not seed Room entities.

- **`app/src/androidTest/java/com/netfieldpro/M6AccessibilityDeviceTest.java`**
  - Line 19: `import androidx.test.ext.junit.rules.ActivityScenarioRule;`
  - Line 49-50: `public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);`
  - Line 52-54: `@Before public void setUp() { TestSetup.skipOnboarding(); }`
  - State seeding: Tests UI accessibility on project list and navigation; creates projects via UI interactions or assumes initial clean state.

- **`app/src/androidTest/java/com/netfieldpro/OnboardingA11yTest.java`**
  - Line 11: `import androidx.test.ext.junit.rules.ActivityScenarioRule;`
  - Line 34-35: `public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);`
  - No `@Before` method.
  - State seeding: Tests onboarding pages (OB1-OB3). Does not seed Room entities.

- **`app/src/androidTest/java/com/netfieldpro/TierBadgeEspressoTest.java`**
  - Line 10: `import androidx.test.ext.junit.rules.ActivityScenarioRule;`
  - Line 24-25: `public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);`
  - Line 27-31: `@Before public void setUp() { NetFieldApp.entitlementManager().setTierOverride(...); }`
  - State seeding: Sets entitlement tier override in prefs/memory; does not seed Room entities.

- **`app/src/androidTest/java/com/netfieldpro/WalkEveryEdgeTest.java`**
  - Line 16: `import androidx.test.ext.junit.rules.ActivityScenarioRule;`
  - Line 36-37: `public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);`
  - Line 39-41: `@Before public void setUp() { TestSetup.skipOnboarding(); }`
  - State seeding: Walks 29 navigation edges in UI; creates a project through UI dialog during edge traversal.

- **`app/src/androidTest/java/com/netfieldpro/WifiScanDeviceTest.java`**
  - Line 117: `WifiScanSource source = NetFieldApp.wifiScanSource();`
  - No `@Before` method.
  - State seeding: Direct component testing of `WifiScanSource`; does not seed Room entities.

- *(Reference helper)* **`app/src/androidTest/java/com/netfieldpro/TestSetup.java`**
  - Line 7: `NetFieldApp.onboardingStore().markDone();`

---

### 2. Exact assertion text and timeout of `WifiScanDeviceTest` and `LatencyDeviceTest`

- **`WifiScanDeviceTest.java`**:
  - Timeout:
    - Line 144: `boolean received = batchLatch.await(20, TimeUnit.SECONDS);` (20 seconds)
  - Assertions:
    - Line 140: `throw new AssertionError("requestScan() threw an exception", t);`
    - Line 151: `assertEquals("BSSID must be normalized", Normalize.bssid(obs.bssid()), obs.bssid());`
    - Line 152: `assertNotEquals("Band must not be UNKNOWN", Band.UNKNOWN, ChannelMath.toBand(obs.freqMHz()));`
    - Line 170: `fail("No wifi scan batch arrived on physical device within 20s");`

- **`LatencyDeviceTest.java`**:
  - Timeouts:
    - Line 79: `boolean completed = latch.await(15, TimeUnit.SECONDS);` (15 seconds)
    - Line 119: `startedLatch.await(100, TimeUnit.MILLISECONDS);` (100 milliseconds)
    - Line 128: `if (elapsedMs > 500) { break; }` (500 milliseconds cancel timeout check)
    - Line 170: `secondLatch.await(15, TimeUnit.SECONDS);` (15 seconds)
  - Assertions:
    - Line 82: `assertNotNull("Outcome must not be null", outcome);`
    - Line 83-84: `assertTrue("Outcome must be OK, UNREACHABLE, or NETWORK_LOST: " + outcome, outcome.startsWith("OK:") || outcome.equals("UNREACHABLE") || outcome.equals("NETWORK_LOST"));`
    - Line 133: `assertTrue("Cancel must return within 500ms, took " + cancelElapsedMs + "ms", cancelElapsedMs <= 500);`
    - Line 172: `assertTrue("Second run immediately after must work", secondRan.get() || !runner.isRunning());`

---

### 3. How `AndroidWifiScanSource` holds the Clock
In `app/src/main/java/com/netfieldpro/wifi/AndroidWifiScanSource.java`:
- Line 29: `private final Clock clock;`
- Line 78-82:
  ```java
  public AndroidWifiScanSource(Context context, Clock clock) {
      this.context = Objects.requireNonNull(context, "context cannot be null").getApplicationContext();
      this.clock = Objects.requireNonNull(clock, "clock cannot be null");
      this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
  }
  ```
The `Clock` instance is held in the private final field named `clock`.

---

### 4. Contents of `docs/permissions.golden.txt`
Full contents (8 lines total):
```
android.permission.ACCESS_FINE_LOCATION
android.permission.ACCESS_COARSE_LOCATION
android.permission.POST_NOTIFICATIONS
android.permission.ACCESS_WIFI_STATE
android.permission.CHANGE_WIFI_STATE
android.permission.ACCESS_NETWORK_STATE
android.permission.INTERNET
?com.netfieldpro.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
```
- Line 8 starts with `?`, matching `?com.netfieldpro.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- The number of **REQUIRED** permission lines (lines without `?`) is **7** (lines 1–7).

---

### 5. `grep -rn "file:///" docs/*.md` and "Total Automated" lines

**`file:///` references in `docs/*.md`:**
```
docs\M2-REPORT.md:335:Raw logs and timestamps are captured in [verify-device.txt](file:///c:/Users/Fahim/Documents/NetField%20Pro/docs/evidence/M2/verify-device.txt).
docs\M3-REPORT.md:28:- **Exit Codes:** [exit-codes.txt](file:///docs/evidence/M3/exit-codes.txt) (all steps exited 0: `verify=0`, `verify-device=0`, `vectors=0`, `selftest=0`, `coldstart=0`)
docs\M3-REPORT.md:29:- **Environment & Device Manifest:** [env.txt](file:///docs/evidence/M3/env.txt)
docs\M3-REPORT.md:30:- **Host Test Suite (`scripts/verify.sh`):** [verify.txt](file:///docs/evidence/M3/verify.txt)
docs\M3-REPORT.md:31:- **On-Device Instrumentation (`scripts/verify-device.sh`):** [verify-device.txt](file:///docs/evidence/M3/verify-device.txt)
docs\M3-REPORT.md:32:- **Frozen Vector Verification (`tools/verify_vectors.py`):** [vectors.txt](file:///docs/evidence/M3/vectors.txt)
docs\M3-REPORT.md:33:- **Self-Test Mutation Check (`tools/verify_vectors.py --selftest`):** [selftest.txt](file:///docs/evidence/M3/selftest.txt)
docs\M3-REPORT.md:34:- **Cold-Start Performance Benchmark (`scripts/coldstart.sh`):** [coldstart.txt](file:///docs/evidence/M3/coldstart.txt)
docs\M3-REPORT.md:42:| **A3-01** | `verify.sh` exit 0; `HOST TOTAL` printed | [verify.txt](file:///docs/evidence/M3/verify.txt#L69-L72) (`HOST TOTAL: tests 217 failures 0 errors 0 skipped 0`) | **PASS** |
docs\M3-REPORT.md:43:| **A3-02** | `verify-device.sh` green on API 30 and latest AVD, serially | [verify-device.txt](file:///docs/evidence/M3/verify-device.txt#L186-L190) (Low_API30: 15/15, High_API35: 15/15) | **PASS** |
docs\M3-REPORT.md:44:| **A3-03** | `verify_vectors.py` `TOTAL 85 PASS 85 FAIL 0` and `SELFTEST OK` | [vectors.txt](file:///docs/evidence/M3/vectors.txt) & [selftest.txt](file:///docs/evidence/M3/selftest.txt) | **PASS** |
docs\M3-REPORT.md:50:| **A3-09** | `M3StringsTest` and `NoHardcodedTextTest` green | [verify.txt](file:///docs/evidence/M3/verify.txt) (`M3StringsTest`, `NoHardcodedTextTest` passed) | **PASS** |
docs\M3-REPORT.md:54:| **A3-13** | `coldstart.txt` shows `DEVICE_CLASS=PHYSICAL`, median ≤ 1800 ms | [coldstart.txt](file:///docs/evidence/M3/coldstart.txt) (`DEVICE_CLASS=PHYSICAL`, Median: 646 ms, Max: 788 ms) | **PASS** |
docs\M3-REPORT.md:55:| **A3-14** | `docs/evidence/M3/exit-codes.txt` is all zeros | [exit-codes.txt](file:///docs/evidence/M3/exit-codes.txt) (5/5 steps exit code 0) | **PASS** |
docs\M4-REPORT.md:27:- **Exit Codes:** [exit-codes.txt](file:///docs/evidence/M4/exit-codes.txt) (`verify=0`, `verify-device=0`, `vectors=0`, `selftest=0`)
docs\M4-REPORT.md:28:- **Environment & Device Manifest:** [env.txt](file:///docs/evidence/M4/env.txt)
docs\M4-REPORT.md:29:- **Host Test Suite (`scripts/verify.sh`):** [verify.txt](file:///docs/evidence/M4/verify.txt)
docs\M4-REPORT.md:33:- **On-Device Instrumentation (`scripts/verify-device.sh`):** [verify-device.txt](file:///docs/evidence/M4/verify-device.txt)
docs\M4-REPORT.md:38:- **Frozen Vector Verification (`tools/verify_vectors.py`):** [vectors.txt](file:///docs/evidence/M4/vectors.txt)
docs\M4-REPORT.md:39:- **Self-Test Mutation Check (`tools/verify_vectors.py --selftest`):** [selftest.txt](file:///docs/evidence/M4/selftest.txt)
docs\M4-REPORT.md:47:| **A4-01** | `verify.sh` exit 0 with `HOST TOTAL`, `verifyCoreDeps` visible, and the permissions check | [verify.txt](file:///docs/evidence/M4/verify.txt) (`HOST TOTAL: tests 272 failures 0 errors 0 skipped 0`, `check-permissions: OK`) | **PASS** |
docs\M4-REPORT.md:48:| **A4-02** | `verify-device.sh` green on API 30 and the newest AVD, serially | [verify-device.txt](file:///docs/evidence/M4/verify-device.txt) (Low_API30: 18/18, High_API35: 18/18) | **PASS** |
docs\M4-REPORT.md:49:| **A4-03** | `verify_vectors.py` `TOTAL 85 PASS 85 FAIL 0` and `SELFTEST OK` | [vectors.txt](file:///docs/evidence/M4/vectors.txt) & [selftest.txt](file:///docs/evidence/M4/selftest.txt) | **PASS** |
docs\M4-REPORT.md:58:| **A4-12** | `exit-codes.txt` is all zeros | [exit-codes.txt](file:///docs/evidence/M4/exit-codes.txt) (all 4 steps exit code 0) | **PASS** |
docs\M5-REPORT.md:27:- **Exit Codes:** [exit-codes.txt](file:///docs/evidence/M5/exit-codes.txt) (`verify=0`, `verify-device=0`, `vectors=0`, `selftest=0`)
docs\M5-REPORT.md:28:- **Environment & Device Manifest:** [env.txt](file:///docs/evidence/M5/env.txt)
docs\M5-REPORT.md:29:- **Host Test Suite (`scripts/verify.sh`):** [verify.txt](file:///docs/evidence/M5/verify.txt)
docs\M5-REPORT.md:33:- **On-Device Instrumentation (`scripts/verify-device.sh`):** [verify-device.txt](file:///docs/evidence/M5/verify-device.txt)
docs\M5-REPORT.md:38:- **Frozen Vector Verification (`tools/verify_vectors.py`):** [vectors.txt](file:///docs/evidence/M5/vectors.txt)
docs\M5-REPORT.md:39:- **Self-Test Mutation Check (`tools/verify_vectors.py --selftest`):** [selftest.txt](file:///docs/evidence/M5/selftest.txt)
docs\M5-REPORT.md:47:| **A5-01** | `verify.sh` and `verify-device.sh` (API 30 and newest) green | [verify.txt](file:///docs/evidence/M5/verify.txt) (`HOST TOTAL: tests 308 failures 0 errors 0 skipped 0`), [verify-device.txt](file:///docs/evidence/M5/verify-device.txt) (High_API35: 21/21, Low_API30: 21/21) | **PASS** |
docs\M5-REPORT.md:48:| **A5-02** | Vectors 85/85 and `SELFTEST OK` | [vectors.txt](file:///docs/evidence/M5/vectors.txt) & [selftest.txt](file:///docs/evidence/M5/selftest.txt) | **PASS** |
docs\M5-REPORT.md:53:| **A5-07** | The permission check passes with `INTERNET` | [verify.txt](file:///docs/evidence/M5/verify.txt) (`check-permissions: OK (8 permissions verified against golden)`) | **PASS** |
docs\M5-REPORT.md:54:| **A5-08** | `:core` clean (R1–R7, `verifyCoreDeps`); schema diff empty; 29 edges unchanged | [verify.txt](file:///docs/evidence/M5/verify.txt) (`verifyCoreDeps`, `ArchRulesTest`), `git diff --exit-code app/schemas` empty, `WalkEveryEdgeTest` (29 edges) | **PASS** |
docs\M5-REPORT.md:55:| **A5-09** | `coldstart.txt` physical, median ≤ 1800 ms; `exit-codes.txt` all zeros | [exit-codes.txt](file:///docs/evidence/M5/exit-codes.txt) all zeros. Physical benchmark preserved from M3 vivo 1906 (median 646 ms ≤ 1800 ms) | **PASS** |
docs\M6-REPORT.md:27:- **Exit Codes:** [exit-codes.txt](file:///docs/evidence/M6/exit-codes.txt) (`verify=0`, `verify-device=0`, `vectors=0`, `selftest=0`)
docs\M6-REPORT.md:28:- **Environment & Device Manifest:** [env.txt](file:///docs/evidence/M6/env.txt)
docs\M6-REPORT.md:29:- **Host Test Suite (`scripts/verify.sh`):** [verify.txt](file:///docs/evidence/M6/verify.txt)
docs\M6-REPORT.md:34:- **On-Device Instrumentation (`scripts/verify-device.sh`):** [verify-device.txt](file:///docs/evidence/M6/verify-device.txt)
docs\M6-REPORT.md:39:- **Frozen Vector Verification (`tools/verify_vectors.py`):** [vectors.txt](file:///docs/evidence/M6/vectors.txt)
docs\M6-REPORT.md:40:- **Self-Test Mutation Check (`tools/verify_vectors.py --selftest`):** [selftest.txt](file:///docs/evidence/M6/selftest.txt)
docs\M6-REPORT.md:48:| **A6-01** | `verify.sh` and `verify-device.sh` (API 30 and newest) green | [verify.txt](file:///docs/evidence/M6/verify.txt) (`HOST TOTAL: tests 382 failures 0 errors 0 skipped 0`), [verify-device.txt](file:///docs/evidence/M6/verify-device.txt) (Low_API30: 31/31, High_API35: 31/31) | **PASS** |
docs\M6-REPORT.md:49:| **A6-02** | Vectors 85/85 and `SELFTEST OK` | [vectors.txt](file:///docs/evidence/M6/vectors.txt) (`TOTAL 85 PASS 85 FAIL 0`) & [selftest.txt](file:///docs/evidence/M6/selftest.txt) (`SELFTEST OK`) | **PASS** |
docs\M6-REPORT.md:56:| **A6-09** | `PlanImportDeviceTest` green on API 30 and the newest AVD | `com.netfieldpro.PlanImportDeviceTest` in [verify-device.txt](file:///docs/evidence/M6/verify-device.txt) | **PASS** |
docs\M6-REPORT.md:58:| **A6-11** | `coldstart.txt` physical, median ≤ 1800 ms; `exit-codes.txt` all zeros | [exit-codes.txt](file:///docs/evidence/M6/exit-codes.txt) all zeros. Physical benchmark preserved from M3 vivo 1906 (median 646 ms ≤ 1800 ms) | **PASS** |
```

**"Total Automated" lines in `docs/*.md`:**
```
docs\M0-REPORT.md:16:| **Total Automated Tests** | **Project** | **47 tests** | **100% GREEN** |
docs\M1-REPORT.md:17:| **Total Automated Tests** | **Project & Verification** | **115 tests + 82 vectors** | **100% GREEN** |
docs\M2-REPORT.md:19:| **Total Automated Tests** | **Full Project Suite** | **181 tests + 85 vectors** | **100% GREEN** |
docs\M3-REPORT.md:20:| **Total Automated Tests** | **Full Project Suite** | **247 tests + 85 vectors** | **100% GREEN** |
docs\M4-REPORT.md:19:| **Total Automated Tests** | **Full Project Suite** | **308 tests + 85 vectors** | **100% GREEN** |
docs\M5-REPORT.md:19:| **Total Automated Tests** | **Full Project Suite** | **350 tests + 85 vectors** | **100% GREEN** |
docs\M6-REPORT.md:19:| **Total Automated Tests** | **Full Project Suite** | **444 tests + 85 vectors** | **100% GREEN** |
```

---

### 6. Repository delete methods used by M6
In `app/src/main/java/com/netfieldpro/data/repository/ProjectRepository.java`:
- Method signature:
  `public void deleteProject(String id)`
- Implementation details:
  ```java
  public void deleteProject(String id) {
      List<FloorRow> floors = dao.getFloorsForProject(id);
      if (floors != null) {
          for (FloorRow f : floors) {
              planFileStore.deletePlanFile(f.id);
          }
      }
      writer.submit(database -> database.netFieldDao().deleteProject(id), null);
      writer.flush();
  }
  ```
- Completion signaling:
  The database deletion task is submitted to `BatchedWriter` (`writer.submit(...)`), and synchronous completion is ensured by calling `writer.flush()`.
  In `BatchedWriter.java`, `flush()` submits an empty task to the single-threaded background executor `executor.submit(() -> {})` and blocks via `future.get()` until all queued write batches have finished committing inside a database transaction.
  Floor plan files on disk are deleted synchronously via `planFileStore.deletePlanFile(f.id)` before the database deletion.
