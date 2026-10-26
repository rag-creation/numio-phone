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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * "Halo" dial pad: thin ring keys, light digits, amber letters, and an amber
 * call button with a soft ring around it. In this version it only types the
 * number: placing calls comes with the Telecom milestone.
 */
@Composable
fun KeypadScreen() {
    var number by rememberSaveable { mutableStateOf("") }
    var showNotice by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current
    fun tap() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

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
        Spacer(Modifier.height(8.dp))
        GlowLine(wide = number.isNotEmpty())
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (showNotice) stringResource(R.string.calling_not_yet) else "",
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

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth().padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(76.dp))
            CallButton(onClick = { tap(); showNotice = true })
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
