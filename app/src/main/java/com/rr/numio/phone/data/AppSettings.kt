package com.rr.numio.phone.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rr.numio.phone.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** App settings, stored only on the phone with DataStore. */
class AppSettings(context: Context) {
    private val store = context.applicationContext.settingsStore
    private val themeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = store.data.map { ThemeMode.fromKey(it[themeKey]) }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[themeKey] = mode.key }
    }

    // Keypad feedback: vibration on by default (it only works while the phone's own
    // touch vibration is on); key tones off by default
    private val vibrateKey = booleanPreferencesKey("keypad_vibrate")
    private val tonesKey = booleanPreferencesKey("keypad_tones")

    val keypadVibrate: Flow<Boolean> = store.data.map { it[vibrateKey] ?: true }
    val keypadTones: Flow<Boolean> = store.data.map { it[tonesKey] ?: false }

    suspend fun setKeypadVibrate(on: Boolean) {
        store.edit { it[vibrateKey] = on }
    }

    suspend fun setKeypadTones(on: Boolean) {
        store.edit { it[tonesKey] = on }
    }

    /** Speed dial: key 1–9 → the number it calls. */
    val speedDial: Flow<Map<Int, String>> = store.data.map { prefs ->
        (1..9).mapNotNull { d -> prefs[speedKey(d)]?.takeIf { it.isNotBlank() }?.let { d to it } }.toMap()
    }

    suspend fun setSpeedDial(digit: Int, number: String?) {
        store.edit { if (number.isNullOrBlank()) it.remove(speedKey(digit)) else it[speedKey(digit)] = number }
    }

    private fun speedKey(digit: Int) = stringPreferencesKey("speed_dial_$digit")
}
