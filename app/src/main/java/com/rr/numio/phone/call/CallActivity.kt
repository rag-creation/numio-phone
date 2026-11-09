package com.rr.numio.phone.call

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.telecom.CallAudioState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rr.numio.phone.MainActivity
import com.rr.numio.phone.data.AppSettings
import com.rr.numio.phone.telecom.CallManager
import com.rr.numio.phone.telecom.CallNotifications
import com.rr.numio.phone.telecom.CallSnapshot
import com.rr.numio.phone.telecom.SimChoice
import com.rr.numio.phone.ui.call.CallActions
import com.rr.numio.phone.ui.call.CallScreen
import com.rr.numio.phone.ui.theme.PhonebyNumioTheme
import com.rr.numio.phone.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * The ringing and on-a-call screen. Shows over the lock screen and turns the
 * screen on for an incoming call. Closes itself when the last call ends.
 */
class CallActivity : ComponentActivity() {

    private var showReplies by mutableStateOf(false)
    private var locked by mutableStateOf(false)
    private var proximityLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge()
        handleIntent(intent)

        val settings = AppSettings(this)
        val keyguard = getSystemService(KeyguardManager::class.java)

        val actions = object : CallActions {
            override fun answer(id: Int) = CallManager.answer(id)
            override fun decline(id: Int) = CallManager.decline(id)
            override fun replyWithMessage(id: Int) {
                // Replying sends a text, so the phone must be unlocked first
                if (keyguard.isKeyguardLocked) {
                    keyguard.requestDismissKeyguard(
                        this@CallActivity,
                        object : KeyguardManager.KeyguardDismissCallback() {
                            override fun onDismissSucceeded() {
                                showReplies = true
                            }
                        },
                    )
                } else {
                    showReplies = true
                }
            }
            override fun sendReply(id: Int, text: String) = CallManager.declineWithMessage(id, text)
            override fun silence() = CallManager.silenceRinger(this@CallActivity)
            override fun hangUp(id: Int) = CallManager.hangUp(id)
            override fun hold(id: Int, hold: Boolean) = if (hold) CallManager.hold(id) else CallManager.unhold(id)
            override fun mute(muted: Boolean) = CallManager.setMuted(muted)
            override fun route(route: Int) = CallManager.setRoute(route)
            override fun dtmf(id: Int, c: Char) = CallManager.dtmf(id, c)
            override fun addCall() {
                startActivity(Intent(this@CallActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            override fun swap(otherId: Int) = CallManager.unhold(otherId)
            override fun chooseSim(id: Int, sim: SimChoice) = CallManager.chooseSim(id, sim)
        }

        setContent {
            val mode by settings.themeMode.collectAsState(initial = ThemeMode.DEFAULT)
            val dark = mode.isDark(isSystemInDarkTheme())
            val snapshot by CallManager.state.collectAsState()

            val view = window.decorView
            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }

            // Close shortly after the last call is gone
            LaunchedEffect(snapshot.primary == null, snapshot.ended == null) {
                if (snapshot.primary == null) {
                    if (snapshot.ended != null) delay(1300)
                    finish()
                }
            }

            PhonebyNumioTheme(dark = dark) {
                CallScreen(
                    snapshot = snapshot,
                    locked = locked,
                    clock = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()),
                    showReplies = showReplies,
                    onCloseReplies = { showReplies = false },
                    actions = actions,
                )
            }
        }

        // Screen off when the phone is at your ear (earpiece calls only), unless turned off in Settings
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(CallManager.state, settings.proximity) { snapshot, on -> snapshot to on }
                    .collect { (snapshot, on) -> updateProximity(snapshot, on) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        CallNotifications.screenVisible = true
        CallManager.refreshNotification()
    }

    override fun onStop() {
        super.onStop()
        CallNotifications.screenVisible = false
        CallManager.refreshNotification()
    }

    override fun onResume() {
        super.onResume()
        locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_ANSWER) {
            CallManager.state.value.calls.firstOrNull { it.isRinging }?.let { CallManager.answer(it.id) }
        }
    }

    private fun updateProximity(snapshot: CallSnapshot, enabled: Boolean) {
        val call = snapshot.primary
        val route = snapshot.audio?.route ?: CallAudioState.ROUTE_EARPIECE
        val wanted = enabled && call != null && (call.isActive || call.isOutgoingStarting) &&
            route == CallAudioState.ROUTE_EARPIECE
        val lock = proximityLock ?: run {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) return
            pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "numio:proximity").also { proximityLock = it }
        }
        if (wanted && !lock.isHeld) {
            lock.acquire(4 * 60 * 60 * 1000L)
        } else if (!wanted && lock.isHeld) {
            lock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
        }
    }

    override fun onDestroy() {
        proximityLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    companion object {
        const val ACTION_ANSWER = "com.rr.numio.phone.ANSWER"
    }
}
