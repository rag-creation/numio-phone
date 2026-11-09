package com.rr.numio.phone.dialpad

// Plain Kotlin (no Android), so it can be unit tested on the computer.

/** One key on the dial pad: the main character and the letters printed under it. */
data class DialKey(val char: Char, val letters: String = "")

/** The typed number and where the cursor is (0 = before the first digit). */
data class Typed(val text: String, val cursor: Int) {
    companion object {
        fun atEnd(text: String) = Typed(text, text.length)
    }
}

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

    /** Long-pressing 1 with nothing typed and no speed dial on it calls this (the SIM's voicemail). */
    const val VOICEMAIL = "voicemail"

    /**
     * Types [c] at the cursor. Same rules as [append]; "+" only goes at the very start,
     * and nothing can be typed in front of a leading "+".
     */
    fun insert(t: Typed, c: Char): Typed {
        val cursor = t.cursor.coerceIn(0, t.text.length)
        if (c !in allowed || t.text.length >= MAX_LENGTH) return t.copy(cursor = cursor)
        val hasPlus = t.text.startsWith("+")
        if (c == '+') {
            if (cursor != 0 || hasPlus) return t.copy(cursor = cursor)
        }
        val at = if (hasPlus && cursor == 0) 1 else cursor
        return Typed(t.text.substring(0, at) + c + t.text.substring(at), at + 1)
    }

    /** Long-press on 0: "+" when the cursor is at the start (and there's no "+" yet), otherwise 0. */
    fun longPressZero(t: Typed): Typed {
        val cursor = t.cursor.coerceIn(0, t.text.length)
        return if (cursor == 0 && !t.text.startsWith("+")) insert(t, '+') else insert(t, '0')
    }

    /** Deletes the character just before the cursor. */
    fun deleteBefore(t: Typed): Typed {
        val cursor = t.cursor.coerceIn(0, t.text.length)
        if (cursor == 0) return t.copy(cursor = 0)
        return Typed(t.text.removeRange(cursor - 1, cursor), cursor - 1)
    }

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
