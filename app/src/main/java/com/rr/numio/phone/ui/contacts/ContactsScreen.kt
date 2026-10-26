package com.rr.numio.phone.ui.contacts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rr.numio.phone.R
import com.rr.numio.phone.ui.EmptyState

// Contacts come in a later milestone (needs READ_CONTACTS, asked for only then).
@Composable
fun ContactsScreen() {
    EmptyState(
        icon = R.drawable.ic_contacts,
        title = stringResource(R.string.contacts_empty_title),
        body = stringResource(R.string.contacts_empty_body),
    )
}
