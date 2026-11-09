package com.rr.numio.phone.ui.recents

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rr.numio.phone.R
import com.rr.numio.phone.call.formatDuration
import com.rr.numio.phone.data.CallEntry
import com.rr.numio.phone.data.Clip
import com.rr.numio.phone.data.CallGrouping
import com.rr.numio.phone.data.CallKind
import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.PhoneData
import com.rr.numio.phone.ui.call.CallName
import com.rr.numio.phone.ui.call.RingButton
import com.rr.numio.phone.ui.common.BlockRow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.rr.numio.phone.ui.common.DetailTopBar
import com.rr.numio.phone.ui.common.PersonAvatar
import com.rr.numio.phone.ui.common.SectionLabel
import com.rr.numio.phone.ui.theme.Numio
import java.time.LocalDate

/** One number: who it is, quick actions, and every call with it. */
@Composable
fun NumberDetailScreen(
    number: String,
    name: String?,
    contact: Contact?,
    history: List<CallEntry>,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onOpenContact: () -> Unit,
    onAddContact: () -> Unit,
    canBlock: Boolean,
    blocked: Boolean,
    onToggleBlock: () -> Unit,
    onDeleteCalls: (List<Long>) -> Unit,
) {
    // A call to delete (long-press on a history row), or -1 for the whole history
    var confirmDelete by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val today = LocalDate.now()
    val title = contact?.name ?: name?.takeIf { it.isNotBlank() } ?: PhoneData.format(context, number)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        DetailTopBar(onBack = onBack, backLabel = stringResource(R.string.back))
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                val view = LocalView.current
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    PersonAvatar(title, contact?.photoUri, 112.dp)
                    Spacer(Modifier.height(16.dp))
                    // Long press the name or number to copy the number
                    Column(
                        Modifier.pointerInput(number) {
                            detectTapGestures(onLongPress = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                Clip.copyNumber(context, number)
                            })
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(title, style = CallName.copy(fontSize = CallName.fontSize * 0.85f), color = Numio.colors.text, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (contact != null || !name.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(PhoneData.format(context, number), style = MaterialTheme.typography.bodyLarge, color = Numio.colors.textSoft)
                        }
                        PhoneData.countryLabel(context, number)?.let { country ->
                            Spacer(Modifier.height(4.dp))
                            Text(country, style = MaterialTheme.typography.bodyMedium, color = Numio.colors.textSoft)
                        }
                    }
                    Spacer(Modifier.height(26.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RingButton(R.drawable.ic_call, stringResource(R.string.call), size = 64.dp, onClick = onCall)
                        RingButton(R.drawable.ic_message, stringResource(R.string.message), size = 64.dp, onClick = onMessage)
                        if (contact != null) {
                            RingButton(R.drawable.ic_contacts, stringResource(R.string.contact), size = 64.dp, onClick = onOpenContact)
                        } else {
                            RingButton(R.drawable.ic_open_contact, stringResource(R.string.add_contact), size = 64.dp, onClick = onAddContact)
                        }
                    }
                }
            }
            item { SectionLabel(stringResource(R.string.history), Modifier.padding(top = 12.dp)) }
            items(history, key = { it.id }) { e -> HistoryRow(e, today, onLongPress = { confirmDelete = e.id }) }
            if (history.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Numio.colors.ring, RoundedCornerShape(14.dp))
                            .clickable(role = Role.Button) { confirmDelete = -1L }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(R.drawable.ic_trash), null, tint = Numio.colors.textSoft, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_number_history), style = MaterialTheme.typography.labelLarge, color = Numio.colors.text)
                    }
                }
            }
            if (canBlock && number.isNotBlank()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    BlockRow(blocked, R.string.block_number, R.string.unblock_number, onToggleBlock)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    confirmDelete?.let { which ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = Numio.colors.background,
            title = {
                Text(
                    stringResource(if (which == -1L) R.string.delete_number_history else R.string.delete_this_call),
                    color = Numio.colors.text,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    onDeleteCalls(if (which == -1L) history.map { it.id } else listOf(which))
                }) {
                    Text(stringResource(R.string.delete), color = Numio.colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text(stringResource(R.string.cancel), color = Numio.colors.text)
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(e: CallEntry, today: LocalDate, onLongPress: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(kindIcon(e.kind)), null, tint = kindColor(e.kind), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                kindLabel(e.kind) + (e.simLabel?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = if (e.kind == CallKind.MISSED) Numio.colors.danger else Numio.colors.text,
            )
            Text(
                dayLabel(CallGrouping.day(e.timeMillis), today) + " · " + timeText(e.timeMillis),
                style = MaterialTheme.typography.bodySmall,
                color = Numio.colors.textSoft,
            )
        }
        if (e.durationSec > 0) {
            Text(
                formatDuration(e.durationSec),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = Numio.colors.textSoft,
            )
        }
    }
}
