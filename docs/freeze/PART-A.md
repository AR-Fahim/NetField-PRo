# NetField Pro: Freeze Pack, Part A (Product & UX)

**Status:** Part A is complete. Drawing the screens exposed **10 amendments (J1–J10)**. None invalidates a locked decision, but each changes a screen or a schema field, so I need your acknowledgment before Part B contracts rely on them. They are listed in §A8, with defaults.

**Contents:** A1 design system · A2 screen inventory and navigation · A3 the 21 screens · A4 tier gating matrix · A5 the PDF report (the deliverable) · A6 three storyboards · A7 product build-up per milestone · A8 amendments and sign-off.

---

## A1. Design System

### A1.1 Color tokens

| Token | Light | Dark | Use |
|---|---|---|---|
| `primary` | `#036434` | `#6FD39A` | Brand, primary buttons, headers |
| `onPrimary` | `#FFFFFF` | `#00391C` | Text on primary |
| `primaryContainer` | `#D6EFE0` | `#03432A` | Selected chips, soft fills |
| `background` | `#F6F8F7` | `#0F1412` | Screen background |
| `surface` | `#FFFFFF` | `#171D1A` | Cards, sheets |
| `onSurface` | `#0F1412` | `#E6ECE9` | Primary text |
| `onSurfaceVariant` | `#4A5651` | `#A9B5AF` | Secondary text |
| `outline` | `#C9D1CD` | `#3C4742` | Dividers, card borders |
| `error` / `warning` / `info` | `#B3261E` / `#8A5A00` / `#1F5FA8` | `#F2B8B5` / `#F0C36D` / `#9CC4F0` | Status text and icons |
| `profileContour` | `#D6007F` | `#FF5CB4` | Active-profile target contour |

Contrast rule: all text/background pairs must be ≥ 4.5:1 and icons ≥ 3:1. `#036434` on white is about 7.3:1.

### A1.2 Heatmap palette (default: CVD-safe, viridis anchors)

The ramp is piecewise-linear in sRGB between anchors, with channels rounded. Higher is better.

| Anchor (dBm) | Color | Grade starting at this anchor |
|---|---|---|
| ≤ −95 | `#440154` | Unusable (below −85) |
| −85 | `#3B528B` | Poor (−85 … −75) |
| −75 | `#21908C` | Fair (−75 … −67) |
| −67 | `#5DC863` | Good (−67 … −55) |
| ≥ −55 | `#FDE725` | Excellent (≥ −55) |

The optional traffic-light palette (Settings) is `#D73027 / #FC8D59 / #FEE08B / #91CF60 / #1A9850`, from Unusable to Excellent.

**Never color alone:**
1. **Contours.** Each grade boundary has its own line style:

| Boundary | Style |
|---|---|
| −55 | solid 1.5 dp |
| −67 | dash 8/4 |
| −75 | dash 4/4 |
| −85 | dot 1/4 |

   Each contour carries inline labels such as "−67", spaced ≥ 120 dp apart.
2. **Pins.** Pins show a grade glyph (E/G/F/P/U) at zoom ≥ 0.5 dp/m. A `LOW_CONFIDENCE` pin is hollow. A `MOVED` pin has a dashed ring.
3. **Legend.** The legend always shows the grade names.
4. **Profile contour.** The active-profile threshold is a 3 dp `profileContour` line with a 1 dp white casing and a chip reading "Target −67".
5. **Heat overlay.** Plan under heat renders desaturated at 45% luminance, and heat alpha is 0.65 (0.75 in dark mode).
6. **Gap Finder hatch.** 45° hatching, 6 dp pitch, `onSurface` at 60% alpha. Each gap gets a badge ("Gap 1 · ≈42 m²").

**Provisional legends for non-RSSI layers.** These are frozen in Part B's constants registry. The same ramp applies, with the first-listed bound best.

| Layer | Boundaries (best → worst) |
|---|---|
| Latency (avg RTT, ms) | ≤10 · ≤25 · ≤50 · ≤100 · >100 |
| Jitter (ms) | ≤5 · ≤10 · ≤15 · ≤30 · >30 |
| Loss (lost of 5, **pins only**) | 0 · 1 · ≥2 (3 classes) |
| Serving PHY rate (Mbps) | ≥600 · ≥300 · ≥150 · ≥50 · <50 |

### A1.3 Typography, layout, components

| Item | Spec |
|---|---|
| Font | Roboto (system). BSSIDs use `monospace`. No bundled fonts. |
| Scale (sp) | Display 28/36 · Title 20/28 · Body 16/24 · Label 14/20 · Caption 12/16 · Mono 14/20 |
| Grid | 8 dp; spacing tokens 4/8/16/24/32 |
| Radius | Card 12, chip 8, sheet-top 24 |
| Elevation | Flat. Cards use a 1 dp `outline` border and no shadow. Sheets get a scrim. |
| Touch target | ≥ 48×48 dp. Primary action 56 dp tall. Bottom bar 72 dp. |
| Motion | 150–200 ms fades and slides. Respect "Remove animations". |
| Haptics | Capture complete = `CONFIRM`. Capture failed = `REJECT`. Countdown tick = `CLOCK_TICK`. Anchor = `CONTEXT_CLICK`. |
| Verdict chip | Glyph + text, never color alone: `✓ PASS` · `✕ FAIL` · `◐ INCOMPLETE` · `⊘ NOT ELIGIBLE` |
| Lock badge | `🔒 Pro` or `🔒 Enterprise` on gated controls. Tapping opens S18 with a `feature` argument. |
| Accessibility | TalkBack labels on pins ("Point 14, Good, minus 61 dBm, tap capture"). Non-canvas screens scale to 200% font. |
| Orientation | **Portrait-only in v1** (J6) |

### A1.4 Canonical strings (exact text for UI tests)

| Key | Text |
|---|---|
| `report_title` | Network Performance Audit |
| `watermark` | UNVERIFIED SAMPLE — NOT FOR CONTRACTUAL AUDIT |
| `grade_verdict` | Verdict-grade evidence |
| `grade_indicative` | Indicative only (scan throttling ON) |
| `reassess_label` | Re-assessment (survey intended for {profile}) |
| `verify_l1_ok` | Signature consistent. Signer not independently identified. |
| `verify_l2_ok` | Full verification passed. Evidence and signing key confirmed. |
| `verify_l2_noattest` | Evidence intact. Device attestation unavailable on this phone. |
| `verify_bad` | Verification failed. Do not rely on this report. |
| `free_summary_title` | Sample coverage summary (not a verdict) |
| `interp_note` | Visual interpolation of measured points |
| `roam_disclaimer` | Measures the technician's phone, not your devices. |
| `date_label` | Finalized (device clock): {datetime} |

