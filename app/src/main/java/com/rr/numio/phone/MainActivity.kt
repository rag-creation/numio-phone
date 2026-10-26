package com.rr.numio.phone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.rr.numio.phone.data.AppSettings
import com.rr.numio.phone.dialpad.DialPad
import com.rr.numio.phone.telecom.DialerRole
import com.rr.numio.phone.ui.PhoneApp
import com.rr.numio.phone.ui.theme.PhonebyNumioTheme
import com.rr.numio.phone.ui.theme.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var isDefault by mutableStateOf(false)
    private var dialRequest by mutableStateOf<String?>(null)
    private var notice by mutableStateOf<String?>(null)

    /** The number waiting while Android asks about the role or permissions. */
    private var pendingCall: String? = null

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = DialerRole.isDefault(this)
        if (isDefault) {
            notice = null
            val waiting = pendingCall
            pendingCall = null
            if (waiting != null) call(waiting) else askPermissions()
        } else {
            pendingCall = null
            notice = getString(R.string.need_default)
        }
    }

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val number = pendingCall ?: return@registerForActivityResult
        pendingCall = null
        if (has(Manifest.permission.CALL_PHONE)) {
            place(number)
        } else {
            notice = getString(R.string.need_call_permission)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = AppSettings(this)
        handleDialIntent(intent)

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
                    isDefault = isDefault,
                    onSetDefault = { requestRole() },
                    onOpenDefaultApps = { startActivity(DialerRole.defaultAppsIntent()) },
                    dialRequest = dialRequest,
                    onDialRequestUsed = { dialRequest = null },
                    notice = notice,
                    onCall = { call(it) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isDefault = DialerRole.isDefault(this)
        if (isDefault && notice == getString(R.string.need_default)) notice = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
    }

    /** DIAL / VIEW with a tel: number from another app: put it on the keypad (never auto-call). */
    private fun handleDialIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_DIAL && intent?.action != Intent.ACTION_VIEW) return
        val data = intent.data ?: return
        if (data.scheme != "tel") return
        val number = DialPad.clean(Uri.decode(data.schemeSpecificPart.orEmpty()))
        if (number.isNotEmpty()) dialRequest = number
    }

    private fun requestRole() {
        val request = DialerRole.requestIntent(this)
        if (request == null) {
            notice = getString(R.string.role_unavailable)
            return
        }
        roleRequest.launch(request)
    }

    /** Call button: become the default phone app, get permission, then place the call. */
    private fun call(number: String) {
        notice = null
        if (!DialerRole.isDefault(this)) {
            pendingCall = number
            requestRole()
            return
        }
        val missing = neededPermissions().filterNot { has(it) }
        if (missing.isNotEmpty()) {
            pendingCall = number
            permissionRequest.launch(missing.toTypedArray())
            return
        }
        place(number)
    }

    private fun place(number: String) {
        if (!DialerRole.placeCall(this, number)) notice = getString(R.string.call_failed)
    }

    /** After becoming the default phone app, ask for notifications so incoming calls can show. */
    private fun askPermissions() {
        val missing = neededPermissions().filterNot { has(it) }
        if (missing.isNotEmpty()) permissionRequest.launch(missing.toTypedArray())
    }

    private fun neededPermissions(): List<String> = buildList {
        add(Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun has(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
