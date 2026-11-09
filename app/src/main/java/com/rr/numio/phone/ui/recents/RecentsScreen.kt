package com.rr.numio.phone.ui.recents

import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rr.numio.phone.R
import com.rr.numio.phone.data.CallGroup
import com.rr.numio.phone.data.CallGrouping
import com.rr.numio.phone.data.CallKind
import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.PhoneData
import com.rr.numio.phone.data.PhoneNumbers
import com.rr.numio.phone.ui.EmptyState
import com.rr.numio.phone.ui.common.PermissionPrompt
import com.rr.numio.phone.ui.common.PersonAvatar
import com.rr.numio.phone.ui.common.RingIconButton
import com.rr.numio.phone.ui.common.SectionLabel
import com.rr.numio.phone.ui.theme.Numio
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

/** Call history from Android's own call log, newest first, grouped by day. */
@Composable
fun RecentsScreen(
    hasPermission: Boolean,
    groups: List<CallGroup>,
    contacts: List<Contact>,
    onAllow: () -> Unit,
    onOpen: (String) -> Unit,
    onCall: (String) -> Unit,
    onMessage: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    var missedOnly by rememberSaveable { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    if (!hasPermission) {
        PermissionPrompt(
            icon = R.drawable.ic_recents,
            title = stringResource(R.string.recents_permission_title),
            body = stringResource(R.string.recents_permission_body),
            button = stringResource(R.string.allow_call_history),
            onClick = onAllow,
        )
        return
    }
    if (groups.isEmpty()) {
        EmptyState(
            icon = R.drawable.ic_recents,
            title = stringResource(R.string.recents_none_title),
            body = stringResource(R.string.recents_none_body),
        )
        return
    }

    val today = LocalDate.now()
    // "Missed" filter: only the missed calls of each group
    val shownGroups = remember(groups, missedOnly) {
        if (!missedOnly) groups
        else groups.mapNotNull { g ->
            g.calls.filter { it.kind == CallKind.MISSED }.takeIf { it.isNotEmpty() }?.let { g.copy(calls = it) }
        }
    }
    // Day label in front of the first group of each day
    val labelled = remember(shownGroups) {
        var lastDay: LocalDate? = null
        shownGroups.map { g ->
            val day = CallGrouping.day(g.latest.timeMillis)
            val showDay = day != lastDay
            lastDay = day
            Triple(g, day, showDay)
        }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            FilterBar(
                missedOnly = missedOnly,
                onMissedOnly = { missedOnly = it },
                onClear = { confirmClear = true },
            )
        }
        if (labelled.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_missed_calls),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Numio.colors.textSoft,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        items(labelled, key = { it.first.latest.id }) { (group, day, showDay) ->
            Column {
                if (showDay) SectionLabel(dayLabel(day, today))
                val photo = contacts.firstOrNull { c -> c.numbers.any { PhoneNumbers.same(it.number, group.number) } }?.photoUri
                // Swipe right to call, left to message
                SwipeRow(
                    enabled = group.number.isNotBlank(),
                    onSwipeRight = { onCall(group.number) },
                    onSwipeLeft = { onMessage(group.number) },
                ) {
                    RecentRow(
                        group,
                        photo,
                        onOpen = { onOpen(group.number) },
                        onCall = { onCall(group.number) },
                        onLongPress = { onLongPress(group.number) },
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Numio.colors.background,
            title = { Text(stringResource(R.string.clear_history_title), color = Numio.colors.text) },
            text = { Text(stringResource(R.string.clear_history_body), color = Numio.colors.textSoft) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearAll() }) {
                    Text(stringResource(R.string.delete), color = Numio.colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel), color = Numio.colors.text)
                }
            },
        )
    }
}

