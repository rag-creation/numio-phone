package com.rr.numio.phone.dialpad

// Plain Kotlin (no Android), so it can be unit tested on the computer.

/** One key on the dial pad: the main character and the letters printed under it. */
data class DialKey(val char: Char, val letters: String = "")

object DialPad {
    // Long numbers (with extension or prefix codes) still fit; anything longer is a typo.
    const val MAX_LENGTH = 40

    /** The 12 keys, row by row, as printed on phone keypads. */
    val keys: List<DialKey> = listOf(
        DialKey('1', ""), DialKey('2', "ABC"), DialKey('3', "DEF"),
        DialKey('4', "GHI"), DialKey('5', "JKL"), DialKey('6', "MNO"),
        DialKey('7', "PQRS"), DialKey('8', "TUV"), DialKey('9', "WXYZ"),
        DialKey('*', ""), DialKey('0', "+"), DialKey('#', ""),
    )

    private val allowed = "0123456789*#+".toSet()

    /**
     * Adds [c] to [number]. Characters that can't be dialled are ignored,
     * "+" is only allowed as the very first character (international prefix),
     * and the number stops growing at [MAX_LENGTH].
     */
    fun append(number: String, c: Char): String {
        if (c !in allowed) return number
        if (number.length >= MAX_LENGTH) return number
        if (c == '+' && number.isNotEmpty()) return number
        return number + c
    }

    /** Long-press on 0 types "+" when the number is empty, otherwise a normal 0. */
    fun longPressZero(number: String): String =
        if (number.isEmpty()) append(number, '+') else append(number, '0')

    /** Removes the last character. */
    fun backspace(number: String): String = number.dropLast(1)

    /**
     * Keeps only dialable characters from pasted text, e.g. "+971 (50) 123-4567" → "+971501234567".
     * A "+" survives only at the start.
     */
    fun clean(text: String): String {
        var out = ""
        for (c in text.trim()) out = append(out, c)
        return out
    }
}
