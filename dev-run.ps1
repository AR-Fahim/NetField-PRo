#!/usr/bin/env -S powershell.exe -ExecutionPolicy Bypass -File
Write-Host "Building debug APK..."

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if ($scriptDir) { Set-Location $scriptDir }

$gradleCmd = if ($IsWindows -ne $false) { ".\gradlew.bat" } else { "./gradlew" }
& $gradleCmd assembleDebug

if ($LASTEXITCODE -eq 0) {

    $apk = Join-Path $scriptDir "app\build\outputs\apk\debug\app-debug.apk"
    if (!(Test-Path $apk)) {
        $apk = ".\app\build\outputs\apk\debug\app-debug.apk"
    }
    if (!(Test-Path $apk)) {
        Write-Error "APK not found at $apk"
        exit 1
    }

    $deviceLines = adb devices | Select-String "\sdevice$"
    $devices = @($deviceLines | ForEach-Object { (($_ -replace "^\s+", "") -split "\s+")[0] } | Where-Object { $_ -ne "" })

    if ($devices.Count -eq 0) {
        Write-Warning "No active devices or emulators found."
        exit 0
    }

    foreach ($device in $devices) {
        Write-Host "Installing on $device..."
        adb -s $device install -r -d -t $apk

        if ($LASTEXITCODE -eq 0) {
            Write-Host "Launching on $device..."
            adb -s $device shell am start -n com.netfieldpro/.MainActivity
        } else {
            Write-Error "Failed to install on $device"
        }
    }
} else {
    Write-Error "Build failed."
}
