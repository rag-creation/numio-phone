package com.rr.numio.phone.ui.recents

import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
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
    onLongPress: (String) -> Unit,
) {
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
    // Day label in front of the first group of each day
    val labelled = remember(groups) {
        var lastDay: LocalDate? = null
        groups.map { g ->
            val day = CallGrouping.day(g.latest.timeMillis)
            val showDay = day != lastDay
            lastDay = day
            Triple(g, day, showDay)
        }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        items(labelled, key = { it.first.latest.id }) { (group, day, showDay) ->
            Column {
                if (showDay) SectionLabel(dayLabel(day, today))
                val photo = contacts.firstOrNull { c -> c.numbers.any { PhoneNumbers.same(it.number, group.number) } }?.photoUri
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
