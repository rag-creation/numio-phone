package com.rr.numio.phone.telecom

import android.Manifest
import android.content.pm.PackageManager
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.rr.numio.phone.data.AppSettings
import com.rr.numio.phone.data.BlockRules
import com.rr.numio.phone.data.PhoneData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android asks the default phone app about every incoming call before it rings.
 * Here Numio's blocking rules are applied: private/hidden numbers, numbers not in
 * your contacts, and blocked country codes. Numbers on the blocked list are already
 * stopped by Android itself. Everything is decided on the phone.
 */
class ScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(details: Call.Details) {
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(details, CallResponse.Builder().build())
            return
        }
        scope.launch {
            val block = try {
                shouldBlock(details)
            } catch (e: Exception) {
                false // never block because something went wrong
            }
            val response = if (block) {
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
                    .setSkipCallLog(false) // still listed in Recents as blocked
                    .build()
            } else {
                CallResponse.Builder().build()
            }
            respondToCall(details, response)
        }
    }

    private suspend fun shouldBlock(details: Call.Details): Boolean {
        val settings = AppSettings(this)
        val number = details.handle?.schemeSpecificPart.orEmpty()
        val hidden = number.isBlank() || details.handlePresentation != TelecomManager.PRESENTATION_ALLOWED
        if (hidden) return settings.blockPrivate.first()

        val codes = settings.blockedCodes.first()
        if (BlockRules.matchesCode(number, codes, PhoneData.countryIso(this))) return true

        if (settings.blockUnknown.first()) {
            // Without contacts access everyone would look unknown: then block no one
            val canRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
            if (canRead && PhoneData(this).nameFor(number) == null) return true
        }
        return false
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