---

## A2. Screen Inventory & Navigation

### A2.1 Inventory (21 screens)

| ID | Screen | Real implementation in |
|---|---|---|
| S01 | Onboarding & Permissions | M3 |
| S02 | Projects List | M6 (tier badge M2) |
| S03 | Project Detail | M6 |
| S04 | Floor Import | M6 |
| S05 | Scale Calibration (+ check) | M6 |
| S06 | Session Setup + Warm-up | M7s |
| S07 | Survey (TAP, PATH, Walk Mode) | M8a (TAP), M7b (PATH) |
| S08 | Point Detail (bottom sheet) | M8a |
| S09 | Heatmap View (+ Gap Finder entry, verdict preview) | M10 (Gap Finder M10b, preview M11a) |
| S10 | Boundary & Walls Editor | M10b |
| S11 | AP Group Editor | M10c |
| S12 | Roaming Analysis | M16 |
| S13 | Finalize Review | M11 |
| S14 | Assessment List / New Assessment | M11 |
| S15 | Assessment Detail | M11 |
| S16 | Report Builder | M12 |
| S17 | Verify Report | M11 |
| S18 | Paywall | M13 |
| S19 | Settings | M2 (sections added by M3, M4, M5, M12, M13) |
| S20 | Diagnostics | M4 (probe rows by M4, M5, M11) |
| S21 | Custom Profile Editor | M11a |

**M0 creates a placeholder Fragment and a nav-graph destination for all 21 screens**, so every edge below exists from day one. Placeholders show the screen ID and name.

### A2.2 Navigation graph

```
 Launch ─(first run)→ S01 ─(survey path)→ S02      S01 ─(verify path)→ S17
        └(else)────→ S02
 S02 ─→ S03 · S17 · S19 · S18(projects cap) · [New Project dialog]→S03
 S03 ─→ S04→S05→S03 · S06→S07 · S07(resume) · S09 · S14(finalized session) · S13(via S09)
 S06 ─→ S07
 S07 ─→ S08(sheet) · S09 · S12(Roaming Audit ends) · S03(exit, confirm if capturing)
 S09 ─→ S07 · S08 · S10 · S11 · S12 · S13 · S18(locked layers/features)
 S10 ─→ S09     S11 ─→ S09
 S12 ─→ S09(show on map) · S11 · S18(locked)
 S13 ─→ S14 (after finalize, auto-opens S15 of Assessment #1) · S18(Free)
 S14 ─→ S15 · S21 · S18(locked profile)
 S15 ─→ S16 · S09(criterion → highlight)
 S16 ─→ [share sheet] · S18(locked) · S17("Verify this report")
 S19 ─→ S20 · S21 · S18 · S01(re-run setup)
 External: ACTION_VIEW/SEND of .zip or .pdf → S17 (pre-loaded)
```

**Back-stack rules:**
- S02 is the root.
- S18 always returns to its caller with a result (`PURCHASED` or `DISMISSED`).
- S07 has a confirm dialog when a capture or walk is active.
- Leaving S07 does not stop the survey foreground service. The service ends on "End session" or after 10 minutes with no activity and no active capture.

### A2.3 Global UX acceptance criteria (apply to every screen)

| # | Criterion |
|---|---|
| U1 | Cold start to S02 interactive in ≤ 2.0 s on the slowest test device |
| U2 | Every interactive element ≥ 48 dp and has a content description |
| U3 | Every screen has defined loading, empty, error and (if gated) locked states (see §A3) |
| U4 | All screens work offline, except S18's purchase flow |
| U5 | Light and dark mode both pass contrast checks. Edge-to-edge insets are handled. |
| U6 | Process death on any screen restores to that screen. S07 restores to idle, with an in-progress capture discarded and an in-progress walk marked `ABORTED` (P10). |
| U7 | No color-only status anywhere (see A1.2) |
| U8 | Any destructive action has a confirm dialog naming the object. Finalize uses press-and-hold (1.5 s). |
| U9 | Heavy work (IDW, PDF, hashing) is off the UI thread, shows progress, and is cancellable where specified |

---

## A3. The 21 Screens

### S01 Onboarding & Permissions (M3)

```
┌───────────────────────────┐          ┌───────────────────────────┐
│          [logo]           │          │ ← Set up for surveying    │
│  Prove your network.      │          │ ✓ Location permission     │
│  Map signal. Export a     │          │ ✗ Nearby Wi-Fi devices[Grant]
│  signed audit report.     │          │ ✓ Location Services ON    │
│         ● ○ ○             │          │ ✓ Notifications           │
│ [ Set up for surveying ]  │          │ ⚠ Scan throttling: ON     │
│ [ Just verify a report ]  │          │    [How to turn off]      │
└───────────────────────────┘          │ [ Continue ]              │
                                       └───────────────────────────┘
```

- 3 swipeable intro cards ("Map", "Measure", "Prove"), then the fork.
- **No permission prompt appears at first launch (J1).** "Just verify a report" goes straight to S17 and requests nothing.
- Required rows: fine location, Nearby Wi-Fi devices (API 33+), Location Services ON. Optional rows: notifications (API 33+) and throttle status. The matrix per API 30–35 is pinned in Part B (P12).
- **Throttle guide sheet:** 4 numbered steps, an "Open Developer options" button, and a "Developer options not visible on this device" branch (MDM or locked OEM). That branch says "Verdict-grade surveys and Roaming Audit unavailable on this device."
- **Permission states:** permanently denied → row shows "Open app settings".
- **Re-check** every row in `onResume`.
- **Done when** all required rows are ✓, then navigate to S02. The same rows reappear as step 1 of S06.

### S02 Projects List (M6)

```
┌───────────────────────────┐
│ NetField Pro  [✔Verify][⚙]│
│ [ Free ▸ ]                │
│ ┌───────────────────────┐ │
│ │ Acme Dental HQ        │ │
│ │ 1 floor · 2 sessions  │ │
│ │ ◐ Draft    2 h ago    │ │
│ └───────────────────────┘ │
│                           │
│                [＋ New]   │
└───────────────────────────┘
```

- **Card chip** is the latest session state: `Draft`, `Finalized`, or `Indicative`.
- **Tier badge** opens S18.
- **New project dialog:** name (required, 1–60 chars), client, site address, technician.
- **Empty state:** "No projects yet" with a [Create your first project] button.
- **Locked:** a Free user with 1 project sees `＋ New 🔒`, which opens S18 with `feature=projects`.
- **Overflow per card:** Rename, Delete (confirm that names the project).
- **Sorting:** most recently updated first.

### S03 Project Detail (M6)

