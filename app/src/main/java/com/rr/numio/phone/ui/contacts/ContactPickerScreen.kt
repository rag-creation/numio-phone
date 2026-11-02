package com.rr.numio.phone.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rr.numio.phone.R
import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.ContactSearch
import com.rr.numio.phone.data.PhoneData
import com.rr.numio.phone.ui.common.DetailTopBar
import com.rr.numio.phone.ui.common.PersonAvatar
import com.rr.numio.phone.ui.theme.Numio

/** Pick which saved contact a number should be added to. */
@Composable
fun ContactPickerScreen(
    number: String,
    contacts: List<Contact>,
    onBack: () -> Unit,
    onPick: (Contact) -> Unit,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(contacts, query) { contacts.filter { ContactSearch.matches(it, query) } }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        DetailTopBar(onBack = onBack, backLabel = stringResource(R.string.back))
        Text(
            stringResource(R.string.add_to_existing_title),
            style = MaterialTheme.typography.titleLarge,
            color = Numio.colors.text,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            PhoneData.format(context, number),
            style = MaterialTheme.typography.bodyMedium,
            color = Numio.colors.accentInk,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        SearchField(query, onChange = { query = it })
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown, key = { it.id }) { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onPick(c) }
                        .padding(horizontal = 24.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PersonAvatar(c.name, c.photoUri, 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.bodyLarge, color = Numio.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        c.numbers.firstOrNull()?.let { n ->
                            Text(
                                PhoneData.format(context, n.number),
                                style = MaterialTheme.typography.bodySmall,
                                color = Numio.colors.textSoft,
                                maxLines = 1,
                            )
                        }
                    }
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
