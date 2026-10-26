package com.rr.numio.phone.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Plain Kotlin (no Android), so it can be unit tested on the computer.

enum class CallKind { INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED }

/** One row of Android's call log. */
data class CallEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val kind: CallKind,
    val timeMillis: Long,
    val durationSec: Long,
    val simLabel: String?,
)

/** Calls to or from the same number in a row on the same day, shown as one line. */
data class CallGroup(
    val number: String,
    val name: String?,
    val calls: List<CallEntry>,
) {
    val latest: CallEntry get() = calls.first()
    val title: String get() = name?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Unknown" }
}

data class Contact(
    val id: Long,
    val lookupKey: String,
    val name: String,
    val numbers: List<ContactNumber>,
    val starred: Boolean,
    val photoUri: String?,
)

/**
 * One saved number. [type] uses Android's phone types (2 = mobile, 1 = home, 3 = work,
 * 7 = other); [label] is the text shown for it.
 */
data class ContactNumber(val number: String, val label: String?, val type: Int = PhoneType.MOBILE)

object PhoneType {
    const val HOME = 1
    const val MOBILE = 2
    const val WORK = 3
    const val OTHER = 7

    /** The four choices the editor cycles through. */
    val choices = listOf(MOBILE, HOME, WORK, OTHER)

    fun next(type: Int): Int = choices[(choices.indexOf(type).coerceAtLeast(0) + 1) % choices.size]
}

/** What the contact editor saves. [id] is null for a new contact. */
data class ContactDraft(
    val id: Long?,
    val name: String,
    val numbers: List<ContactNumber>,
    val starred: Boolean,
) {
    /** Numbers worth saving: dialable characters only, empty rows dropped, no duplicates. */
    val cleanNumbers: List<ContactNumber>
        get() {
            val out = mutableListOf<ContactNumber>()
            for (n in numbers) {
                val cleaned = n.number.trim()
                if (cleaned.none { it.isDigit() }) continue
                if (out.any { PhoneNumbers.same(it.number, cleaned) }) continue
                out += n.copy(number = cleaned)
            }
            return out
        }

    /** A contact needs a name or at least one number. */
    val canSave: Boolean get() = name.isNotBlank() || cleanNumbers.isNotEmpty()

    /** Name to save: the typed name, else the first number. */
    val savedName: String get() = name.trim().ifBlank { cleanNumbers.firstOrNull()?.number.orEmpty() }

    companion object {
        fun from(contact: Contact) = ContactDraft(contact.id, contact.name, contact.numbers, contact.starred)
        fun new(number: String? = null) = ContactDraft(
            id = null,
            name = "",
            numbers = listOf(ContactNumber(number.orEmpty(), null, PhoneType.MOBILE)),
            starred = false,
        )
    }
}

object PhoneNumbers {
    /** Digits only, keeping a leading "+". */
    fun normalize(number: String): String {
        val trimmed = number.trim()
        val digits = trimmed.filter { it.isDigit() }
        return if (trimmed.startsWith("+")) "+$digits" else digits
    }

    /**
     * Key for "is this the same phone number": the last 9 digits, so
     * +971 50 123 4567 and 050 123 4567 match.
     */
    fun matchKey(number: String): String = number.filter { it.isDigit() }.takeLast(9)

    fun same(a: String, b: String): Boolean {
        val ka = matchKey(a)
        return ka.isNotEmpty() && ka == matchKey(b)
    }
}

object CallGrouping {
    /** [entries] newest first. Consecutive calls with the same number on the same day become one group. */
    fun group(entries: List<CallEntry>, zone: ZoneId = ZoneId.systemDefault()): List<CallGroup> {
        val groups = mutableListOf<CallGroup>()
        var current = mutableListOf<CallEntry>()
        for (e in entries) {
            val last = current.lastOrNull()
            if (last != null && (!PhoneNumbers.same(last.number, e.number) || day(last.timeMillis, zone) != day(e.timeMillis, zone))) {
                groups += toGroup(current)
                current = mutableListOf()
            }
            current += e
        }
        if (current.isNotEmpty()) groups += toGroup(current)
        return groups
    }

    private fun toGroup(calls: List<CallEntry>) = CallGroup(
        number = calls.first().number,
        name = calls.firstNotNullOfOrNull { it.name?.takeIf { n -> n.isNotBlank() } },
        calls = calls.toList(),
    )

    fun day(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}

object ContactSearch {
    /** Name contains the text, or (2+ digits typed) one of the numbers contains those digits. */
    fun matches(contact: Contact, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        if (contact.name.contains(q, ignoreCase = true)) return true
        val digits = q.filter { it.isDigit() }
        if (digits.length >= 2 && digits.length == q.count { it.isDigit() || it == '+' || it == ' ' || it == '-' }) {
            return contact.numbers.any { it.number.filter { c -> c.isDigit() }.contains(digits) }
        }
        return false
    }

    /** "A".."Z" for names starting with a Latin letter, the first character otherwise, "#" for digits. */
    fun section(name: String): String {
        val c = name.trim().firstOrNull() ?: return "#"
        return when {
            c.isDigit() -> "#"
            c.isLetter() -> c.uppercaseChar().toString()
            else -> "#"
        }
    }

    /** Favourites first? No: callers show them separately. This sorts A–Z, ignoring case. */
    fun sorted(contacts: List<Contact>): List<Contact> =
        contacts.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}
