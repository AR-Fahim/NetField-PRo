# Freeze Pack Part B: Engineering & Math

**Status.** Part B is complete as a draft, but it is not yet freezable. Computing the golden vectors by hand surfaced 17 audit items (K1–K17), including 3 critical ones. Every vector below uses my recommended default and is tagged `[Kn]`, so an override only invalidates the vectors carrying that tag. All numbers were computed by hand. The first thing M1 does is re-verify every vector with an independent tool (Python or `sha256sum`). Any mismatch comes back to you as an audit item.

---

## §0. Your decisions applied

| Decision | Effect |
|---|---|
| Traffic-light is the default palette; viridis ("Color-blind safe") is a toggle in S19 and a quick-switch on S09 | Anchors in §3.11. Side effects are in K10. |
| Free gets the wall tool and wall-aware heatmaps | Unchanged from Part A. |
| M13b: Firebase Auth, with `S00` before S01/S02 | Hooks and rules in K15–K17, §3.18. Order is now `… M13 → M13b`. The screen count is 21 plus an experimental `S00`. |

---

## §1. Audit report (answer "accept all K1–K17" or list overrides)

| ID | Sev | Finding (where found) | Recommended default |
|---|---|---|---|
| **K1** | **Critical** | **QR signature scope is a hole.** `sg` was never defined as signing anything beyond `ah`. If only `ah` is signed, an attacker can edit `vd`, `c` or `ts` in the QR and Level 1 still passes. WebCrypto (J9) also cannot verify a pre-hashed digest. | `sg` = ECDSA-P256-SHA256 over the canonical bytes of the QR core, meaning the payload without `sg`: `{c,ah,eh,ip,pf,pk,sid,ts,v,vd}`, keys in UTF-16 order. `ah` = SHA-256 of the canonical Assessment excluding `sig`. `Assessment.sig` is the same signature, stored as DER in the ZIP and raw r‖s in the QR. There is no circularity: the signature covers `ah`, and `ah` excludes the signature. Level 2 rebuilds the QR core from Assessment and Evidence and verifies the same signature. |
| **K2** | **Critical** | **The grid cap is violated.** `g = max(0.5, √(area/25000))` with per-axis `ceil` gives 224×112 = 25,088 cells on a 200×100 m bbox. A 1000×20 m bbox gives 25,737 (vector GR-2). | `areaM²` is the bbox area. Round `g` to 6 dp. While `cols·rows > 25000`, set `g = round6(g × 1.01)`. All geometry uses the rounded `g`, which is stored in the Assessment. |
| **K3** | **Critical (decision)** | **Verdict precedence is unspecified.** What wins when masked >5% and a criterion is NOT MET? | `NOT_ELIGIBLE` > `INCOMPLETE` (any required criterion NOT_EVALUATED, or masked >5%) > `FAIL` > `PASS`. The PDF still shows every NOT MET row. Alternative: FAIL beats INCOMPLETE for non-coverage criteria. I recommend the simple order. |
| **K4** | High | **Display rounding can show a passing number on a failing row.** 1799/2000 = 89.95% rounds to "90.0%" next to `NOT MET` (VD-4). | Amend P7. Compare on exact integers or unrounded values. For display and the QR `c` array, round conservatively: floor for "≥" criteria, ceil for "≤" criteria. Stored metrics stay fixed-decimal HALF_EVEN at 3 dp. |
| **K5** | High | **IDW candidate semantics are ambiguous.** Does a sample beyond `R_eff` still contribute weight? | No. Candidates are samples with `d_eff ≤ R_eff`. Take the k=12 smallest by `d_eff`, ties broken by sample id ascending. There is a small weight discontinuity at the `R_eff` edge, which I accept. |
| **K6** | High | **Wall kernel rules are undefined** for endpoint touch, vertex pass-through, collinear overlap, near-wall samples and dedupe scope. | Sign-change rule, defined in §3.8. There is no geometric tolerance in the kernel. The UI shows a "Pin is within 0.15 m of a wall: confirm side" chip. Dedupe (<0.05 m) applies across different chains only, keeping the max P. Same-chain crossings are never deduped. |
| **K7** | High | **Jitter with lost probes is undefined.** | Use consecutive received RTTs in send order, skipping lost probes. Fewer than 2 received gives `null` (excluded from the verdict mean). If every point is null, the criterion is NOT_EVALUATED. |
| **K8** | High | **The reference channel set is unspecified, and warm-up scans have no class.** | The reference set is the set of frequencies (MHz) heard in ≥2 of the 3 warm-up scans, over all SSIDs. `sweepClass` gets a third value, `WARMUP`. An empty reference set means warm-up failure. Sweep classification is done in integers (§3.4). |
| **K9** | High | **Pin quality handling is silent.** `quality` is a single enum, so MOVED and LOW_CONFIDENCE can't both be stored. | MOVED outranks LOW_CONFIDENCE, and `nScans` is stored separately so both stay derivable. All pins feed IDW at their source weight. All TAP pins count in the CCTV 100% rule. The report prints counts of LOW_CONFIDENCE and MOVED pins. S13 gets a soft warning when any exist. |
| **K10** | High | **The palette swap has side effects.** (a) The active-profile contour `#D6007F` is nearly indistinguishable from `#D73027` and `#FC8D59`. (b) Pale yellow fills break fixed-color glyph text. (c) The PDF palette is unspecified. | (a) Cobalt `#0047AB` (dark mode `#8AB4FF`) in traffic-light mode; magenta stays in viridis. (b) Glyph text is whichever of black or white gives the higher contrast ratio against the fill. (c) The PDF uses the S19 palette at export time and prints the palette name in the legend. |
| **K11** | Med | **QR payload gaps.** The `c` array was not itemized, `ts` was undefined, and the capacity claim needs re-checking. | `c` = `[coverage, secondary, p95Rtt, meanJitter, pooledLoss, worstPointLoss, stickyCount, maskedPct]`, each ×10 with the K4 rounding. Sentinels: −1 is NOT_REQUIRED, −2 is NOT_EVALUATED. `ts` = `assessedAt` in ms. Capacity is itemized in §3.15. The worst case is **380 B**. |
| **K12** | Med | **Schema gaps.** (1) No ID format. (2) Array order unspecified. (3) Null handling would change old hashes when columns are added. (4) `RawSample` lacks `ssid` and `freqMHz`, which neighbors.json needs. (5) `SurveySession.contentHash` and `.signature` duplicate Evidence and Assessment. (6) `finalizedAt` is not hash-covered. (7) The 2^53 threshold is off by one. | (1) IDs are 16 random bytes as 22-char base64url. (2) Every array has a normative sort key (§2). (3) A null field means the key is omitted. (4) Add both columns to `RawSample`. (5) Replace them with `evidenceSha256` and `publicKeyFp`; the signature lives on Assessment. (6) Put `finalizedAtMs` in the Evidence manifest. (7) Numbers if |v| ≤ 9007199254740991, otherwise strings. |
| **K13** | Med | **Sticky metric definitions are loose.** | Event start = completion time of the first qualifying sweep in the opening run. End = the last qualifying sweep's time, or the roam time if closed by a roam. `ttr` = roam time − start, only when closed by ROAM. Ping-pong is inclusive (≤10.0 s). Any BSSID change closes an open event. Session end closes it with reason `SESSION_END`. |
| **K14** | Med | **PATH binning is undefined.** | Bin = `floor(round(s·1000)/1500)`, where `s` is the arc-length position in m. Per BSSID, the bin value is the median RSSI. Bin position = mean of member positions. Weight is 0.5. |
| **K15** | High (M13b) | **M13b can't be removable "without touching any screen" unless M0 builds hooks.** The launch gate, the S19 "Log Out" row and the S00 nav node would otherwise edit existing screens and graphs. | M0 adds three hooks: `StartRouter` (§3.18), a `SettingsExtensionRegistry` list that S19 renders generically, and `nav_auth_slot.xml` included from the main graph. The `gated` flavor overrides the slot file with S00. M13b adds Gradle flavors `gated` and `open`: Firebase and `google-services` live only in `gated`; `open` has `NoOpAuthGatewayImpl`. Removal checklist: delete `src/gated`, drop the flavor, delete `google-services.json`. |
| **K16** | High (M13b) | **U4 ("offline everywhere except purchase") conflicts with a first login.** | Amend U4 to: "offline except purchase and first alpha sign-in". Other rules: a sign-out is blocked while a survey session is active; Back from S17 entered via S00 returns to S00; sign-out clears the back stack to S00. |
| **K17** | Med (M13b) | **The 5-method interface is not usable alone.** It has no async contract, no error model and no password reset. A tester who forgets a password is locked out. | Add `sendPasswordReset(email, cb)`, an `AuthCallback` and an `AuthError` enum (§3.18). Exclude Firebase prefs from backup via `data_extraction_rules`. Alpha data collected is the email plus last sign-in time, so the privacy policy and Play Data Safety form must say so. Human prerequisites for this: Firebase project, Email/Password provider enabled, a gated-only `google-services.json` (gitignored), and one test on a non-GMS device. |

