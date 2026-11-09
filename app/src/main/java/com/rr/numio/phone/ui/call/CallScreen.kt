package com.rr.numio.phone.ui.call

import android.telecom.CallAudioState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.phone.R
import com.rr.numio.phone.call.formatDuration
import com.rr.numio.phone.data.PhoneData
import com.rr.numio.phone.dialpad.DialPad
import com.rr.numio.phone.telecom.CallInfo
import com.rr.numio.phone.telecom.CallSnapshot
import com.rr.numio.phone.telecom.SimChoice
import com.rr.numio.phone.ui.theme.KeyDigit
import com.rr.numio.phone.ui.theme.KeyLetters
import com.rr.numio.phone.ui.theme.Numio
import kotlinx.coroutines.delay

/** Everything the call screen can ask the call engine to do. */
interface CallActions {
    fun answer(id: Int)
    fun decline(id: Int)
    fun replyWithMessage(id: Int)
    fun sendReply(id: Int, text: String)
    fun silence()
    fun hangUp(id: Int)
    fun hold(id: Int, hold: Boolean)
    fun mute(muted: Boolean)
    fun route(route: Int)
    fun dtmf(id: Int, c: Char)
    fun addCall()
    fun swap(otherId: Int)
    fun chooseSim(id: Int, sim: SimChoice)
}

@Composable
fun CallScreen(
    snapshot: CallSnapshot,
    locked: Boolean,
    clock: String,
    showReplies: Boolean,
    onCloseReplies: () -> Unit,
    actions: CallActions,
) {
    val call = snapshot.primary
    Box(
        Modifier
            .fillMaxSize()
            .background(Numio.colors.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                call == null -> EndedScreen(snapshot.ended)
                call.needsSim -> SimPicker(call, actions)
                call.isRinging -> RingingScreen(call, locked, clock, actions)
                else -> OnCallScreen(call, snapshot.other, snapshot.audio, actions)
            }
        }
        if (showReplies && call != null && call.isRinging) {
            QuickReplies(
                onPick = { actions.sendReply(call.id, it); onCloseReplies() },
                onClose = onCloseReplies,
            )
        }
    }
}

// ---------------- Ringing ----------------

@Composable
private fun ColumnScope.RingingScreen(call: CallInfo, locked: Boolean, clock: String, actions: CallActions) {
    if (locked) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_lock), null, tint = Numio.colors.textSoft, modifier = Modifier.size(14.dp))
            Text(
                stringResource(R.string.locked_time, clock),
                style = MaterialTheme.typography.bodyMedium,
                color = Numio.colors.textSoft,
            )
        }
        Spacer(Modifier.height(14.dp))
    }
    val country = PhoneData.countryLabel(LocalContext.current, call.number)
    CallLabel(listOfNotNull(stringResource(R.string.incoming_call), country, call.simLabel).joinToString(" · "))

    CallerBlock(call, pulsing = true, modifier = Modifier.weight(1f))

    // Decline · (reply, silence) · Answer, on the keypad's three columns
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(1.dp, Numio.colors.ring, CircleShape)
                        .clickable(role = Role.Button) { actions.decline(call.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_call_end),
                        contentDescription = stringResource(R.string.decline),
                        tint = Numio.colors.danger,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Text(stringResource(R.string.decline), style = MaterialTheme.typography.labelLarge, color = Numio.colors.textSoft)
        }

        Column(
            Modifier.padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SmallRing(R.drawable.ic_message, stringResource(R.string.reply_with_message), badge = locked) {
                actions.replyWithMessage(call.id)
            }
            SmallRing(R.drawable.ic_bell_off, stringResource(R.string.silence)) { actions.silence() }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BigRoundButton(
                icon = R.drawable.ic_call,
                description = stringResource(R.string.answer),
                fill = Numio.colors.accent,
                iconTint = Numio.colors.onAccent,
                halo = Numio.colors.callRing,
                onClick = { actions.answer(call.id) },
            )
            Text(
                stringResource(R.string.answer),
                style = MaterialTheme.typography.labelLarge,
                color = Numio.colors.text,
            )
        }
    }
}

