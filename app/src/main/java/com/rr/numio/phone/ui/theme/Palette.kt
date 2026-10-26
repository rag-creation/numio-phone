package com.rr.numio.phone.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Official Numio palette (getnumio.org). Never swap Numio amber for a generic mustard.
// Only the background, text and accent values below are brand colours; the in-between
// greys are tints of them for surfaces, lines and secondary text.
data class PhonePalette(
    val background: Color,
    val surface: Color,      // dial keys, setting rows
    val surfacePressed: Color,
    val divider: Color,
    val text: Color,
    val textSoft: Color,     // secondary text, letters under digits
    val textFaint: Color,    // hints
    val accent: Color,       // amber fills: call button, selected markers
    val accentInk: Color,    // amber used as text or a thin line
    val onAccent: Color,     // text / icons on an amber fill
    val isLight: Boolean,
)

object Palettes {
    val Dark = PhonePalette(
        background = Color(0xFF0F0F0E),
        surface = Color(0xFF1A1A18),
        surfacePressed = Color(0xFF2A2925),
        divider = Color(0xFF262622),
        text = Color(0xFFF1EEE6),
        textSoft = Color(0xFFA9A59B),
        textFaint = Color(0xFF8A867D),
        accent = Color(0xFFFFC21A),
        accentInk = Color(0xFFFFC21A),
        onAccent = Color(0xFF1A1400),
        isLight = false,
    )

    val Light = PhonePalette(
        background = Color(0xFFF6F4EE),
        surface = Color(0xFFECE9E0),
        surfacePressed = Color(0xFFDFDBD0),
        divider = Color(0xFFDEDAD0),
        text = Color(0xFF191815),
        textSoft = Color(0xFF5F5B52),
        textFaint = Color(0xFF75716A),
        accent = Color(0xFFE9A800),
        accentInk = Color(0xFF7A5600),
        onAccent = Color(0xFF1A1400),
        isLight = true,
    )
}

val LocalPhonePalette = staticCompositionLocalOf { Palettes.Dark }