```
┌───────────────────────────┐
│ ← Acme Dental HQ        ⋮ │
│ Client Acme · 12 Oak St   │
│ FLOORS                    │
│ ┌ Floor 1 · 42×28 m ────┐ │
│ │ [thumb]  Scale ✓ ±1.8%│ │
│ │ ▸ Session 2 ✔ FINAL.  │ │
│ │ ▸ Session 1 ◐ DRAFT   │ │
│ └───────────────────────┘ │
│ [＋ Add floor]            │
│ [ ▶ New survey session ]  │
└───────────────────────────┘
```

- **Session row tap:**
  - DRAFT: opens the heatmap (S09), with a "Resume survey" button there.
  - FINALIZED: opens S14.
- **Session overflow:** Rename, Delete (DRAFT only), Export CSV, Clone as revision (FINALIZED).
- **Floor overflow:** Re-calibrate scale (blocked if any session is FINALIZED, with the reason shown), Delete.
- **Locked:** `＋ Add floor 🔒` when Free has 1 floor. A third session on a floor is blocked with the Free-limit paywall.
- **Empty:** "Add a floor plan to begin."
- **Primary button** is disabled until a floor with a calibrated scale exists, with helper text.

### S04 Floor Import (M6)

```
┌───────────────────────────┐
│ ← Add floor               │
│ Name  [Floor 1         ]  │
│ Level [ 1 ]  (integer)    │
│ ┌ preview ──────────────┐ │
│ │      (plan image)     │ │
│ └───────────────────────┘ │
│ [ Choose image or PDF ]   │
│ PDF page  [−] 1 [+]       │
│ [ Continue → Set scale ]  │
└───────────────────────────┘
```

- **Sources:** system document picker (PNG, JPG, PDF). Camera capture is v1.x.
- **Decode rules:** EXIF rotation honored. Decode is capped at 4096 px on the long side, with a note "Resized to 4096 px". Files > 50 MB are rejected.
- **States:** decoding spinner; error ("Couldn't read this file" for corrupt or unsupported files).
- **Continue** is disabled until a name (1–40 chars), an integer level and a decoded image exist.
- The plan's `planSha256` is computed at save time.

### S05 Scale Calibration (M6)

```
┌───────────────────────────┐
│ ← Set scale      Step 1/2 │
│ ┌ plan (pinch/pan) ─────┐ │
│ │   A●━━━━━━━━━━━●B     │ │
│ └───────────────────────┘ │
│ Distance A–B [ 12.0 ] m   │
│ Line = 38% of image width │
│ ⚠ Short line (<15%)       │
│ [Use room width instead]  │
│ [ Save & check scale ]    │
└───────────────────────────┘
```

- **Step 1 (LINE):** two draggable handles with a magnifier loupe, and a numeric distance input (> 0). The line must span ≥ 15% of the longer image side, or Save is disabled with the warning.
- **ROOM_WIDTH alternative:** "Image width = [ ] m". It is labeled **Approximate** and sets `scaleMethod=ROOM_WIDTH`.
- **Step 2 (check, skippable):** draw a perpendicular line of known length. The app shows "Measured 11.6 m vs entered 12.0 m (3.3%)".
  - A difference > 5% shows "Plan may not be to scale", with [Redo] or [Accept anyway].
  - Skip or accept-anyway sets `scaleChecked=false`, and the report prints "Scale not cross-checked".
- Shows `scaleUncertaintyPct` after save.
- Unit display follows Settings (m or ft). Storage is always meters.

### S06 Session Setup + Warm-up (M7s)

```
┌───────────────────────────┐
│ ← New session    Step 2/3 │
│ ●──●──○                   │
│ Warm-up   ▓▓▓▓▓░  2/3 scans│
│ Choose target network(s): │
│ ☑ AcmeStaff  -52 · Connected
│ ☐ AcmeGuest  -54          │
│ ☐ (hidden)   -71          │
│ ✔ Verdict-grade evidence  │
│ [ Next ]                  │
└───────────────────────────┘
```

**Step 1: Environment.** Rows for permissions, Location Services, scan throttle (live), screen policy, Wi-Fi connected. The throttle row has the same guide sheet as S01.

**Step 2: Warm-up.**
- It runs 3 scans (~12–15 s). These also run the timestamp plausibility test and the link-cadence probe (if not cached per `Build.FINGERPRINT`).
- It builds the reference channel set and lists networks seen, sorted by RSSI, with the connected network tagged.
- The user picks ≥ 1 target SSID (≤ 60 BSSIDs total).

**Step 3: Configure.**
- Session name (default "Session N").
- **Intended profile** (see J8). Locked profiles show badges.
- **Latency method** is auto-selected by the probe waterfall (ICMP → TCP gateway → none) and shows "ICMP → 192.168.1.1 ✓" with [Test again]. It is locked for the session.
- **Session type:** Standard or Roaming Audit `🔒 Enterprise`.
- **Mode suggestion:** if the floor area > 400 m² the screen suggests "Use PATH for corridors and aisles."
- **Evidence-grade banner:** `grade_verdict` or `grade_indicative`. Indicative sessions offer "Continue as Indicative (link-only)", which skips scan warm-up and uses the connected SSID as the sole target.

**Connected-SSID rule (J4):** if the connected SSID is not in the target set, a warning appears: "Latency and serving-AP data will not be evaluated for the target network." The verdict criteria for latency become `NOT_EVALUATED`.

**Start survey** starts the foreground service, `WifiLock` and wake lock, and navigates to S07.

**States:** warm-up failure (0 scans in 20 s) shows "Scans blocked. Turn off throttling or continue as Indicative."

### S07 Survey (M8a, M7b)

**Idle, TAP mode:**
```
┌───────────────────────────────┐
│ ← Floor 1 · Session 2   ◐  ⋮  │
│ [ TAP | PATH ]   Heat overlay ○│
├───────────────────────────────┤
│                               │
│     [ floor plan canvas ]     │
│    ●E  ●G     ◌ (low-conf)    │
│          ◉ ← pin, draggable   │
│                               │
├───────────────────────────────┤
│ LIVE AcmeStaff −61 dBm 5GHz   │
│ a4:5d:… · 12 ms · ch36 · 866M │
│ Scans FULL · next ≈4 s        │
├───────────────────────────────┤
│ 14 pts · ✓ verdict-grade [Heat]│
└───────────────────────────────┘
```

**TAP behavior (restating the accepted E2 design):**
1. Tapping the plan places a pin and **starts capture immediately**. Capture time T0 is the tap's monotonic time.
2. The pin stays **draggable until capture completes**. Dragging never restarts capture.
3. The ring shows "Sweep 1/2 ✓ · 2/2 …", then "Latency…" (~1.2 s on an idle radio), then "Done ✓" with the grade glyph and a `CONFIRM` haptic.
4. Taps elsewhere during capture are ignored, with a snackbar "Finish current point". An ✕ on the ring cancels and discards.
5. The timeout outcomes follow the accepted table:
   - ≥ 2 sweeps → `OK`.
   - 1 sweep → `LOW_CONFIDENCE` (hollow pin).
   - 0 sweeps → discarded, with "Retry / check Developer Options".
