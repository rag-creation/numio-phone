package com.rr.numio.phone.ui.keypad

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rr.numio.phone.R
import com.rr.numio.phone.data.Clip
import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.PhoneData
import com.rr.numio.phone.dialpad.SmartDial
import com.rr.numio.phone.dialpad.SmartDialMatch
import com.rr.numio.phone.ui.common.PersonAvatar
import com.rr.numio.phone.dialpad.DialKey
import com.rr.numio.phone.dialpad.DialPad
import com.rr.numio.phone.ui.theme.KeyDigit
import com.rr.numio.phone.ui.theme.KeyLetters
import com.rr.numio.phone.ui.theme.Numio
import com.rr.numio.phone.ui.theme.NumberDisplay

/**
 * "Halo" dial pad: thin ring keys, light digits, amber letters, and an amber
 * call button with a soft ring around it.
 */
@Composable
fun KeypadScreen(
    dialRequest: String?,
    onDialRequestUsed: () -> Unit,
    notice: String?,
    contacts: List<Contact>,
    onCall: (String) -> Unit,
    onSaveNumber: (String) -> Unit,
) {
    var number by rememberSaveable { mutableStateOf("") }
    var numberMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // Smart dial: contacts whose name (by the letters on the keys) or number matches
    val matches = remember(number, contacts) { SmartDial.search(contacts, number) }
    val view = LocalView.current
    // A number handed over by another app (a tel: link) fills the keypad
    LaunchedEffect(dialRequest) {
        if (dialRequest != null) {
            number = DialPad.clean(dialRequest)
            onDialRequestUsed()
        }
    }
    fun tap() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            // Rows that don't fit on a small screen are cut off at the top, never over the keys
            Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            verticalArrangement = Arrangement.Bottom,
        ) {
            matches.forEach { match ->
                SmartDialRow(match, onClick = { tap(); onCall(match.number.number) })
            }
            if (matches.isNotEmpty()) Spacer(Modifier.height(8.dp))
        }

        // Long press the number: copy, paste or save it
        Box(
            Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        numberMenu = true
                    })
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = NumberDisplay,
                color = Numio.colors.text,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .semantics { contentDescription = number },
            )
            DropdownMenu(
                expanded = numberMenu,
                onDismissRequest = { numberMenu = false },
                containerColor = Numio.colors.background,
            ) {
                if (number.isNotEmpty()) {
                    NumberMenuItem(R.drawable.ic_copy, stringResource(R.string.copy_number)) {
                        numberMenu = false
                        Clip.copyNumber(context, number)
                    }
                }
                NumberMenuItem(R.drawable.ic_paste, stringResource(R.string.paste)) {
                    numberMenu = false
                    Clip.pasteText(context)?.let { pasted ->
                        val cleaned = DialPad.clean(pasted)
                        if (cleaned.isNotEmpty()) number = cleaned
                    }
                }
                if (number.isNotEmpty()) {
                    NumberMenuItem(R.drawable.ic_person_add, stringResource(R.string.save_to_contacts)) {
                        numberMenu = false
                        onSaveNumber(number)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        GlowLine(wide = number.isNotEmpty())
        Spacer(Modifier.height(10.dp))
        Text(
            text = notice.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = Numio.colors.textSoft,
            textAlign = TextAlign.Center,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        DialPad.keys.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { key ->
                    HaloKey(
                        key = key,
                        onClick = {
                            tap()
                            number = DialPad.append(number, key.char)
                        },
                        onLongClick = if (key.char == '0') {
                            {
                                tap()
                                number = DialPad.longPressZero(number)
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth().padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(76.dp))
            CallButton(onClick = { tap(); if (number.isNotEmpty()) onCall(number) })
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                if (number.isNotEmpty()) {
                    BackspaceButton(
                        onClick = { tap(); number = DialPad.backspace(number) },
                        onLongClick = { tap(); number = "" },
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberMenuItem(icon: Int, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyLarge, color = Numio.colors.text) },
        leadingIcon = { Icon(painterResource(icon), null, tint = Numio.colors.accentInk, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
    )
}

// Thin amber line under the number; in dark mode it has a soft glow
@Composable
private fun GlowLine(wide: Boolean) {
    val width = if (wide) 56.dp else 24.dp
    val accent = Numio.colors.accent
    Box(Modifier.width(width + 16.dp).height(10.dp), contentAlignment = Alignment.Center) {
        if (!Numio.colors.isLight) {
            Box(
                Modifier
                    .width(width + 12.dp)
                    .height(10.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, accent.copy(alpha = 0.28f), Color.Transparent),
                        ),
                        RoundedCornerShape(5.dp),
                    ),
            )
        }
        Box(
            Modifier
                .width(width)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(accent),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HaloKey(key: DialKey, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = Numio.colors
    Column(
        Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(if (pressed) colors.keyPressed else Color.Transparent)
            .border(1.dp, if (pressed) colors.accent else colors.ring, CircleShape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(key.char.toString(), style = KeyDigit, color = colors.text)
        Spacer(Modifier.height(2.dp))
        // Every key keeps the same height, with or without letters
        Text(key.letters.ifEmpty { " " }, style = KeyLetters, color = colors.accentInk)
    }
}

@Composable
private fun CallButton(onClick: () -> Unit) {
    val description = stringResource(R.string.call)
    Box(
        Modifier
            .size(92.dp)
            .border(1.dp, Numio.colors.callRing, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Numio.colors.accent)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_call),
                contentDescription = null,
                tint = Numio.colors.onAccent,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackspaceButton(onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_backspace),
            contentDescription = stringResource(R.string.backspace),
            tint = Numio.colors.textSoft,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** A contact found by smart dial: tap to call. */
@Composable
private fun SmartDialRow(match: SmartDialMatch, onClick: () -> Unit) {
    val context = LocalContext.current
    val shown = PhoneData.format(context, match.number.number)
    val description = stringResource(R.string.call) + " " + match.contact.name
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonAvatar(match.contact.name, match.contact.photoUri, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                match.contact.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Numio.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(match.number.label, shown).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (match.byName) Numio.colors.textSoft else Numio.colors.accentInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painterResource(R.drawable.ic_call),
            contentDescription = null,
            tint = Numio.colors.accent,
            modifier = Modifier.size(20.dp),
        )
    }
}
