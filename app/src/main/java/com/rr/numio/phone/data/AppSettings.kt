package com.rr.numio.phone.data

import android.content.Context
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
}