6. Device movement during capture sets `MOVED` (dashed ring) and shows a non-blocking toast.
7. A zoom warning chip "1 dp > 0.5 m, zoom in for accuracy" appears when the scale is too coarse.
8. Tapping an existing pin opens S08.
9. In Indicative (link-only) sessions, TAP completes on link samples instead of scans. The exact count is defined in Part B.

**PATH mode, idle (route drawing):**
```
│ [ TAP | PATH ]  Live corner mode ○ │
│  canvas: route ●━━●━━●  (vertices)  │
│  [Undo vertex] [Clear]              │
│ bottom: Length 34 m   [ ▶ Start walk ]
```
- Tapping adds route vertices. Start is enabled with ≥ 2 vertices.
- **Live corner mode** (J-restating G9 refinement 2.2): no pre-drawing. The tech taps the start, walks, and taps each corner while stopped. Each tap appends a vertex and an anchor.
- Countdown is configurable (0/2/3 s, default 2 s) with `CLOCK_TICK` haptics.

**PATH mode, Walk Mode (touch-shielded, J2):**
```
┌───────────────────────────────┐
│ ● WALKING     00:23   18.2 m  │
│ ┌ route ────────────────────┐ │
│ │ ──●━━━◉─────○─────○       │ │
│ └───────────────────────────┘ │
│ Anchor 2 of 4                 │
│ Vol ▲ = anchor · Vol ▼ = undo │
│ LIVE −63 dBm · scans 5        │
│ [ slide to end walk  ▶▶▶ ]    │
└───────────────────────────────┘
```
- Screen stays on and **all touches are ignored** except the slide-to-end control. This prevents pocket taps.
- Volume Up records an anchor at the next route vertex. Volume Down undoes the last anchor. Both are consumed so media volume doesn't change.
- A roam produces a short vibration and a tick on the route.
- `ACTION_SCREEN_OFF` is logged as a data gap.
- **End summary sheet:**

| Row | Content |
|---|---|
| Length / duration / speed | Speed gate 0.3–2.5 m/s |
| Anchors | n of m |
| Samples | Scans and link samples logged |
| Actions | [Save segment] [Discard]. Out-of-gate speed or anchor mismatch shows "Confirm path". |

**Roaming Audit sessions:** a `ROAMING` pill, a roam-event ticker, and forced screen-on. Ending the audit opens S12.

**States:**

| State | Behavior |
|---|---|
| Indicative session | Banner `grade_indicative`; TAP/PATH collect link only |
| Wi-Fi disconnected | Live strip shows "Not connected"; captures still run (scans) and latency is N/A |
| Permission revoked mid-session | Blocking dialog → S01 |
| Throttle turned ON mid-session | Sweeps after that are flagged; banner "Throttling ON, data after {time} is indicative" |

### S08 Point Detail (bottom sheet, M8a)

```
│ Point #14 · TAP · 10:42:07 │
│ Quality OK · 2 FULL sweeps │
│ Position 12.4 m , 8.1 m    │
│ BSSID          Band RSSI n min max
│ a4:5d:36:11:… 5G  −61  2 −62 −60
│ a4:5d:36:11:… 2.4 −48  2 −49 −47
│ b8:…  (not heard · −95 imputed) italic
│ LATENCY ICMP→gw  recv 5/5  avg 12 ms  jitter 2.1  loss 0
│ [ Re-capture ]        [ Delete ]
```
- Imputed rows are italic with the "not heard" tag. They are **never** shown as measured numbers.
- **Delete and Re-capture** are DRAFT-only. Re-capture removes the old point and starts a capture at the same position.
- For PATH points, the sheet shows source `PATH`, the interpolated position, the segment, and `±` uncertainty.
- The latency block shows `duringScan` as an "indicative" label for PATH points.

### S09 Heatmap View (M10; Gap Finder M10b; verdict preview M11a)

```
┌───────────────────────────────┐
│ ← Heatmap              Gaps ◐ │
│ Layer [Coverage · best AP ▾]  │
│ Band  [All|2.4|5|6]           │
├───────────────────────────────┤
│  dimmed plan + heat + contours│
│  pins ● · boundary ┄ · walls ▬│
│  "Visual interpolation of     │
│   measured points"            │
├───────────────────────────────┤
│ ▓▓▓▓▓▓▓▓▓▓▓▓▓  −95 … −55 dBm  │
│ Unusable Poor Fair Good Excel.│
│ ┌ Draft preview · Office ───┐ │
│ │ ≥ −67 dBm: 93.2% (≥90%) ✓ │ │
│ │ Masked 2.1%               │ │
│ └───────────────────────────┘ │
│ [ Boundary & walls ] [ APs ]  │
│ [ ▶ Resume survey ]  [Finalize]│
└───────────────────────────────┘
```

- **Layers:**

| Layer | Notes |
|---|---|
| Coverage (SSID best) | Max RSSI across the target SSID's BSSIDs per sample, then IDW |
| Per-BSSID | Picker |
| Secondary overlap | Second-best physical AP |
| Experienced signal (serving AP) | Never counts toward coverage criteria |
| Latency / Jitter / PHY rate | Interpolated |
| Loss | Pins only, never interpolated |

- **Toggles:** pins, contours, profile contour (P11), boundary, walls.
- **Disabled layers** show a reason chip, e.g., "Needs scan data (Indicative session)" or "Probes unreachable".
- **Compute:** IDW runs off-thread. The stale overlay dims, a "Computing… Cancel" chip shows, and the target is ≤ 1.2 s. Switching layers cancels the previous compute.
- **Bottom card:**
  - **Free:** `free_summary_title` with % of **surveyed (unmasked) area** per grade. The denominator is labeled, and no verdict badge appears.
  - **Pro/Enterprise DRAFT:** the verdict preview under the intended profile (see J8), with `INCOMPLETE` or `NOT_EVALUATED` rows shown honestly.
- **Gap Finder:** hatches masked clusters inside the boundary, with the badge "3 gaps · 118 m²". Tapping cycles through them and centers the map. Without a boundary it shows "Draw a boundary to find gaps".
- **FINALIZED session:** read-only. Buttons are replaced by [Assessments].
- **Empty (no points):** "Capture at least 3 points to render a heatmap." The IDW minimum is pinned in Part B.

### S10 Boundary & Walls Editor (M10b)

