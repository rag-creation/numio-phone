package com.rr.numio.phone

import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.ContactNumber
import com.rr.numio.phone.dialpad.SmartDial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartDialTest {

    private var nextId = 1L
    private fun contact(name: String, number: String, starred: Boolean = false) = Contact(
        id = nextId++, lookupKey = name, name = name,
        numbers = listOf(ContactNumber(number, null)), starred = starred, photoUri = null,
    )

    private val ganesh = contact("Ganesh", "0501112233")
    private val ganeshKumar = contact("Ganesh Kumar", "0559998877")
    private val hari = contact("Hari", "0524263000")
    private val malayalam = contact("അമ്മ", "0501234567")
    private val all = listOf(ganesh, ganeshKumar, hari, malayalam)

    private fun names(typed: String) = SmartDial.search(all, typed, limit = 10).map { it.contact.name }

    @Test
    fun lettersBecomeKeypadDigits() {
        assertEquals("426374", SmartDial.toDigits("Ganesh"))
        assertEquals("426374", SmartDial.toDigits("GANESH"))
        assertEquals("7363", SmartDial.toDigits("René"))
        assertEquals("", SmartDial.toDigits("അമ്മ"))
    }

    @Test
    fun findsGaneshByTypingHisName() {
        assertEquals(listOf("Ganesh", "Ganesh Kumar"), names("426374"))
    }

    @Test
    fun findsBySecondWordAndByInitials() {
        assertEquals(listOf("Ganesh Kumar"), names("58627"))   // Kumar
        assertTrue("Ganesh Kumar" in names("45"))               // G K
    }

    @Test
    fun findsByPartOfTheNumberAfterNameMatches() {
        // 4263 starts "Gane…" (name) and is also inside Hari's number: names come first
        assertEquals(listOf("Ganesh", "Ganesh Kumar", "Hari"), names("4263"))
        assertEquals(listOf("Ganesh Kumar"), names("99988"))
    }

    @Test
    fun oneOrTwoDigitsOnlyMatchNames() {
        assertEquals(listOf("Ganesh", "Ganesh Kumar", "Hari"), names("4"))
        assertEquals(emptyList<String>(), names("05"))
    }

    @Test
    fun favouritesFirstAmongEqualMatches() {
        val gita = contact("Gita", "0501000000", starred = true)
        val gina = contact("Gina", "0502000000")
        assertEquals(listOf("Gita", "Gina"), SmartDial.search(listOf(gina, gita), "44").map { it.contact.name })
    }

    @Test
    fun nothingForEmptyOrSymbols() {
        assertEquals(emptyList<String>(), names(""))
        assertEquals(emptyList<String>(), names("*#"))
        assertEquals(emptyList<String>(), names("42*"))
    }

    @Test
    fun keepsTheLimit() {
        assertEquals(2, SmartDial.search(all, "4", limit = 2).size)
    }
}
