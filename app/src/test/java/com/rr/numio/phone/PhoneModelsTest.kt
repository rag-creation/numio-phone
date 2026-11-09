package com.rr.numio.phone

import com.rr.numio.phone.data.CallEntry
import com.rr.numio.phone.data.CallGrouping
import com.rr.numio.phone.data.CallKind
import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.ContactNumber
import com.rr.numio.phone.data.ContactNumberPlan
import com.rr.numio.phone.data.ContactSearch
import com.rr.numio.phone.data.PhoneNumbers
import com.rr.numio.phone.data.PhoneType
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

class ContactDraftTest {
    private fun n(number: String) = com.rr.numio.phone.data.ContactNumber(number, null)

    @Test
    fun dropsEmptyAndDuplicateNumbers() {
        val d = com.rr.numio.phone.data.ContactDraft(null, "Amma", listOf(n(" "), n("+971 50 123 4567"), n("0501234567"), n("04 555 1234")), false)
        assertEquals(listOf("+971 50 123 4567", "04 555 1234"), d.cleanNumbers.map { it.number })
    }

    @Test
    fun needsNameOrNumber() {
        assertFalse(com.rr.numio.phone.data.ContactDraft(null, "  ", listOf(n("")), false).canSave)
        assertTrue(com.rr.numio.phone.data.ContactDraft(null, "Amma", emptyList(), false).canSave)
        assertTrue(com.rr.numio.phone.data.ContactDraft(null, "", listOf(n("0501234567")), false).canSave)
    }

    @Test
    fun nameFallsBackToNumber() {
        assertEquals("0501234567", com.rr.numio.phone.data.ContactDraft(null, "", listOf(n("0501234567")), false).savedName)
        assertEquals("Amma", com.rr.numio.phone.data.ContactDraft(null, " Amma ", emptyList(), false).savedName)
    }

    @Test
    fun labelCycleWraps() {
        val t = com.rr.numio.phone.data.PhoneType
        assertEquals(t.HOME, t.next(t.MOBILE))
        assertEquals(t.MOBILE, t.next(t.OTHER))
        assertEquals(t.HOME, t.next(99))
    }
}

class ContactNumberPlanTest {
    private fun e(id: Long, number: String, type: Int = PhoneType.MOBILE, label: String? = null) =
        ContactNumberPlan.Existing(id, number, type, label)

    private fun kept(number: String, type: Int = PhoneType.MOBILE, label: String? = null, from: String = number) =
        ContactNumber(number, label, type, from)

    @Test
    fun unchangedNumbersAreLeftAlone() {
        val plan = ContactNumberPlan.plan(
            listOf(e(1, "+971501234567"), e(2, "0441234567", PhoneType.CUSTOM, "Dubai SIM")),
            listOf(kept("+971501234567"), kept("0441234567", PhoneType.CUSTOM, "Dubai SIM")),
        )
        assertTrue(plan.updates.isEmpty())
        assertTrue(plan.deletes.isEmpty())
        assertTrue(plan.inserts.isEmpty())
    }

    @Test
    fun customLabelIsKeptWhenTheNumberIsChanged() {
        val plan = ContactNumberPlan.plan(
            listOf(e(5, "0441234567", PhoneType.CUSTOM, "Dubai SIM")),
            listOf(kept("0441234999", PhoneType.CUSTOM, "Dubai SIM", from = "0441234567")),
        )
        assertEquals(1, plan.updates.size)
        assertEquals(5L, plan.updates[0].first)
        assertEquals("0441234999", plan.updates[0].second.number)
        assertEquals("Dubai SIM", plan.updates[0].second.label)
        assertTrue(plan.deletes.isEmpty())
        assertTrue(plan.inserts.isEmpty())
    }

    @Test
    fun removedNumbersAreDeletedAndNewOnesAdded() {
        val plan = ContactNumberPlan.plan(
            listOf(e(1, "0501234567"), e(2, "0551234567")),
            listOf(kept("0501234567"), ContactNumber("+919876543210", null, PhoneType.HOME)),
        )
        assertEquals(listOf(2L), plan.deletes)
        assertEquals(listOf("+919876543210"), plan.inserts.map { it.number })
        assertTrue(plan.updates.isEmpty())
    }

    @Test
    fun changingTypeUpdatesInPlaceAndDropsTheOldLabel() {
        val plan = ContactNumberPlan.plan(
            listOf(e(3, "0501234567", PhoneType.CUSTOM, "Old phone")),
            listOf(kept("0501234567", PhoneType.WORK, "Old phone")),
        )
        assertEquals(3L, plan.updates.single().first)
        assertEquals(PhoneType.WORK, plan.updates.single().second.type)
        assertEquals(null, plan.updates.single().second.label)
    }

    @Test
    fun sameNumberSavedTwiceKeepsBothCopiesWhenUntouched() {
        // Shown as one row in the editor, but both copies stay on the phone
        val plan = ContactNumberPlan.plan(
            listOf(e(1, "+971501234567"), e(2, "0501234567")),
            listOf(kept("+971501234567")),
        )
        assertTrue(plan.deletes.isEmpty())
        assertTrue(plan.updates.isEmpty())
        assertTrue(plan.inserts.isEmpty())
    }

    @Test
    fun sameNumberSavedTwiceLeavesNoStaleCopyWhenChanged() {
        val plan = ContactNumberPlan.plan(
            listOf(e(1, "+971501234567"), e(2, "0501234567")),
            listOf(kept("0509999999", from = "+971501234567")),
        )
        assertEquals(1L, plan.updates.single().first)
        assertEquals(listOf(2L), plan.deletes)
        assertTrue(plan.inserts.isEmpty())
    }

    @Test
    fun typeCycleIncludesTheNumbersOwnLabel() {
        assertEquals(PhoneType.MOBILE, PhoneType.next(PhoneType.CUSTOM, "Dubai SIM"))
        assertEquals(PhoneType.CUSTOM, PhoneType.next(PhoneType.OTHER, "Dubai SIM"))
        assertEquals(PhoneType.MOBILE, PhoneType.next(PhoneType.OTHER))
    }
}
