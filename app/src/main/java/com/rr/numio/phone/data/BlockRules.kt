package com.rr.numio.phone.data

// Plain Kotlin (no Android), so it can be unit tested on the computer.

/** Blocking whole countries by calling code (e.g. +92), on top of single blocked numbers. */
object BlockRules {

    /** "+92", "0092", "92 " → "92". Null if it isn't a calling code we know. */
    fun cleanCode(input: String): String? {
        var digits = input.trim().filter { it.isDigit() }
        if (input.trim().startsWith("00")) digits = digits.drop(2)
        if (digits.isEmpty() || digits.length > 4) return null
        // Check the code is real: a made-up long number with it must map to a country
        return if (CallerCountry.isoFor("+${digits}1234567890") != null) digits else null
    }

    /** True if [number] is an international number starting with one of the blocked [codes]. */
    fun matchesCode(number: String, codes: Set<String>, homeIso: String = ""): Boolean {
        if (codes.isEmpty()) return false
        val digits = CallerCountry.internationalDigitsOf(number, homeIso) ?: return false
        return codes.any { it.isNotEmpty() && digits.startsWith(it) }
    }
}
