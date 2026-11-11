package com.netfieldpro.core.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.model.enums.ScaleMethod;

public class ProjectRulesTest {

    @Test
    public void testVectorNR1() {
        assertEquals(NameRules.Status.OK, NameRules.validate("  Dental Office  "));
        assertEquals("Dental Office", NameRules.normalize("  Dental Office  "));

        assertEquals(NameRules.Status.EMPTY, NameRules.validate(""));
        assertEquals(NameRules.Status.EMPTY, NameRules.validate("   "));
        assertEquals(NameRules.Status.EMPTY, NameRules.validate(null));

        StringBuilder sb60 = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            sb60.append('a');
        }
        assertEquals(NameRules.Status.OK, NameRules.validate(sb60.toString()));

        StringBuilder sb61 = new StringBuilder();
        for (int i = 0; i < 61; i++) {
            sb61.append('a');
        }
        assertEquals(NameRules.Status.TOO_LONG, NameRules.validate(sb61.toString()));

        assertEquals(NameRules.Status.INVALID_CHARS, NameRules.validate("A\nB"));

        String emoji = "🙂";
        assertEquals(1, emoji.codePointCount(0, emoji.length()));
        assertEquals(NameRules.Status.OK, NameRules.validate(emoji));

        assertNull(NameRules.normalizeSite("  "));
        assertNull(NameRules.normalizeSite(""));
        assertNull(NameRules.normalizeSite(null));
        assertEquals("HQ", NameRules.normalizeSite("  HQ  "));
    }

    @Test
    public void testVectorLV1() {
        assertEquals(0L, LevelRules.defaultNextLevel(Collections.emptyList()));
        assertEquals(0L, LevelRules.defaultNextLevel(null));

        assertEquals(4L, LevelRules.defaultNextLevel(Arrays.asList(0L, 1L, 3L)));
        assertEquals(0L, LevelRules.defaultNextLevel(Collections.singletonList(-1L)));
        assertEquals(-1L, LevelRules.defaultNextLevel(Arrays.asList(-3L, -2L)));
        assertEquals(99L, LevelRules.defaultNextLevel(Collections.singletonList(99L)));

        assertTrue(LevelRules.isValid(-5L));
        assertTrue(LevelRules.isValid(99L));
        assertTrue(LevelRules.isValid(0L));
        assertFalse(LevelRules.isValid(-6L));
        assertFalse(LevelRules.isValid(100L));
    }

    @Test
    public void testVectorSOA() {
        // Projects (created 5,"b"), (9,"a"), (9,"c") -> [a, c, b]
        String idA = "AAAAAAAAAAAAAAAAAAAAAa";
        String idB = "AAAAAAAAAAAAAAAAAAAAAb";
        String idC = "AAAAAAAAAAAAAAAAAAAAAc";

        Project pb = new Project(idB, "B", null, null, 5L);
        Project pa = new Project(idA, "A", null, null, 9L);
        Project pc = new Project(idC, "C", null, null, 9L);

        List<Project> sortedProjects = ProjectSort.sort(Arrays.asList(pb, pa, pc));
        assertEquals(idA, sortedProjects.get(0).id());
        assertEquals(idC, sortedProjects.get(1).id());
        assertEquals(idB, sortedProjects.get(2).id());

        // Floors (1,"y"), (−1,"z"), (1,"x") -> [z, x, y]
        String idX = "AAAAAAAAAAAAAAAAAAAAAx";
        String idY = "AAAAAAAAAAAAAAAAAAAAAy";
        String idZ = "AAAAAAAAAAAAAAAAAAAAAz";
        String projId = "AAAAAAAAAAAAAAAAAAAAAP";

        Floor fy = new Floor(idY, projId, 1L, "sha", 1000, 1000, 0.02, null, ScaleMethod.LINE, 5.0, false, null, null, null, null, null);
        Floor fz = new Floor(idZ, projId, -1L, "sha", 1000, 1000, 0.02, null, ScaleMethod.LINE, 5.0, false, null, null, null, null, null);
        Floor fx = new Floor(idX, projId, 1L, "sha", 1000, 1000, 0.02, null, ScaleMethod.LINE, 5.0, false, null, null, null, null, null);

        List<Floor> sortedFloors = FloorSort.sort(Arrays.asList(fy, fz, fx));
        assertEquals(idZ, sortedFloors.get(0).id());
        assertEquals(idX, sortedFloors.get(1).id());
        assertEquals(idY, sortedFloors.get(2).id());
    }
}