```
┌───────────────────────────────┐
│ ← Boundary & walls     [Undo] │
│ [Boundary][Wall][Select][Erase]│
├───────────────────────────────┤
│  plan + boundary ┄┄ + walls ▬ │
├───────────────────────────────┤
│ Boundary: [Fit to image · inset 1 m] [Draw polygon]
│ Material: [Drywall][Glass][Brick][Concrete][Metal]
│ Snap ☑ (≤ 0.5 m)      Length 4.2 m
│ [ Done ]                      │
└───────────────────────────────┘
```

- **Fit to image:** a rectangle with a configurable inset (default 1.0 m or 5%, whichever is larger) and draggable corner/edge handles.
- **Draw polygon:** ≥ 3 vertices, area ≥ 1 m², no self-intersection. An invalid polygon shows the error "Boundary edges cross" and Done is disabled.
- **Wall tool:** tap vertices to build a polyline chain, [Finish chain] to end it. A selected chain can change material or be deleted. The limit is 200 chains (error at the limit).
- **Material presets:** the five in the accepted table, each with a glyph and name on the chip.
- **Finalized floor:** read-only banner "Finalized evidence is locked. Clone as revision to edit."
- Live segment lengths use the calibrated scale.

### S11 AP Group Editor (M10c)

```
┌───────────────────────────────┐
│ ← Access points               │
│ Grouping: heuristic           │
│ ▾ AP 1 "Lab ceiling" best −58 │
│    a4:5d:36:11:22:10 5G  AcmeStaff
│    a4:5d:36:11:22:11 2.4G AcmeStaff
│ ▾ AP 2                best −63│
│ ☐ select → [Merge] [Split out]│
│ [ Reset to heuristic ] [Save] │
└───────────────────────────────┘
```
- Target-network BSSIDs only.
- The default grouping is "first 5 octets same, low nibble of the last differs".
- The user can merge, split out, and rename APs. Saving flips the label to "user-edited".
- Saving a grouping on a FINALIZED session is blocked. The screen is read-only there.
- A BSSID can belong to exactly one AP. An empty AP is auto-removed.

### S12 Roaming Analysis (M16)

```
┌───────────────────────────────┐
│ ← Roaming · Session 2         │
│ (banner: prerequisites)       │
│ 7 roams · 1 ping-pong · 2 sticky
│ time-to-roam ±3 s · max RTT@roam 148 ms
│ TIMELINE                      │
│ AP-1 ▇▇▇▇▇▇▁                  │
│ AP-3        ▇▇▇▇▇▇▇           │
│ RSSI ─╲╱──╲___╱──             │
│ EVENTS                        │
│ ⚠ STICKY 10:44:12 · 42 s      │
│   AP-1 −79 vs AP-3 −66 [Map]  │
│ ⇄ ROAM 10:44:54 AP-1→AP-3     │
│   ±3 s · RTT 31 ms            │
│ roam_disclaimer               │
└───────────────────────────────┘
```
- **Banner requirement:** throttling OFF or an external probe (v1.x), plus screen-on. If unmet, it shows "Roaming analysis needs scan throttling OFF" with the guide.
- **Metrics:** roam count, ping-pong roams, time-to-roam (shown as `± cadence`), max RTT and loss within ±3 s of each roam, sticky-event duration and location.
- **Events:** tapping [Map] opens S09 with the route and the event highlighted. Positions of PATH-derived events are labeled "derived".
- **Locked for Pro:** a blurred example with `🔒 Enterprise`, and a button to S18.
- **Empty:** "No roaming data. Run a Roaming Audit session."

### S13 Finalize Review (M11)

```
┌───────────────────────────────┐
│ ← Finalize session            │
│ ✓ Scale calibrated ±1.8%      │
│ ✓ Boundary defined            │
│ ⚠ Masked area 6.3% · 2 gaps [Open Gap Finder]
│ ✓ AP grouping: heuristic [Review]
│ ✓ Evidence: verdict-grade     │
│ Intended profile [Office ▾]   │
│ ℹ Finalizing locks all evidence.
│ [ Hold to finalize ▮▮▮▯ ]     │
└───────────────────────────────┘
```
- **Hard blockers (button disabled):** no boundary, no points, an unfinished path, or Free tier.
- **Soft warnings:** masked > 5%, Indicative grade, or heuristic AP grouping.
- **Free tier:** the footer is replaced by an upsell card ("Finalize & sign with Pro") with [See plans] → S18.
- **On finalize** (hold 1.5 s):
  1. Show progress "Hashing evidence…".
  2. Create the Keystore key on first use.
  3. Write the evidence parts manifest.
  4. Auto-create Assessment #1 under the intended profile.
  5. Open S15.
- **Failure:** any failure rolls the session back to DRAFT with an error sheet and a retry.

### S14 Assessment List (M11)

```
│ ← Assessments                 │
│ Evidence 7c3f…  finalized 10:51
│ ┌ Office Standard · ✓ PASS ──┐│
│ ┌ Re-assessment · CCTV · ✕ FAIL┐
│ [ ＋ New assessment ]         │
```
- "New assessment" is a profile picker sheet: presets, custom profiles, and [Create custom…] → S21.
- Locked profiles show a badge. Picking one opens S18.
- A row for a profile different from the intended one shows `reassess_label`.
- Lapsed users: rows are viewable and shareable, and "New assessment" is locked.

### S15 Assessment Detail (M11)

```
│ ← Assessment                  │
│ ✓ PASS  Office Standard       │
│ Algo v1.0 · Evidence 7c3f…    │
│ ✓ Coverage ≥ −67: 93.2% (≥90%)│
│ ✓ p95 RTT 31 ms (≤ 50)        │
│ ✕ Jitter 17.4 ms (≤ 15)       │
│ ○ Roaming: not evaluated      │
│ Masked 2.1% · grade verdict   │
│ [ Generate report ▸ ]         │
```
- Each criterion row: glyph, name, measured, target, status word. Statuses are `MET`, `NOT MET`, `NOT EVALUATED`, `NOT REQUIRED`.
- Tapping a criterion opens S09 with the relevant layer or failing cells highlighted.
- Verdict rule: any `NOT EVALUATED` among required criteria → `INCOMPLETE`. Masked > 5% → `INCOMPLETE`. Indicative grade → `NOT_ELIGIBLE`.

### S16 Report Builder (M12)

