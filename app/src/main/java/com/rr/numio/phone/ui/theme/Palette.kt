package com.rr.numio.phone.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Official Numio palette (getnumio.org). Never swap Numio amber for a generic mustard.
// Only the background, text and accent values below are brand colours; the in-between
// greys are tints of them for surfaces, lines and secondary text.
data class PhonePalette(
    val background: Color,
    val surface: Color,      // setting rows
    val ring: Color,         // thin outline of a dial key
    val keyPressed: Color,   // amber wash inside a pressed key
    val callRing: Color,     // soft halo around the call button
    val divider: Color,
    val text: Color,
    val textSoft: Color,     // secondary text, letters under digits
    val textFaint: Color,    // hints
    val accent: Color,       // amber fills: call button, selected markers
    val accentInk: Color,    // amber used as text or a thin line
    val onAccent: Color,     // text / icons on an amber fill
    val danger: Color,       // decline icon, muted red
    val dangerFill: Color,   // end-call button
    val onDanger: Color,
    val isLight: Boolean,
)

object Palettes {
    val Dark = PhonePalette(
        background = Color(0xFF0F0F0E),
        surface = Color(0xFF1A1A18),
        ring = Color(0xFF2E2D29),
        keyPressed = Color(0x1AFFC21A),
        callRing = Color(0x59FFC21A),
        divider = Color(0xFF262622),
        text = Color(0xFFF1EEE6),
        textSoft = Color(0xFFA9A59B),
        textFaint = Color(0xFF8A867D),
        accent = Color(0xFFFFC21A),
        accentInk = Color(0xFFFFC21A),
        onAccent = Color(0xFF1A1400),
        danger = Color(0xFFE06A55),
        dangerFill = Color(0xFFC9432F),
        onDanger = Color(0xFFFFF6F2),
        isLight = false,
    )

    val Light = PhonePalette(
        background = Color(0xFFF6F4EE),
        surface = Color(0xFFECE9E0),
        ring = Color(0xFFD3CEC1),
        keyPressed = Color(0x24E9A800),
        callRing = Color(0x73E9A800),
        divider = Color(0xFFDEDAD0),
        text = Color(0xFF191815),
        textSoft = Color(0xFF5F5B52),
        textFaint = Color(0xFF75716A),
        accent = Color(0xFFE9A800),
        accentInk = Color(0xFF7A5600),
        onAccent = Color(0xFF1A1400),
        danger = Color(0xFFB0402D),
        dangerFill = Color(0xFFC9432F),
        onDanger = Color(0xFFFFF6F2),
        isLight = true,
    )
}

val LocalPhonePalette = staticCompositionLocalOf { Palettes.Dark }
