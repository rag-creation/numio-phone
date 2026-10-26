package com.rr.numio.phone.telecom

import android.content.Intent
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
        // Incoming calls show through the call notification (full screen when locked,
        // a card when unlocked). Outgoing calls open the call screen straight away.
        // Without notification access we open the screen directly, so a call is never missed.
        if (info == null || !info.isRinging || !CallNotifications.lastPostOk) {
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
