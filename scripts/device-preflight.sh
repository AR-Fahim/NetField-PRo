#!/usr/bin/env bash
set -euo pipefail

serial="${1:-}"
if [ -z "$serial" ]; then
    echo "Usage: scripts/device-preflight.sh <serial>" >&2
    exit 1
fi

model=$(adb -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
sdk=$(adb -s "$serial" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')
qemu=$(adb -s "$serial" shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r')

if [ "$qemu" = "1" ]; then
    dev_class="EMULATOR"
else
    dev_class="PHYSICAL"
fi

echo "PREFLIGHT $serial model=$model sdk=$sdk class=$dev_class"

# 2. wifi status from cmd wifi status
# (first line contains "Wifi is enabled"/"Wifi is disabled"; connected=yes iff a line contains "connected to"). Never print the SSID.
wifi_out=$(adb -s "$serial" shell cmd wifi status 2>/dev/null || true)
first_wifi_line=$(echo "$wifi_out" | head -n 1 | tr -d '\r')

if echo "$first_wifi_line" | grep -qi "Wifi is enabled"; then
    wifi_state="enabled"
elif echo "$first_wifi_line" | grep -qi "Wifi is disabled"; then
    wifi_state="disabled"
else
    wifi_state="disabled"
fi

if echo "$wifi_out" | grep -q "connected to"; then
    connected="yes"
else
    connected="no"
fi

echo "PREFLIGHT wifi=$wifi_state connected=$connected"

# 3. location_mode, throttle_setting and wakefulness
loc_mode=$(adb -s "$serial" shell settings get secure location_mode 2>/dev/null | tr -d '\r')
throttle=$(adb -s "$serial" shell settings get global wifi_scan_throttle_enabled 2>/dev/null | tr -d '\r')

if [ -z "$loc_mode" ]; then
    loc_mode="null"
fi
if [ -z "$throttle" ]; then
    throttle="null"
fi

wake_line=$(adb -s "$serial" shell dumpsys power 2>/dev/null | grep -m 1 "mWakefulness=" | tr -d '\r' || true)
wakefulness="unknown"
if [ -n "$wake_line" ]; then
    wakefulness=$(echo "$wake_line" | sed 's/.*mWakefulness=//' | awk '{print $1}')
fi

echo "PREFLIGHT location_mode=$loc_mode throttle_setting=$throttle wakefulness=$wakefulness"

# Exit 3 under D5:
# D5 PREFLIGHT blocks (exit 3) on a physical device if Wi-Fi is disabled or location_mode is 0, unless PREFLIGHT_ADVISORY=1. On an emulator it is advisory.
if [ "$dev_class" = "PHYSICAL" ] && [ "${PREFLIGHT_ADVISORY:-0}" != "1" ]; then
    if [ "$wifi_state" = "disabled" ] || [ "$loc_mode" = "0" ]; then
        echo "PREFLIGHT BLOCKED: wifi is $wifi_state, location_mode is $loc_mode" >&2
        exit 3
    fi
fi

exit 0
