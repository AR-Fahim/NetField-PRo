package com.netfieldpro.core.settings;

public enum ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    public static ThemeMode parseOrDefault(String s) {
        if (s == null) {
            return SYSTEM;
        }
        for (ThemeMode mode : values()) {
            if (mode.name().equals(s)) {
                return mode;
            }
        }
        return SYSTEM;
    }
}
