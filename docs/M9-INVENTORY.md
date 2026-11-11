# M9 Inventory

## 1. Shape of `tools/vectors.json` Entries (Field Names)

- **MD (Median):**
  - `MD-1`: number (`-61.5`)
  - `MD-2`: `{"v1": -61.0, "v2": -70.0}`
- **IM (Imputer):**
  - `IM-1`: `{"imputed_C": -95.0, "not_imputed_B": false, "ssid_best": -61.0}`
- **WL (Wall Kernel):**
  - `WL-1`..`WL-7`, `WL-9`, `WL-10`: `{"crossings": int, "penalty": double}`
  - `WL-8`: `{"crossings": int, "nearWall": boolean}`
- **GR (Grid & Boundary):**
  - `GR-1`: `{"g": 0.5, "cols": 80, "rows": 50, "cells": 4000}`
  - `GR-2`: `{"g": 0.903371, "cols": 222, "rows": 111, "cells": 24642}`
  - `GR-3`: `{"cols": 5, "rows": 4, "cells": 20}`
  - `GR-4`: `{"p1": "inside", "p2": "inside", "p3": "outside"}`
- **ID (IDW):**
  - `ID-1`: number (`-56.1538`)
  - `ID-2`: number (`-53.6364`)
  - `ID-3`: number (`-61.5294`)
  - `ID-4`: `null` (masked)
  - `ID-5`: number (`-70.0`)
- **RR (Radius Policy):**
  - `RR-1`: `{"NN": [6, 6, 8], "median": 6, "R": 12, "R_eff_office": 10, "R_eff_smart_home": 12}`
  - `RR-2`: `{"R_1m": 4, "R_20m": 25, "R_1sample": 25}`
  - `RR-3`: `{"median": 4, "R": 8}`
- **PL (Palette):**
  - `PL-1`: `{"c_neg100": "#D73027", "c_neg55": "#1A9850", "c_neg75": "#FEE08B"}`
  - `PL-2`: `{"rgb": [200, 216, 118], "hex": "#C8D876"}`
  - `PL-3`: `{"rgb": [76, 175, 87], "hex": "#4CAF57"}`
  - `PL-4`: `{"rgb": [63, 172, 120], "hex": "#3FAC78"}`
  - `PL-5`: `{"bg_fee08b": "black", "bg_d73027": "white"}`
- **SO (Secondary Overlap):**
  - `SO-1`: number (`-71.0`)
  - `SO-2`: number (`-95.0`)

## 2. How M4's Tests Load `vectors.json`

M4's tests in `WifiCoreTest.java` test vectors by name in dedicated JUnit test methods (`testCH1`, `testNM1`, etc.) with exact expected constants matching `vectors.json`. `tools/verify_vectors.py` (executed as part of `scripts/fast.sh`) verifies `tools/vectors.json` directly against Python reference calculations.

## 3. What `WallMaterials` Exposes in `:core`

In package `com.netfieldpro.core.model.WallMaterials`:
- `DRYWALL_LIGHT = new WallMaterial(MaterialId.DRYWALL_LIGHT, 3.0, 3.0, 4.5)`
- `GLASS_PARTITION = new WallMaterial(MaterialId.GLASS_PARTITION, 2.5, 2.5, 4.0)`
- `BRICK_MEDIUM = new WallMaterial(MaterialId.BRICK_MEDIUM, 8.0, 8.0, 12.0)`
- `CONCRETE_HEAVY = new WallMaterial(MaterialId.CONCRETE_HEAVY, 15.0, 12.0, 18.0)`
- `METAL_ELEVATOR = new WallMaterial(MaterialId.METAL_ELEVATOR, 25.0, 20.0, 28.0)`
- `ALL = List.of(DRYWALL_LIGHT, GLASS_PARTITION, BRICK_MEDIUM, CONCRETE_HEAVY, METAL_ELEVATOR)`

## 4. FixedDecimal API

In package `com.netfieldpro.core.util.FixedDecimal`:
- `public static String format(double v, int dp)`

## 5. HeatPalette Enum Location

In package `com.netfieldpro.core.settings.HeatPalette`:
- `TRAFFIC_LIGHT`
- `VIRIDIS`
- `public static HeatPalette parseOrDefault(String s)`

## 6. Debug Menu Button Pattern

In `app/src/main/res/layout/fragment_debug_data_lab.xml` and `DebugDataLabFragment.java`:
- Buttons are `MaterialButton` with IDs such as `btn_export_canonical`, `btn_seed_demo_pass`, etc.
- View binding / `findViewById` wires button listeners in `onViewCreated`.

## 7. Functions in `verify_vectors.py` Implementing Heat Math

- Wall kernel: `wall_crossings(P, Q, chains, materials)` (line 349)
- IDW: Section §3.10 IDW calculation (line 448)
- Radius: `radius_calc(nn_list)` (line 470)
- Grid & Boundary: Section §3.9 Grid and boundary (line 427)
- Palette: `interp_color(val, anchors)`, `to_hex(rgb)`, `rel_lum(rgb)`, `best_text_color(bg_hex)` (lines 483-547)
- Secondary overlap: `sec_overlap(group_maxes)` (line 551)
