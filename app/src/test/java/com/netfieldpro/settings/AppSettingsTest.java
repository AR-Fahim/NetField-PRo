package com.netfieldpro.settings;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.core.settings.HeatPalette;
import com.netfieldpro.core.settings.ThemeMode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AppSettingsTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(AppSettings.PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void testAS1_defaults() {
        AppSettings settings = new AppSettings(context);
        assertEquals(ThemeMode.SYSTEM, settings.getThemeMode());
        assertEquals(HeatPalette.TRAFFIC_LIGHT, settings.getHeatPalette());
    }

    @Test
    public void testAS2_parsing() {
        assertEquals(ThemeMode.DARK, ThemeMode.parseOrDefault("DARK"));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parseOrDefault("dark"));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parseOrDefault("garbage"));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parseOrDefault(null));

        assertEquals(HeatPalette.VIRIDIS, HeatPalette.parseOrDefault("VIRIDIS"));
        assertEquals(HeatPalette.TRAFFIC_LIGHT, HeatPalette.parseOrDefault("garbage"));
        assertEquals(HeatPalette.TRAFFIC_LIGHT, HeatPalette.parseOrDefault(null));
    }

    @Test
    public void testAS3_roundTripSurvivesFreshInstance() {
        AppSettings first = new AppSettings(context);
        first.setThemeMode(ThemeMode.DARK);
        first.setHeatPalette(HeatPalette.VIRIDIS);

        AppSettings second = new AppSettings(context);
        assertEquals(ThemeMode.DARK, second.getThemeMode());
        assertEquals(HeatPalette.VIRIDIS, second.getHeatPalette());
    }
}