---

## §2. Consolidated Schema v1.0

**Conventions (normative):**

- IDs are 22-char base64url strings. Only `RawSample` uses an autoincrement `long`, and it is exported by array position.
- `D(n)` means a double stored natively and serialized as a fixed-decimal string with n dp (exact binary expansion, HALF_EVEN, no `-0`).
- `I` is int64, serialized as a number if |v| ≤ 9007199254740991 and as a string otherwise.
- Every `*Ns` field is an offset from `SurveySession.monoEpochNs`.
- A null field means the key is omitted from canonical JSON.
- "Frozen" covers semantics, enums and canonical rules. Additive nullable columns remain legal via Room migrations.

**Stored entities** (`sort` is the array order in canonical JSON):

| Entity | Fields | Sort |
|---|---|---|
| Project | id, name, client?, site?, createdAtMs I | id |
| Floor | id, projectId, levelIndex I, planSha256, planWpx, planHpx, metersPerPixel D(9), northOffsetDeg D(3)?, scaleMethod `LINE\|ROOM_WIDTH`, scaleUncertaintyPct D(3), scaleChecked B, alignX/alignY/alignRotDeg?, scaleP1/P2 | levelIndex |
| SurveySession | id, floorId, name, state `DRAFT\|FINALIZED`, createdAtMs, monoEpochNs I, schemaVersion, appVersion, algoVersion, throttleAtStart B, timingQuality `GOOD\|DEGRADED_TIMING`, evidenceGrade `VERDICT\|INDICATIVE`, sessionType `STANDARD\|ROAMING_AUDIT`, intendedProfileId, intendedProfileChanges I, rssiOffsetDb D(1) (render only), linkCadenceMs I, cadenceFlag `MEASURED\|DEFAULT`, probeMethod `ICMP\|TCP\|NONE`, referenceFreqsMHz [I], receiverId, deviceInfoId, capabilityReport (object), finalizedAtMs?, evidenceSha256?, publicKeyFp? | n/a |
| DeviceInfo | id, model (≤32), manufacturer, sdk, wifiStandards, linkCadenceMs?, buildFingerprint | id |
| Scan | id, sessionId, pointId?, sweepClass `FULL\|PARTIAL\|WARMUP`, startNs, endNs, arrivalNs, tsPlausible B, throttleOffAtScan B | startNs, id |
| RawSample | rowId, scanId, bssid, ssid, freqMHz, rssi, scanResultTsNs I? | rowId |
| SurveyPoint | id, sessionId, x D(3), y D(3), source `TAP\|PATH\|DR`, quality `OK\|LOW_CONFIDENCE\|MOVED`, tMonoNs, tWallMs, nScans, positionUncertaintyM D(2), segmentId? | tMonoNs, id |
| Measurement | id, pointId, protocol `WIFI\|BLE\|CELL\|ZIGBEE`, bssidOrId, ssid?, freqMHz, channel, widthMHz?, source `SCAN\|ACTIVE_LINK\|PROBE`, receiverId, n, rssiMedian D(1), rssiMin, rssiMax, lqi?, panId? | pointId, bssidOrId |
| LatencyResult | id, pointId, method `TCP\|ICMP`, target, boundIface, scope `GATEWAY\|WAN`, sent, recv, minMs D(3), avgMs D(3), maxMs D(3), jitterMs D(3)?, lossCount, duringScan B, **connectedBssid, ssid** (J4), rtts [D(3)] | pointId |
| LinkSample | id, sessionId, observedAtNs, bssid, **ssid** (J4), rssi, freqMHz, txMbps?, rxMbps? | observedAtNs |
| ConnectionEvent | id, sessionId, type `CONNECT\|DISCONNECT\|ROAM`, atNs, fromBssid?, toBssid?, pointId? | atNs |
| PathSegment | id, sessionId, state `DONE\|ABORTED`, rawStartNs, rawEndNs, trimStartNs, trimEndNs, speedMps D(3), waypoints [{x,y,tNs}], anchors [{vertexIdx,tNs}], mode `ROUTE\|LIVE_CORNER` | rawStartNs |
| AccessPointMarker | id, floorId, x, y, bssid?, txPowerDbm? | id |
| TargetNetwork | id, sessionId, ssid | ssid |
| ApGroup | id, sessionId, label, origin `HEURISTIC\|USER`, memberBssids [S] | label |
| FloorBoundary | floorId, vertices [{x,y}] | n/a |
| Wall | id, floorId, materialId, vertices [{x,y}] | id |
| WallMaterial | materialId, penaltyM D(1), lossDb24 D(1), lossDb5 D(1) | materialId |
| RequirementProfile | id, kind `PRESET\|CUSTOM`, thresholds {…}, bandPolicy, maxInfluenceM, criterionEnabled{} | id |
| Assessment | assessmentId, evidenceSha256, profileId, profileSnapshot, intendedProfileId, isReassessment B, thresholds, computedMetrics, criteria [{id,status,required,measured}], verdict `PASS\|FAIL\|INCOMPLETE\|NOT_ELIGIBLE`, assessedAtMs, algoVersion, gridG D(6), publicKey (44), sig (86 raw, base64url) | assessedAtMs |

