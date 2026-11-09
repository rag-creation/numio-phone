package com.rr.numio.phone.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rr.numio.phone.R
import com.rr.numio.phone.ui.contacts.ContactsScreen
import com.rr.numio.phone.ui.contacts.ContactDetailScreen
import com.rr.numio.phone.ui.recents.NumberDetailScreen
import com.rr.numio.phone.data.CallGrouping
import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.ContactDraft
import com.rr.numio.phone.ui.contacts.ContactEditScreen
import com.rr.numio.phone.data.PhoneData
import com.rr.numio.phone.data.PhoneNumbers
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.rr.numio.phone.ui.keypad.KeypadScreen
import com.rr.numio.phone.ui.recents.RecentsScreen
import com.rr.numio.phone.ui.settings.SettingsScreen
import com.rr.numio.phone.ui.theme.Numio
import com.rr.numio.phone.ui.theme.NumioWordmark
import com.rr.numio.phone.ui.theme.ThemeMode
import androidx.compose.runtime.LaunchedEffect

enum class Tab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    RECENTS(R.string.tab_recents, R.drawable.ic_recents),
    CONTACTS(R.string.tab_contacts, R.drawable.ic_contacts),
    KEYPAD(R.string.tab_keypad, R.drawable.ic_keypad),
}

/** The app shell: three tabs, details pages and Settings. Permissions are asked for only from a tab's own button. */
@Composable
fun PhoneApp(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    versionName: String,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onOpenDefaultApps: () -> Unit,
    canFullScreen: Boolean,
    onAllowFullScreen: () -> Unit,
    dialRequest: String?,
    onDialRequestUsed: () -> Unit,
    notice: String?,
    onCall: (String) -> Unit,
    phone: PhoneData,
    permissionTick: Int,
    onAllowCallLog: () -> Unit,
    onAllowContacts: () -> Unit,
    onMessage: (String) -> Unit,
    onOpenContact: (Contact) -> Unit,
    onSaveContact: (ContactDraft, (Boolean) -> Unit) -> Unit,
    onDeleteContact: (Long, (Boolean) -> Unit) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.KEYPAD) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    // Details page on top of the tabs: "n:<number>" for a number, "c:<id>" for a contact
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    // Contact editor on top of everything: "new", "new:<number>" or "edit:<id>"
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    // Read straight from Android's call log and contacts; re-read when access changes
    val hasLog = remember(permissionTick) { phone.hasCallLog() }
    val hasContacts = remember(permissionTick) { phone.hasContacts() }
    val entries by remember(permissionTick) { phone.callLog() }.collectAsState(initial = emptyList())
    val contacts by remember(permissionTick) { phone.contacts() }.collectAsState(initial = emptyList())
    val groups = remember(entries) { CallGrouping.group(entries) }
    // A tel: link from another app always lands on the keypad
    LaunchedEffect(dialRequest) {
        if (dialRequest != null) {
            tab = Tab.KEYPAD
            showSettings = false
            detail = null
            editor = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Numio.colors.background),
    ) {
        val editing = editor
        val current = detail
        if (editing != null) {
            BackHandler { editor = null }
            val existing = if (editing.startsWith("edit:")) {
                contacts.firstOrNull { it.id.toString() == editing.removePrefix("edit:") }
            } else {
                null
            }
            if (editing.startsWith("edit:") && existing == null) {
                // The contact was deleted elsewhere
                LaunchedEffect(editing) { editor = null }
                return@Box
            }
            val prefill = if (editing.startsWith("new:")) editing.removePrefix("new:") else null
            val initial = existing?.let { ContactDraft.from(it) } ?: ContactDraft.new(prefill)
            ContactEditScreen(
                initial = initial,
                photoUri = existing?.photoUri,
                saving = saving,
                onCancel = { editor = null },
                onSave = { draft ->
                    saving = true
                    onSaveContact(draft) { ok ->
                        saving = false
                        if (ok) editor = null
                    }
                },
                onDelete = existing?.let { c ->
                    {
                        saving = true
                        onDeleteContact(c.id) { ok ->
                            saving = false
                            if (ok) {
                                editor = null
                                detail = null
                            }
                        }
                    }
                },
            )
        } else if (current != null && !showSettings) {
            BackHandler { detail = null }
            if (current.startsWith("n:")) {
                val number = current.removePrefix("n:")
                NumberDetailScreen(
                    number = number,
                    name = groups.firstOrNull { PhoneNumbers.same(it.number, number) }?.name,
                    contact = contacts.firstOrNull { c -> c.numbers.any { PhoneNumbers.same(it.number, number) } },
                    history = entries.filter { PhoneNumbers.same(it.number, number) },
                    onBack = { detail = null },
                    onCall = { onCall(number) },
                    onMessage = { onMessage(number) },
                    onOpenContact = {
                        contacts.firstOrNull { c -> c.numbers.any { PhoneNumbers.same(it.number, number) } }?.let(onOpenContact)
                    },
                    onAddContact = { editor = "new:$number" },
                )
            } else {
                val contact = contacts.firstOrNull { it.id.toString() == current.removePrefix("c:") }
                if (contact == null) {
                    LaunchedEffect(current) { detail = null }
                } else {
                    ContactDetailScreen(
                        contact = contact,
                        recent = entries.filter { e -> contact.numbers.any { PhoneNumbers.same(it.number, e.number) } },
                        onBack = { detail = null },
                        onCall = onCall,
                        onMessage = onMessage,
                        onOpenInContacts = { onOpenContact(contact) },
                        onEdit = { editor = "edit:${contact.id}" },
                    )
                }
            }
        } else if (showSettings) {
            BackHandler { showSettings = false }
            SettingsScreen(
                themeMode = themeMode,
                onThemeMode = onThemeMode,
                versionName = versionName,
                isDefault = isDefault,
                onSetDefault = onSetDefault,
                onOpenDefaultApps = onOpenDefaultApps,
                canFullScreen = canFullScreen,
                onAllowFullScreen = onAllowFullScreen,
                onBack = { showSettings = false },
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                TopBar(onSettings = { showSettings = true })
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        Tab.RECENTS -> RecentsScreen(
                            hasPermission = hasLog,
                            groups = groups,
                            contacts = contacts,
                            onAllow = onAllowCallLog,
                            onOpen = { detail = "n:$it" },
                            onCall = onCall,
                        )
                        Tab.CONTACTS -> ContactsScreen(
                            hasPermission = hasContacts,
                            contacts = contacts,
                            onAllow = onAllowContacts,
                            onOpen = { detail = "c:${it.id}" },
                            onCall = onCall,
                            onAdd = { editor = "new" },
                        )
                        Tab.KEYPAD -> KeypadScreen(
                            dialRequest = dialRequest,
                            onDialRequestUsed = onDialRequestUsed,
                            notice = notice,
                            onCall = onCall,
                        )
                    }
                }
                BottomTabs(selected = tab, onSelect = { tab = it })
            }
        }
    }
}

// Quiet signature top-left: the small "Numio." wordmark, then the settings gear
@Composable
private fun TopBar(onSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumioWordmark(height = 15.dp)
        Spacer(Modifier.weight(1f))
        RoundIconButton(
            icon = R.drawable.ic_settings,
            description = stringResource(R.string.settings),
            onClick = onSettings,
        )
    }
}

@Composable
fun RoundIconButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = description,
            tint = Numio.colors.textSoft,
            modifier = Modifier.size(22.dp),
        )
    }
}

// Quiet tab row: icon + label, a short amber line marks the open tab
@Composable
private fun BottomTabs(selected: Tab, onSelect: (Tab) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Numio.colors.divider))
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Tab.entries.forEach { t ->
                val isSelected = t == selected
                val color = if (isSelected) Numio.colors.text else Numio.colors.textFaint
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Tab) { onSelect(t) }
                        .semantics { this.selected = isSelected }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painterResource(t.icon),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(t.label),
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isSelected) Numio.colors.accent else Numio.colors.background),
                    )
                }
            }
        }
    }
}