```
┌───────────────────────────────┐
│ ← Report                      │
│ [ P1 ][ P2 ][ P3 ]…  (preview)│
│ Brand color  ● #036434 [Pick] │
│ Logo [Choose]   Company [   ] │
│ Paper [ A4 | Letter ]         │
│ Mask BSSIDs in PDF   ☐        │
│ Exclude other networks from   │
│ package              ☑        │
│ Pages: ☑ floors ☑ latency ☑ AP│
│ [ Share PDF ]  [ Export .zip 🔒]
└───────────────────────────────┘
```
- Preview renders thumbnails (≤ 4 s total generation for 2 floors).
- Free users see the watermark, a locked brand color and logo, no QR, and no `.zip`.
- Exports use `FileProvider` and the share sheet.
- Branding is cosmetic. It is **not** part of the signed evidence.
- **Validation:** the brand color must keep text contrast ≥ 4.5:1 on header bands, and otherwise shows an error.
- **Error:** the PDF size ceiling of 10 MB, with an automatic downscale retry.

### S17 Verify Report (M11)

```
┌───────────────────────────────┐
│ ← Verify report               │
│ [ Scan QR ] [ Open PDF ]      │
│ [ Open package (.zip) ]       │
│ ┌ ✓ Signature consistent ───┐ │
│ │ Verdict PASS · Office     │ │
│ │ Finalized (device clock): …│ │
│ │ Signer not independently  │ │
│ │ identified.               │ │
│ └───────────────────────────┘ │
│ Compare to printed values:    │
│  Coverage 93.2 · p95 31 · …   │
└───────────────────────────────┘
```
- **Three inputs:**
  1. Scan QR (camera permission is requested just-in-time here only).
  2. **Open PDF** renders the integrity page via `PdfRenderer` and decodes the QR (J3). This lets a client who received the PDF on the same phone verify without a second screen.
  3. Open package (.zip) for Level 2.
- **Results:**

| State | Text |
|---|---|
| Level 1 OK | `verify_l1_ok` |
| Level 2 OK | `verify_l2_ok` |
| Level 2, no attestation | `verify_l2_noattest` |
| Failure | `verify_bad`, with the failing check named |

- The "Compare to printed values" block lists the `c` array from the QR, so a visual comparison with the PDF's tables is possible.
- Always free. Works for lapsed and unsubscribed users.
- Also reachable from external `ACTION_VIEW`/`SEND` of `.zip` and `.pdf`.

### S18 Paywall (M13)

```
┌───────────────────────────────┐
│ ✕                             │
│ Finalize & sign reports       │
│ [ Free ] [ Pro ★ ] [ Enterprise ]
│ ✓ Unlimited projects          │
│ ✓ Signed, branded PDF + QR    │
│ ✓ Audit package (.zip)        │
│ Pro  $29 / month              │
│ [ Continue with Pro ]         │
│ Restore purchases · Terms · Privacy
└───────────────────────────────┘
```
- The headline and preselected plan depend on the `feature` argument. Roaming and `WAREHOUSE_ROBOTICS` preselect Enterprise.
- The Integrator tier does not appear until v1.x.
- **States:** prices loading, offline ("Connect to the internet to subscribe"), purchase pending, success (return `PURCHASED` and the caller re-evaluates), cancelled, billing unavailable.
- Monthly billing only in v1.
- A 7-day offline grace applies, with a clock-rollback guard.

### S19 Settings (M2 base)

Sections: **Plan** (tier, manage subscription, restore) · **Appearance** (system/light/dark, palette CVD or traffic-light) · **Units** (m/ft) · **Survey** (PATH countdown 0/2/3 s, RSSI offset dB default 0) · **Latency** (target override, method override) · **Branding** (company, technician, logo, default color) · **Reports** (paper, default masking) · **Profiles** (list → S21) · **Device** (Diagnostics → S20, Recalibrate link cadence, Re-run setup → S01) · **About** (version, licenses, privacy policy).

### S20 Diagnostics (M4, M5, M11)

```
│ ← Diagnostics                 │
│ ✓ Scan timestamps plausible   │
│ ✓ Link cadence 3000 ms (cached)│
│ ⚠ Scan throttle: ON           │
│ ✓ FULL-sweep detection        │
│ ✓ ICMP socket (unprivileged)  │
│ ✓ TCP gateway :443            │
│ ✓ Keystore signing            │
│ ⚠ Key attestation: unsupported│
│ [ Run all ] [ Export CapabilityReport.json ]
```
- Each row shows ✓, ⚠ or ✕, a one-line cause, and the last-run time.
- The exported file has the same schema stored in a session's `capabilityReport`.
- Runs offline.
- A **manual Device Validation Checklist** per milestone accompanies this screen (M4, M5, M11).

### S21 Custom Profile Editor (M11a)

Fields:
- Name (unique, 1–40).
- Each criterion has an enable toggle: RSSI target (dBm, area %), secondary overlap (dBm, area %), p95 RTT (ms), mean jitter (ms), pooled loss (%), per-point loss cap (%), sticky max (`🔒 Enterprise`).
- `bandPolicy`, `maxInfluenceM`.

Rules:
- A disabled criterion renders `NOT REQUIRED` and is **not** `NOT EVALUATED`.
- Ranges are validated inline: RSSI −100…−30, area 1–100%, and so on.
- The profile snapshot is copied into each Assessment, so later edits never alter signed results.

---

## A4. Tier × Feature × Screen Gating Matrix

| Feature | Screen | Free | Pro ($29) | Enterprise ($299) | Locked behavior |
|---|---|---|---|---|---|
| Projects / floors / sessions per floor | S02 S03 | 1 / 1 / 2 | ∞ / ∞ / ∞ | ∞ / ∞ / ∞ | Badge → S18 |
| Live readings, TAP, PATH | S07 | ✓ | ✓ | ✓ | |
| All heat layers, contours, Gap Finder | S09 | ✓ | ✓ | ✓ | |
| Boundary, walls, AP groups | S10 S11 | ✓ | ✓ | ✓ | |
| Sample coverage summary | S09 | ✓ | n/a (preview) | n/a | |
| Verdict preview (draft) | S09 | ✗ | ✓ | ✓ | Not shown |
| CSV export | S03 | ✓ | ✓ | ✓ | |
| Watermarked unsigned PDF | S16 | ✓ | n/a | n/a | |
| Finalize + sign | S13 | ✗ | ✓ | ✓ | Upsell card |
| Clean PDF + brand color/logo + QR | S16 | ✗ | ✓ | ✓ | Locked controls |
| Audit package `.zip` | S16 | ✗ | ✓ | ✓ | `🔒` |
| Presets Office, CCTV, Smart Home | S14 S21 | ✗ | ✓ | ✓ | `🔒` |
| Custom profiles | S21 | ✗ | ✓ (no sticky criterion) | ✓ | `🔒` |
| `WAREHOUSE_ROBOTICS` profile | S14 | ✗ | `🔒` | ✓ | Badge + tooltip |
| Re-assessments | S14 | ✗ | ✓ | ✓ | |
| Roaming Audit session + analysis | S06 S07 S12 | ✗ | ✗ | ✓ | `🔒 Enterprise` |
| **Verify** (QR, PDF, ZIP) | S17 | ✓ | ✓ | ✓ | Never locked |
| Diagnostics | S20 | ✓ | ✓ | ✓ | |