Other fields: `core.json` also carries the immutable session header. The Evidence manifest is `{schemaVersion, finalizedAtMs, parts:[{name, sha256, omitted?}]}`.

**Derived, never stored:** imputation (−95), secondary-overlap values, PATH event positions, grid, IDW surface, masks, grades, legend buckets, sticky events (stored only inside `Assessment.computedMetrics`), verdict.

**J4 check:** `LinkSample.ssid`, `LatencyResult.connectedBssid` and `.ssid` are present. ✓

---

## §3. State machines and pure-module contracts

### 3.0 State machines (all single-threaded on the session executor, fed immutable timestamped events, with no clock reads)

**SM-1 Session runtime**

| State | Event | Guard | Next | Actions |
|---|---|---|---|---|
| CREATED | WARMUP_START | perms+Location ON | WARMING_UP | start ScanScheduler, WifiLock, FGS |
| WARMING_UP | SCAN (3rd) | ref set non-empty | READY | store WARMUP scans, learn ref set (K8), cadence probe |
| WARMING_UP | TIMEOUT 20 s | <3 scans or empty ref | WARMUP_FAILED | show "Scans blocked…" |
| WARMUP_FAILED | CONTINUE_INDICATIVE | n/a | READY (grade=INDICATIVE) | link-only |
| READY | START_SURVEY | target SSIDs chosen | ACTIVE | persist session DRAFT |
| ACTIVE | SCREEN_OFF | n/a | ACTIVE | log data-gap event |
| ACTIVE | IDLE_10MIN | no capture/walk | ENDED | stop FGS |
| ACTIVE | END_SESSION | no capture or walk, or user confirmed | ENDED | abort capture or path |
| any | PROCESS_DEATH | n/a | (restart) → S07 idle | discard capture, mark PathSegment ABORTED |

**SM-2 TAP capture**

