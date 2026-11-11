#!/usr/bin/env bash
set -euo pipefail

TAG="${1:-}"
if [ -z "$TAG" ]; then
    echo "Usage: scripts/evidence.sh <TAG>" >&2
    exit 1
fi

OUT_DIR="docs/evidence/$TAG"
mkdir -p "$OUT_DIR"
rm -f "$OUT_DIR/exit-codes.txt"

export EVIDENCE_DIR="$OUT_DIR"

if [ -n "${PHYSICAL_SERIAL:-}" ]; then
    export COLDSTART_SERIAL="$PHYSICAL_SERIAL"
    if [ -n "${DEVICES:-}" ]; then
        export DEVICES="$DEVICES $PHYSICAL_SERIAL"
    elif [ -n "${AVDS:-}" ]; then
        export DEVICES="$AVDS $PHYSICAL_SERIAL"
    else
        export DEVICES="Low_API30 High_API35 $PHYSICAL_SERIAL"
    fi
else
    if [ "${ALLOW_NO_PHYSICAL:-0}" != "1" ]; then
        echo "PHYSICAL_SERIAL required" >&2
        exit 4
    else
        echo "no-physical=1" >> "$OUT_DIR/exit-codes.txt"
    fi
fi

{
    echo "=== Date (UTC) ==="
    date -u
    echo ""
    echo "=== HEAD ==="
    git rev-parse HEAD
    echo ""
    echo "=== git status --porcelain ==="
    dirty=$(git status --porcelain)
    if [ -n "$dirty" ]; then
        echo "WARNING: Working tree is dirty:"
        echo "$dirty"
    else
        echo "Clean"
    fi
    echo ""
    echo "=== java -version ==="
    java -version 2>&1
    echo ""
    echo "=== ./gradlew --version ==="
    ./gradlew --version
    echo ""
    echo "=== adb devices -l ==="
    adb devices -l
    echo ""
    echo "=== Devices ==="
    devs=$(adb devices | tr -d '\r' | awk '$2=="device"{print $1}')
    for d in $devs; do
        echo "--- Device: $d ---"
        echo "model:       $(adb -s "$d" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
        echo "sdk:         $(adb -s "$d" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')"
        echo "qemu:        $(adb -s "$d" shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r')"
        echo "fingerprint: $(adb -s "$d" shell getprop ro.build.fingerprint 2>/dev/null | tr -d '\r')"
    done
    if [ -n "${DEVICES:-}" ]; then
        echo "--- Configured Devices ---"
        echo "$DEVICES"
    fi
} > "$OUT_DIR/env.txt"

overall_exit=0

run_step() {
    local name="$1"
    shift
    local logfile="$OUT_DIR/$name.txt"
    local code=0
    set +e
    "$@" 2>&1 | tee "$logfile"
    code=${PIPESTATUS[0]}
    set -e
    echo "$name=$code" >> "$OUT_DIR/exit-codes.txt"
    if [ "$code" -ne 0 ]; then
        overall_exit=1
    fi
}

./gradlew --stop >/dev/null 2>&1 || true
run_step "verify" bash scripts/verify.sh
run_step "verify-device" bash scripts/verify-device.sh
run_step "vectors" python tools/verify_vectors.py
run_step "selftest" python tools/verify_vectors.py --selftest

if [ -n "${COLDSTART_SERIAL:-}" ]; then
    run_step "coldstart" bash scripts/coldstart.sh
fi

if [ -f "docs/${TAG}-REPORT.md" ]; then
    cr_code=0
    python scripts/check-report.py "docs/${TAG}-REPORT.md" || cr_code=$?
    echo "check-report=$cr_code" >> "$OUT_DIR/exit-codes.txt"
    if [ "$cr_code" -ne 0 ]; then
        overall_exit=1
    fi
fi

if [ "$overall_exit" -ne 0 ]; then
    echo "Evidence generation failed (non-zero exit code recorded)." >&2
    exit 1
fi

echo "Evidence successfully collected in $OUT_DIR"