Notes:
- **Lapsed subscribers:** everything finalized stays viewable, verifiable and re-shareable. New finalize, assessments and clean-PDF generation lock.
- **Data capture is never gated.** Link samples and connection events are recorded in every tier, and only analysis UI is gated.

---

## A5. The Deliverable: Network Performance Audit PDF

Every page has a header band in the brand color, a footer with `Page n/N · Session {short id}`, and no searchable-text guarantee. **Free** adds a diagonal `watermark` on every page, has no verdict and no QR, and prints "Unsigned" on the final page.

```
P1 COVER                    P2 EXECUTIVE SUMMARY
┌────────────────────┐      ┌────────────────────┐
│ [logo]             │      │ ✓ PASS             │
│ Network Performance│      │ Office Standard    │
│ Audit              │      │ Criterion|Req|Meas|Status
│ Acme Dental HQ     │      │ Coverage ≥−67 | ≥90% | 93.2% | ✓ MET
│ Client · Site      │      │ p95 RTT  | ≤50 | 31 | ✓ MET
│ Date · Technician  │      │ Jitter   | ≤15 | 17.4 | ✕ NOT MET
│ Device · Android   │      │ Area by grade bar  │
│ [ ✓ PASS ]         │      │ Key findings (3)   │
│ re-assessment label│      └────────────────────┘
└────────────────────┘
P3.. FLOOR + HEATMAP        PN LATENCY & LOSS
┌────────────────────┐      ┌────────────────────┐
│ Floor 1 · 1:200    │      │ Method ICMP→gateway│
│ [plan+heat+pins]   │      │ Pooled p95, jitter,│
│ contours, walls,   │      │ loss, per-point    │
│ boundary, scale bar│      │ table (TAP)        │
│ legend + grades    │      │ PATH = indicative  │
│ interp_note        │      └────────────────────┘
└────────────────────┘
PN ROAMING (Enterprise)     PN ACCESS POINTS
┌────────────────────┐      ┌────────────────────┐
│ Summary + events   │      │ AP | BSSID | Band  │
│ table              │      │ Ch | best RSSI     │
│ roam_disclaimer    │      │ Grouping: heuristic│
└────────────────────┘      │ or user-edited     │
                            └────────────────────┘
PN METHODOLOGY              LAST: INTEGRITY
┌────────────────────┐      ┌────────────────────┐
│ Receiver (device), │      │ [QR ≥ 5 cm vector] │
│ sweep rules, K=2,  │      │ Evidence/Assess.   │
│ imputation, IDW,   │      │ hashes, key fp,    │
│ wall penalties,    │      │ attestation status,│
│ RSSI ≠ throughput, │      │ date_label, app/   │
│ observer effect,   │      │ algo versions      │
│ phone ≠ client     │      │ Verify: in-app or  │
│ device, scale      │      │ sha256sum + openssl│
│ uncertainty        │      └────────────────────┘
└────────────────────┘
```

Constraints (frozen in M12 acceptance): output ≤ 10 MB, images ≤ 200 dpi / 2000 px, vector QR, manual pagination of tables, and A4 or Letter.

---

## A6. Three Storyboards

### SB1: First run → first Pro PDF (Free → Pro funnel)

Persona: Dana, independent IT tech, 320 m² dental office, one Android phone.

| # | Screen | User action | System response / visible result | Time |
|---|---|---|---|---|
| 1 | S01 | Opens app, taps "Set up for surveying" | Permission rows; grants location, nearby Wi-Fi, notifications; throttle row ⚠ | ~90 s |
| 2 | S01 sheet | Follows throttle guide, returns | Row flips to ✓ on `onResume` | ~60 s |
| 3 | S02 → S03 | New project "Acme Dental" | Project card appears; S03 with no floors | 20 s |
| 4 | S04 | Picks the PDF plan, page 1 | Preview ≤ 3 s | 20 s |
| 5 | S05 | Drags A–B across a known 12.0 m wall, enters 12.0 | "±1.8%"; check step passes | 60 s |
| 6 | S06 step 1–2 | Starts new session | Warm-up 3/3; ticks "AcmeStaff"; "✓ Verdict-grade" | ~30 s |
| 7 | S06 step 3 | Sees profile row locked (Free) | Latency "ICMP → gateway ✓"; Start | 15 s |
| 8 | S07 | Taps 18 room points | Each capture ≈ 7 s; hollow pin if timeout | ~6 min |
| 9 | S07 PATH | Draws corridor route, Start, walks, ends | Summary 21 m / 17 s / 1.2 m/s ✓; save | ~2 min |
| 10 | S09 | Opens heatmap | Coverage layer; "Sample coverage summary"; Gap Finder shows 2 gaps | |
| 11 | S07 | Re-surveys gaps | Gaps shrink after recompute | ~4 min |
| 12 | S10 | Fit boundary, draws 6 walls | Heatmap re-renders honoring walls | ~3 min |
| 13 | S13 → S18 | Taps Finalize | Free upsell → paywall (feature=finalize) | |
| 14 | S18 | Buys Pro | Returns to S13, button enabled | 1 min |
| 15 | S13 | Picks Office Standard, holds 1.5 s | "Hashing…", then S15 shows ✓ PASS | ≤ 10 s |
| 16 | S16 | Sets color `#036434`, logo, Share PDF | Share sheet; email sent | 1 min |
| 17 | S17 (client) | Installs free app, "Open PDF" | `verify_l1_ok`; compares printed values | 30 s |

### SB2: Warehouse roaming audit (Enterprise)

Persona: Marco, facility manager, 8,000 m² warehouse with AGVs.

| # | Screen | Action | Result |
|---|---|---|---|
| 1 | S06 step 1 | Throttle ON detected | Verdict-grade blocked. Guide opens. MDM variant: "Roaming Audit unavailable on this device" |
| 2 | S06 step 2–3 | Picks target `WH-AGV`; profile `WAREHOUSE_ROBOTICS` ✓ (Enterprise); type = Roaming Audit | ICMP fails, TCP :443 passes → "TCP → gateway:443 ✓", locked |
| 3 | S07 PATH | Pre-draws a 6-vertex aisle loop; Start; touch shield | Vol ▲ at each vertex; roam vibrations |
| 4 | S07 | Slide to end | Summary: anchors 6/6, speed 1.1 m/s |
| 5 | S07 TAP | 40 aisle-intersection points | Latency per TAP point (needed for verdict) |
| 6 | S12 | Opens analysis | 11 roams, 2 sticky, max RTT@roam 148 ms |
| 7 | S12 → S09 | Taps [Map] on a sticky event | Route and event highlighted |
| 8 | S11 | Merges two misgrouped BSSIDs | "user-edited" |
| 9 | S10 | Fit boundary; racking not drawn as walls; elevator = Metal | |
| 10 | S09 | Verdict preview shows `INCOMPLETE` (masked 7%) | Gap Finder; resurvey (session still DRAFT) |
| 11 | S13 → S15 | Finalize | Verdict `FAIL` (sticky 2 > 0). Criteria list shows exactly why |
| 12 | S16 | Report with roaming page; export `.zip` | Package includes attestation |

