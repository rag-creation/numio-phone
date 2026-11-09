package com.rr.numio.phone

import com.rr.numio.phone.data.BlockRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockRulesTest {
    @Test
    fun cleansTypedCodes() {
        assertEquals("92", BlockRules.cleanCode("+92"))
        assertEquals("92", BlockRules.cleanCode(" 0092 "))
        assertEquals("234", BlockRules.cleanCode("234"))
        assertEquals("1", BlockRules.cleanCode("+1"))
        assertNull(BlockRules.cleanCode("+"))
        assertNull(BlockRules.cleanCode("+12345"))
    }

    @Test
    fun matchesInternationalNumbersOnly() {
        val codes = setOf("92")
        assertTrue(BlockRules.matchesCode("+92 300 1234567", codes))
        assertTrue(BlockRules.matchesCode("0092 300 1234567", codes, "AE"))
        assertFalse(BlockRules.matchesCode("+91 98765 43210", codes))
        // A local number that happens to start with 92 is never blocked
        assertFalse(BlockRules.matchesCode("923001234", codes, "AE"))
        assertFalse(BlockRules.matchesCode("+92 300 1234567", emptySet()))
    }
}
