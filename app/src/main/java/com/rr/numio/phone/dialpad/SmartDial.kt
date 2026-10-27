package com.rr.numio.phone.dialpad

import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.ContactNumber
import java.text.Normalizer

// Plain Kotlin (no Android), so it can be unit tested on the computer.

/** A contact found while typing on the keypad, with the number a tap would call. */
data class SmartDialMatch(val contact: Contact, val number: ContactNumber, val byName: Boolean)

/**
 * Smart dial: typing on the keypad finds contacts by the letters on the keys,
 * like old phones. 4-2-6-3-7-4 finds "Ganesh"; 4-5 finds "Ganesh Kumar" by his initials.
 * It also finds contacts whose number contains the typed digits.
 */
object SmartDial {

    private val letterDigit: Map<Char, Char> = buildMap {
        DialPad.keys.forEach { key -> key.letters.lowercase().forEach { put(it, key.char) } }
        remove('+')
    }

    /** Latin letters as keypad digits ("Ganesh" → "426374"); digits stay; anything else is dropped. */
    fun toDigits(text: String): String {
        val plain = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD) // é → e
        return buildString {
            for (c in plain) {
                when {
                    c in 'a'..'z' -> append(letterDigit.getValue(c))
                    c in '0'..'9' -> append(c)
                }
            }
        }
    }

    private fun words(name: String): List<String> =
        name.split(' ', '-', '.', '_', ',', '(', ')').map { toDigits(it) }.filter { it.isNotEmpty() }

    /** How well the name matches the typed digits: 3 = from the start, 2 = start of another word, 1 = initials, 0 = no. */
    fun nameScore(name: String, digits: String): Int {
        if (digits.isEmpty()) return 0
        val ws = words(name)
        if (ws.isEmpty()) return 0
        if (ws.joinToString("").startsWith(digits)) return 3
        // "Ganesh Kumar": 58627 (Kumar), or "Kumar" then more of the name
        for (i in 1 until ws.size) {
            if (ws.drop(i).joinToString("").startsWith(digits)) return 2
        }
        if (digits.length >= 2 && ws.joinToString("") { it.take(1) }.startsWith(digits)) return 1
        return 0
    }

    /**
     * Contacts matching what's typed, best first, at most [limit].
     * Name matches come before number matches; favourites first, then A–Z.
     */
    fun search(contacts: List<Contact>, typed: String, limit: Int = 3): List<SmartDialMatch> {
        val digits = typed.filter { it.isDigit() }
        // Codes like *#06# are not names or numbers to look up
        if (digits.isEmpty() || typed.any { it == '*' || it == '#' }) return emptyList()

        data class Scored(val match: SmartDialMatch, val score: Int)
        val scored = contacts.mapNotNull { c ->
            if (c.numbers.isEmpty()) return@mapNotNull null
            val byName = nameScore(c.name, digits)
            if (byName > 0) {
                Scored(SmartDialMatch(c, c.numbers.first(), byName = true), 10 + byName)
            } else if (digits.length >= 3) {
                val hit = c.numbers.firstOrNull { it.number.filter { ch -> ch.isDigit() }.contains(digits) }
                hit?.let { Scored(SmartDialMatch(c, it, byName = false), 1) }
            } else {
                null
            }
        }
        return scored
            .sortedWith(
                compareByDescending<Scored> { it.score }
                    .thenByDescending { it.match.contact.starred }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.match.contact.name }
            )
            .take(limit)
            .map { it.match }
    }
}
