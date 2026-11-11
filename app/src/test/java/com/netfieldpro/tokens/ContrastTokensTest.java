package com.netfieldpro.tokens;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import com.netfieldpro.core.color.Contrast;

public class ContrastTokensTest {

    private void checkPairs(Map<String, String> colors, String modeName) {
        int onSurface = Contrast.parseHex(colors.get("nf_on_surface"));
        int surface = Contrast.parseHex(colors.get("nf_surface"));
        int background = Contrast.parseHex(colors.get("nf_background"));
        int onSurfaceVariant = Contrast.parseHex(colors.get("nf_on_surface_variant"));
        int primary = Contrast.parseHex(colors.get("nf_primary"));
        int primaryContainer = Contrast.parseHex(colors.get("nf_primary_container"));
        int onPrimary = Contrast.parseHex(colors.get("nf_on_primary"));
        int error = Contrast.parseHex(colors.get("nf_error"));
        int warning = Contrast.parseHex(colors.get("nf_warning"));
        int info = Contrast.parseHex(colors.get("nf_info"));

        assertRatio(modeName + " onSurface/surface", onSurface, surface);
        assertRatio(modeName + " onSurface/background", onSurface, background);

        assertRatio(modeName + " onSurfaceVariant/surface", onSurfaceVariant, surface);
        assertRatio(modeName + " onSurfaceVariant/background", onSurfaceVariant, background);

        assertRatio(modeName + " primary/surface", primary, surface);
        assertRatio(modeName + " primary/primaryContainer", primary, primaryContainer);

        assertRatio(modeName + " onPrimary/primary", onPrimary, primary);

        assertRatio(modeName + " error/surface", error, surface);
        assertRatio(modeName + " warning/surface", warning, surface);
        assertRatio(modeName + " info/surface", info, surface);

        assertRatio(modeName + " onSurface/primaryContainer", onSurface, primaryContainer);
    }

    private void assertRatio(String label, int c1, int c2) {
        double r = Contrast.ratio(c1, c2);
        assertTrue(label + " ratio was " + r + " expected >= 4.5", r >= 4.5);
    }

    @Test
    public void testLightModeContrast() throws Exception {
        File f = TokensTest.findFile("src/main/res/values/colors.xml");
        Map<String, String> colors = TokensTest.parseXmlMap(f, "color", "name");
        checkPairs(colors, "Light");
    }

    @Test
    public void testDarkModeContrast() throws Exception {
        File f = TokensTest.findFile("src/main/res/values-night/colors.xml");
        Map<String, String> colors = TokensTest.parseXmlMap(f, "color", "name");
        checkPairs(colors, "Dark");
    }
}

