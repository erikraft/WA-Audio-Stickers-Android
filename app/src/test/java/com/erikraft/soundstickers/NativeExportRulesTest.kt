package com.erikraft.soundstickers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeExportRulesTest {
    @Test
    fun automaticFpsSelectsTheNearestSupportedRateAndCapsAtThirty() {
        assertEquals(30, NativeExportRules.chooseFps(60.0, false))
        assertEquals(30, NativeExportRules.chooseFps(30.0, false))
        assertEquals(25, NativeExportRules.chooseFps(24.0, false))
        assertEquals(15, NativeExportRules.chooseFps(15.0, false))
        assertEquals(10, NativeExportRules.chooseFps(10.0, false))
        assertEquals(5, NativeExportRules.chooseFps(5.0, false))
    }

    @Test
    fun automaticFpsUsesThirtyWhenSourceMetadataIsMissingOrInvalid() {
        assertEquals(30, NativeExportRules.chooseFps(null, false))
        assertEquals(30, NativeExportRules.chooseFps(Double.NaN, false))
        assertEquals(30, NativeExportRules.chooseFps(0.0, false))
    }

    @Test
    fun stillImagesUseFiveFps() {
        assertEquals(5, NativeExportRules.chooseFps(30.0, true))
    }

    @Test
    fun manualFpsOnlyAcceptsSupportedIntegers() {
        assertEquals(25, NativeExportRules.resolveFps("25", 30.0, false))
        assertEquals(5, NativeExportRules.resolveFps("5", null, false))
        assertNull(NativeExportRules.resolveFps("23.976", 30.0, false))
        assertNull(NativeExportRules.resolveFps("0", 30.0, false))
        assertNull(NativeExportRules.resolveFps("999", 30.0, false))
    }

    @Test
    fun clipRangeMustBeFinitePositiveInsideSourceAndAtMostTenSeconds() {
        assertTrue(NativeExportRules.isValidClip(0.0, 10.0, 12.0))
        assertTrue(NativeExportRules.isValidClip(1.0, 10.5, 12.0))
        assertFalse(NativeExportRules.isValidClip(0.0, 10.01, 12.0))
        assertFalse(NativeExportRules.isValidClip(2.0, 12.01, 12.0))
        assertFalse(NativeExportRules.isValidClip(Double.NaN, 3.0, 12.0))
        assertFalse(NativeExportRules.isValidClip(3.0, 3.0, 12.0))
        assertFalse(NativeExportRules.isValidClip(3.0, 3.05, 12.0))
        assertTrue(NativeExportRules.isValidClip(3.0, 3.1, 12.0))
    }
}
