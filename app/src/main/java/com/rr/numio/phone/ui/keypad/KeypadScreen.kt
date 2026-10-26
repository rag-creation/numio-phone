package com.rr.numio.phone.ui.keypad

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.rr.numio.phone.dialpad.DialKey
import com.rr.numio.phone.dialpad.DialPad
import com.rr.numio.phone.ui.theme.KeyDigit
import com.rr.numio.phone.ui.theme.KeyLetters
import com.rr.numio.phone.ui.theme.Numio
import com.rr.numio.phone.ui.theme.NumberDisplay

/**
 * Dial pad. In this first version it only types the number: placing calls
 * comes with the Telecom milestone, so the call button explains that instead.
 */
@Composable
fun KeypadScreen() {
    var number by rememberSaveable { mutableStateOf("") }
    var showNotice by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current
    fun tap() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        // Number + thin amber line under it
        Text(
            text = number,
            style = NumberDisplay,
            color = Numio.colors.text,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = number },
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .width(if (number.isEmpty()) 24.dp else 56.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Numio.colors.accent),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (showNotice) stringResource(R.string.calling_not_yet) else "",
            style = MaterialTheme.typography.bodySmall,
            color = Numio.colors.textSoft,
            textAlign = TextAlign.Center,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        // 4 × 3 keys
        DialPad.keys.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { key ->
                    DialKeyButton(
                        key = key,
                        onClick = {
                            tap()
                            showNotice = false
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

        Spacer(Modifier.height(14.dp))

        // Call button in the middle, backspace on the right
        Row(
            Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(72.dp))
            CallButton(onClick = { tap(); showNotice = true })
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKeyButton(key: DialKey, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (pressed) Numio.colors.surfacePressed else Numio.colors.surface)
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
        Text(key.char.toString(), style = KeyDigit, color = Numio.colors.text)
        // Every key keeps the same height, with or without letters
        Text(key.letters.ifEmpty { " " }, style = KeyLetters, color = Numio.colors.textSoft)
    }
}

@Composable
private fun CallButton(onClick: () -> Unit) {
    val description = stringResource(R.string.call)
    Box(
        Modifier
            .size(72.dp)
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
