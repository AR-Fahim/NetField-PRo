package com.netfieldpro.core.wifi;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Normalize {
    private Normalize() {}

    private static final Pattern BSSID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){5}$");

    public static String bssid(String raw) {
        if (raw == null || raw.length() != 17 || !BSSID_PATTERN.matcher(raw).matches()) {
            return null;
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if ("02:00:00:00:00:00".equals(lower)) {
            return null;
        }
        return lower;
    }

    public static String scanSsid(String raw) {
        return raw == null ? "" : raw;
    }

    public static String connectedSsid(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw;
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        if (s.isEmpty() || "<unknown ssid>".equals(s)) {
            return null;
        }
        return s;
    }

    public static Long scanResultTsToNs(long tsMicros) {
        if (tsMicros <= 0) {
            return null;
        }
        return tsMicros * 1000L;
    }
}