### SB3: Re-assessment and lapse

Persona: Dana, 6 weeks later. The client asks, "Is this good enough for CCTV?"

| # | Screen | Action | Result |
|---|---|---|---|
| 1 | S03 → S14 | Opens finalized session | Assessment list |
| 2 | S14 | New assessment → `CCTV_SURVEILLANCE` | Computed from frozen evidence, no re-survey |
| 3 | S15 | Reads result | `✕ FAIL` (pins below −65); labeled `reassess_label` |
| 4 | S16 | Exports PDF | Label on cover; QR carries both `pf` and `ip` (J5) |
| 5 | S17 (client) | Scans QR | Verify shows profile CCTV and "Intended: Office" |
| 6 | (later) | Subscription lapses | Finalized items remain viewable and shareable |
| 7 | S14 | Tries another assessment | `🔒` → S18 |
| 8 | S17 | Verifies an old report | Still works, always free |

---

## A7. What You See After Each Milestone (the product, brick by brick)

| After | On the device |
|---|---|
| **M0** | App launches to S02 placeholder. Debug menu opens all 21 placeholder destinations. A style-guide screen shows tokens, palette ramp and components in light and dark. |
| **M1** | Debug screen creates and lists projects, floors and sessions from the frozen schema. Canonical JSON export of a seeded session passes golden tests. |
| **M2** | S02 shows the tier badge. Settings → Plan has a debug tier switcher. Locked badges navigate to a stub S18. |
| **M3** | S01 fully working: permission rows, throttle guide, `onResume` re-check. |
| **M4** | A live strip (SSID, RSSI, band, BSSID, scan status) in a debug screen. Diagnostics shows timestamp, cadence and throttle rows. |
| **M5** | The live strip adds latency. Diagnostics shows probe rows and the chosen method. |
| **M6** | Create project → add floor → import → calibrate scale → verified. S02/S03/S04/S05 real. |
| **M7a** | Zoom, pan and pin rendering on the canvas (no capture yet). |
| **M7s** | S06 wizard with warm-up and target selection. |
| **M8a** | TAP capture end-to-end: pins, S08 detail, imputed rows, timeouts. |
| **M8b** | PATH engine produces positioned samples (headless plus debug trigger). |
| **M7b** | PATH UI: route drawing, countdown, Walk Mode, anchors, summary. |
| **M9** | IDW, barrier geometry and legend are available in code, with a debug test-render screen. |
| **M10** | S09 real: layers, legend, contours, pins. S07 overlay toggle. Free summary card. |
| **M10b** | S10 boundary and walls. Gap Finder. Heatmap honors walls. |
| **M10c** | S11 AP grouping. |
| **M16** | S12 roaming analysis (Enterprise-gated). |
| **M11a** | Verdict engine, verdict preview on S09, S21 profile editor. |
| **M11** | S13 finalize, signing, S14/S15 assessments, S17 verify (QR, PDF, ZIP). |
| **M12** | S16 report builder and the PDF (§A5). |
| **M13** | S18 real purchases, enforcement of every gate, restore, grace, lapse behavior. |

---

## A8. Amendments Found While Drawing Part A (J1–J10)

None blocks Part B. Each has a default, and I recommend accepting all.

| ID | Finding | Default |
|---|---|---|
| **J1** | Onboarding asked for permissions first, but H9 says clients verify for free. A client who only wants to verify would be blocked by location prompts. | No prompts at first launch. S01 forks into "Set up for surveying" and "Just verify a report". Permissions are requested just-in-time and again in S06 step 1. |
| **J2** | Volume-key anchors need the screen on and the phone pocketed, which causes accidental canvas taps. | **Walk Mode touch shield** (S07). Vol ▲ = anchor, Vol ▼ = undo, slide-to-end is the only touch target. Part B adds the anchor-mismatch rules. |
| **J3** | A client receiving the PDF on the same phone can't scan its QR with that phone. | S17 **Open PDF** renders the integrity page with `PdfRenderer` and decodes the QR. The camera permission is only requested for "Scan QR". |
| **J4** | The latency probe and serving-AP data describe the *connected* network. If the target SSID isn't the connected one, they don't describe the target. `LinkSample` and `LatencyResult` also lack SSID and BSSID in C9. | Schema adds `ssid` to `LinkSample`, and `connectedBssid` and `ssid` to `LatencyResult`. If the connected SSID is not in the target set, latency criteria are `NOT_EVALUATED` and link layers are labeled off-target. |
| **J5** | K7 says the QR shows a `pf` mismatch for re-assessments, but the H2 payload has no intended-profile field. | Add `ip` (2-char intended-profile code). The payload grows ~9 B to ≈384 B. Part B re-verifies the QR version capacity. |
| **J6** | Rotation during capture or walk adds state-restoration risk. | Portrait-only in v1. Landscape and tablet layouts are v1.x. |
| **J7** | Free allowances were not enumerated. | Free gets all layers, boundary, walls, AP groups and CSV. Free's sample summary denominator is **unmasked surveyed cells**, labeled as such, and carries no verdict. |
| **J8** | `intendedProfileId` is "recorded at setup", but Free users can't see profiles, and users may change their minds before finalizing. | Editable while DRAFT, **frozen at finalize**, and the number of changes is stored in the Evidence. Free sessions set it at finalize. A Free user has never seen a verdict preview, so this can't be shopped. |
| **J9** | Clients on iOS or desktop can't verify Level 1, because the Verify screen is Android-only. | **Defer** a static web verifier to v1.x. The raw 64-byte r‖s signature and base64url fields in the QR are WebCrypto-compatible, so the payload stays ready. The PDF footer lists the `sha256sum` + `openssl` path for Level 2. |
| **J10** | The Survey screen's heat overlay toggle, exports' privacy default and non-RSSI legends were unspecified. | Overlay is added in M10 and recomputes after each completed point (debounced). The export default **excludes non-target networks**, which is safe because the manifest keeps hashes. The legends in A1.2 are provisional until Part B's constants registry. |

---
