package com.netfieldpro.core.settings;

public enum HeatPalette {
    TRAFFIC_LIGHT,
    VIRIDIS;

    public static HeatPalette parseOrDefault(String s) {
        if (s == null) {
            return TRAFFIC_LIGHT;
        }
        for (HeatPalette palette : values()) {
            if (palette.name().equals(s)) {
                return palette;
            }
        }
        return TRAFFIC_LIGHT;
    }
}
