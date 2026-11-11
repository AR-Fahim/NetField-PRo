#!/usr/bin/env bash
set -euo pipefail

if [ -n "${DEVICES:-}" ]; then
    read -r -a dev_targets <<< "$DEVICES"
elif [ -n "${AVDS:-}" ]; then
    read -r -a dev_targets <<< "$AVDS"
else
    dev_targets=($(adb devices | tr -d '\r' | awk '$2=="device"{print $1}'))
fi

if [ ${#dev_targets[@]} -eq 0 ]; then
    echo "No attached devices found." >&2
    exit 1
fi

known_avds=()
while IFS= read -r line; do
    trimmed=$(echo "$line" | tr -d '\r' | xargs)
    if [ -n "$trimmed" ]; then
        known_avds+=("$trimmed")
    fi
done < <(emulator -list-avds 2>/dev/null || true)

mkdir -p app/build/outputs/androidTest-results-all
rm -rf app/build/outputs/androidTest-results-all/*
has_failure=0

for target in "${dev_targets[@]}"; do
    is_avd=0
    for a in "${known_avds[@]}"; do
        if [ "$target" = "$a" ]; then
            is_avd=1
            break
        fi
    done

    emu_pid=""
    if [ "$is_avd" -eq 1 ]; then
        emu_port=""
        if [ "$target" = "High_API35" ]; then
            emu_port="-port 5556"
            expected_serial="emulator-5556"
        else
            emu_port="-port 5554"
            expected_serial="emulator-5554"
        fi
        echo "=== Launching AVD: $target headless ($expected_serial) ==="
        emulator -avd "$target" $emu_port -no-window -no-audio -no-snapshot-save -no-boot-anim &
        emu_pid=$!
        while true; do
            boot_done=$(adb -s "$expected_serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
            if [ "$boot_done" = "1" ]; then
                dev="$expected_serial"
                break
            fi
            sleep 2
        done
    else
        dev="$target"
    fi

    # Run preflight
    preflight_out=$(bash scripts/device-preflight.sh "$dev")
    echo "$preflight_out"

    if [ -n "${EVIDENCE_DIR:-}" ]; then
        echo "$preflight_out" > "$EVIDENCE_DIR/preflight-$dev.txt"
        if [ "$target" != "$dev" ]; then
            echo "$preflight_out" > "$EVIDENCE_DIR/preflight-$target.txt"
        fi
    fi

    dev_class=$(echo "$preflight_out" | grep "^PREFLIGHT $dev" | sed -n 's/.*class=\([^ ]*\).*/\1/p')
    if [ "$dev_class" = "PHYSICAL" ]; then
        if [ "${WIPE_OK:-0}" != "1" ]; then
            echo "REFUSING: physical device would be wiped. Re-run with WIPE_OK=1"
            exit 2
        else
            echo "PREFLIGHT WIPE_WARNING app data of com.netfieldpro will be erased by tests (WIPE_OK=1 given)"
            if [ -n "${EVIDENCE_DIR:-}" ]; then
                echo "PREFLIGHT WIPE_WARNING app data of com.netfieldpro will be erased by tests (WIPE_OK=1 given)" >> "$EVIDENCE_DIR/preflight-$dev.txt"
                if [ "$target" != "$dev" ]; then
                    echo "PREFLIGHT WIPE_WARNING app data of com.netfieldpro will be erased by tests (WIPE_OK=1 given)" >> "$EVIDENCE_DIR/preflight-$target.txt"
                fi
            fi
        fi
    fi

    # Scale settings and location mode
    adb -s "$dev" shell settings put global window_animation_scale 0
    adb -s "$dev" shell settings put global transition_animation_scale 0
    adb -s "$dev" shell settings put global animator_duration_scale 0
    adb -s "$dev" shell settings put secure location_mode 3

    # Clear logcat before test run for facts logging
    adb -s "$dev" logcat -c 2>/dev/null || true

    code=0
    ANDROID_SERIAL="$dev" ./gradlew :app:connectedOpenDebugAndroidTest || code=$?
    if [ "$code" -ne 0 ]; then
        has_failure=1
    fi

    # Collect facts if EVIDENCE_DIR is set
    if [ -n "${EVIDENCE_DIR:-}" ]; then
        adb -s "$dev" logcat -d -s NF_FACT:I > "$EVIDENCE_DIR/facts-$dev.txt" 2>/dev/null || true
        if [ "$target" != "$dev" ]; then
            cp "$EVIDENCE_DIR/facts-$dev.txt" "$EVIDENCE_DIR/facts-$target.txt" 2>/dev/null || true
        fi
    fi

    cp app/build/outputs/androidTest-results/connected/debug/TEST-*.xml app/build/outputs/androidTest-results-all/ 2>/dev/null || true

    if [ "$is_avd" -eq 1 ] && [ -n "$emu_pid" ]; then
        adb -s "$dev" emu kill >/dev/null 2>&1 || true
        wait "$emu_pid" 2>/dev/null || true
        sleep 3
    fi
done

mkdir -p app/build/outputs/androidTest-results/connected/debug
cp app/build/outputs/androidTest-results-all/TEST-*.xml app/build/outputs/androidTest-results/connected/debug/ 2>/dev/null || true
rm -rf app/build/outputs/androidTest-results-all

counts_status=0
python scripts/test-counts.py || counts_status=$?

if [ "$has_failure" -ne 0 ]; then
    echo "One or more devices failed." >&2
    exit 1
fi

if [ "$counts_status" -ne 0 ]; then
    echo "test-counts.py failed." >&2
    exit "$counts_status"
fi
