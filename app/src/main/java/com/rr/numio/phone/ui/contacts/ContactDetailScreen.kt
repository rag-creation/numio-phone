package com.rr.numio.phone.ui.contacts

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rr.numio.phone.R
import com.rr.numio.phone.call.formatDuration
import com.rr.numio.phone.data.CallEntry
import com.rr.numio.phone.data.CallGrouping
import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.PhoneData
import com.rr.numio.phone.ui.call.CallName
import com.rr.numio.phone.ui.common.DetailTopBar
import com.rr.numio.phone.ui.common.PersonAvatar
import com.rr.numio.phone.ui.common.RingIconButton
import com.rr.numio.phone.ui.common.SectionLabel
import com.rr.numio.phone.ui.recents.dayLabel
import com.rr.numio.phone.ui.recents.kindColor
import com.rr.numio.phone.ui.recents.kindIcon
import com.rr.numio.phone.ui.recents.kindLabel
import com.rr.numio.phone.ui.recents.timeText
import com.rr.numio.phone.ui.theme.Numio
import java.time.LocalDate

/** A contact: their numbers (call / message each) and recent calls with them. */
@Composable
fun ContactDetailScreen(
    contact: Contact,
    recent: List<CallEntry>,
    onBack: () -> Unit,
    onCall: (String) -> Unit,
    onMessage: (String) -> Unit,
    onOpenInContacts: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        DetailTopBar(onBack = onBack, backLabel = stringResource(R.string.back)) {
            if (contact.starred) {
                Icon(painterResource(R.drawable.ic_star), stringResource(R.string.favourite), tint = Numio.colors.accentInk, modifier = Modifier.padding(12.dp).size(22.dp))
            }
            RingIconButton(R.drawable.ic_pencil, stringResource(R.string.edit_contact), onClick = onEdit)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    PersonAvatar(contact.name, contact.photoUri, 120.dp, ring = if (contact.starred) Numio.colors.accent else Numio.colors.ring)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        contact.name,
                        style = CallName.copy(fontSize = CallName.fontSize * 0.85f),
                        color = Numio.colors.text,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item { SectionLabel(stringResource(R.string.numbers), Modifier.padding(top = 16.dp)) }
            items(contact.numbers, key = { it.number }) { n ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onCall(n.number) }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(PhoneData.format(context, n.number), style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"), color = Numio.colors.text)
                        n.label?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Numio.colors.textSoft) }
                    }
                    RingIconButton(R.drawable.ic_message, stringResource(R.string.message)) { onMessage(n.number) }
                    Spacer(Modifier.width(10.dp))
                    RingIconButton(R.drawable.ic_call, stringResource(R.string.call), tint = Numio.colors.accentInk) { onCall(n.number) }
                }
            }
            if (recent.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.recent_calls), Modifier.padding(top = 12.dp)) }
                items(recent.take(10), key = { it.id }) { e ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(kindIcon(e.kind)), null, tint = kindColor(e.kind), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            kindLabel(e.kind) + " · " + dayLabel(CallGrouping.day(e.timeMillis), today) + " · " + timeText(e.timeMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = Numio.colors.textSoft,
                            modifier = Modifier.weight(1f),
                        )
                        if (e.durationSec > 0) {
                            Text(formatDuration(e.durationSec), style = MaterialTheme.typography.bodySmall, color = Numio.colors.textSoft)
                        }
                    }
                }
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Numio.colors.ring, RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClick = onOpenInContacts)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.open_in_contacts), style = MaterialTheme.typography.labelLarge, color = Numio.colors.text)
                }
            }
        }
    }
}
