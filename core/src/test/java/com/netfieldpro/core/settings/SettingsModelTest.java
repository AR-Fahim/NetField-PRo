package com.netfieldpro.core.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SettingsModelTest {

    @Test
    public void testAS1_defaults() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parseOrDefault(null));
        assertEquals(HeatPalette.TRAFFIC_LIGHT, HeatPalette.parseOrDefault(null));
    }

    @Test
    public void testAS2_parsing() {
        assertEquals(ThemeMode.DARK, ThemeMode.parseOrDefault("DARK"));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parseOrDefault("dark"));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parseOrDefault("garbage"));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parseOrDefault(null));

        assertEquals(HeatPalette.VIRIDIS, HeatPalette.parseOrDefault("VIRIDIS"));
        assertEquals(HeatPalette.TRAFFIC_LIGHT, HeatPalette.parseOrDefault("viridis"));
        assertEquals(HeatPalette.TRAFFIC_LIGHT, HeatPalette.parseOrDefault("unknown"));
        assertEquals(HeatPalette.TRAFFIC_LIGHT, HeatPalette.parseOrDefault(null));
    }
}
