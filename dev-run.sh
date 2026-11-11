#!/usr/bin/env bash
set -e

echo "Building debug APK..."
./gradlew assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
    echo "Error: APK not found at $APK" >&2
    exit 1
fi

DEVICES=$(adb devices | awk '$2=="device" {print $1}')
if [ -z "$DEVICES" ]; then
    echo "Warning: No active devices or emulators found."
    exit 0
fi

for device in $DEVICES; do
    echo "Installing on $device..."
    adb -s "$device" install -r -d -t "$APK"
    echo "Launching on $device..."
    adb -s "$device" shell am start -n com.netfieldpro/.MainActivity
done
