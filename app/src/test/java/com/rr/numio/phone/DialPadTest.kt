package com.rr.numio.phone

import com.rr.numio.phone.dialpad.DialPad
import org.junit.Assert.assertEquals
import org.junit.Test

class DialPadTest {

    @Test
    fun typesDigitsStarAndHash() {
        var n = ""
        "0501234567*#".forEach { n = DialPad.append(n, it) }
        assertEquals("0501234567*#", n)
    }

    @Test
    fun ignoresCharactersThatCannotBeDialled() {
        assertEquals("12", DialPad.append("12", 'a'))
        assertEquals("12", DialPad.append("12", ' '))
        assertEquals("12", DialPad.append("12", '-'))
    }

    @Test
    fun plusOnlyAtTheStart() {
        assertEquals("+", DialPad.append("", '+'))
        assertEquals("971", DialPad.append("971", '+'))
    }

    @Test
    fun longPressZeroGivesPlusOnlyWhenEmpty() {
        assertEquals("+", DialPad.longPressZero(""))
        assertEquals("+0", DialPad.longPressZero("+"))
        assertEquals("050", DialPad.longPressZero("05"))
    }

    @Test
    fun stopsAtMaxLength() {
        var n = ""
        repeat(DialPad.MAX_LENGTH + 10) { n = DialPad.append(n, '9') }
        assertEquals(DialPad.MAX_LENGTH, n.length)
    }

    @Test
    fun backspaceRemovesLastAndIsSafeWhenEmpty() {
        assertEquals("05", DialPad.backspace("050"))
        assertEquals("", DialPad.backspace(""))
    }

    @Test
    fun cleansPastedNumbers() {
        assertEquals("+971501234567", DialPad.clean("  +971 (50) 123-4567 "))
        assertEquals("04123", DialPad.clean("04+123"))
    }

    @Test
    fun keypadHasTwelveKeysInPhoneOrder() {
        assertEquals("123456789*0#", DialPad.keys.joinToString("") { it.char.toString() })
        assertEquals("ABC", DialPad.keys[1].letters)
        assertEquals("WXYZ", DialPad.keys[8].letters)
    }

    // ---- Editing in the middle with a cursor ----

    @Test
    fun insertsAtTheCursor() {
        // 123456789 with the cursor after 3: typing 0 gives 1230456789
        val t = DialPad.insert(com.rr.numio.phone.dialpad.Typed("123456789", 3), '0')
        assertEquals("1230456789", t.text)
        assertEquals(4, t.cursor)
    }

    @Test
    fun deletesTheDigitBeforeTheCursor() {
        val t = DialPad.deleteBefore(com.rr.numio.phone.dialpad.Typed("123456789", 3))
        assertEquals("12456789", t.text)
        assertEquals(2, t.cursor)
        // Nothing before the cursor: nothing deleted
        assertEquals("123", DialPad.deleteBefore(com.rr.numio.phone.dialpad.Typed("123", 0)).text)
    }

    @Test
    fun plusOnlyAtTheStartWithCursor() {
        val start = com.rr.numio.phone.dialpad.Typed("971", 0)
        assertEquals("+971", DialPad.insert(start, '+').text)
        assertEquals("971", DialPad.insert(com.rr.numio.phone.dialpad.Typed("971", 2), '+').text)
        assertEquals("+971", DialPad.longPressZero(start).text)
        assertEquals("9701", DialPad.longPressZero(com.rr.numio.phone.dialpad.Typed("971", 2)).text)
    }

    @Test
    fun nothingGoesInFrontOfAPlus() {
        val t = DialPad.insert(com.rr.numio.phone.dialpad.Typed("+971", 0), '5')
        assertEquals("+5971", t.text)
        assertEquals(2, t.cursor)
    }

    @Test
    fun typingAtTheEndWorksAsBefore() {
        var t = com.rr.numio.phone.dialpad.Typed.atEnd("")
        "0501234567".forEach { t = DialPad.insert(t, it) }
        assertEquals("0501234567", t.text)
        assertEquals(10, t.cursor)
    }
}
