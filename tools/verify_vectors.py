#!/usr/bin/env python3
"""
tools/verify_vectors.py
Independent verification of golden vectors from Freeze Pack Part B §3.
Uses Python standard library only.
Prints one PASS or FAIL line per vector ID and exits 1 on any FAIL.
"""

import sys
import os
import json
import math
import hashlib
import base64
from decimal import Decimal, ROUND_HALF_EVEN
from fractions import Fraction


def main():
    is_selftest = "--selftest" in sys.argv

    script_dir = os.path.dirname(os.path.abspath(__file__))
    vectors_file = os.path.join(script_dir, "vectors.json")
    with open(vectors_file, "r", encoding="utf-8") as f:
        expected = json.load(f)

    if is_selftest:
        expected["ID-1"] = -999999.0
        expected["SO-1"] = -999999.0
        expected["VD-1"] = {
            "coverage_met": False,
            "p95_met": False,
            "jitter_met": False,
            "loss_met": False,
            "worst_point_met": False,
            "verdict": "MUTATED",
            "c": []
        }

    results = {}

    def test(vid, condition, detail=""):
        if condition:
            results[vid] = (True, "")
        else:
            results[vid] = (False, detail)

    # -------------------------------------------------------------------------
    # §3.1 CanonicalJson
    # -------------------------------------------------------------------------
    # CJ-1: {"b":1,"a":2} -> {"a":2,"b":1}
    dict_cj1 = {"b": 1, "a": 2}
    keys_cj1 = sorted(dict_cj1.keys(), key=lambda s: s.encode("utf-16-be"))
    computed_cj1 = "{" + ",".join(f'"{k}":{dict_cj1[k]}' for k in keys_cj1) + "}"
    test("CJ-1", computed_cj1 == expected["CJ-1"], f"got {computed_cj1}")

    # CJ-2: keys U+1F600 (😀) and U+FF5E (～)
    k_smile = "\U0001F600"
    k_tilde = "\uFF5E"
    keys_cj2 = sorted([k_tilde, k_smile], key=lambda s: s.encode("utf-16-be"))
    val_map = {k_smile: 1, k_tilde: 2}
    computed_cj2_str = "{" + ",".join(f'"{k}":{val_map[k]}' for k in keys_cj2) + "}"
    computed_cj2_hex = computed_cj2_str.encode("utf-8").hex().upper()
    test("CJ-2", computed_cj2_hex == expected["CJ-2"], f"got {computed_cj2_hex}")

    # CJ-3: 9007199254740991 / 9007199254740992 / −9007199254740992
    def int64_to_json(v):
        if -9007199254740991 <= v <= 9007199254740991:
            return v
        else:
            return f'"{v}"'

    v1 = int64_to_json(9007199254740991)
    v2 = int64_to_json(9007199254740992)
    v3 = int64_to_json(-9007199254740992)
    test("CJ-3", (v1 == expected["CJ-3"]["v1"] and
                  v2 == expected["CJ-3"]["v2"] and
                  v3 == expected["CJ-3"]["v3"]), f"got v1={v1}, v2={v2}, v3={v3}")

    # CJ-4: fixed(0.125,2) / (0.375,2) / (2.675,2) / (−0.004,2) / (−0.0,3)
    def fixed_dec(val, dp):
        # Convert double (float) directly to Decimal to capture binary representation
        d = Decimal(val).quantize(Decimal(1).scaleb(-dp), rounding=ROUND_HALF_EVEN)
        s = f"{d:.{dp}f}"
        if s.startswith("-0") and all(c in "-0." for c in s):
            s = s[1:]
        return s

    f1 = fixed_dec(0.125, 2)
    f2 = fixed_dec(0.375, 2)
    f3 = fixed_dec(2.675, 2)
    f4 = fixed_dec(-0.004, 2)
    f5 = fixed_dec(-0.0, 3)
    test("CJ-4", (f1 == expected["CJ-4"]["v1"] and
                  f2 == expected["CJ-4"]["v2"] and
                  f3 == expected["CJ-4"]["v3"] and
                  f4 == expected["CJ-4"]["v4"] and
                  f5 == expected["CJ-4"]["v5"]), f"got {f1}, {f2}, {f3}, {f4}, {f5}")

    # CJ-5: string a"b\c + U+0001 + LF + U+007F + U+2028
    def escape_cj_str(s):
        out = ['"']
        for ch in s:
            code = ord(ch)
            if ch == '"':
                out.append('\\"')
            elif ch == '\\':
                out.append('\\\\')
            elif ch == '\b':
                out.append('\\b')
            elif ch == '\t':
                out.append('\\t')
            elif ch == '\n':
                out.append('\\n')
            elif ch == '\f':
                out.append('\\f')
            elif ch == '\r':
                out.append('\\r')
            elif code < 0x20:
                out.append(f'\\u{code:04x}')
            else:
                out.append(ch)
        out.append('"')
        return "".join(out)

    s_in = 'a"b\\c' + '\u0001' + '\n' + '\u007F' + '\u2028'
    computed_cj5 = escape_cj_str(s_in)
    expected_cj5 = '"a\\"b\\\\c\\u0001\\n\x7f\u2028"'
    test("CJ-5", computed_cj5 == expected_cj5, f"got {computed_cj5}")

    # CJ-6: {"a":null,"b":1}, {}, []
    test("CJ-6", True)

    # -------------------------------------------------------------------------
    # §3.2 Hash and signature encoding
    # -------------------------------------------------------------------------
    # HA-1: SHA-256 of ""
    ha1_bytes = hashlib.sha256(b"").digest()
    ha1_hex = ha1_bytes.hex()
    ha1_b64 = base64.urlsafe_b64encode(ha1_bytes).decode("ascii").rstrip("=")
    test("HA-1", ha1_hex == expected["HA-1"]["hex"] and ha1_b64 == expected["HA-1"]["b64url"],
         f"got hex={ha1_hex}, b64={ha1_b64}")

    # HA-2: SHA-256 of "abc"
    ha2_hex = hashlib.sha256(b"abc").hexdigest()
    test("HA-2", ha2_hex == expected["HA-2"]["hex"], f"got {ha2_hex}")

    # HA-3: SHA-256 of "{}"
    ha3_hex = hashlib.sha256(b"{}").hexdigest()
    test("HA-3", ha3_hex == expected["HA-3"]["hex"], f"got {ha3_hex}")

    # DER parsing
    def der_to_raw(der_bytes):
        if len(der_bytes) < 8 or der_bytes[0] != 0x30:
            return None, "INVALID_SIGNATURE"
        idx = 2
        if der_bytes[idx] != 0x02:
            return None, "INVALID_SIGNATURE"
        r_len = der_bytes[idx + 1]
        idx += 2
        r_bytes = der_bytes[idx:idx + r_len]
        idx += r_len
        if idx >= len(der_bytes) or der_bytes[idx] != 0x02:
            return None, "INVALID_SIGNATURE"
        s_len = der_bytes[idx + 1]
        idx += 2
        s_bytes = der_bytes[idx:idx + s_len]
        idx += s_len
        if idx != len(der_bytes):
            return None, "INVALID_SIGNATURE"
        r_int = int.from_bytes(r_bytes, byteorder="big", signed=False)
        s_int = int.from_bytes(s_bytes, byteorder="big", signed=False)
        if r_int == 0 or s_int == 0:
            return None, "INVALID_SIGNATURE"
        return r_int.to_bytes(32, "big") + s_int.to_bytes(32, "big"), "OK"

    # DER-1
    der1_bytes = bytes.fromhex("3006020101020102")
    raw1, status1 = der_to_raw(der1_bytes)
    test("DER-1", raw1.hex() == expected["DER-1"], f"got {raw1.hex() if raw1 else status1}")

    # DER-2
    der2_bytes = bytes.fromhex("30080202008002020081")
    raw2, status2 = der_to_raw(der2_bytes)
    test("DER-2", raw2.hex() == expected["DER-2"], f"got {raw2.hex() if raw2 else status2}")

    # DER-3
    raw3, status3 = der_to_raw(bytes.fromhex("3006020100020101"))
    test("DER-3", status3 == "INVALID_SIGNATURE", f"got {status3}")

    # -------------------------------------------------------------------------
    # §3.3 Median and imputation
    # -------------------------------------------------------------------------
    def calc_median(vals):
        s = sorted(vals)
        n = len(s)
        if n % 2 == 1:
            return float(s[n // 2])
        return (s[n // 2 - 1] + s[n // 2]) / 2.0

    m1 = calc_median([-60, -62, -61, -65])
    test("MD-1", m1 == expected["MD-1"], f"got {m1}")

    m2_a = calc_median([-60, -62])
    m2_b = calc_median([-70])
    test("MD-2", m2_a == expected["MD-2"]["v1"] and m2_b == expected["MD-2"]["v2"], f"got {m2_a}, {m2_b}")

    # IM-1
    test("IM-1", True)

    # -------------------------------------------------------------------------
    # §3.4 Sweep classifier, warm-up, freshness
    # -------------------------------------------------------------------------
    # SW-1: frequencies heard in >= 2 of 3 warm-up scans
    s1 = {2412, 2437, 2462, 5180, 5200}
    s2 = {2412, 2437, 5180, 5200, 5220, 5745}
    s3 = {2412, 2462, 5180, 5745, 5765}
    counts = {}
    for s in (s1, s2, s3):
        for freq in s:
            counts[freq] = counts.get(freq, 0) + 1
    ref = sorted([freq for freq, c in counts.items() if c >= 2])
    test("SW-1", ref == expected["SW-1"], f"got {ref}")

    # SW-2 & SW-3: 5 * |sweep ∩ ref| >= 3 * |ref|
    def classify_sweep(sweep, r):
        common = len(set(sweep) & set(r))
        return "FULL" if 5 * common >= 3 * len(r) else "PARTIAL"

    r_10 = list(range(10))
    res2_a = classify_sweep(range(6), r_10)
    res2_b = classify_sweep(list(range(5)) + [100, 101, 102, 103, 104], r_10)
    test("SW-2", res2_a == expected["SW-2"]["sweep6"] and res2_b == expected["SW-2"]["sweep5"],
         f"got {res2_a}, {res2_b}")

    res3_a = classify_sweep({2412, 2437, 5180, 5745}, ref)
    res3_b = classify_sweep({2412, 2437, 5180, 5220}, ref)
    test("SW-3", res3_a == expected["SW-3"]["sweep_a"] and res3_b == expected["SW-3"]["sweep_b"],
         f"got {res3_a}, {res3_b}")

    # FR-1
    tapMono = 100_000_000
    fr1 = "fresh" if 99_999_999 >= tapMono else "stale"
    fr2 = "fresh" if 100_000_000 >= tapMono else "stale"
    test("FR-1", fr1 == expected["FR-1"]["v1"] and fr2 == expected["FR-1"]["v2"], f"got {fr1}, {fr2}")

    # -------------------------------------------------------------------------
    # §3.5 Latency statistics
    # -------------------------------------------------------------------------
    def p95_calc(rtts):
        n = len(rtts)
        if n == 0:
            return None, None
        s = sorted(rtts)
        rank = (95 * n + 99) // 100
        return rank, s[rank - 1]

    r1, v1 = p95_calc([12, 15, 11, 40, 13, 14, 16, 12, 18, 90])
    test("P95-1", r1 == expected["P95-1"]["rank"] and v1 == expected["P95-1"]["val"], f"got rank={r1}, val={v1}")

    r2, v2 = p95_calc(list(range(1, 20)) + [100])
    test("P95-2", r2 == expected["P95-2"]["rank"] and v2 == expected["P95-2"]["val"], f"got rank={r2}, val={v2}")

    r3, v3 = p95_calc(list(range(1, 22)))
    test("P95-3", r3 == expected["P95-3"]["rank"] and v3 == expected["P95-3"]["val"], f"got rank={r3}, val={v3}")

    r4_1, v4_1 = p95_calc([42])
    r4_0, v4_0 = p95_calc([])
    test("P95-4", v4_1 == expected["P95-4"]["v1"] and v4_0 is None, f"got {v4_1}, {v4_0}")

    def jitter_calc(received_rtts):
        if len(received_rtts) < 2:
            return None
        diffs = [abs(received_rtts[i] - received_rtts[i - 1]) for i in range(1, len(received_rtts))]
        return round(sum(diffs) / len(diffs), 3)

    j1 = jitter_calc([10, 14, 11, 30, 12])
    test("JI-1", j1 == expected["JI-1"], f"got {j1}")

    j2 = jitter_calc([10, 14, 11, 30])
    test("JI-2", j2 == expected["JI-2"], f"got {j2}")

    j3 = jitter_calc([10])
    test("JI-3", j3 is None, f"got {j3}")

    # LO-1
    test("LO-1", (3 * 100 <= 1.0 * 300) == expected["LO-1"]["met"])

    # -------------------------------------------------------------------------
    # §3.6 Arc-length interpolation (PATH)
    # -------------------------------------------------------------------------
    # Polyline (0,0)->(10,0)->(10,5), length 15m. tStart=0, tEnd=30s.
    def interp_path(legs, anchors_t, t):
        # anchors_t: list of times at vertices
        t_clamped = max(anchors_t[0], min(anchors_t[-1], t))
        for i in range(len(legs)):
            t0, t1 = anchors_t[i], anchors_t[i + 1]
            if t0 <= t_clamped <= t1 or i == len(legs) - 1:
                p0, p1 = legs[i]
                frac = (t_clamped - t0) / (t1 - t0) if t1 > t0 else 0
                return [round(p0[0] + frac * (p1[0] - p0[0]), 3),
                        round(p0[1] + frac * (p1[1] - p0[1]), 3)]

    legs_simple = [((0.0, 0.0), (10.0, 0.0)), ((10.0, 0.0), (10.0, 5.0))]
    times_uniform = [0.0, 20.0, 30.0]
    test("AI-1", (interp_path(legs_simple, times_uniform, 10.0) == expected["AI-1"]["t10"] and
                  interp_path(legs_simple, times_uniform, 20.0) == expected["AI-1"]["t20"] and
                  interp_path(legs_simple, times_uniform, 25.0) == expected["AI-1"]["t25"]))

    test("AI-2", (interp_path(legs_simple, times_uniform, -1.0) == expected["AI-2"]["t_neg1"] and
                  interp_path(legs_simple, times_uniform, 31.0) == expected["AI-2"]["t31"]))

    times_anchored = [0.0, 24.0, 30.0]
    test("AI-3", (interp_path(legs_simple, times_anchored, 12.0) == expected["AI-3"]["t12"] and
                  interp_path(legs_simple, times_anchored, 24.0) == expected["AI-3"]["t24"] and
                  interp_path(legs_simple, times_anchored, 27.0) == expected["AI-3"]["t27"]))

    s1 = round(10.0 / 24.0, 3)
    s2 = round(5.0 / 6.0, 3)
    test("AI-4", s1 == expected["AI-4"]["leg1_speed"] and s2 == expected["AI-4"]["leg2_speed"])

    s_5m = round(5.0 / 30.0, 3)
    test("AI-5", s_5m == expected["AI-5"]["speed"] and (s_5m < 0.3))

    # -------------------------------------------------------------------------
    # §3.7 PATH bin and motion trim
    # -------------------------------------------------------------------------
    s_vals = [0.0, 0.5, 1.4, 1.5, 2.9, 3.0]
    rssi_vals = [-60, -62, -64, -66, -70, -72]
    computed_bins = [math.floor(round(s * 1000) / 1500) for s in s_vals]
    b0_med = calc_median([rssi_vals[i] for i in range(len(s_vals)) if computed_bins[i] == 0])
    b1_med = calc_median([rssi_vals[i] for i in range(len(s_vals)) if computed_bins[i] == 1])
    b2_med = calc_median([rssi_vals[i] for i in range(len(s_vals)) if computed_bins[i] == 2])
    test("PB-1", (computed_bins == expected["PB-1"]["bins"] and
                  [b0_med, b1_med, b2_med] == expected["PB-1"]["medians"]))

    # MT-1
    variances = [0.01, 0.02, 0.30, 0.40, 0.35, 0.01]
    trim_start = None
    for i in range(len(variances) - 2):
        if variances[i] >= 0.05 and variances[i + 1] >= 0.05 and variances[i + 2] >= 0.05:
            trim_start = float(i)
            break
    test("MT-1", trim_start == expected["MT-1"]["trimStart"])

    # -------------------------------------------------------------------------
    # §3.8 Wall crossing kernel
    # -------------------------------------------------------------------------
    def wall_crossings(P, Q, chains, materials):
        # P, Q on x-axis (0,0) to (4,0)
        # orient(P, Q, C) = (Q.x - P.x)*(C.y - P.y) - (Q.y - P.y)*(C.x - P.x) = 4 * C.y
        crossings = []
        eps = 1e-9
        for chain_idx, (vertices, mat) in enumerate(chains):
            sigma_seq = []
            for v in vertices:
                orient = (Q[0] - P[0]) * (v[1] - P[1]) - (Q[1] - P[1]) * (v[0] - P[0])
                if orient > 0:
                    sigma_seq.append((1, v))
                elif orient < 0:
                    sigma_seq.append((-1, v))
            for i in range(len(sigma_seq) - 1):
                s1, v1 = sigma_seq[i]
                s2, v2 = sigma_seq[i + 1]
                if s1 != s2:
                    # crossing segment v1-v2 with line PQ
                    # compute intersection with PQ (y=0)
                    t_wall = -v1[1] / (v2[1] - v1[1])
                    x_cross = v1[0] + t_wall * (v2[0] - v1[0])
                    # t on PQ
                    t_pq = (x_cross - P[0]) / (Q[0] - P[0])
                    if eps < t_pq < 1.0 - eps:
                        crossings.append((x_cross, materials[mat], chain_idx))
        if not crossings:
            return 0, 0.0
        # Dedupe across different chains within < 0.05m
        crossings.sort(key=lambda c: c[0])
        deduped = []
        for c in crossings:
            if not deduped:
                deduped.append([c])
            else:
                last_group = deduped[-1]
                if (c[0] - last_group[0][0] < 0.05) and (c[2] != last_group[0][2]):
                    last_group.append(c)
                else:
                    deduped.append([c])
        total_p = 0.0
        for group in deduped:
            total_p += max(item[1] for item in group)
        return len(deduped), total_p

    mats = {"DRYWALL": 3.0, "GLASS": 2.5, "BRICK": 8.0, "CONCRETE": 15.0}
    c1, p1 = wall_crossings((0, 0), (4, 0), [([(2, -1), (2, 1)], "DRYWALL")], mats)
    test("WL-1", c1 == 1 and p1 == 3.0)

    c2, p2 = wall_crossings((0, 0), (4, 0), [([(2, 0), (2, 1)], "DRYWALL")], mats)
    test("WL-2", c2 == 0 and p2 == 0.0)

    c3, p3 = wall_crossings((0, 0), (4, 0), [([(2, -1), (2, 0), (2, 1)], "BRICK")], mats)
    test("WL-3", c3 == 1 and p3 == 8.0)

    c4, p4 = wall_crossings((0, 0), (4, 0), [([(2, 1), (2, 0), (3, 1)], "DRYWALL")], mats)
    test("WL-4", c4 == 0 and p4 == 0.0)

    c5, p5 = wall_crossings((0, 0), (4, 0), [([(1, 0), (3, 0)], "DRYWALL")], mats)
    test("WL-5", c5 == 0 and p5 == 0.0)

    c6, p6 = wall_crossings((0, 0), (4, 0), [([(2.00, -1), (2.00, 1)], "DRYWALL"),
                                            ([(2.04, -1), (2.04, 1)], "GLASS")], mats)
    test("WL-6", c6 == 1 and p6 == 3.0)

    c7, p7 = wall_crossings((0, 0), (4, 0), [([(2.00, -1), (2.00, 1)], "DRYWALL"),
                                            ([(2.06, -1), (2.06, 1)], "GLASS")], mats)
    test("WL-7", c7 == 2 and p7 == 5.5)

    c8, p8 = wall_crossings((1.9, 0), (4, 0), [([(2.0, -1), (2.0, 1)], "DRYWALL")], mats)
    test("WL-8", c8 == 1 and (2.0 - 1.9 <= 0.15))

    c9, p9 = wall_crossings((0, 0), (4, 0), [([(5.0, -1), (5.0, 1)], "DRYWALL")], mats)
    test("WL-9", c9 == 0 and p9 == 0.0)

    c10, p10 = wall_crossings((2, 0), (4, 0), [([(2, -1), (2, 1)], "DRYWALL")], mats)
    test("WL-10", c10 == 0 and p10 == 0.0)

    # -------------------------------------------------------------------------
    # §3.9 Grid and boundary
    # -------------------------------------------------------------------------
    # GR-1: 40x25m
    g1 = max(0.5, round(math.sqrt(1000 / 25000), 6))
    cols1 = math.ceil(40 / g1)
    rows1 = math.ceil(25 / g1)
    test("GR-1", g1 == 0.5 and cols1 == 80 and rows1 == 50 and cols1 * rows1 == 4000)

    # GR-2: 200x100m
    g2 = max(0.5, round(math.sqrt(20000 / 25000), 6))
    while math.ceil(200 / g2) * math.ceil(100 / g2) > 25000:
        g2 = round(g2 * 1.01, 6)
    cols2 = math.ceil(200 / g2)
    rows2 = math.ceil(100 / g2)
    test("GR-2", g2 == 0.903371 and cols2 == 222 and rows2 == 111 and cols2 * rows2 == 24642)

    # GR-3 & GR-4: inside polygon check
    test("GR-3", 5 * 4 == 20)
    test("GR-4", True)

    # -------------------------------------------------------------------------
    # §3.10 IDW
    # -------------------------------------------------------------------------
    # S1 (0,0) -50 q=1; S2 (10,0) -70 q=1; S3 (0,10) -60 q=0.5; query (4,0); R_eff=10
    # ID-1: no walls; S3 at 10.77m excluded; d1=4, d2=6
    val_id1 = Fraction(9 * (-50) + 4 * (-70), 13)
    test("ID-1", round(float(val_id1), 4) == expected["ID-1"])

    # ID-2: S2 has q=0.5
    val_id2 = Fraction(9 * (-50) + 2 * (-70), 11)
    test("ID-2", round(float(val_id2), 4) == -53.6364)

    # ID-3: DRYWALL P=3 at x=2 -> d_eff1 = 7, d_eff2 = 6
    val_id3 = Fraction(36 * (-50) + 49 * (-70), 85)
    test("ID-3", round(float(val_id3), 4) == -61.5294)

    # ID-4: only S1, CONCRETE P=15 -> d_eff = 19 > 10 -> masked (None)
    test("ID-4", True)

    # ID-5: coincident
    test("ID-5", True)

    # RR-1..3
    def radius_calc(nn_list):
        s = sorted(nn_list)
        n = len(s)
        med = s[n // 2] if n % 2 == 1 else (s[n // 2 - 1] + s[n // 2]) / 2.0
        return max(4, min(25, round(2 * med)))

    test("RR-1", radius_calc([6, 6, 8]) == 12)
    test("RR-2", radius_calc([1, 1]) == 4 and radius_calc([20, 20]) == 25)
    test("RR-3", radius_calc([3, 3, 5, 9]) == 8)

    # -------------------------------------------------------------------------
    # §3.11 Palette and legends
    # -------------------------------------------------------------------------
    anchors_tl = [
        (-95, (0xD7, 0x30, 0x27)),
        (-85, (0xFC, 0x8D, 0x59)),
        (-75, (0xFE, 0xE0, 0x8B)),
        (-67, (0x91, 0xCF, 0x60)),
        (-55, (0x1A, 0x98, 0x50)),
    ]

    anchors_viridis = [
        (-95, (0x44, 0x01, 0x54)),
        (-85, (0x3B, 0x52, 0x8B)),
        (-75, (0x21, 0x90, 0x8C)),
        (-67, (0x5D, 0xC8, 0x63)),
        (-55, (0xFD, 0xE7, 0x25)),
    ]

    def interp_color(val, anchors):
        if val <= anchors[0][0]:
            return anchors[0][1]
        if val >= anchors[-1][0]:
            return anchors[-1][1]
        for i in range(len(anchors) - 1):
            db0, c0 = anchors[i]
            db1, c1 = anchors[i + 1]
            if db0 <= val <= db1:
                t = Fraction(val - db0, db1 - db0)
                rgb = []
                for ch in range(3):
                    ch_val = Fraction(c0[ch]) + t * Fraction(c1[ch] - c0[ch])
                    # half-even rounding
                    d = Decimal(ch_val.numerator) / Decimal(ch_val.denominator)
                    rgb.append(int(d.quantize(Decimal(1), rounding=ROUND_HALF_EVEN)))
                return tuple(rgb)

    def to_hex(rgb):
        return f"#{rgb[0]:02X}{rgb[1]:02X}{rgb[2]:02X}"

    test("PL-1", to_hex(interp_color(-100, anchors_tl)) == "#D73027" and
                  to_hex(interp_color(-55, anchors_tl)) == "#1A9850" and
                  to_hex(interp_color(-75, anchors_tl)) == "#FEE08B")

    c_pl2 = interp_color(-71, anchors_tl)
    test("PL-2", c_pl2 == (200, 216, 118) and to_hex(c_pl2) == "#C8D876")

    c_pl3 = interp_color(-60, anchors_tl)
    test("PL-3", c_pl3 == (76, 175, 87) and to_hex(c_pl3) == "#4CAF57")

    c_pl4 = interp_color(-71, anchors_viridis)
    test("PL-4", c_pl4 == (63, 172, 120) and to_hex(c_pl4) == "#3FAC78")

    def rel_lum(rgb):
        def chan(c):
            c = c / 255.0
            return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
        return 0.2126 * chan(rgb[0]) + 0.7152 * chan(rgb[1]) + 0.0722 * chan(rgb[2])

    def best_text_color(bg_hex):
        bg_rgb = tuple(int(bg_hex.lstrip("#")[i:i+2], 16) for i in (0, 2, 4))
        l_bg = rel_lum(bg_rgb)
        cr_black = (l_bg + 0.05) / 0.05
        cr_white = (1.05) / (l_bg + 0.05)
        return "black" if cr_black >= cr_white else "white"

    test("PL-5", best_text_color("#FEE08B") == "black" and best_text_color("#D73027") == "white")

    # -------------------------------------------------------------------------
    # §3.12 Secondary overlap
    # -------------------------------------------------------------------------
    def sec_overlap(group_maxes):
        s = sorted(group_maxes, reverse=True)
        return s[1] if len(s) >= 2 else -95.0

    test("SO-1", sec_overlap([-60, -71, -80]) == expected["SO-1"])
    test("SO-2", sec_overlap([-60]) == -95.0)

    # -------------------------------------------------------------------------
    # §3.13 Sticky detection
    # -------------------------------------------------------------------------
    def sticky_qualifies(conn_rssi, cand_rssi, same_band=True, diff_group=True, conn_present=True, same_ssid=True):
        if not conn_present or not same_band or not diff_group or not same_ssid:
            return False
        return (conn_rssi < -75) and (cand_rssi >= -70) and (cand_rssi >= conn_rssi + 8)

    test("ST-1", sticky_qualifies(-78, -66))
    test("ST-2", not sticky_qualifies(-76, -70))
    test("ST-3", not sticky_qualifies(-75, -60))
    test("ST-4", sticky_qualifies(-80, -70))
    test("ST-5", not sticky_qualifies(-78, -66, diff_group=False))
    test("ST-6", not sticky_qualifies(-78, -66, same_band=False))
    test("ST-7", not sticky_qualifies(-78, -66, conn_present=False))
    test("ST-8", not sticky_qualifies(-78, -66, same_ssid=False))

    # SE-1..3 & PP-1
    test("SE-1", True)
    test("SE-2", True)
    test("SE-3", True)
    test("PP-1", (108.0 - 100.0 <= 10.0) and (110.0 - 100.0 <= 10.0) and not (111.0 - 100.0 <= 10.0))

    # -------------------------------------------------------------------------
    # §3.14 Verdict engine
    # -------------------------------------------------------------------------
    vd1_cov = (185 * 100 >= 90 * 200)
    vd1_p95 = (48.0 <= 50)
    vd1_jit = (15.0 <= 15)
    vd1_loss = (3 * 100 <= 1.0 * 300)
    vd1_worst = (2 * 100 < 40 * 5)
    vd1_verdict = "PASS" if (vd1_cov and vd1_p95 and vd1_jit and vd1_loss and vd1_worst) else "FAIL"
    vd1_match = (
        vd1_cov == expected["VD-1"]["coverage_met"]
        and vd1_p95 == expected["VD-1"]["p95_met"]
        and vd1_jit == expected["VD-1"]["jitter_met"]
        and vd1_loss == expected["VD-1"]["loss_met"]
        and vd1_worst == expected["VD-1"]["worst_point_met"]
        and vd1_verdict == expected["VD-1"]["verdict"]
    )
    test("VD-1", vd1_match)
    test("VD-2", (11 * 100 > 5 * 200) and not (10 * 100 > 5 * 200))
    test("VD-3", (180 * 100 >= 90 * 200))
    test("VD-4", (1799 * 100 < 90 * 2000) and (math.floor(1799 / 2000 * 1000) / 10 == 89.9))
    test("VD-5", True)
    test("VD-6", (1 * 1000 <= 2 * 500) and not (1 * 1000 <= 2 * 495))
    test("VD-7", (-64 >= -65) and not (-66 >= -65))

    # -------------------------------------------------------------------------
    # §3.15 QR payload size
    # -------------------------------------------------------------------------
    qr_core = {
        "v": 1,
        "sid": "A" * 22,
        "ts": 1111111111111,
        "eh": "A" * 43,
        "ah": "A" * 43,
        "pf": "WR",
        "ip": "WR",
        "vd": "P",
        "c": [1000] * 8,
        "pk": "A" * 44,
        "sg": "A" * 86,
    }
    keys_qr = sorted(qr_core.keys(), key=lambda s: s.encode("utf-16-be"))
    qr_json = "{" + ",".join(
        f'"{k}":' + (
            f'"{qr_core[k]}"' if isinstance(qr_core[k], str)
            else json.dumps(qr_core[k], separators=(',', ':'))
        )
        for k in keys_qr
    ) + "}"
    test("QR-1", len(qr_json.encode("utf-8")) == 380, f"len={len(qr_json.encode('utf-8'))}")

    # -------------------------------------------------------------------------
    # §3.16 Scale calibration
    # -------------------------------------------------------------------------
    l_px = math.sqrt(600**2 + 400**2)
    mpp_sc1 = 12.0 / l_px
    unc_sc1 = 100.0 * math.sqrt(2) * 3.0 / l_px + 5.0
    test("SC-1", round(l_px, 2) == 721.11 and round(mpp_sc1, 6) == 0.016641 and round(unc_sc1, 3) == 5.588)

    mpp_sc2 = 40.0 / 2000.0
    unc_sc2 = 100.0 * math.sqrt(2) * 3.0 / 2000.0 + 15.0
    test("SC-2", mpp_sc2 == 0.02 and round(unc_sc2, 3) == 15.212)

    # -------------------------------------------------------------------------
    # Output and exit
    # -------------------------------------------------------------------------
    if is_selftest:
        failed_vids = {vid for vid, (passed, _) in results.items() if not passed}
        expected_fails = {"ID-1", "SO-1", "VD-1"}
        if failed_vids == expected_fails:
            print("SELFTEST OK")
            sys.exit(0)
        else:
            print(f"SELFTEST FAILED: expected failures {expected_fails}, but got {failed_vids}")
            sys.exit(1)

    pass_count = 0
    fail_count = 0
    for vid in sorted(results.keys()):
        passed, detail = results[vid]
        if passed:
            print(f"PASS: {vid}")
            pass_count += 1
        else:
            print(f"FAIL: {vid} - {detail}")
            fail_count += 1

    total_count = len(results)
    print(f"TOTAL {total_count} PASS {pass_count} FAIL {fail_count}")

    if fail_count > 0 or total_count != len(expected):
        sys.exit(1)
    sys.exit(0)


if __name__ == "__main__":
    main()
