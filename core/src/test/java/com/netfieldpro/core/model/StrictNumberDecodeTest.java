package com.netfieldpro.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.serial.DecimalFields;
import com.netfieldpro.core.model.serial.IntFields;

public class StrictNumberDecodeTest {

    @Test
    public void testIntS1_reject_string_5() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                IntFields.parseLong(new JsonString("5"), "f"));
        assertEquals("NOT_CANONICAL_INT:f", ex.getMessage());
    }

    @Test
    public void testIntS2_reject_string_minus9007199254740991() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                IntFields.parseLong(new JsonString("-9007199254740991"), "f"));
        assertEquals("NOT_CANONICAL_INT:f", ex.getMessage());
    }

    @Test
    public void testIntS3_accept_string_9007199254740992() {
        long v = IntFields.parseLong(new JsonString("9007199254740992"), "f");
        assertEquals(9007199254740992L, v);
    }

    @Test
    public void testIntS4_reject_string_09007199254740992() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                IntFields.parseLong(new JsonString("09007199254740992"), "f"));
        assertEquals("NOT_CANONICAL_INT:f", ex.getMessage());
    }

    @Test
    public void testIntS5_reject_string_plus9007199254740992() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                IntFields.parseLong(new JsonString("+9007199254740992"), "f"));
        assertEquals("NOT_CANONICAL_INT:f", ex.getMessage());
    }

    @Test
    public void testIntS6_accept_string_9223372036854775807() {
        long v = IntFields.parseLong(new JsonString("9223372036854775807"), "f");
        assertEquals(9223372036854775807L, v);
    }

    @Test
    public void testIntS7_reject_string_9223372036854775808() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                IntFields.parseLong(new JsonString("9223372036854775808"), "f"));
        assertEquals("NOT_CANONICAL_INT:f", ex.getMessage());
    }

    @Test
    public void testDecS1_accept_dp1_minus61_5() {
        double d = DecimalFields.parseDouble(new JsonString("-61.5"), "d", 1);
        assertEquals(-61.5, d, 1e-9);
    }

    @Test
    public void testDecS2_reject_dp1_invalid_formats() {
        String[] rejected = {"-61.50", "-0.0", "61", ".5", "1e1"};
        for (String input : rejected) {
            IllegalArgumentException ex = assertThrows("Expected rejection for " + input,
                    IllegalArgumentException.class, () ->
                            DecimalFields.parseDouble(new JsonString(input), "d", 1));
            assertEquals("NOT_CANONICAL_DECIMAL:d", ex.getMessage());
        }
    }

    @Test
    public void testDecS3_dp0() {
        double d = DecimalFields.parseDouble(new JsonString("-2"), "d", 0);
        assertEquals(-2.0, d, 1e-9);

        String[] rejected = {"-0", "02"};
        for (String input : rejected) {
            IllegalArgumentException ex = assertThrows("Expected rejection for " + input,
                    IllegalArgumentException.class, () ->
                            DecimalFields.parseDouble(new JsonString(input), "d", 0));
            assertEquals("NOT_CANONICAL_DECIMAL:d", ex.getMessage());
        }
    }
}