| State | Event | Guard | Next | Actions |
|---|---|---|---|---|
| IDLE | CAPTURE_TAP | session ACTIVE | AWAIT_SCANS | record tapMono, start 15 s timer |
| AWAIT_SCANS | SCAN_ARRIVED | `EXTRA_RESULTS_UPDATED` and fresh results exist | AWAIT_SCANS | classify sweep; count if FULL; keep ts ≥ tapMono (K8) |
| AWAIT_SCANS | FULL count = 2 | n/a | PROBING | cancel timer |
| AWAIT_SCANS | TIMEOUT | FULL ≥1 | PROBING | quality LOW_CONFIDENCE |
| AWAIT_SCANS | TIMEOUT | FULL = 0 | IDLE | discard, retry prompt |
| AWAIT_SCANS/PROBING | PIN_DRAG | n/a | same | update x,y |
| AWAIT_SCANS/PROBING | MOTION_VARIANCE | n/a | same | set MOVED flag (K9) |
| PROBING | LATENCY_DONE | n/a | COMMITTING | latency starts after the last required scan, radio idle |
| COMMITTING | WRITE_OK | n/a | IDLE | single batched Room write |
| any | CANCEL / PROCESS_DEATH | n/a | IDLE | discard everything |

**SM-3 PATH**

| State | Event | Guard | Next | Actions |
|---|---|---|---|---|
| IDLE | ROUTE_CONFIRMED | ≥2 vertices | ARMED | n/a |
| IDLE | LIVE_CORNER_START | n/a | WALKING | mode LIVE_CORNER |
| ARMED | START | n/a | COUNTDOWN | 0/2/3 s, tick haptics |
| COUNTDOWN | TICK_END | n/a | WALKING | tStart = mono now |
| WALKING | VOL_UP | anchors < vertices−1 | WALKING | add anchor; haptic CONTEXT_CLICK |
| WALKING | VOL_DOWN | anchors > 0 | WALKING | undo anchor |
| WALKING | CORNER_TAP (live mode) | n/a | WALKING | anchor at tapped x,y |
| WALKING | SLIDE_END | n/a | FINISHING | tEnd = mono now |
| FINISHING | TRIM_DONE | speed gate OK | COMMITTING | interpolate (§3.5) |
| FINISHING | TRIM_DONE | speed out of 0.3–2.5 | CONFIRM_PATH | user accepts or discards |
| CONFIRM_PATH | ACCEPT / DISCARD | n/a | COMMITTING / IDLE | n/a |
| WALKING | PROCESS_DEATH | n/a | IDLE | segment ABORTED, orphan samples excluded |
| COMMITTING | WRITE_OK | n/a | IDLE | n/a |

All touches are ignored in WALKING except SLIDE_END (J2).

**SM-4 Finalize** (atomic: one Room transaction at COMMIT; the session stays DRAFT until then)

| State | Event | Guard | Next | Actions |
|---|---|---|---|---|
| DRAFT | HOLD_1500MS | no hard blockers | PRECHECK | n/a |
| PRECHECK | OK | n/a | HASH_PARTS | finalizedAtMs = now |
| HASH_PARTS | DONE | n/a | ENSURE_KEY | stream-hash parts |
| ENSURE_KEY | DONE | n/a | MANIFEST | create Keystore key if absent; attestation or `OEM_UNSUPPORTED` |
| MANIFEST | DONE | n/a | ASSESS | evidenceSha256 |
| ASSESS | DONE | n/a | SIGN | compute metrics and verdict |
| SIGN | DONE | n/a | COMMIT | build QR core, sign (K1) |
| COMMIT | TX_OK | n/a | FINALIZED | open S15 |
| any | FAIL / PROCESS_DEATH | n/a | DRAFT | no partial rows (key may persist) |

### 3.1–3.17 Module contracts and golden vectors

Module signatures are in the traceability table (§5). Vectors follow. "Out" is the exact expected output.

#### 3.1 CanonicalJson (RFC 8785 subset)

Rules: keys sorted by UTF-16 code units; no whitespace; null omitted [K12]; integers as numbers if |v| ≤ 9007199254740991, otherwise strings; floats only as fixed-decimal strings; strings escaped as `\" \\ \b \t \n \f \r`, other control characters below 0x20 as `\u00xx` (lowercase hex), and nothing else escaped.

| ID | In | Out |
|---|---|---|
| CJ-1 | `{"b":1,"a":2}` | `{"a":2,"b":1}` |
| CJ-2 | keys U+1F600 (😀) and U+FF5E (～) | UTF-16 order puts 😀 first. Bytes `7B 22 F0 9F 98 80 22 3A 31 2C 22 EF BD 9E 22 3A 32 7D`. UTF-8 order would be wrong. |
| CJ-3 | 9007199254740991 / 9007199254740992 / −9007199254740992 | `9007199254740991` / `"9007199254740992"` / `"-9007199254740992"` [K12] |
| CJ-4 | fixed(0.125,2) / (0.375,2) / (2.675,2) / (−0.004,2) / (−0.0,3) | `"0.12"` / `"0.38"` / `"2.67"` (binary value is 2.67499…) / `"0.00"` / `"0.000"` |
| CJ-5 | string `a"b\c` + U+0001 + LF + U+007F + U+2028 | `"a\"b\\c\u0001\n` then DEL and U+2028 raw `"` |
| CJ-6 | `{"a":null,"b":1}`, `{}`, `[]` | `{"b":1}`, `{}`, `[]` |

#### 3.2 Hash and signature encoding

| ID | In | Out |
|---|---|---|
| HA-1 | SHA-256 of "" | hex `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`; b64url `47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU` (43 chars) |
| HA-2 | SHA-256 of "abc" | `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad` |
| HA-3 | SHA-256 of `{}` | `44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a` |
| DER-1 | DER `30 06 02 01 01 02 01 02` | raw 64 B = 31×`00`,`01` ‖ 31×`00`,`02` |
| DER-2 | DER `30 08 02 02 00 80 02 02 00 81` | raw = 31×`00`,`80` ‖ 31×`00`,`81` |
| DER-3 | raw r=0 or s=0 | reject (`INVALID_SIGNATURE`) |

