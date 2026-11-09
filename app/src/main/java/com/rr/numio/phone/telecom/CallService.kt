package com.rr.numio.phone.telecom

import android.app.KeyguardManager
import android.content.Intent
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.rr.numio.phone.call.CallActivity

/**
 * Android binds this service when Phone by Numio is the default phone app.
 * It receives every call (incoming and outgoing) and hands it to [CallManager].
 * Ringing and vibration are done by Android itself.
 */
class CallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        CallManager.service = this
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.onCallAdded(this, call)
        val info = CallManager.state.value.calls.lastOrNull()
        // Outgoing calls open the call screen straight away. An incoming call opens it
        // too when the phone is locked or the screen is off: the screen wakes itself, and
        // it works even when Android doesn't allow our full-screen notification (Samsung
        // and sideloaded apps). When you're using the phone, the call card shows instead.
        // Without notification access we always open the screen, so a call is never missed.
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        val lockedOrOff = !power.isInteractive || keyguard.isKeyguardLocked
        if (info == null || !info.isRinging || lockedOrOff || !CallNotifications.lastPostOk) {
            startActivity(
                Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.onCallRemoved(call)
    }

    @Deprecated("Still the simplest way to follow speaker/earpiece/Bluetooth on all versions")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        @Suppress("DEPRECATION")
        super.onCallAudioStateChanged(audioState)
        CallManager.onAudioChanged(audioState)
    }

    override fun onDestroy() {
        CallManager.service = null
        CallManager.clear()
        CallNotifications.cancelAll(this)
        super.onDestroy()
    }
}
