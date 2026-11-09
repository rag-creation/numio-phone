package com.rr.numio.phone.telecom

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import com.rr.numio.phone.data.PhoneData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the call screens need to know about one call. */
data class CallInfo(
    val id: Int,
    val state: Int,
    val number: String,
    val name: String?,
    val simLabel: String?,
    val connectTimeMillis: Long,
    val canHold: Boolean,
    val isConference: Boolean,
    val simChoices: List<SimChoice>,
) {
    val isRinging get() = state == Call.STATE_RINGING || state == STATE_SIMULATED_RINGING
    val isOutgoingStarting get() = state == Call.STATE_DIALING || state == Call.STATE_CONNECTING ||
        state == Call.STATE_NEW || state == STATE_PULLING
    val isActive get() = state == Call.STATE_ACTIVE
    val isHolding get() = state == Call.STATE_HOLDING
    val needsSim get() = state == Call.STATE_SELECT_PHONE_ACCOUNT
    val isEnded get() = state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING

    /** Name if known, else the number. */
    val title get() = name?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Unknown" }

    companion object {
        // Constants newer than our min SDK, same values as Call.STATE_*
        const val STATE_PULLING = 11
        const val STATE_SIMULATED_RINGING = 13
    }
}

data class SimChoice(val handle: PhoneAccountHandle, val label: String)

data class CallSnapshot(
    val calls: List<CallInfo> = emptyList(),
    val audio: CallAudioState? = null,
    /** The call that just ended, shown for a moment as "Call ended". */
    val ended: CallInfo? = null,
) {
    /** The call the screen should show: ringing first, then starting, then active, then held. */
    val primary: CallInfo?
        get() = calls.firstOrNull { it.isRinging }
            ?: calls.firstOrNull { it.needsSim }
            ?: calls.firstOrNull { it.isOutgoingStarting }
            ?: calls.firstOrNull { it.isActive }
            ?: calls.firstOrNull { it.isHolding }

    /** A second call waiting on hold behind the primary one. */
    val other: CallInfo?
        get() = primary?.let { p -> calls.firstOrNull { it.id != p.id && !it.isEnded } }
}

/**
 * Holds the live calls Android hands to [CallService] and turns them into a
 * [CallSnapshot] the screens observe. All actions go through here.
 */
object CallManager {
    private val calls = mutableListOf<Call>()
    private val _state = MutableStateFlow(CallSnapshot())
    val state: StateFlow<CallSnapshot> = _state.asStateFlow()