#### 3.3 Median and imputation (read-time)

Rule: only FULL fresh scans are aggregated (PARTIAL scans are stored but excluded); even n gives the mean of the middle two; 1 dp.

| ID | In | Out |
|---|---|---|
| MD-1 | [−60,−62,−61,−65] | −61.5 |
| MD-2 | [−60,−62] / [−70] | −61.0 / −70.0 |
| IM-1 | universe {A,B,C}; point has A(n=2, −61.0), B(n=1, −80.0), C absent | C = −95.0 imputed; B is not imputed; SSID-best = −61.0 |

#### 3.4 Sweep classifier, warm-up, freshness [K8]

Rule: FULL iff `5·|sweepFreqs ∩ ref| ≥ 3·|ref|`.

| ID | In | Out |
|---|---|---|
| SW-1 | warm-up scans s1={2412,2437,2462,5180,5200}, s2={2412,2437,5180,5200,5220,5745}, s3={2412,2462,5180,5745,5765} | ref = [2412,2437,2462,5180,5200,5745] (counts ≥2) |
| SW-2 | ref of size 10; sweep with 6 in-ref freqs / sweep with 5 in-ref + 5 off-ref | FULL (30≥30) / PARTIAL (25<30) |
| SW-3 | ref from SW-1 (size 6, needs ≥4); sweep {2412,2437,5180,5745} / {2412,2437,5180,5220} | FULL (20≥18) / PARTIAL (15<18) |
| FR-1 | tapMono = 100_000_000 µs; result ts 99_999_999 / 100_000_000 | stale / fresh (`≥`) |

#### 3.5 Latency statistics

Rules: p95 rank = `(95·N + 99) / 100` integer division; jitter per K7; loss pooled as `Σlost / Σsent` using integers.

| ID | In | Out |
|---|---|---|
| P95-1 | [12,15,11,40,13,14,16,12,18,90] | sorted [11,12,12,13,14,15,16,18,40,90]; rank 10; **90** |
| P95-2 | 1..19 and 100 (N=20) | rank 19; **19** |
| P95-3 | 1..21 | rank 20; **20** |
| P95-4 | N=1 / N=0 | that value / null |
| JI-1 | [10,14,11,30,12] | diffs 4,3,19,18; **11.000** |
| JI-2 | [10, lost, 14, 11, 30] | received 10,14,11,30; diffs 4,3,19; 26/3 = **8.667** [K7] |
| JI-3 | one received | null |
| LO-1 | 3 lost of 300 sent | pooled 1.0% (`3·100 ≤ 1.0·300` is MET) |

#### 3.6 Arc-length interpolation (PATH)

Polyline (0,0)→(10,0)→(10,5), tStart=0, tEnd=30 s, length 15 m.

| ID | In | Out |
|---|---|---|
| AI-1 | t=10 s / t=20 s / t=25 s | (5,0) / (10,0) exactly / (10,2.5) |
| AI-2 | t=−1 s / t=31 s | (0,0) / (10,5) (clamped) |
| AI-3 | anchor at vertex 1 at t=24 s; t=12 / t=24 / t=27 | (5,0) / (10,0) / (10,2.5) (piecewise per leg) |
| AI-4 | speed gate for AI-3 | legs 10/24 = 0.417 and 5/6 = 0.833 m/s, both OK |
| AI-5 | 5 m route in 30 s | 0.167 m/s, below 0.3, so CONFIRM_PATH |

#### 3.7 PATH bin and motion trim [K14]

| ID | In | Out |
|---|---|---|
| PB-1 | arc s = 0.0, 0.5, 1.4, 1.5, 2.9, 3.0 m; RSSI −60, −62, −64, −66, −70, −72 | bins 0,0,0,1,1,2; medians −62.0, −68.0, −72.0 |
| MT-1 | 1 s window variances [0.01,0.02,0.30,0.40,0.35,0.01] with threshold 0.05 | onset at window 2, so trimStart = 2.0 s (3 consecutive windows above threshold). Stop uses the symmetric rule. |

#### 3.8 Wall crossing kernel [K6]

Rule: for each chain, take the sign σ of `orient(P,Q,c_i)` for each vertex. Drop σ=0 vertices from the sequence. Each σ change between consecutive non-zero vertices is one crossing. The crossing point must lie within PQ at `ε < t < 1−ε` (ε = 1e-9); an exact touch at an endpoint does not count. Then dedupe across chains within <0.05 m, keeping the max penalty. P=(0,0), Q=(4,0).

| ID | Wall chain(s) | Crossings / ΣP |
|---|---|---|
| WL-1 proper | (2,−1)-(2,1) DRYWALL | 1 / 3.0 |
| WL-2 endpoint touch | (2,0)-(2,1) | 0 / 0 |
| WL-3 vertex pass-through | (2,−1),(2,0),(2,1) BRICK | 1 / 8.0 (σ −,0,+ is one change) |
| WL-4 graze | (2,1),(2,0),(3,1) | 0 (σ +,0,+) |
| WL-5 collinear | (1,0)-(3,0) | 0 |
| WL-6 dedupe | A DRYWALL at x=2.00, B GLASS at x=2.04 | 1 / 3.0 (max) |
| WL-7 no dedupe | A at x=2.00, B at x=2.06 | 2 / 5.5 |
| WL-8 near wall | P=(1.9,0), wall x=2 | 1 crossing; `nearWall` chip true (distance 0.1 ≤ 0.15) |
| WL-9 outside range | wall at x=5 | 0 |
| WL-10 sample on wall | P=(2,0), wall (2,−1)-(2,1) | 0 (t=0, not > ε) |

