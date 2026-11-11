package com.netfieldpro.wifi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.netfieldpro.core.wifi.EnvironmentAdvisor;
import com.netfieldpro.core.wifi.Normalize;

public class WifiEnvironmentProbe {

    public record ConnectedInfo(
            String bssid,
            String ssid,
            int freqMHz,
            int rssiDbm
    ) {}

    private final Context context;
    private final WifiManager wifiManager;
    private final LocationManager locationManager;

    public WifiEnvironmentProbe(Context context) {
        this.context = context.getApplicationContext();
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        this.locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean hasLocationPermission() {
        int fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLocationServicesEnabled() {
        if (locationManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            int mode = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF
            );
            return mode != Settings.Secure.LOCATION_MODE_OFF;
        }
    }

    public boolean isWifiEnabled() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    public EnvironmentAdvisor.ThrottleState isScanThrottleEnabled() {
        try {
            if (Build.VERSION.SDK_INT >= 34 && wifiManager != null) {
                return wifiManager.isScanThrottleEnabled()
                        ? EnvironmentAdvisor.ThrottleState.ON
                        : EnvironmentAdvisor.ThrottleState.OFF;
            }
            int val = Settings.Global.getInt(context.getContentResolver(), "wifi_scan_throttle_enabled");
            return val == 1
                    ? EnvironmentAdvisor.ThrottleState.ON
                    : EnvironmentAdvisor.ThrottleState.OFF;
        } catch (Exception e) {
            return EnvironmentAdvisor.ThrottleState.UNKNOWN;
        }
    }

    public ConnectedInfo getConnectedInfo() {
        if (wifiManager == null) {
            return null;
        }
        try {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info == null) {
                return null;
            }
            String bssid = Normalize.bssid(info.getBSSID());
            String ssid = Normalize.connectedSsid(info.getSSID());
            if (bssid == null) {
                return null;
            }
            return new ConnectedInfo(bssid, ssid, info.getFrequency(), info.getRssi());
        } catch (Exception e) {
            return null;
        }
    }

    public EnvironmentAdvisor.Advice getAdvice() {
        return EnvironmentAdvisor.advise(
                hasLocationPermission(),
                isLocationServicesEnabled(),
                isWifiEnabled(),
                isScanThrottleEnabled()
        );
    }
}
