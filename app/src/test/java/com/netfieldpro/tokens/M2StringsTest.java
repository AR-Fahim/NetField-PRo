package com.netfieldpro.tokens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.netfieldpro.core.entitlement.Feature;

@RunWith(RobolectricTestRunner.class)
public class M2StringsTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testAll27FeatureStringsExistAndNonEmpty() {
        assertEquals(27, Feature.values().length);
        for (Feature f : Feature.values()) {
            String resName = "feature_" + f.name().toLowerCase(Locale.ROOT);
            int resId = context.getResources().getIdentifier(resName, "string", context.getPackageName());
            assertNotEquals("String identifier not found for " + resName, 0, resId);
            String val = context.getString(resId);
            assertNotNull(val);
            assertNotEquals("", val.trim());
        }
    }

    @Test
    public void testTiersAndLockStrings() {
        assertStringEquals("Free", "tier_free");
        assertStringEquals("Pro", "tier_pro");
        assertStringEquals("Enterprise", "tier_enterprise");
        assertStringEquals("Plan: %1$s. Opens plan details.", "tier_badge_desc");
        assertStringEquals("Requires %1$s", "lock_requires");
    }

    @Test
    public void testPaywallStrings() {
        assertStringEquals("Your plan: %1$s", "paywall_your_plan");
        assertStringEquals("Choose a plan", "paywall_choose_plan");
        assertStringEquals("Already included in your plan", "paywall_included");
        assertStringEquals("Continue", "paywall_continue");
        assertStringEquals("Upgrade not available in this build", "paywall_cta_unavailable");
        assertStringEquals("Not now", "paywall_not_now");
        assertStringEquals("Free", "paywall_col_free");
        assertStringEquals("Pro", "paywall_col_pro");
        assertStringEquals("Enterprise", "paywall_col_ent");
        assertStringEquals("Included", "paywall_yes");
        assertStringEquals("Not included", "paywall_no");
    }

    @Test
    public void testComparisonRowStrings() {
        assertStringEquals("Capture, heatmaps, CSV", "paywall_row_capture");
        assertStringEquals("Verify any report", "paywall_row_verify");
        assertStringEquals("Verdicts, signed reports, QR", "paywall_row_sign");
        assertStringEquals("Office, CCTV, Smart Home presets and custom profiles", "paywall_row_presets");
        assertStringEquals("Roaming audit", "paywall_row_roaming");
        assertStringEquals("Warehouse robotics profile", "paywall_row_warehouse");
    }

    @Test
    public void testSettingsStrings() {
        assertStringEquals("Plan", "settings_section_plan");
        assertStringEquals("Appearance", "settings_section_appearance");
        assertStringEquals("Device", "settings_section_device");
        assertStringEquals("Profiles", "settings_section_profiles");
        assertStringEquals("About", "settings_section_about");
        assertStringEquals("Current plan", "settings_current_plan");
        assertStringEquals("Manage plan", "settings_manage_plan");
        assertStringEquals("Theme", "settings_theme");
        assertStringEquals("System default", "theme_system");
        assertStringEquals("Light", "theme_light");
        assertStringEquals("Dark", "theme_dark");
        assertStringEquals("Heatmap colors", "settings_palette");
        assertStringEquals("Traffic light (default)", "palette_traffic");
        assertStringEquals("Color-vision safe (viridis)", "palette_viridis");
        assertStringEquals("Diagnostics", "settings_diagnostics");
        assertStringEquals("Custom profiles", "settings_custom_profiles");
        assertStringEquals("Version", "settings_version");
    }

    @Test
    public void testFeatureTitleStringsExactValues() {
        assertStringEquals("Tap-to-measure survey", "feature_tap_capture");
        assertStringEquals("Walking path survey", "feature_path_capture");
        assertStringEquals("Heatmap layers", "feature_heat_layers");
        assertStringEquals("Coverage contours", "feature_contours");
        assertStringEquals("Gap Finder", "feature_gap_finder");
        assertStringEquals("Floor boundary", "feature_boundary_tool");
        assertStringEquals("Wall tool", "feature_wall_tool");
        assertStringEquals("Access point groups", "feature_ap_groups");
        assertStringEquals("CSV export", "feature_csv_export");
        assertStringEquals("Sample coverage summary", "feature_sample_coverage_summary");
        assertStringEquals("Diagnostics", "feature_diagnostics");
        assertStringEquals("Verify a report", "feature_verify_report");
        assertStringEquals("Share an existing report", "feature_share_existing_report");
        assertStringEquals("Watermarked sample PDF", "feature_watermarked_pdf");
        assertStringEquals("Verdict preview", "feature_verdict_preview");
        assertStringEquals("Finalize and sign", "feature_finalize_sign");
        assertStringEquals("Clean PDF report", "feature_clean_pdf");
        assertStringEquals("Report branding", "feature_branding");
        assertStringEquals("QR integrity seal", "feature_qr_seal");
        assertStringEquals("Evidence package (.zip)", "feature_zip_package");
        assertStringEquals("Office standard profile", "feature_preset_office");
        assertStringEquals("CCTV surveillance profile", "feature_preset_cctv");
        assertStringEquals("Smart home IoT profile", "feature_preset_smart_home");
        assertStringEquals("Custom profiles", "feature_custom_profiles");
        assertStringEquals("Re-assessments", "feature_re_assessments");
        assertStringEquals("Roaming audit", "feature_roaming_audit");
        assertStringEquals("Warehouse robotics profile", "feature_preset_warehouse");
    }

    @Test
    public void testDebugStrings() {
        assertStringEquals("[DEV] Simulate %1$s purchase", "paywall_dev_simulate");
        assertStringEquals("Feature gate lab", "gatelab_title");
        assertStringEquals("Try", "gatelab_try");
        assertStringEquals("✓ Allowed", "gatelab_allowed");
        assertStringEquals("— n/a", "gatelab_na");
        assertStringEquals("Allowed: %1$s", "gatelab_try_result");
    }

    private void assertStringEquals(String expected, String name) {
        int resId = context.getResources().getIdentifier(name, "string", context.getPackageName());
        assertNotEquals("Resource not found: " + name, 0, resId);
        assertEquals("Mismatch for " + name, expected, context.getString(resId));
    }
}

