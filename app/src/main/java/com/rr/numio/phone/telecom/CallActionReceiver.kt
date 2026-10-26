package com.rr.numio.phone.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Decline / hang-up buttons on the call notification. Not exported. */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DECLINE -> CallManager.declineRinging()
            ACTION_HANG_UP -> CallManager.hangUpPrimary()
        }
    }

    companion object {
        const val ACTION_DECLINE = "com.rr.numio.phone.DECLINE"
        const val ACTION_HANG_UP = "com.rr.numio.phone.HANG_UP"
    }
}