/** All / Missed, and a bin to clear the history. */
@Composable
private fun FilterBar(missedOnly: Boolean, onMissedOnly: (Boolean) -> Unit, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(stringResource(R.string.filter_all), selected = !missedOnly) { onMissedOnly(false) }
        Spacer(Modifier.width(8.dp))
        FilterChip(stringResource(R.string.filter_missed), selected = missedOnly) { onMissedOnly(true) }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClear),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_trash),
                stringResource(R.string.clear_history_title),
                tint = Numio.colors.textSoft,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Numio.colors.onAccent else Numio.colors.textSoft,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Numio.colors.accent else Color.Transparent)
            .border(1.dp, if (selected) Numio.colors.accent else Numio.colors.ring, RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/**
 * Slide a row sideways: right shows an amber call, left a message. Past a third of the
 * width the action runs and the row springs back.
 */
@Composable
private fun SwipeRow(
    enabled: Boolean,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val right by rememberUpdatedState(onSwipeRight)
    val left by rememberUpdatedState(onSwipeLeft)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = constraints.maxWidth.toFloat()
        val threshold = width / 3f
        val x = offset.value
        if (x != 0f) {
            val toCall = x > 0
            Row(
                Modifier
                    .matchParentSize()
                    .background(if (toCall) Numio.colors.accent else Numio.colors.surface)
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (toCall) Arrangement.Start else Arrangement.End,
            ) {
                Icon(
                    painterResource(if (toCall) R.drawable.ic_call else R.drawable.ic_message),
                    null,
                    tint = if (toCall) Numio.colors.onAccent else Numio.colors.accentInk,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(Numio.colors.background)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val v = offset.value
                                if (v > threshold) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    right()
                                } else if (v < -threshold) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    left()
                                }
                                offset.animateTo(0f)
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f) } },
                    ) { change, amount ->
                        change.consume()
                        scope.launch { offset.snapTo((offset.value + amount).coerceIn(-width, width)) }
                    }
                },
        ) {
            content()
        }
    }
}

@Composable
fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> stringResource(R.string.today)
    today.minusDays(1) -> stringResource(R.string.yesterday)
    else -> day.format(DateTimeFormatter.ofPattern(if (day.year == today.year) "EEE d MMM" else "d MMM yyyy"))
}

@Composable
fun timeText(millis: Long): String {
    val context = LocalContext.current
    return DateFormat.getTimeFormat(context).format(Date(millis))
}

@Composable
fun kindIcon(kind: CallKind): Int = when (kind) {
    CallKind.OUTGOING -> R.drawable.ic_call_out
    CallKind.MISSED -> R.drawable.ic_call_missed
    CallKind.REJECTED -> R.drawable.ic_call_end
    CallKind.BLOCKED -> R.drawable.ic_block
    CallKind.INCOMING -> R.drawable.ic_call_in
}

@Composable
fun kindLabel(kind: CallKind): String = stringResource(
    when (kind) {
        CallKind.OUTGOING -> R.string.kind_outgoing
        CallKind.MISSED -> R.string.kind_missed
        CallKind.REJECTED -> R.string.kind_declined
        CallKind.BLOCKED -> R.string.kind_blocked
        CallKind.INCOMING -> R.string.kind_incoming
    },
)

@Composable
fun kindColor(kind: CallKind): Color =
    if (kind == CallKind.MISSED) Numio.colors.danger else Numio.colors.textSoft

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentRow(group: CallGroup, photoUri: String?, onOpen: () -> Unit, onCall: () -> Unit, onLongPress: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val latest = group.latest
    val missed = latest.kind == CallKind.MISSED
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                role = Role.Button,
                onClick = onOpen,
                // Hidden numbers can't be copied or saved
                onLongClick = if (group.number.isNotBlank()) {
                    {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLongPress()
                    }
                } else {
                    null
                },
            )
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonAvatar(group.name, photoUri, 46.dp, ring = if (missed) Numio.colors.danger.copy(alpha = 0.6f) else Numio.colors.ring)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            val unknown = stringResource(R.string.unknown)
            val shown = group.name?.takeIf { it.isNotBlank() }
                ?: PhoneData.format(context, group.number).ifBlank { unknown }
            Text(
                shown,
                style = MaterialTheme.typography.bodyLarge,
                color = if (missed) Numio.colors.danger else Numio.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(kindIcon(latest.kind)), null, tint = kindColor(latest.kind), modifier = Modifier.size(14.dp))
                val parts = buildList {
                    if (group.calls.size > 1) add("(${group.calls.size})")
                    latest.simLabel?.let { add(it) }
                    PhoneData.countryFlag(context, group.number)?.let { add(it) }
                    add(timeText(latest.timeMillis))
                }
                Text(
                    parts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Numio.colors.textSoft,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        if (group.number.isNotBlank()) {
            RingIconButton(R.drawable.ic_call, stringResource(R.string.call), onClick = onCall)
        }
    }
}