@Composable
private fun SmallRing(icon: Int, description: String, badge: Boolean = false, onClick: () -> Unit) {
    Box {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.dp, Numio.colors.ring, CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), null, tint = Numio.colors.textSoft, modifier = Modifier.size(20.dp))
        }
        if (badge) {
            // Lock badge: replying needs the phone unlocked
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Numio.colors.background)
                    .border(1.dp, Numio.colors.ring, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_lock), null, tint = Numio.colors.textFaint, modifier = Modifier.size(10.dp))
            }
        }
    }
}

// ---------------- Caller ----------------

@Composable
private fun CallerBlock(call: CallInfo, pulsing: Boolean, modifier: Modifier = Modifier, below: @Composable () -> Unit = {}) {
    val hasName = !call.name.isNullOrBlank()
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (pulsing) {
            HaloRings(pulsing = true, size = 300.dp) {
                Avatar(call.title, hasName, 132.dp, Numio.colors.accent, Numio.colors.keyPressed)
            }
        } else {
            HaloRings(pulsing = false, size = 176.dp) {
                Avatar(call.title, hasName, 120.dp, Numio.colors.ring)
            }
        }
        Spacer(Modifier.height(if (pulsing) 4.dp else 18.dp))
        Text(
            call.title,
            style = CallName,
            color = Numio.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (hasName && call.number.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                call.number,
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                color = Numio.colors.textSoft,
            )
        }
        below()
    }
}

// ---------------- On a call ----------------

@Composable
private fun ColumnScope.OnCallScreen(call: CallInfo, other: CallInfo?, audio: CallAudioState?, actions: CallActions) {
    var showKeypad by rememberSaveable { mutableStateOf(false) }
    var typed by rememberSaveable { mutableStateOf("") }

    val label = when {
        call.isOutgoingStarting -> stringResource(R.string.calling)
        call.isHolding -> stringResource(R.string.on_hold)
        else -> stringResource(R.string.on_call)
    }
    val country = PhoneData.countryLabel(LocalContext.current, call.number)
    CallLabel(listOfNotNull(label, country, call.simLabel).joinToString(" · "))

    if (showKeypad) {
        // Small header so the tones you type stay visible
        Spacer(Modifier.height(20.dp))
        Text(call.title, style = CallName.copy(fontSize = 26.sp), color = Numio.colors.text, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        Text(
            typed.ifEmpty { " " },
            style = CallTimer,
            color = Numio.colors.accentInk,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
        )
        Spacer(Modifier.weight(1f))
        InCallKeypad { c ->
            typed = (typed + c).takeLast(DialPad.MAX_LENGTH)
            actions.dtmf(call.id, c)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.hide_keypad),
            style = MaterialTheme.typography.labelLarge,
            color = Numio.colors.accentInk,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { showKeypad = false }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    } else {
        CallerBlock(call, pulsing = false, modifier = Modifier.weight(1f)) {
            Spacer(Modifier.height(14.dp))
            CallStatusLine(call)
            if (other != null) {
                Spacer(Modifier.height(18.dp))
                OtherCallChip(other) { actions.swap(other.id) }
            }
        }

        val route = audio?.route ?: CallAudioState.ROUTE_EARPIECE
        val supported = audio?.supportedRouteMask ?: 0
        val speakerOn = route == CallAudioState.ROUTE_SPEAKER
        val btOn = route == CallAudioState.ROUTE_BLUETOOTH
        val btAvailable = supported and CallAudioState.ROUTE_BLUETOOTH != 0
        val muted = audio?.isMuted == true

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RingButton(R.drawable.ic_mic_off, stringResource(R.string.mute), on = muted) { actions.mute(!muted) }
                RingButton(R.drawable.ic_dialpad_dots, stringResource(R.string.keypad)) { showKeypad = true }
                RingButton(R.drawable.ic_speaker, stringResource(R.string.speaker), on = speakerOn) {
                    actions.route(if (speakerOn) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_SPEAKER)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RingButton(
                    R.drawable.ic_pause, stringResource(R.string.hold),
                    on = call.isHolding, enabled = call.canHold || call.isHolding,
                ) { actions.hold(call.id, !call.isHolding) }
                RingButton(R.drawable.ic_person_add, stringResource(R.string.add_call), enabled = call.isActive) {
                    actions.addCall()
                }
                RingButton(R.drawable.ic_bluetooth, stringResource(R.string.bluetooth), on = btOn, enabled = btAvailable) {
                    actions.route(if (btOn) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_BLUETOOTH)
                }
            }
        }
    }

    Spacer(Modifier.height(30.dp))
    BigRoundButton(
        icon = R.drawable.ic_call_end,
        description = stringResource(R.string.end_call),
        fill = Numio.colors.dangerFill,
        iconTint = Numio.colors.onDanger,
        onClick = { actions.hangUp(call.id) },
    )
}

