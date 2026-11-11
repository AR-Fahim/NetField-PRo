# M4 Inventory

## 1. AndroidClock.monoNs() source line
File: `app/src/main/java/com/netfieldpro/time/AndroidClock.java`
Line 9:
```java
return SystemClock.elapsedRealtimeNanos();
```
The timebase is `SystemClock.elapsedRealtimeNanos()`. Wi-Fi `ScanResult.timestamp` is in microseconds on `elapsedRealtime`, so `timestampNs = ScanResult.timestamp * 1000` shares the identical timebase. No change to `AndroidClock.monoNs()` is required.

## 2. M1 TraceEvent fields
File: `core/src/main/java/com/netfieldpro/core/trace/TraceEvent.java`
Definition:
```java
public record TraceEvent(long tNs, String kind, JsonObject data)
```
Fields:
- `long tNs`: event timestamp in monotonic nanoseconds
- `String kind`: event type identifier (e.g., `"wifi.batch"`)
- `JsonObject data`: structured JSON payload

`TraceEvent` can carry a structured `JsonObject` payload as required by D8.

## 3. S20 nav destination, args, and inbound edges
File: `app/src/main/res/navigation/nav_main.xml`
- Destination element:
  ```xml
  <fragment
      android:id="@+id/nav_s20"
      android:name="com.netfieldpro.ui.placeholder.PlaceholderFragment"
      android:label="@string/screen_s20_title">
      <argument
          android:name="screenId"
          app:argType="string"
          android:defaultValue="S20" />
  </fragment>
  ```
- Destination ID: `@id/nav_s20`
- Current class: `com.netfieldpro.ui.placeholder.PlaceholderFragment`
- Arguments: `screenId` (type `string`, defaultValue `"S20"`)
- Inbound edge:
  - From `nav_s19` (Settings): action `action_s19_to_s20` with `app:destination="@id/nav_s20"`

## 4. verify-device.sh structure
File: `scripts/verify-device.sh`
- Shebang and error handling: `#!/usr/bin/env bash` and `set -euo pipefail`.
- Target device resolution: uses `$DEVICES` environment variable if present; otherwise queries `adb devices` for state `device`. Fails if no devices are attached.
- Scale restoration: captures initial scales (`window_animation_scale`, `transition_animation_scale`, `animator_duration_scale`) into associative arrays and restores them on exit via `trap restore_scales EXIT`.
- Serial device test execution:
  - Iterates over each device sequentially.
  - Logs model and sdk version via `adb shell getprop`.
  - Disables animations (`settings put global ... 0`).
  - Runs `ANDROID_SERIAL="$dev" ./gradlew :app:connectedDebugAndroidTest`.
  - Aggregates individual device JUnit XML outputs from `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` into `app/build/outputs/androidTest-results-all/`.
- Results reporting: moves collected XMLs back to `app/build/outputs/androidTest-results/connected/debug/` and executes `python scripts/test-counts.py`.
- Exits 1 if any device failed or if `test-counts.py` exited non-zero.

## 5. How instrumented tests currently skip onboarding (T5)
In M3, instrumented tests bypassed onboarding by calling `NetFieldApp.onboardingStore().markDone()` directly inside `@Before setUp()` in:
- `app/src/androidTest/java/com/netfieldpro/DebugAndStyleGuideTest.java` (line 38)
- `app/src/androidTest/java/com/netfieldpro/WalkEveryEdgeTest.java` (line 33)

Per T5 resolution, this is centralized into `TestSetup.skipOnboarding()` in `app/src/androidTest/java/com/netfieldpro/TestSetup.java`.

## 6. RawSample, Scan, and SurveySession.capabilityReport field names
- `RawSample` (`com.netfieldpro.core.model.entity.RawSample`):
  - `long rowId`
  - `String scanId`
  - `String bssid`
  - `String ssid`
  - `int freqMHz`
  - `int rssi`
  - `Long scanResultTsNs`
- `Scan` (`com.netfieldpro.core.model.entity.Scan`):
  - `String id`
  - `String sessionId`
  - `String pointId`
  - `SweepClass sweepClass`
  - `long startNs`
  - `long endNs`
  - `long arrivalNs`
  - `boolean tsPlausible`
  - `boolean throttleOffAtScan`
- `SurveySession.capabilityReport` (`com.netfieldpro.core.model.entity.SurveySession`):
  - Field name: `capabilityReport`
  - Type: `com.netfieldpro.core.json.JsonObject`
