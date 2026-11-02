package com.rr.numio.phone.ui.contacts

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rr.numio.phone.R
import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.ContactSearch
import com.rr.numio.phone.data.PhoneData
import com.rr.numio.phone.ui.EmptyState
import com.rr.numio.phone.ui.common.PermissionPrompt
import com.rr.numio.phone.ui.common.PersonAvatar
import com.rr.numio.phone.ui.common.RingIconButton
import com.rr.numio.phone.ui.common.SectionLabel
import com.rr.numio.phone.ui.theme.Numio

/** Contacts from the phone: search, favourites, then A–Z. */
@Composable
fun ContactsScreen(
    hasPermission: Boolean,
    contacts: List<Contact>,
    onAllow: () -> Unit,
    onOpen: (Contact) -> Unit,
    onCall: (String) -> Unit,
    onAdd: () -> Unit,
    onLongPress: (Contact) -> Unit,
) {
    if (!hasPermission) {
        PermissionPrompt(
            icon = R.drawable.ic_contacts,
            title = stringResource(R.string.contacts_permission_title),
            body = stringResource(R.string.contacts_permission_body),
            button = stringResource(R.string.allow_contacts),
            onClick = onAllow,
        )
        return
    }

    var query by rememberSaveable { mutableStateOf("") }
    val favourites = remember(contacts) { contacts.filter { it.starred } }
    val shown = remember(contacts, query) { contacts.filter { ContactSearch.matches(it, query) } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SearchField(query, onChange = { query = it }) }
            RingIconButton(R.drawable.ic_plus, stringResource(R.string.new_contact), size = 48.dp, tint = Numio.colors.accentInk, onClick = onAdd)
        }
        if (contacts.isEmpty()) {
            EmptyState(
                icon = R.drawable.ic_contacts,
                title = stringResource(R.string.contacts_none_title),
                body = stringResource(R.string.contacts_none_body),
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (query.isBlank() && favourites.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.favourites)) }
                item {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(favourites, key = { it.id }) { c -> FavouriteChip(c, onClick = { onOpen(c) }, onLongPress = { onLongPress(c) }) }
                    }
                }
            }
            val seen = HashSet<String>()
            shown.forEach { c ->
                val section = ContactSearch.section(c.name)
                // One letter heading per section (a symbol-name sorted later never repeats "#")
                if (query.isBlank() && seen.add(section)) {
                    item(key = "s_$section") { SectionLabel(section) }
                }
                item(key = c.id) {
                    ContactRow(
                        c,
                        onOpen = { onOpen(c) },
                        onCall = { c.numbers.firstOrNull()?.let { onCall(it.number) } },
                        onLongPress = { onLongPress(c) },
                    )
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_matches),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Numio.colors.textSoft,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
internal fun SearchField(query: String, onChange: (String) -> Unit) {
    val hint = stringResource(R.string.search_contacts)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Numio.colors.ring, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_search), null, tint = Numio.colors.textSoft, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(hint, style = MaterialTheme.typography.bodyLarge, color = Numio.colors.textFaint)
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Numio.colors.text),
                cursorBrush = SolidColor(Numio.colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
            )
        }
        if (query.isNotEmpty()) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.clear), tint = Numio.colors.textSoft, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavouriteChip(contact: Contact, onClick: () -> Unit, onLongPress: () -> Unit) {
    val view = LocalView.current
    Column(
        Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongPress()
                },
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PersonAvatar(contact.name, contact.photoUri, 56.dp, ring = Numio.colors.accent)
        Spacer(Modifier.height(6.dp))
        Text(
            contact.name.substringBefore(' '),
            style = MaterialTheme.typography.labelMedium,
            color = Numio.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactRow(contact: Contact, onOpen: () -> Unit, onCall: () -> Unit, onLongPress: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                role = Role.Button,
                onClick = onOpen,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongPress()
                },
            )
            .padding(horizontal = 24.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonAvatar(contact.name, contact.photoUri, 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(contact.name, style = MaterialTheme.typography.bodyLarge, color = Numio.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            contact.numbers.firstOrNull()?.let { n ->
                Text(
                    listOfNotNull(n.label, PhoneData.format(context, n.number)).joinToString(" · ") +
                        if (contact.numbers.size > 1) "  +${contact.numbers.size - 1}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Numio.colors.textSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        if (contact.numbers.isNotEmpty()) {
            RingIconButton(R.drawable.ic_call, stringResource(R.string.call), onClick = onCall)
        }
    }
}