/** Live timer once connected, else what's happening. */
@Composable
private fun CallStatusLine(call: CallInfo) {
    if (call.isActive && call.connectTimeMillis > 0) {
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(call.id) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1000)
            }
        }
        Text(
            formatDuration((now - call.connectTimeMillis) / 1000),
            style = CallTimer,
            color = Numio.colors.accentInk,
        )
    } else {
        Text(
            when {
                call.isHolding -> stringResource(R.string.on_hold)
                else -> stringResource(R.string.calling)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Numio.colors.textSoft,
        )
    }
}

@Composable
private fun OtherCallChip(other: CallInfo, onSwap: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Numio.colors.ring, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onSwap)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(R.drawable.ic_swap), null, tint = Numio.colors.accentInk, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.other_on_hold, other.title),
            style = MaterialTheme.typography.labelLarge,
            color = Numio.colors.textSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InCallKeypad(onKey: (Char) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
        DialPad.keys.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    Column(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(1.dp, Numio.colors.ring, CircleShape)
                            .clickable(role = Role.Button) { onKey(key.char) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(key.char.toString(), style = KeyDigit.copy(fontSize = 28.sp), color = Numio.colors.text)
                        Text(key.letters.ifEmpty { " " }, style = KeyLetters, color = Numio.colors.accentInk)
                    }
                }
            }
        }
    }
}

// ---------------- Choose SIM ----------------

@Composable
private fun ColumnScope.SimPicker(call: CallInfo, actions: CallActions) {
    CallLabel(stringResource(R.string.choose_sim))
    CallerBlock(call, pulsing = false, modifier = Modifier.weight(1f))
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        call.simChoices.forEach { sim ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .border(1.dp, Numio.colors.accent, RoundedCornerShape(30.dp))
                    .clickable(role = Role.Button) { actions.chooseSim(call.id, sim) },
                contentAlignment = Alignment.Center,
            ) {
                Text(sim.label, style = MaterialTheme.typography.titleMedium, color = Numio.colors.text)
            }
        }
    }
    Spacer(Modifier.height(30.dp))
    BigRoundButton(
        icon = R.drawable.ic_call_end,
        description = stringResource(R.string.end_call),
        fill = Numio.colors.dangerFill,
        iconTint = Numio.colors.onDanger,
        onClick = { actions.hangUp(call.id) },
    )
}

// ---------------- Ended ----------------

@Composable
private fun ColumnScope.EndedScreen(ended: CallInfo?) {
    CallLabel(stringResource(R.string.call_ended), color = Numio.colors.textSoft)
    if (ended != null) {
        CallerBlock(ended, pulsing = false, modifier = Modifier.weight(1f)) {
            if (ended.connectTimeMillis > 0) {
                Spacer(Modifier.height(14.dp))
                Text(
                    formatDuration((System.currentTimeMillis() - ended.connectTimeMillis) / 1000),
                    style = CallTimer,
                    color = Numio.colors.textSoft,
                )
            }
        }
    } else {
        Spacer(Modifier.weight(1f))
    }
}

// ---------------- Quick replies ----------------

@Composable
private fun QuickReplies(onPick: (String) -> Unit, onClose: () -> Unit) {
    val replies = stringArrayResource(R.array.quick_replies)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Numio.colors.background)
                .border(1.dp, Numio.colors.ring, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CallLabel(stringResource(R.string.reply_with_message))
            Spacer(Modifier.height(4.dp))
            replies.forEach { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Numio.colors.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Numio.colors.ring, RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button) { onPick(text) }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                )
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}
