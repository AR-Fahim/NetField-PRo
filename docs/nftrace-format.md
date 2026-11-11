# .nftrace Format Specification (v1)

## Overview

The `.nftrace` format is a lightweight, line-delimited streaming trace format used by NetField Pro to record hardware telemetry, sensor data, and user actions.

## Normative Specification

- **Encoding:** UTF-8, one JSON object per line, `\n` terminated (LF), with the final line also `\n` terminated. Each line is written in canonical form (keys sorted in UTF-16 lexicographical order). A line is at most 1,048,576 bytes (1 MB).
- **Line 1 (Header):**
  - Canonical JSON object containing the following keys (in UTF-16 order):
    - `createdAtMs` (int64): Epoch millisecond timestamp when recording started.
    - `deviceModel` (string): Hardware device model (e.g. `Pixel 7`).
    - `monoEpochNs` (int64): Monotonic nanosecond origin for the trace session.
    - `nftrace` (integer): Format major version, value `1`.
    - `sdk` (integer): Android SDK API level (e.g. `34`).
- **Event Lines (Line 2+):**
  - Canonical JSON object containing:
    - `d` (object): Event payload data.
    - `k` (string): Event kind name.
    - `t` (int64): Monotonic nanosecond offset from `monoEpochNs`.
- **Ordering:**
  - The timestamp offset `t` is non-decreasing across event lines.
- **Registered Kinds:**
  - `WIFI_SCAN`
  - `LINK`
  - `SENSOR_SUMMARY`
  - `NET_EVENT`
  - `SCREEN`
  - `PROBE_RESULT`
  - `USER`
  - M0 defines only the envelope. Each subsequent milestone defines its specific payload schema when it first records that kind.
  - Readers must preserve and safely ignore unknown kinds.

## Golden Fixture

Fixture path: `core/src/testFixtures/resources/traces/sample_v1.nftrace`

Content:
```
{"createdAtMs":1700000000000,"deviceModel":"Pixel 7","monoEpochNs":5000000000,"nftrace":1,"sdk":34}
{"d":{"seq":1},"k":"USER","t":0}
{"d":{"seq":2},"k":"USER","t":1500000000}
```

SHA-256: `367b4a45039739a764853a456acbb1fd000963643d79681ea0803e907908331d`

