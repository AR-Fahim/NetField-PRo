package com.netfieldpro.core.entitlement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class EntitlementTest {

    @Test
    public void testEN1_and_EN2() throws Exception {
        // EN-2
        assertEquals(27, Feature.values().length);
        assertEquals(Tier.FREE, Tier.values()[0]);
        assertEquals(0, Tier.FREE.rank());
        assertEquals(Tier.PRO, Tier.values()[1]);
        assertEquals(1, Tier.PRO.rank());
        assertEquals(Tier.ENTERPRISE, Tier.values()[2]);
        assertEquals(2, Tier.ENTERPRISE.rank());

        // EN-1
        InputStream is = getClass().getResourceAsStream("/entitlement_matrix.csv");
        assertNotNull("entitlement_matrix.csv must be present", is);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        }
        assertEquals(28, lines.size());
        assertEquals("feature,FREE,PRO,ENTERPRISE", lines.get(0));

        for (int i = 0; i < 27; i++) {
            String[] parts = lines.get(i + 1).split(",");
            assertEquals(4, parts.length);
            Feature expectedFeature = Feature.values()[i];
            assertEquals(expectedFeature.name(), parts[0]);

            for (int t = 0; t < 3; t++) {
                Tier tier = Tier.values()[t];
                String code = parts[t + 1];
                EntitlementDecision decision = EntitlementPolicy.decide(tier, expectedFeature);
                switch (code) {
                    case "A" -> assertEquals(EntitlementDecision.allowed(), decision);
                    case "LP" -> assertEquals(EntitlementDecision.locked(Tier.PRO), decision);
                    case "LE" -> assertEquals(EntitlementDecision.locked(Tier.ENTERPRISE), decision);
                    case "N" -> assertEquals(EntitlementDecision.notApplicable(), decision);
                    default -> throw new AssertionError("Unknown code in CSV: " + code);
                }
            }
        }
    }

    @Test
    public void testEN3_lockedRequiredTierConsistency() {
        for (Feature f : Feature.values()) {
            for (Tier t : Tier.values()) {
                EntitlementDecision d = EntitlementPolicy.decide(t, f);
                if (d.kind() == DecisionKind.LOCKED) {
                    Tier req = d.requiredTier();
                    assertNotNull("requiredTier cannot be null for LOCKED", req);
                    assertEquals("Required tier must be ALLOWED",
                            EntitlementDecision.allowed(), EntitlementPolicy.decide(req, f));
                    for (Tier lower : Tier.values()) {
                        if (lower.rank() < req.rank()) {
                            assertEquals("Tier lower than requiredTier must be LOCKED",
                                    DecisionKind.LOCKED, EntitlementPolicy.decide(lower, f).kind());
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testEN4_monotonicityExceptWatermarkedPdf() {
        for (Feature f : Feature.values()) {
            if (f == Feature.WATERMARKED_PDF) {
                continue;
            }
            for (Tier t : Tier.values()) {
                if (EntitlementPolicy.decide(t, f).kind() == DecisionKind.ALLOWED) {
                    for (Tier higher : Tier.values()) {
                        if (higher.rank() > t.rank()) {
                            assertEquals("Higher tier must be ALLOWED for feature " + f,
                                    EntitlementDecision.allowed(), EntitlementPolicy.decide(higher, f));
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testEN5_nullArgumentsThrowNpe() {
        assertThrows(NullPointerException.class, () -> EntitlementPolicy.decide(null, Feature.TAP_CAPTURE));
        assertThrows(NullPointerException.class, () -> EntitlementPolicy.decide(Tier.FREE, null));
    }

    @Test
    public void testQU1_to_QU10() {
        // QU-1: FREE, PROJECT, 0 -> allowed, limit 1
        QuotaDecision q1 = QuotaPolicy.canCreate(Tier.FREE, Resource.PROJECT, 0);
        assertTrue(q1.allowed());
        assertEquals(1, q1.limit());
        assertNull(q1.upgradeTo());

        // QU-2: FREE, PROJECT, 1 -> denied, limit 1, upgradeTo PRO
        QuotaDecision q2 = QuotaPolicy.canCreate(Tier.FREE, Resource.PROJECT, 1);
        assertFalse(q2.allowed());
        assertEquals(1, q2.limit());
        assertEquals(Tier.PRO, q2.upgradeTo());

        // QU-3: FREE, FLOOR, 0 -> allowed
        QuotaDecision q3 = QuotaPolicy.canCreate(Tier.FREE, Resource.FLOOR, 0);
        assertTrue(q3.allowed());
        assertEquals(1, q3.limit());
        assertNull(q3.upgradeTo());

        // QU-4: FREE, FLOOR, 1 -> denied
        QuotaDecision q4 = QuotaPolicy.canCreate(Tier.FREE, Resource.FLOOR, 1);
        assertFalse(q4.allowed());
        assertEquals(1, q4.limit());
        assertEquals(Tier.PRO, q4.upgradeTo());

        // QU-5: FREE, SESSION, 1 -> allowed
        QuotaDecision q5 = QuotaPolicy.canCreate(Tier.FREE, Resource.SESSION, 1);
        assertTrue(q5.allowed());
        assertEquals(2, q5.limit());
        assertNull(q5.upgradeTo());

        // QU-6: FREE, SESSION, 2 -> denied
        QuotaDecision q6 = QuotaPolicy.canCreate(Tier.FREE, Resource.SESSION, 2);
        assertFalse(q6.allowed());
        assertEquals(2, q6.limit());
        assertEquals(Tier.PRO, q6.upgradeTo());

        // QU-7: FREE, SESSION, 7 -> denied (lapsed user)
        QuotaDecision q7 = QuotaPolicy.canCreate(Tier.FREE, Resource.SESSION, 7);
        assertFalse(q7.allowed());
        assertEquals(2, q7.limit());
        assertEquals(Tier.PRO, q7.upgradeTo());

        // QU-8: PRO, PROJECT, 1_000_000 -> allowed, limit -1
        QuotaDecision q8 = QuotaPolicy.canCreate(Tier.PRO, Resource.PROJECT, 1_000_000);
        assertTrue(q8.allowed());
        assertEquals(-1, q8.limit());
        assertNull(q8.upgradeTo());

        // QU-9: ENTERPRISE, SESSION, 500 -> allowed
        QuotaDecision q9 = QuotaPolicy.canCreate(Tier.ENTERPRISE, Resource.SESSION, 500);
        assertTrue(q9.allowed());
        assertEquals(-1, q9.limit());
        assertNull(q9.upgradeTo());

        // QU-10: count -1 -> IAE
        assertThrows(IllegalArgumentException.class, () ->
                QuotaPolicy.canCreate(Tier.FREE, Resource.PROJECT, -1));
    }
}