#### 3.9 Grid and boundary [K2]

| ID | In | Out |
|---|---|---|
| GR-1 | bbox 40×25 m | √(1000/25000) = 0.2, so g=0.5; 80×50 = 4000 cells |
| GR-2 | bbox 200×100 m | g0 = 0.894427 gives 224×112 = 25088 > 25000. g=round6(0.894427·1.01) = 0.903371 gives 222×111 = **24642**. |
| GR-3 | rect polygon (0,0),(2.25,0),(2.25,2),(0,2); g=0.5 | cols=ceil(4.5)=5, rows=4; cell (4,·) centers at x=2.25 lie on the edge, so inside; N=20 (edge-inclusive) |
| GR-4 | square 0..10; points (10,5) / (10,10) / (10.0000001,5) | inside / inside / outside |

#### 3.10 IDW [K5]

Rules: p=2, k=12, candidates `d_eff ≤ R_eff`, ties by sample id, `d<1e-6` returns the sample value, weight `q/d_eff²`.

Radius: R = clamp(2 × median Euclidean nearest-neighbor distance, 4, 25), and R = 25 when there are fewer than 2 samples.

Samples: S1 (0,0) −50 q=1; S2 (10,0) −70 q=1; S3 (0,10) −60 q=0.5; R_eff=10; query (4,0).

| ID | Case | Out |
|---|---|---|
| ID-1 | no walls; S3 at 10.77 m is excluded | (9·−50 + 4·−70)/13 = −730/13 = **−56.1538** |
| ID-2 | S2 has q=0.5 | (9·−50 + 2·−70)/11 = −590/11 = **−53.6364** |
| ID-3 | DRYWALL (P=3) at x=2 between S1 and the query; d_eff1 = 7, d_eff2 = 6 | (36·−50 + 49·−70)/85 = −5230/85 = **−61.5294** |
| ID-4 | only S1; CONCRETE (P=15) at x=2; d_eff = 19 | masked (null), although the Euclidean distance is 4 |
| ID-5 | query (10,0), or within 1e-7 of S2 | exactly −70.0 |
| RR-1 | samples (0,0),(6,0),(6,8) | NN = 6,6,8, median 6, R=12; R_eff = 10 (OFFICE) or 12 (SMART_HOME) |
| RR-2 | 2 samples at 1 m / at 20 m / one sample | R = 4 / 25 / 25 |
| RR-3 | NN = [3,3,5,9] | median 4, R = 8 |

#### 3.11 Palette and legends [K10]

Traffic-light anchors (dBm → hex): −95 `#D73027`, −85 `#FC8D59`, −75 `#FEE08B`, −67 `#91CF60`, −55 `#1A9850`. Interpolation is piecewise-linear in sRGB with HALF_EVEN per channel, and values are clamped outside the range.

| ID | In | Out |
|---|---|---|
| PL-1 | −100 / −55 / −75 | `#D73027` / `#1A9850` / `#FEE08B` |
| PL-2 | −71 (t=0.5) | (199.5, 215.5, 117.5) → (200,216,118) = `#C8D876` |
| PL-3 | −60 (t=7/12) | (75.58, 174.92, 86.67) → (76,175,87) = `#4CAF57` |
| PL-4 | viridis at −71 | (63,172,120) = `#3FAC78` |
| PL-5 | glyph text on `#FEE08B` / `#D73027` | black / white (higher contrast ratio) |

Non-RSSI legend boundaries are frozen as listed in the Part A summary.

#### 3.12 Secondary overlap

| ID | In | Out |
|---|---|---|
| SO-1 | groups G1{−60,−62}, G2{−71}, G3{−80} | group maxes −60, −71, −80; **second = −71** |
| SO-2 | only G1 heard | −95 (imputed) |

#### 3.13 Sticky detection [K13]

A sweep qualifies iff the connected BSSID is present, its scan RSSI is < −75, and there is a candidate with the same SSID, same band, different ApGroup, RSSI ≥ −70 and RSSI ≥ connected+8. The candidate must be from the same receiver.

| ID | Connected / candidate | Out |
|---|---|---|
| ST-1 | −78 / −66 | qualifies |
| ST-2 | −76 / −70 | does not qualify (−70 ≥ −68 fails; the +8 clause binds) |
| ST-3 | −75 / −60 | does not qualify (−75 is not < −75) |
| ST-4 | −80 / −70 | qualifies (both thresholds inclusive) |
| ST-5 | candidate in the same ApGroup | does not qualify |
| ST-6 | candidate on 5 GHz, connected on 2.4 GHz | does not qualify |
| ST-7 | connected BSSID absent from the sweep | does not qualify (P9) |
| ST-8 | different SSID | does not qualify |
| SE-1 | sweeps at t=0,4,8,12,16,20 s as Q,Q,N,Q,N,N | opens at t=4 with start 0; N at t=8 then Q at t=12 resets the counter; closes at t=20 (reason NON_QUALIFYING), end=12, **duration 12.0 s** |
| SE-2 | Q,Q,Q at t=0,4,8, roam at t=9.5 | event start 0, end 9.5, ttr=9.5, reason ROAM |
| SE-3 | Q at t=0, roam at t=2 | no event (never opened) |
| PP-1 | A→B at 100.0 s, B→A at 108.0 / 110.0 / 111.0 | ping-pong / ping-pong (inclusive) / no |

