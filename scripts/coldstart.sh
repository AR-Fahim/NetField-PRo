#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.netfieldpro"
ACTIVITY="com.netfieldpro/.MainActivity"
ADVISORY="${ADVISORY:-0}"

SERIAL="${COLDSTART_SERIAL:-${ANDROID_SERIAL:-}}"
if [ -n "$SERIAL" ]; then
    ADB=(adb -s "$SERIAL")
else
    ADB=(adb)
fi

model=$("${ADB[@]}" shell getprop ro.product.model | tr -d '\r')
sdk=$("${ADB[@]}" shell getprop ro.build.version.sdk | tr -d '\r')
qemu=$("${ADB[@]}" shell getprop ro.kernel.qemu | tr -d '\r')

if [ "$qemu" = "1" ] || [[ "$model" == *"sdk_gphone"* ]] || [[ "$model" == *"Emulator"* ]]; then
    DEVICE_CLASS="EMULATOR"
else
    DEVICE_CLASS="PHYSICAL"
fi

echo "model=$model"
echo "sdk=$sdk"
echo "ro.kernel.qemu=$qemu"
echo "DEVICE_CLASS=$DEVICE_CLASS"

if [ "$DEVICE_CLASS" = "EMULATOR" ] && [ "$ADVISORY" != "1" ]; then
    echo "Cold start requires a physical device (got $DEVICE_CLASS; set ADVISORY=1 to override). Exiting." >&2
    exit 2
fi

if [ -f "app/build/outputs/apk/open/release/app-open-release.apk" ]; then
    "${ADB[@]}" install -r -d "app/build/outputs/apk/open/release/app-open-release.apk" > /dev/null 2>&1 || true
elif [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    "${ADB[@]}" install -r -d "app/build/outputs/apk/release/app-release.apk" > /dev/null 2>&1 || true
fi

pkg_dump=$("${ADB[@]}" shell dumpsys package "$PACKAGE" 2>&1 || true)
if echo "$pkg_dump" | grep -qi "DEBUGGABLE"; then
    echo "Error: package $PACKAGE is DEBUGGABLE. Release APK must be installed." >&2
    exit 2
fi

echo "Starting cold-start measurement (10 runs)..."

declare -a times=()

for i in $(seq 1 10); do
    "${ADB[@]}" shell am force-stop "$PACKAGE" > /dev/null 2>&1 || true
    output=$("${ADB[@]}" shell am start -W -n "$ACTIVITY" 2>&1)
    total_time=$(echo "$output" | grep "TotalTime:" | awk '{print $2}' | tr -d '\r')
    if [ -z "$total_time" ]; then
        echo "Run $i: Failed to parse TotalTime from output:"
        echo "$output"
        exit 1
    fi
    times+=("$total_time")
    echo "Run $i: ${total_time} ms"
done

sorted=($(printf '%s\n' "${times[@]}" | sort -n))
med1=${sorted[4]}
med2=${sorted[5]}
median=$(( (med1 + med2) / 2 ))
max=${sorted[9]}

echo "----------------------------------------"
echo "All 10 runs: ${times[*]}"
echo "Sorted runs: ${sorted[*]}"
echo "Median TotalTime: ${median} ms"
echo "Max TotalTime:    ${max} ms"
echo "----------------------------------------"

if [ "$median" -gt 2000 ]; then
    echo "Cold start FAIL: Median ($median ms) exceeds 2000 ms budget!"
    if [ "$ADVISORY" = "1" ]; then
        echo "ADVISORY=1 set; exiting with 0 despite exceeding budget."
        exit 0
    else
        exit 1
    fi
fi

echo "Cold start PASS: Median ($median ms) <= 2000 ms budget."
exit 0
