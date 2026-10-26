package com.rr.numio.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import com.rr.numio.phone.data.AppSettings
import com.rr.numio.phone.ui.PhoneApp
import com.rr.numio.phone.ui.theme.PhonebyNumioTheme
import com.rr.numio.phone.ui.theme.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = AppSettings(this)

        setContent {
            val scope = rememberCoroutineScope()
            val mode by settings.themeMode.collectAsState(initial = ThemeMode.DEFAULT)
            val dark = mode.isDark(isSystemInDarkTheme())

            // Status and navigation bar icons follow the app's theme, not the phone's
            val view = window.decorView
            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }

            val versionName = remember {
                packageManager.getPackageInfo(packageName, 0).versionName ?: ""
            }

            PhonebyNumioTheme(dark = dark) {
                PhoneApp(
                    themeMode = mode,
                    onThemeMode = { scope.launch { settings.setThemeMode(it) } },
                    versionName = versionName,
                )
            }
        }
    }
}