#### 3.14 Verdict engine [K3, K4]

Order: INDICATIVE → NOT_ELIGIBLE; else any required NOT_EVALUATED or masked >5% (`masked·100 > 5·N`) → INCOMPLETE; else any required NOT MET → FAIL; else PASS. Comparisons use integer cross-multiplication.

Profile OFFICE_STANDARD (coverage ≥ −67 over ≥90%, p95 ≤ 50, mean jitter ≤ 15, pooled loss ≤ 1.0%, per-point loss < 40%).

| ID | Inputs | Out |
|---|---|---|
| VD-1 | N=200, M=185; p95=48.0; jitter 15.0; lost 3/300; worst point 2/5 | coverage 92.5 MET; p95 MET; jitter MET (≤); loss 300 ≤ 300 MET; per-point `200 < 200` false, NOT MET → **FAIL**. `c = [925,−1,480,150,10,400,−1,0]` |
| VD-2 | VD-1 plus masked = 11 of 200 | 1100 > 1000 → **INCOMPLETE** (masked = 10 gives 1000 > 1000 false → FAIL) |
| VD-3 | N=200, M=180 | `18000 ≥ 18000` MET; with all other criteria met → **PASS** |
| VD-4 | N=2000, M=1799 | `179900 < 180000` → NOT MET. Display (K4) "89.9", `c` coverage = 899 |
| VD-5 | evidenceGrade INDICATIVE | **NOT_ELIGIBLE** (QR `vd:"N"`) |
| VD-6 | WAREHOUSE pooled loss ≤ 0.2%: 1 lost of 500 / 1 lost of 495 | MET (`1000 ≤ 1000`) / NOT MET |
| VD-7 | CCTV pin medians A=−64, B=−70, threshold −65 | pin best −64, MET; a pin whose best is −66 fails the criterion |

#### 3.15 QR payload size (re-verified after J5) [K11]

Worst case, with 4-digit `c` values:

| Part | Bytes |
|---|---|
| `{` + `}` | 2 |
| `"v":1,` | 6 |
| `"sid":"…",` (22) | 31 |
| `"ts":…,` (13 digits) | 19 |
| `"eh":"…",` + `"ah":"…",` (43 each) | 102 |
| `"pf","ip","vd"` | 29 |
| `"c":[8 ints],` | 46 |
| `"pk":"…",` (44) | 52 |
| `"sg":"…"` (86) | 93 |
| **Total** | **380** |

At EC level M in byte mode, my reading of the ISO 18004 table is V15 = 412 B and V16 = 450 B. If that is right, the payload fits V15 with 32 B spare and V16 with 70 B spare. That also means the earlier note "V14-M holds ~330 B" was off by one version (330 B is about V13). I cannot machine-check the table, so the M11 test asserts using the QR library's own encoder: payload ≤ 384 B and chosen version ≤ 16. The print-size scan test at 5 cm is also a required test.

#### 3.16 Scale calibration

| ID | In | Out |
|---|---|---|
| SC-1 | LINE (100,100)→(700,500) px, real 12.0 m, image 2000×1000 | L_px = √520000 = 721.110; mpp ≈ 0.016641; uncertainty = 100·√2·3/721.110 + 5 = **5.588%**; span ≥ 15% of 2000 px (300 px) ✓ |
| SC-2 | ROOM_WIDTH: image width 2000 px = 40 m | mpp = 0.02; uncertainty = 0.212 + 15 = 15.212% |

#### 3.17 Entitlements

Pure decision table: `(tier, feature) → ALLOW | LOCK(tier)`, matching the Part A tier matrix. Test: every cell in the matrix is a parameterized case.

### 3.18 M13b contracts

```java
interface AuthSessionGateway {            // :app, com.netfieldpro.auth, no android.* types
  boolean hasActiveSession();             // sync read of the cached user
  String  getCurrentUserEmail();          // null if none
  void signIn(String email, String pw, AuthCallback cb);
  void register(String email, String pw, AuthCallback cb);
  void sendPasswordReset(String email, AuthCallback cb);   // K17
  void signOut();
}
interface AuthCallback { void onSuccess(); void onError(AuthError e); }
enum AuthError { INVALID_CREDENTIALS, EMAIL_IN_USE, WEAK_PASSWORD, INVALID_EMAIL, NETWORK, TOO_MANY_REQUESTS, UNKNOWN }
```

`FirebaseAuthGatewayImpl` is the only class that imports Firebase (`src/gated`). `NoOpAuthGatewayImpl` (`src/open`) always returns `hasActiveSession() == true`.

**StartRouter** (pure decision table):

| Intent | hasActiveSession | firstLaunchDone | Destination |
|---|---|---|---|
| VIEW or SEND of .pdf/.zip | any | any | **S17** (bypasses S00) |
| MAIN | false | any | S00 |
| MAIN | true | false | S01 |
| MAIN | true | true | S02 |
| S00 `[Just verify a report]` | n/a | n/a | S17; Back returns to S00 |
| sign-in success | n/a | false / true | S01 / S02 |

**Tests:** 7 router cases above; NoOp gateway never routes to S00; sign-out is rejected while SM-1 is ACTIVE; no Firebase symbol appears in the `open` flavor APK (dex scan); `:core` has no dependency on `:app`. The Firebase prefs are excluded from backup.

---

## §4. Constants registry

