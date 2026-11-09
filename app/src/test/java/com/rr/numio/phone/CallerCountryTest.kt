package com.rr.numio.phone

import com.rr.numio.phone.data.CallerCountry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class CallerCountryTest {
    @Test
    fun findsCountryFromCallingCode() {
        assertEquals("IN", CallerCountry.isoFor("+91 98765 43210"))
        assertEquals("AE", CallerCountry.isoFor("+971 50 123 4567"))
        assertEquals("SA", CallerCountry.isoFor("+966 50 123 4567"))
        assertEquals("GB", CallerCountry.isoFor("+44 7700 900123"))
        assertEquals("DE", CallerCountry.isoFor("+49 151 23456789"))
    }

    @Test
    fun doubleZeroPrefixCountsAsInternational() {
        assertEquals("IN", CallerCountry.isoFor("0091 9876543210", "AE"))
    }

    @Test
    fun localNumbersHaveNoCountry() {
        assertNull(CallerCountry.isoFor("050 123 4567", "AE"))
        assertNull(CallerCountry.isoFor("+971"))
    }

    @Test
    fun plusOneIsSplitByAreaCode() {
        assertEquals("US", CallerCountry.isoFor("+1 212 555 0100"))
        assertEquals("CA", CallerCountry.isoFor("+1 416 555 0100"))
        assertEquals("JM", CallerCountry.isoFor("+1 876 555 0100"))
        assertEquals("GB", CallerCountry.isoFor("011 44 7700 900123", "US"))
    }

    @Test
    fun plusSevenIsRussiaOrKazakhstan() {
        assertEquals("RU", CallerCountry.isoFor("+7 912 345 6789"))
        assertEquals("KZ", CallerCountry.isoFor("+7 701 234 5678"))
    }

    @Test
    fun labelSkipsTheHomeCountry() {
        assertEquals("🇮🇳 India", CallerCountry.label("+919876543210", "AE", Locale.ENGLISH))
        assertNull(CallerCountry.label("+971501234567", "AE", Locale.ENGLISH))
        assertEquals("🇦🇪 UAE", CallerCountry.label("+971501234567", "IN", Locale.ENGLISH))
        assertNull(CallerCountry.label("0501234567", "AE", Locale.ENGLISH))
    }

    @Test
    fun flagIsBuiltFromTheCountryCode() {
        assertEquals("🇮🇳", CallerCountry.flag("IN"))
        assertEquals("🇦🇪", CallerCountry.flag("ae"))
        assertEquals("", CallerCountry.flag("X"))
    }
}
