package com.netfieldpro.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.AccessPointMarker;
import com.netfieldpro.core.model.entity.ApGroup;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.entity.FloorBoundary;
import com.netfieldpro.core.model.entity.TargetNetwork;
import com.netfieldpro.core.model.entity.Wall;
import com.netfieldpro.core.model.enums.ApGroupOrigin;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.model.serial.AccessPointMarkerSerializer;
import com.netfieldpro.core.model.serial.ApGroupSerializer;
import com.netfieldpro.core.model.serial.AssessmentSerializer;
import com.netfieldpro.core.model.serial.FloorBoundarySerializer;
import com.netfieldpro.core.model.serial.TargetNetworkSerializer;
import com.netfieldpro.core.model.serial.WallSerializer;

public class GroupCTest {

    private static final String ID_A = "AAAAAAAAAAAAAAAAAAAAAA";
    private static final String ID_B = "BBBBBBBBBBBBBBBBBBBBBB";
    private static final String ID_C = "CCCCCCCCCCCCCCCCCCCCCC";
    private static final String DUMMY_PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String DUMMY_SIG = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    @Test
    public void testRegistryCompleteness() {
        assertEquals(20, EntityRegistry.ALL.size());

        File entityDir = new File("src/main/java/com/netfieldpro/core/model/entity");
        if (!entityDir.exists()) {
            entityDir = new File("core/src/main/java/com/netfieldpro/core/model/entity");
        }
        assertTrue("Entity directory must exist", entityDir.exists() && entityDir.isDirectory());

        File[] files = entityDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertEquals("Exactly 20 entity java files expected", 20, files.length);

        Set<String> fileClassNames = Arrays.stream(files)
                .map(f -> f.getName().replace(".java", ""))
                .collect(Collectors.toSet());

        Set<String> registryNames = EntityRegistry.ALL.stream()
                .map(EntitySpec::name)
                .collect(Collectors.toSet());

        assertEquals("Simple names must match registry names", registryNames, fileClassNames);
    }

