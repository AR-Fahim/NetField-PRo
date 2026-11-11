package com.netfieldpro.tokens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Map;

import org.junit.Test;

public class CanonicalStringsTest {

    @Test
    public void testCanonicalStrings() throws Exception {
        File f = TokensTest.findFile("src/main/res/values/strings.xml");
        Map<String, String> strings = TokensTest.parseXmlMap(f, "string", "name");

        assertEquals("Network Performance Audit", resolve(strings.get("report_title")));
        assertEquals("UNVERIFIED SAMPLE — NOT FOR CONTRACTUAL AUDIT", resolve(strings.get("watermark")));
        assertEquals("Verdict-grade evidence", resolve(strings.get("grade_verdict")));
        assertEquals("Indicative only (scan throttling ON)", resolve(strings.get("grade_indicative")));
        assertEquals("Re-assessment (survey intended for %1$s)", resolve(strings.get("reassess_label")));
        assertEquals("Signature consistent. Signer not independently identified.", resolve(strings.get("verify_l1_ok")));
        assertEquals("Full verification passed. Evidence and signing key confirmed.", resolve(strings.get("verify_l2_ok")));
        assertEquals("Evidence intact. Device attestation unavailable on this phone.", resolve(strings.get("verify_l2_noattest")));
        assertEquals("Verification failed. Do not rely on this report.", resolve(strings.get("verify_bad")));
        assertEquals("Sample coverage summary (not a verdict)", resolve(strings.get("free_summary_title")));
        assertEquals("Visual interpolation of measured points", resolve(strings.get("interp_note")));
        assertEquals("Measures the technician's phone, not your devices.", resolve(strings.get("roam_disclaimer")));
        assertEquals("Finalized (device clock): %1$s", resolve(strings.get("date_label")));
    }

    private String resolve(String xmlVal) {
        assertNotNull(xmlVal);
        return xmlVal.replace("\\'", "'");
    }
}

