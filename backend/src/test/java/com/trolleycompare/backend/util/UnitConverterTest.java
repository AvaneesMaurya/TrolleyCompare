package com.trolleycompare.backend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnitConverterTest {

    @Test
    void normalizesSupportedUnitsAndLeavesUnrecognizedUnitsUnchanged() {
        assertEquals(2500.0, UnitConverter.normalizeToBasePath(2.5, "kg"));
        assertEquals(250.0, UnitConverter.normalizeToBasePath(250, "g"));
        assertEquals(3000.0, UnitConverter.normalizeToBasePath(3, " L "));
        assertEquals(300.0, UnitConverter.normalizeToBasePath(300, "ML"));
        assertEquals(4.0, UnitConverter.normalizeToBasePath(4, "pcs"));
        assertEquals(5.0, UnitConverter.normalizeToBasePath(5, null));
    }

    @Test
    void identifiesWeightVolumeAndCountCategoriesIgnoringCaseAndWhitespace() {
        assertEquals("weight", UnitConverter.getUnitCategory(" Kg "));
        assertEquals("weight", UnitConverter.getUnitCategory("g"));
        assertEquals("volume", UnitConverter.getUnitCategory("L"));
        assertEquals("volume", UnitConverter.getUnitCategory(" ml "));
        assertEquals("count", UnitConverter.getUnitCategory("pack"));
        assertEquals("count", UnitConverter.getUnitCategory(null));
    }
}
