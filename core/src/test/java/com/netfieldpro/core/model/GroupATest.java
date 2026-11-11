package com.netfieldpro.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonBool;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.DeviceInfo;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.model.entity.RequirementProfile;
import com.netfieldpro.core.model.entity.SurveySession;
import com.netfieldpro.core.model.entity.WallMaterial;
import com.netfieldpro.core.model.enums.BandPolicy;
import com.netfieldpro.core.model.enums.CadenceFlag;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.model.enums.ProfileKind;
import com.netfieldpro.core.model.enums.ScaleMethod;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.model.enums.SessionType;
import com.netfieldpro.core.model.enums.TimingQuality;
import com.netfieldpro.core.model.serial.DeviceInfoSerializer;
import com.netfieldpro.core.model.serial.FloorSerializer;
import com.netfieldpro.core.model.serial.ProjectSerializer;
import com.netfieldpro.core.model.serial.RequirementProfileSerializer;
import com.netfieldpro.core.model.serial.SurveySessionSerializer;
import com.netfieldpro.core.model.serial.WallMaterialSerializer;

public class GroupATest {

    private static final String ID_A = "AAAAAAAAAAAAAAAAAAAAAA";
    private static final String ID_B = "BBBBBBBBBBBBBBBBBBBBBB";
    private static final String ID_C = "CCCCCCCCCCCCCCCCCCCCCC";
    private static final String ID_D = "DDDDDDDDDDDDDDDDDDDDDD";

