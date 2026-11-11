#!/usr/bin/env python3
"""
scripts/check-permissions.py
Validates permissions in the release APK against docs/permissions.golden.txt using aapt2.
Standard library only.
"""

import glob
import os
import re
import subprocess
import sys


def find_sdk_dir():
    candidates = [
        os.environ.get("ANDROID_HOME"),
        os.environ.get("ANDROID_SDK_ROOT"),
    ]
    for c in candidates:
        if c and os.path.isdir(c):
            return c

    # Fallback to local.properties
    proj_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    local_props = os.path.join(proj_root, "local.properties")
    if os.path.isfile(local_props):
        with open(local_props, "r", encoding="utf-8") as f:
            for line in f:
                if line.startswith("sdk.dir="):
                    val = line.split("=", 1)[1].strip().replace("\\:", ":").replace("\\\\", "/")
                    if os.path.isdir(val):
                        return val
    return None


def find_newest_aapt2(sdk_dir):
    build_tools_dir = os.path.join(sdk_dir, "build-tools")
    if not os.path.isdir(build_tools_dir):
        return None

    def version_key(name):
        return [int(x) for x in re.findall(r"\d+", name)]

    versions = [d for d in os.listdir(build_tools_dir) if os.path.isdir(os.path.join(build_tools_dir, d))]
    versions.sort(key=version_key, reverse=True)

    exe_names = ["aapt2.exe", "aapt2"]
    for v in versions:
        for exe in exe_names:
            p = os.path.join(build_tools_dir, v, exe)
            if os.path.isfile(p):
                return p
    return None


def main():
    sdk_dir = find_sdk_dir()
    if not sdk_dir:
        print("ERROR: Could not locate Android SDK directory.", file=sys.stderr)
        sys.exit(1)

    aapt2 = find_newest_aapt2(sdk_dir)
    if not aapt2:
        print("ERROR: Could not find aapt2 in SDK build-tools.", file=sys.stderr)
        sys.exit(1)

    proj_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    apk_pattern = os.path.join(proj_root, "app", "build", "outputs", "apk", "open", "release", "*.apk")
    apks = glob.glob(apk_pattern)
    if not apks:
        apk_pattern = os.path.join(proj_root, "app", "build", "outputs", "apk", "release", "*.apk")
        apks = glob.glob(apk_pattern)
    if not apks:
        print(f"ERROR: No release APK found at {apk_pattern}", file=sys.stderr)
        sys.exit(1)

    # Prefer app-release.apk if present
    apk_path = apks[0]
    for apk in apks:
        if os.path.basename(apk) == "app-release.apk":
            apk_path = apk
            break

    golden_path = os.path.join(proj_root, "docs", "permissions.golden.txt")
    if not os.path.isfile(golden_path):
        print(f"ERROR: Golden file not found at {golden_path}", file=sys.stderr)
        sys.exit(1)

    required_perms = set()
    optional_perms = set()
    with open(golden_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if line.startswith("?"):
                optional_perms.add(line[1:].strip())
            else:
                required_perms.add(line)

    res = subprocess.run([aapt2, "dump", "permissions", apk_path], capture_output=True, text=True)
    if res.returncode != 0:
        print(f"ERROR running aapt2: {res.stderr}", file=sys.stderr)
        sys.exit(res.returncode)

    actual_perms = set(re.findall(r"uses-permission:\s*name='([^']+)'", res.stdout))

    missing = required_perms - actual_perms
    allowed = required_perms | optional_perms
    unexpected = actual_perms - allowed

    if missing or unexpected:
        print("ERROR: Release APK permissions mismatch!", file=sys.stderr)
        if missing:
            print(f"  Missing required permissions: {sorted(missing)}", file=sys.stderr)
        if unexpected:
            print(f"  Unexpected permissions: {sorted(unexpected)}", file=sys.stderr)
        sys.exit(1)

    print(f"check-permissions: OK ({len(actual_perms)} permissions verified against golden)")
    sys.exit(0)


if __name__ == "__main__":
    main()