    @Test
    public void testInvariants() {
        // AccessPointMarker
        assertThrowsIllegalArgument(() -> new AccessPointMarker("invalid", ID_A, 0, 0, null, null));
        assertThrowsIllegalArgument(() -> new AccessPointMarker(ID_A, "invalid", 0, 0, null, null));
        assertThrowsIllegalArgument(() -> new AccessPointMarker(ID_A, ID_A, Double.NaN, 0, null, null));
        assertThrowsIllegalArgument(() -> new AccessPointMarker(ID_A, ID_A, 0, Double.POSITIVE_INFINITY, null, null));
        assertThrowsIllegalArgument(() -> new AccessPointMarker(ID_A, ID_A, 0, 0, "x".repeat(1025), null));

        // TargetNetwork
        assertThrowsIllegalArgument(() -> new TargetNetwork("invalid", ID_A, "SSID"));
        assertThrowsIllegalArgument(() -> new TargetNetwork(ID_A, "invalid", "SSID"));
        assertThrowsIllegalArgument(() -> new TargetNetwork(ID_A, ID_A, null));

        // ApGroup
        assertThrowsIllegalArgument(() -> new ApGroup("invalid", ID_A, "lbl", ApGroupOrigin.USER, List.of()));
        assertThrowsIllegalArgument(() -> new ApGroup(ID_A, "invalid", "lbl", ApGroupOrigin.USER, List.of()));
        assertThrowsIllegalArgument(() -> new ApGroup(ID_A, ID_A, null, ApGroupOrigin.USER, List.of()));
        assertThrowsIllegalArgument(() -> new ApGroup(ID_A, ID_A, "lbl", null, List.of()));
        assertThrowsIllegalArgument(() -> new ApGroup(ID_A, ID_A, "lbl", ApGroupOrigin.USER, null));

        // FloorBoundary
        assertThrowsIllegalArgument(() -> new FloorBoundary("invalid", List.of()));
        assertThrowsIllegalArgument(() -> new FloorBoundary(ID_A, null));
        assertThrowsIllegalArgument(() -> new FloorBoundary.Vertex(Double.NaN, 0));
        assertThrowsIllegalArgument(() -> new FloorBoundary.Vertex(0, Double.POSITIVE_INFINITY));

        // Wall
        assertThrowsIllegalArgument(() -> new Wall("invalid", ID_A, MaterialId.DRYWALL_LIGHT, List.of()));
        assertThrowsIllegalArgument(() -> new Wall(ID_A, "invalid", MaterialId.DRYWALL_LIGHT, List.of()));
        assertThrowsIllegalArgument(() -> new Wall(ID_A, ID_A, null, List.of(new Wall.Vertex(0, 0), new Wall.Vertex(1, 1))));
        assertThrowsIllegalArgument(() -> new Wall(ID_A, ID_A, MaterialId.DRYWALL_LIGHT, null));
        assertThrowsIllegalArgument(() -> new Wall(ID_A, ID_A, MaterialId.DRYWALL_LIGHT, List.of(new Wall.Vertex(0, 0)))); // < 2 vertices
        assertThrowsIllegalArgument(() -> new Wall.Vertex(Double.NaN, 0));
        assertThrowsIllegalArgument(() -> new Wall.Vertex(0, Double.POSITIVE_INFINITY));

        // Assessment
        assertThrowsIllegalArgument(() -> new Assessment("invalid", "hash", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 0L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG));
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, null, ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 0L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG));
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", "invalid", new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 0L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG));
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, null, ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 0L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG));
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, new JsonObject(), "invalid", false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 0L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG));
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, new JsonObject(), ID_A, false, null, new JsonObject(), new JsonArray(), Verdict.PASS, 0L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG));
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, new JsonObject(), ID_A, false, new JsonObject(), null, new JsonArray(), Verdict.PASS, 0L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG));
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), null, Verdict.PASS, 0L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG));
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), null, 0L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG));
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, -1L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG));
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 0L, null, 0.5, DUMMY_PUBKEY, DUMMY_SIG));
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 0L, "1.0", 0.0, DUMMY_PUBKEY, DUMMY_SIG)); // gridG <= 0
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 0L, "1.0", -0.5, DUMMY_PUBKEY, DUMMY_SIG)); // gridG < 0
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 0L, "1.0", 0.5, "short", DUMMY_SIG)); // pubkey != 44
        assertThrowsIllegalArgument(() -> new Assessment(ID_A, "hash", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 0L, "1.0", 0.5, DUMMY_PUBKEY, "short")); // sig != 86
    }

    @Test
    public void testSorting() {
        // ApGroup sort by label then id
        ApGroup g1 = new ApGroup(ID_A, ID_B, "Group B", ApGroupOrigin.USER, List.of("00:11:22:33:44:55"));
        ApGroup g2 = new ApGroup(ID_B, ID_B, "Group A", ApGroupOrigin.USER, List.of("00:11:22:33:44:55"));
        ApGroup g3 = new ApGroup(ID_C, ID_B, "Group B", ApGroupOrigin.USER, List.of("00:11:22:33:44:55"));

        List<ApGroup> list = new ArrayList<>(List.of(g1, g2, g3));
        list.sort(EntityRegistry.AP_GROUP.sortComparator());
        assertEquals(List.of(g2, g1, g3), list);

        // Assessment sort by assessedAtMs then id
        Assessment a1 = new Assessment(ID_C, "h", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 200L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG);
        Assessment a2 = new Assessment(ID_A, "h", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 100L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG);
        Assessment a3 = new Assessment(ID_B, "h", ID_A, new JsonObject(), ID_A, false, new JsonObject(), new JsonObject(), new JsonArray(), Verdict.PASS, 200L, "1.0", 0.5, DUMMY_PUBKEY, DUMMY_SIG);

        List<Assessment> aList = new ArrayList<>(List.of(a1, a2, a3));
        aList.sort(EntityRegistry.ASSESSMENT.sortComparator());
        assertEquals(List.of(a2, a3, a1), aList); // 100L (a2) then 200L with ID_B (a3) < ID_C (a1)
    }

    @Test
    public void testKeyOrderAndRoundTrip() throws Exception {
        AccessPointMarker apm = EntityRegistry.ACCESS_POINT_MARKER.sampleFactory().get();
        JsonObject jApm = AccessPointMarkerSerializer.toJson(apm);
        assertStrictKeyOrder(jApm);
        assertEquals(apm, AccessPointMarkerSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jApm))));

        TargetNetwork tn = EntityRegistry.TARGET_NETWORK.sampleFactory().get();
        JsonObject jTn = TargetNetworkSerializer.toJson(tn);
        assertStrictKeyOrder(jTn);
        assertEquals(tn, TargetNetworkSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jTn))));

        ApGroup apg = EntityRegistry.AP_GROUP.sampleFactory().get();
        JsonObject jApg = ApGroupSerializer.toJson(apg);
        assertStrictKeyOrder(jApg);
        assertEquals(apg, ApGroupSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jApg))));

        FloorBoundary fb = EntityRegistry.FLOOR_BOUNDARY.sampleFactory().get();
        JsonObject jFb = FloorBoundarySerializer.toJson(fb);
        assertStrictKeyOrder(jFb);
        assertEquals(fb, FloorBoundarySerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jFb))));

        Wall w = EntityRegistry.WALL.sampleFactory().get();
        JsonObject jW = WallSerializer.toJson(w);
        assertStrictKeyOrder(jW);
        assertEquals(w, WallSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jW))));

        Assessment as = EntityRegistry.ASSESSMENT.sampleFactory().get();
        JsonObject jAs = AssessmentSerializer.toJson(as);
        assertStrictKeyOrder(jAs);
        assertFalse("Assessment serialized JSON must NOT contain sessionId", jAs.properties().containsKey("sessionId"));
        assertEquals(as, AssessmentSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jAs))));
    }

    private static void assertStrictKeyOrder(JsonObject obj) {
        String prev = null;
        for (Map.Entry<String, JsonValue> entry : obj.properties().entrySet()) {
            if (prev != null) {
                assertTrue("Keys not in strict UTF-16 ascending order: " + prev + " >= " + entry.getKey(),
                        prev.compareTo(entry.getKey()) < 0);
            }
            prev = entry.getKey();
            if (entry.getValue() instanceof JsonObject nested) {
                assertStrictKeyOrder(nested);
            }
        }
    }

    private static void assertThrowsIllegalArgument(Runnable r) {
        try {
            r.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(": "));
        }
    }
}
