package com.rr.numio.phone

import com.rr.numio.phone.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun darkAndLightIgnoreThePhoneSetting() {
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
    }

    @Test
    fun systemFollowsThePhone() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
    }

    @Test
    fun savedKeysRoundTripAndUnknownFallsBack() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromKey(it.key)) }
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromKey("purple"))
    }
}