    @Test
    public void testGE1_Project() {
        Project p = new Project(ID_A, "Dental Office", null, "HQ", 1700000000000L);
        JsonObject json = ProjectSerializer.toJson(p);
        byte[] bytes = CanonicalJsonWriter.toBytes(json);
        assertEquals("{\"createdAtMs\":1700000000000,\"id\":\"AAAAAAAAAAAAAAAAAAAAAA\",\"name\":\"Dental Office\",\"site\":\"HQ\"}",
                new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testGE4_WallMaterial() {
        WallMaterial wm = WallMaterials.DRYWALL_LIGHT;
        JsonObject json = WallMaterialSerializer.toJson(wm);
        byte[] bytes = CanonicalJsonWriter.toBytes(json);
        assertEquals("{\"lossDb24\":\"3.0\",\"lossDb5\":\"4.5\",\"materialId\":\"DRYWALL_LIGHT\",\"penaltyM\":\"3.0\"}",
                new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testWallMaterialsTable() {
        assertEquals(5, WallMaterials.ALL.size());

        WallMaterial m1 = WallMaterials.DRYWALL_LIGHT;
        assertEquals(MaterialId.DRYWALL_LIGHT, m1.materialId());
        assertEquals(3.0, m1.penaltyM(), 1e-9);
        assertEquals(3.0, m1.lossDb24(), 1e-9);
        assertEquals(4.5, m1.lossDb5(), 1e-9);

        WallMaterial m2 = WallMaterials.GLASS_PARTITION;
        assertEquals(MaterialId.GLASS_PARTITION, m2.materialId());
        assertEquals(2.5, m2.penaltyM(), 1e-9);
        assertEquals(2.5, m2.lossDb24(), 1e-9);
        assertEquals(4.0, m2.lossDb5(), 1e-9);

        WallMaterial m3 = WallMaterials.BRICK_MEDIUM;
        assertEquals(MaterialId.BRICK_MEDIUM, m3.materialId());
        assertEquals(8.0, m3.penaltyM(), 1e-9);
        assertEquals(8.0, m3.lossDb24(), 1e-9);
        assertEquals(12.0, m3.lossDb5(), 1e-9);

        WallMaterial m4 = WallMaterials.CONCRETE_HEAVY;
        assertEquals(MaterialId.CONCRETE_HEAVY, m4.materialId());
        assertEquals(15.0, m4.penaltyM(), 1e-9);
        assertEquals(12.0, m4.lossDb24(), 1e-9);
        assertEquals(18.0, m4.lossDb5(), 1e-9);

        WallMaterial m5 = WallMaterials.METAL_ELEVATOR;
        assertEquals(MaterialId.METAL_ELEVATOR, m5.materialId());
        assertEquals(25.0, m5.penaltyM(), 1e-9);
        assertEquals(20.0, m5.lossDb24(), 1e-9);
        assertEquals(28.0, m5.lossDb5(), 1e-9);
    }

    @Test
    public void testInvariants_GroupA() {
        // Project
        assertThrowsIllegalArgument(() -> new Project("invalid", "Name", null, null, 0L));
        assertThrowsIllegalArgument(() -> new Project(ID_A, null, null, null, 0L));
        assertThrowsIllegalArgument(() -> new Project(ID_A, "Name", null, null, -1L));

        // Floor
        assertThrowsIllegalArgument(() -> new Floor(ID_A, ID_B, 1L, "sha", 100, 100, -1.0, null, ScaleMethod.LINE, 5.0, true, null, null, null, null, null));
        assertThrowsIllegalArgument(() -> new Floor(ID_A, ID_B, 1L, "sha", 100, 100, 0.05, null, ScaleMethod.LINE, -1.0, true, null, null, null, null, null));

        // DeviceInfo
        assertThrowsIllegalArgument(() -> new DeviceInfo(ID_A, "A".repeat(33), "mfg", 34, "wifi", 1000L, "fp"));
        assertThrowsIllegalArgument(() -> new DeviceInfo(ID_A, "model", "mfg", 34, "wifi", -1L, "fp"));

        // SurveySession
        // Finalized missing required fields
        assertThrowsIllegalArgument(() -> new SurveySession(
                ID_A, ID_B, "Session", SessionState.FINALIZED, 0L, 0L, 1, "1.0", "1.0", false,
                TimingQuality.GOOD, EvidenceGrade.VERDICT, SessionType.STANDARD, ID_C, 0L, 0.0, 1000L,
                CadenceFlag.DEFAULT, ProbeMethod.ICMP, List.of(2412L), "rec", ID_D, new JsonObject(),
                null, "sha", "key"
        ));
        // Draft with finalized fields
        assertThrowsIllegalArgument(() -> new SurveySession(
                ID_A, ID_B, "Session", SessionState.DRAFT, 0L, 0L, 1, "1.0", "1.0", false,
                TimingQuality.GOOD, EvidenceGrade.VERDICT, SessionType.STANDARD, ID_C, 0L, 0.0, 1000L,
                CadenceFlag.DEFAULT, ProbeMethod.ICMP, List.of(2412L), "rec", ID_D, new JsonObject(),
                1700000000000L, null, null
        ));
        // linkCadenceMs <= 0
        assertThrowsIllegalArgument(() -> new SurveySession(
                ID_A, ID_B, "Session", SessionState.DRAFT, 0L, 0L, 1, "1.0", "1.0", false,
                TimingQuality.GOOD, EvidenceGrade.VERDICT, SessionType.STANDARD, ID_C, 0L, 0.0, 0L,
                CadenceFlag.DEFAULT, ProbeMethod.ICMP, List.of(2412L), "rec", ID_D, new JsonObject(),
                null, null, null
        ));

        // WallMaterial
        assertThrowsIllegalArgument(() -> new WallMaterial(MaterialId.DRYWALL_LIGHT, -1.0, 3.0, 4.5));

        // RequirementProfile
        assertThrowsIllegalArgument(() -> new RequirementProfile(ID_A, ProfileKind.PRESET, new JsonObject(), BandPolicy.BEST_ANY, -1.0, Map.of()));
    }

    @Test
    public void testSort_GroupA() {
        Project p1 = new Project("AAAAAAAAAAAAAAAAAAAAAA", "P1", null, null, 1L);
        Project p2 = new Project("BBBBBBBBBBBBBBBBBBBBBB", "P2", null, null, 2L);
        List<Project> list = new ArrayList<>(List.of(p2, p1));
        list.sort(EntityRegistry.PROJECT.sortComparator());
        assertEquals("AAAAAAAAAAAAAAAAAAAAAA", list.get(0).id());
        assertEquals("BBBBBBBBBBBBBBBBBBBBBB", list.get(1).id());

        Floor f1 = new Floor("BBBBBBBBBBBBBBBBBBBBBB", ID_A, 1L, "sha", 100, 100, 0.05, null, ScaleMethod.LINE, 5.0, true, null, null, null, null, null);
        Floor f2 = new Floor("AAAAAAAAAAAAAAAAAAAAAA", ID_A, 2L, "sha", 100, 100, 0.05, null, ScaleMethod.LINE, 5.0, true, null, null, null, null, null);
        Floor f3 = new Floor("AAAAAAAAAAAAAAAAAAAAAA", ID_A, 1L, "sha", 100, 100, 0.05, null, ScaleMethod.LINE, 5.0, true, null, null, null, null, null);
        List<Floor> floors = new ArrayList<>(List.of(f1, f2, f3));
        floors.sort(EntityRegistry.FLOOR.sortComparator());
        // levelIndex 1 id A, levelIndex 1 id B, levelIndex 2 id A
        assertEquals(f3, floors.get(0));
        assertEquals(f1, floors.get(1));
        assertEquals(f2, floors.get(2));
    }

    @Test
    public void testKeyOrder_and_JsonRoundTrip_GroupA() throws Exception {
        Project p = EntityRegistry.PROJECT.sampleFactory().get();
        JsonObject jsonP = ProjectSerializer.toJson(p);
        assertStrictKeyOrder(jsonP);
        assertEquals(p, ProjectSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jsonP))));

        Floor f = EntityRegistry.FLOOR.sampleFactory().get();
        JsonObject jsonF = FloorSerializer.toJson(f);
        assertStrictKeyOrder(jsonF);
        assertEquals(f, FloorSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jsonF))));

        DeviceInfo d = EntityRegistry.DEVICE_INFO.sampleFactory().get();
        JsonObject jsonD = DeviceInfoSerializer.toJson(d);
        assertStrictKeyOrder(jsonD);
        assertEquals(d, DeviceInfoSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jsonD))));

        SurveySession s = EntityRegistry.SURVEY_SESSION.sampleFactory().get();
        JsonObject jsonS = SurveySessionSerializer.toJson(s);
        assertStrictKeyOrder(jsonS);
        assertEquals(s, SurveySessionSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jsonS))));

        WallMaterial w = EntityRegistry.WALL_MATERIAL.sampleFactory().get();
        JsonObject jsonW = WallMaterialSerializer.toJson(w);
        assertStrictKeyOrder(jsonW);
        assertEquals(w, WallMaterialSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jsonW))));

        RequirementProfile r = EntityRegistry.REQUIREMENT_PROFILE.sampleFactory().get();
        JsonObject jsonR = RequirementProfileSerializer.toJson(r);
        assertStrictKeyOrder(jsonR);
        assertEquals(r, RequirementProfileSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jsonR))));
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
