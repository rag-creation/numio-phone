package com.rr.numio.phone.ui.theme

// Plain Kotlin (no Android), so it can be unit tested on the computer.

/** The appearance choice in Settings. */
enum class ThemeMode(val key: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system");

    /** True when the app should draw in dark colours. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        DARK -> true
        LIGHT -> false
        SYSTEM -> systemDark
    }

    companion object {
        val DEFAULT = SYSTEM

        /** Reads a saved value; anything unknown falls back to [DEFAULT]. */
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
