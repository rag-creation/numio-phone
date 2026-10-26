package com.rr.numio.phone

import com.rr.numio.phone.data.CallEntry
import com.rr.numio.phone.data.CallGrouping
import com.rr.numio.phone.data.CallKind
import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.ContactNumber
import com.rr.numio.phone.data.ContactSearch
import com.rr.numio.phone.data.PhoneNumbers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PhoneModelsTest {
    private val zone = ZoneId.of("Asia/Dubai")

    private fun at(day: Int, hour: Int) =
        LocalDateTime.of(2026, 10, day, hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun call(id: Long, number: String, day: Int, hour: Int, kind: CallKind = CallKind.INCOMING) =
        CallEntry(id, number, null, kind, at(day, hour), 0, null)

    @Test
    fun sameNumberInDifferentFormatsMatches() {
        assertTrue(PhoneNumbers.same("+971 50 123 4567", "0501234567"))
        assertFalse(PhoneNumbers.same("0501234567", "0501234568"))
        assertFalse(PhoneNumbers.same("", ""))
    }

    @Test
    fun normalizeKeepsLeadingPlus() {
        assertEquals("+971501234567", PhoneNumbers.normalize(" +971 (50) 123-4567 "))
        assertEquals("0501234567", PhoneNumbers.normalize("050-123 4567"))
    }

    @Test
    fun groupsConsecutiveCallsSameNumberSameDay() {
        // newest first
        val calls = listOf(
            call(1, "0501234567", 26, 21),
            call(2, "+971501234567", 26, 20, CallKind.MISSED),
            call(3, "0559998888", 26, 19),
            call(4, "0501234567", 26, 18),
            call(5, "0501234567", 25, 22),
        )
        val groups = CallGrouping.group(calls, zone)
        assertEquals(listOf(2, 1, 1, 1), groups.map { it.calls.size })
        assertEquals(1L, groups[0].latest.id)
    }

    @Test
    fun emptyLogGivesNoGroups() {
        assertTrue(CallGrouping.group(emptyList(), zone).isEmpty())
    }

    private fun contact(name: String, vararg numbers: String) =
        Contact(1, "k", name, numbers.map { ContactNumber(it, null) }, false, null)

    @Test
    fun searchByNameOrDigits() {
        val c = contact("Amma", "+91 94470 00000")
        assertTrue(ContactSearch.matches(c, "amm"))
        assertTrue(ContactSearch.matches(c, "9447"))
        assertTrue(ContactSearch.matches(c, ""))
        assertFalse(ContactSearch.matches(c, "Achan"))
        assertFalse(ContactSearch.matches(c, "1"))
    }

    @Test
    fun sectionLetters() {
        assertEquals("A", ContactSearch.section("amma"))
        assertEquals("#", ContactSearch.section("24x7 Taxi"))
        assertEquals("#", ContactSearch.section(""))
        assertEquals("അ", ContactSearch.section("അമ്മ"))
    }

    @Test
    fun sortsAlphabeticallyIgnoringCase() {
        val sorted = ContactSearch.sorted(listOf(contact("bala"), contact("Anu"), contact("achan")))
        assertEquals(listOf("achan", "Anu", "bala"), sorted.map { it.name })
    }
}