IDs are `K_*` in `Constants.java`. Items marked *prov.* are provisional and are tuned only by recorded traces in M4/M8b; changing them requires a new `algoVersion`.

| Group | Constants |
|---|---|
| Capture | K=2 FULL; timeout 15000 ms; warm-up 3 scans; warm-up fail 20000 ms; ref-set min scans 2 of 3; FULL ratio 3/5; impute −95; target BSSID cap 60; timestamp plausibility *prov.*: future tolerance 1 s, past max 30 s |
| Latency | probes 5; spacing 200 ms; TCP timeout 1000 ms; p95 rank formula; ports 443→80→53 |
| IDW/grid | p=2; k=12; R clamp 4–25 m; coincident 1e-6 m; w_TAP 1.0, w_PATH 0.5; g min 0.5 m; max cells 25000; g step ×1.01; g decimals 6; PATH bin 1.5 m |
| Walls | dedupe 0.05 m; near-wall chip 0.15 m; ε 1e-9; materials as in Part A |
| PATH | speed gate 0.3–2.5 m/s; countdown 0/2/3 s default 2; accel 50 Hz, window 1 s, 3 windows, variance threshold *prov.* 0.05 (m/s²)² |
| Sticky | connected < −75; candidate ≥ −70; delta ≥ 8; 2 qualifying to open; 2 non-qualifying to close; ping-pong ≤ 10.0 s; roam window ±3 s |
| Verdict | masked > 5% (integer rule); hold 1.5 s; profile thresholds as in Part A |
| QR | max 384 B; version ≤ 16; EC M; print ≥ 5 cm |
| Limits | 20,000 m²; 300 TAP; 1,500 scans; 300k RawSample; 20k link samples; 200 wall chains; IDW layer ≤ 1,200 ms; PDF ≤ 4.0 s and ≤ 10 MB |
| Scale | ε 3 px; fixed 5% (LINE) / 15% (ROOM_WIDTH); min span 15% of the longer side |
| Palette | traffic-light and viridis anchors; contour styles; active-profile contour cobalt / magenta [K10] |
| Auth (M13b) | password min length 6; first sign-in needs network |

---

## §5. Traceability matrix

| Module (:core unless noted) | Signature (summary) | Screens | Milestone | Tests |
|---|---|---|---|---|
| CanonicalJson | `byte[] encode(JsonValue)` | S13, S15, S16, S17 | M1 | CJ-1..6 |
| Hashing, Base64Url, EcdsaCodec | `sha256(InputStream)`; `derToRaw/rawToDer` | S13, S17 | M1, M11 | HA-1..3, DER-1..3 |
| Affine3x3 / ScaleCalculator | `fromTwoPoints(...)`; `uncertaintyPct(...)` | S04, S05 | M0, M6 | SC-1..2 |
| EntitlementPolicy | `decide(tier, feature)` | S18, all lock badges | M2 | full matrix |
| SweepClassifier | `learnRef(scans)`; `classify(freqs, ref)` | S06 | M4, M7s | SW-1..3, FR-1 |
| Median / Imputer | `median(int[])`; `impute(universe, rows)` | S08, S09 | M8a, M9 | MD-1..2, IM-1 |
| CaptureEngine (SM-2) | `onEvent(CaptureEvent)` | S07, S08 | M8a | transition table, fresh/timeout cases |
| LatencyStats | `p95(int[])`; `jitter(rtts)`; `pooledLoss(...)` | S06, S07, S16 | M5, M11a | P95-1..4, JI-1..3, LO-1 |
| PathInterpolator, PathBinner, MotionTrim | `position(t)`; `bin(s)`; `onset(var[])` | S07 | M8b, M7b | AI-1..5, PB-1, MT-1 |
| WallGeometry | `crossings(P,Q,walls)` | S10, S09 | M9, M10b | WL-1..10 |
| GridBuilder | `build(bbox)`; `inside(poly,p)` | S09, S13 | M9 | GR-1..4 |
| IdwEngine | `interpolate(grid,samples,walls,R_eff)` | S07, S09 | M9, M10 | ID-1..5, RR-1..3 |
| Palette / Legends | `color(dbm, palette)` | S09, S16 | M9 | PL-1..5 |
| SecondaryOverlap | `second(groupMaxes)` | S09 | M9 | SO-1..2 |
| StickyDetector, RoamMetrics | `onSweep(...)`; `onRoam(...)` | S12 | M16 | ST-1..8, SE-1..3, PP-1 |
| VerdictEngine | `evaluate(profile, metrics, grade)` | S13, S15, S21 | M11a | VD-1..7 |
| QrPayloadBuilder, QrSigner | `core(...)`; `sign(core)` | S15, S16, S17 | M11, M12 | §3.15 size test, K1 tamper tests |
| StartRouter, AuthSessionGateway (`:app`) | table in §3.18 | S00, S01, S02, S17, S19 | M0 hook, M13b | router cases, flavor dex scan |

---

## §6. What I need from you

1. **Reply "accept all K1–K17" or list overrides.** K3 (verdict precedence), K4 (conservative display rounding) and K9 (pin quality) are the three that change product behavior, so please read them closely.
2. **Confirm the M13b extras (K15–K17):** password reset added to the gateway, the `gated`/`open` flavor split, and the U4 amendment.
3. **Confirm the vector re-verification rule:** M1 re-verifies every vector with an independent tool, and any mismatch comes back as an audit item.

After your answers I'll patch only the tagged vectors and re-issue the changed sections. If the changes introduce no new blocking conflict, that is the greenlight for M0/M1. I will not write any milestone text until you say go.