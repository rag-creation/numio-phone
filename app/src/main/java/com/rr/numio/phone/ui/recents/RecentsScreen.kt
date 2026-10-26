package com.rr.numio.phone.ui.recents

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rr.numio.phone.R
import com.rr.numio.phone.ui.EmptyState

// Call history comes in a later milestone (needs READ_CALL_LOG, asked for only then).
@Composable
fun RecentsScreen() {
    EmptyState(
        icon = R.drawable.ic_recents,
        title = stringResource(R.string.recents_empty_title),
        body = stringResource(R.string.recents_empty_body),
    )
}
