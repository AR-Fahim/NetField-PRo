package com.netfieldpro.core.geom;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class PolygonValidatorTest {

    @Test
    public void testPV1_square10x10IsOk() {
        List<Pt> square = List.of(
                new Pt(0.0, 0.0),
                new Pt(10.0, 0.0),
                new Pt(10.0, 10.0),
                new Pt(0.0, 10.0)
        );
        assertEquals(PolygonValidator.Result.OK, PolygonValidator.validate(square));
        assertEquals(100.0, PolygonValidator.shoelaceArea(square), 1e-6);
    }

    @Test
    public void testPV2_bowTieIsSelfIntersect() {
        List<Pt> bowTie = List.of(
                new Pt(0.0, 0.0),
                new Pt(10.0, 10.0),
                new Pt(10.0, 0.0),
                new Pt(0.0, 10.0)
        );
        assertEquals(PolygonValidator.Result.SELF_INTERSECT, PolygonValidator.validate(bowTie));
    }

    @Test
    public void testPV3_twoPointsIsTooFew() {
        List<Pt> twoPoints = List.of(
                new Pt(0.0, 0.0),
                new Pt(10.0, 10.0)
        );
        assertEquals(PolygonValidator.Result.TOO_FEW, PolygonValidator.validate(twoPoints));
    }

    @Test
    public void testPV4_areaPointFiveIsTooSmall() {
        List<Pt> smallTriangle = List.of(
                new Pt(0.0, 0.0),
                new Pt(1.0, 0.0),
                new Pt(0.0, 1.0)
        );
        assertEquals(0.5, PolygonValidator.shoelaceArea(smallTriangle), 1e-6);
        assertEquals(PolygonValidator.Result.TOO_SMALL, PolygonValidator.validate(smallTriangle));
    }

    @Test
    public void testPV5_collinearIsTooSmall() {
        List<Pt> collinear = List.of(
                new Pt(0.0, 0.0),
                new Pt(5.0, 0.0),
                new Pt(10.0, 0.0)
        );
        assertEquals(0.0, PolygonValidator.shoelaceArea(collinear), 1e-6);
        assertEquals(PolygonValidator.Result.TOO_SMALL, PolygonValidator.validate(collinear));
    }
}

