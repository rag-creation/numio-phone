package com.rr.numio.phone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun PhonebyNumioTheme(dark: Boolean, content: @Composable () -> Unit) {
    val palette = if (dark) Palettes.Dark else Palettes.Light
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        background = palette.background,
        onBackground = palette.text,
        surface = palette.background,
        onSurface = palette.text,
        surfaceVariant = palette.surface,
        onSurfaceVariant = palette.textSoft,
        outline = palette.divider,
    )
    CompositionLocalProvider(LocalPhonePalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = PhoneTypography, content = content)
    }
}

/** Short way for screens to read the active palette: `Numio.colors.accent`. */
object Numio {
    val colors: PhonePalette
        @Composable @ReadOnlyComposable get() = LocalPhonePalette.current
}