    internal var service: CallService? = null
    private var appContext: Context? = null
    private val main = Handler(Looper.getMainLooper())

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = publish()
        override fun onDetailsChanged(call: Call, details: Call.Details) = publish()
        override fun onConferenceableCallsChanged(call: Call, conferenceableCalls: List<Call>) = publish()
    }

    internal fun onCallAdded(context: Context, call: Call) {
        appContext = context.applicationContext
        calls += call
        call.registerCallback(callback)
        publish(ended = null)
    }

    internal fun onCallRemoved(call: Call) {
        val info = toInfo(call)
        call.unregisterCallback(callback)
        calls -= call
        publish(ended = info)
        // Forget the "Call ended" note after a moment
        main.postDelayed({ if (_state.value.ended?.id == info.id) publish(ended = null) }, 1500)
    }

    /** Re-post the notification, e.g. when the call screen opens or closes. */
    fun refreshNotification() {
        appContext?.let { CallNotifications.update(it, _state.value) }
    }

    internal fun onAudioChanged(audio: CallAudioState?) {
        _state.value = _state.value.copy(audio = audio)
    }

    internal fun clear() {
        names.clear()
        calls.forEach { it.unregisterCallback(callback) }
        calls.clear()
        _state.value = CallSnapshot()
    }

    private fun publish(ended: CallInfo? = _state.value.ended) {
        val snapshot = CallSnapshot(
            calls = calls.map { toInfo(it) },
            audio = _state.value.audio,
            ended = ended,
        )
        _state.value = snapshot
        appContext?.let { CallNotifications.update(it, snapshot) }
    }

    private fun find(id: Int) = calls.firstOrNull { idOf(it) == id }
    private fun idOf(call: Call) = System.identityHashCode(call)

    @Suppress("DEPRECATION")
    private fun stateOf(call: Call): Int =
        if (Build.VERSION.SDK_INT >= 31) call.details.state else call.state

    private fun toInfo(call: Call): CallInfo {
        val d = call.details
        val number = d.handle?.schemeSpecificPart.orEmpty()
        val fromAndroid = if (Build.VERSION.SDK_INT >= 30) {
            d.contactDisplayName ?: d.callerDisplayName
        } else {
            d.callerDisplayName
        }
        // Android often leaves the name empty; look it up in contacts when we're allowed to
        val name = fromAndroid?.takeIf { it.isNotBlank() } ?: lookUpName(number)
        return CallInfo(
            id = idOf(call),
            state = stateOf(call),
            number = number,
            name = name,
            simLabel = d.accountHandle?.let { labelOf(it) },
            connectTimeMillis = d.connectTimeMillis,
            canHold = d.can(Call.Details.CAPABILITY_HOLD),
            isConference = d.hasProperty(Call.Details.PROPERTY_CONFERENCE),
            simChoices = if (stateOf(call) == Call.STATE_SELECT_PHONE_ACCOUNT) simChoices(call) else emptyList(),
        )
    }

    private val names = HashMap<String, String?>()

    private fun lookUpName(number: String): String? {
        if (number.isBlank()) return null
        return names.getOrPut(number) { appContext?.let { PhoneData(it).nameFor(number) } }
    }

    private fun simChoices(call: Call): List<SimChoice> {
        @Suppress("DEPRECATION")
        val handles = call.details.intentExtras
            ?.getParcelableArrayList<PhoneAccountHandle>(Call.AVAILABLE_PHONE_ACCOUNTS)
            ?: return emptyList()
        return handles.mapIndexed { i, h -> SimChoice(h, labelOf(h) ?: "SIM ${i + 1}") }
    }

    private fun labelOf(handle: PhoneAccountHandle): String? = try {
        val telecom = appContext?.getSystemService(TelecomManager::class.java)
        telecom?.getPhoneAccount(handle)?.label?.toString()
    } catch (e: SecurityException) {
        null
    }

    // ---- Actions ----

    fun answer(id: Int) = find(id)?.answer(0) ?: Unit
    fun decline(id: Int) = find(id)?.reject(false, null) ?: Unit
    fun declineWithMessage(id: Int, text: String) = find(id)?.reject(true, text) ?: Unit
    fun hangUp(id: Int) = find(id)?.disconnect() ?: Unit
    fun hold(id: Int) = find(id)?.hold() ?: Unit
    fun unhold(id: Int) = find(id)?.unhold() ?: Unit
    fun chooseSim(id: Int, sim: SimChoice) = find(id)?.phoneAccountSelected(sim.handle, false) ?: Unit

    fun dtmf(id: Int, c: Char) {
        val call = find(id) ?: return
        call.playDtmfTone(c)
        call.stopDtmfTone()
    }

    /** Ends whichever call is showing (used by the notification's hang-up button). */
    fun hangUpPrimary() = _state.value.primary?.let { hangUp(it.id) } ?: Unit
    fun declineRinging() = _state.value.calls.firstOrNull { it.isRinging }?.let { decline(it.id) } ?: Unit

    fun setMuted(muted: Boolean) = service?.setMuted(muted) ?: Unit

    fun setRoute(route: Int) = service?.setAudioRoute(route) ?: Unit

    @android.annotation.SuppressLint("MissingPermission")
    fun silenceRinger(context: Context) {
        try {
            context.getSystemService(TelecomManager::class.java)?.silenceRinger()
        } catch (e: SecurityException) {
            // Only the default phone app may do this
        }
    }
}
