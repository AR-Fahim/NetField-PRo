# M5 Inventory

## 1. M1 LatencyResult Fields
Located at `core/src/main/java/com/netfieldpro/core/model/entity/LatencyResult.java`:
- `String id`: Unique entity ID (22-char base64url)
- `String pointId`: Parent point ID
- `com.netfieldpro.core.model.enums.LatencyMethod method`: LatencyMethod enum (`TCP`, `ICMP`)
- `String target`: Target IP / host string
- `String boundIface`: Bound network interface name
- `com.netfieldpro.core.model.enums.LatencyScope scope`: LatencyScope enum
- `int sent`: Total probes sent (must equal `recv + lossCount`)
- `int recv`: Successful probes received (must equal `rtts.size()`)
- `Double minMs`: Minimum RTT in milliseconds (null if `recv == 0`)
- `Double avgMs`: Average RTT in milliseconds (null if `recv == 0`)
- `Double maxMs`: Maximum RTT in milliseconds (null if `recv == 0`)
- `Double jitterMs`: Mean jitter in milliseconds (null if `recv < 2`)
- `int lossCount`: Number of lost probes
- `boolean duringScan`: Whether probe was executed during Wi-Fi scan
- `String connectedBssid`: Connected AP BSSID
- `String ssid`: Connected SSID
- `List<Double> rtts`: List of individual RTTs in milliseconds (size == recv)

## 2. ProbeMethod Enum
Located at `core/src/main/java/com/netfieldpro/core/model/enums/ProbeMethod.java`:
- Package: `com.netfieldpro.core.model.enums`
- Enum name: `ProbeMethod`
- Values:
  - `ICMP`
  - `TCP`
  - `NONE`
Note: Per M5-0 mandate, this existing enum must be reused and never duplicated.

## 3. Clock Interface
Located at `core/src/main/java/com/netfieldpro/core/time/Clock.java`:
- Package: `com.netfieldpro.core.time`
- Methods:
  - `long monoNs()`: Monotonic nanosecond timestamp
  - `long wallMs()`: Wall clock epoch milliseconds

## 4. FakeClock
Located at `core/src/testFixtures/java/com/netfieldpro/core/time/FakeClock.java`:
- Package: `com.netfieldpro.core.time`
- Implements `Clock`
- Methods:
  - `FakeClock(long monoNs, long wallMs)`
  - `FakeClock advanceMs(long ms)`
  - `FakeClock advanceNs(long ns)`
  - `FakeClock jumpWallMs(long delta)`
  - `long monoNs()`
  - `long wallMs()`

## 5. Sleeper Interface
- Status: None exists in the codebase prior to M5.
- M5-1 requirement: Define `Sleeper` interface in `:core` and `FakeSleeper` in `:core` testFixtures that advances `FakeClock`.

## 6. Human Prerequisites (D1)
- Play Data Safety declaration: NetField Pro requests `android.permission.INTERNET` solely for local LAN probes (ICMP/TCP datagrams/connections to the local Wi-Fi gateway or local DNS resolver). The Play Data Safety text must state: "no data leaves the device".

