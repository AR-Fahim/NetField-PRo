package com.netfieldpro.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.netfieldpro.core.settings.HeatPalette;
import com.netfieldpro.core.settings.ThemeMode;

public class AppSettings {

    public static final String PREF_NAME = "nf_settings";
    public static final String KEY_THEME_MODE = "theme_mode";
    public static final String KEY_HEAT_PALETTE = "heat_palette";

    private final SharedPreferences prefs;

    public static AppSettings get(Context context) {
        return new AppSettings(context);
    }

    public AppSettings(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public AppSettings(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public ThemeMode getTheme() {
        return getThemeMode();
    }

    public void setTheme(ThemeMode mode) {
        setThemeMode(mode);
    }

    public ThemeMode getThemeMode() {
        String s = prefs.getString(KEY_THEME_MODE, null);
        return ThemeMode.parseOrDefault(s);
    }

    public void setThemeMode(ThemeMode mode) {
        ThemeMode m = (mode != null) ? mode : ThemeMode.SYSTEM;
        prefs.edit().putString(KEY_THEME_MODE, m.name()).apply();
    }

    public HeatPalette getHeatPalette() {
        String s = prefs.getString(KEY_HEAT_PALETTE, null);
        return HeatPalette.parseOrDefault(s);
    }

    public void setHeatPalette(HeatPalette palette) {
        HeatPalette p = (palette != null) ? palette : HeatPalette.TRAFFIC_LIGHT;
        prefs.edit().putString(KEY_HEAT_PALETTE, p.name()).apply();
    }
}
