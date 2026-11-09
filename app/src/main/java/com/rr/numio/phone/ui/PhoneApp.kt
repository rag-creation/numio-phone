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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import com.rr.numio.phone.data.AppSettings
import com.rr.numio.phone.data.Blocking
import com.rr.numio.phone.telecom.MissedCalls
import com.rr.numio.phone.data.CallKind
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.rr.numio.phone.data.Clip
import com.rr.numio.phone.data.ContactNumber
import com.rr.numio.phone.data.PhoneType
import com.rr.numio.phone.ui.common.ActionSheet
import com.rr.numio.phone.ui.common.SheetAction
import com.rr.numio.phone.ui.contacts.ContactPickerScreen
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
    recentsRequest: Int,
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
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val speedDial by settings.speedDial.collectAsState(initial = emptyMap())
    val keypadVibrate by settings.keypadVibrate.collectAsState(initial = true)
    val keypadTones by settings.keypadTones.collectAsState(initial = false)
    // Speed dial key being set up (contact picker open), and a contact with several numbers to choose from
    var speedPick by rememberSaveable { mutableStateOf<Int?>(null) }
    var speedChoice by remember { mutableStateOf<Contact?>(null) }
    val proximity by settings.proximity.collectAsState(initial = true)
    val blockPrivate by settings.blockPrivate.collectAsState(initial = false)
    val blockUnknown by settings.blockUnknown.collectAsState(initial = false)
    val blockedCodes by settings.blockedCodes.collectAsState(initial = emptySet())
    var tab by rememberSaveable { mutableStateOf(Tab.KEYPAD) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    // Details page on top of the tabs: "n:<number>" for a number, "c:<id>" for a contact
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    // Contact editor on top of everything: "new", "new:<number>" or "edit:<id>"
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    // Long-press sheet: "n:<number>" (a call), "c:<id>" (a contact), "s:<number>" (save a number)
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    // "Add to existing contact" picker for this number
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Read straight from Android's call log and contacts; re-read when access changes
    val hasLog = remember(permissionTick) { phone.hasCallLog() }
    val hasContacts = remember(permissionTick) { phone.hasContacts() }
    val entries by remember(permissionTick) { phone.callLog() }.collectAsState(initial = emptyList())
    val contacts by remember(permissionTick) { phone.contacts() }.collectAsState(initial = emptyList())
    val groups = remember(entries) { CallGrouping.group(entries) }

    // Android's blocked list (readable only by the default phone app); re-read after a change
    var blockTick by remember { mutableStateOf(0) }
    val blocking = remember { Blocking(context) }
    val canBlock = remember(isDefault, blockTick) { blocking.canBlock() }
    val blocked = remember(isDefault, blockTick) { blocking.list() }
    fun isBlocked(number: String) = blocked.any { it.number == number || PhoneNumbers.same(it.number, number) }
    fun setBlocked(numbers: List<String>, block: Boolean) {
        val ok = numbers.all { if (block) isBlocked(it) || blocking.block(it) else blocking.unblock(it) || !isBlocked(it) }
        blockTick++
        val message = when {
            !ok -> R.string.block_failed
            block -> R.string.blocked_done
            else -> R.string.unblocked_done
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // The missed-call notification was tapped: open Recents
    LaunchedEffect(recentsRequest) {
        if (recentsRequest > 0) {
            tab = Tab.RECENTS
            showSettings = false
            detail = null
            editor = null
            picker = null
            sheet = null
        }
    }
    // Looking at Recents counts as seeing the missed calls
    LaunchedEffect(tab, isDefault) {
        if (tab == Tab.RECENTS && isDefault) MissedCalls.markSeen(context)
    }
    // A tel: link from another app always lands on the keypad
    LaunchedEffect(dialRequest) {
        if (dialRequest != null) {
            tab = Tab.KEYPAD
            showSettings = false
            detail = null
            editor = null
            picker = null
            sheet = null
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
            // "add:<id>:<number>" opens that contact with the number added at the end
            val adding = if (editing.startsWith("add:")) {
                val rest = editing.removePrefix("add:")
                val id = rest.substringBefore(':')
                contacts.firstOrNull { it.id.toString() == id }?.let { c -> c to rest.substringAfter(':') }
            } else {
                null
            }
            if (editing.startsWith("add:") && adding == null) {
                LaunchedEffect(editing) { editor = null }
                return@Box
            }
            val prefill = if (editing.startsWith("new:")) editing.removePrefix("new:") else null
            val initial = when {
                existing != null -> ContactDraft.from(existing)
                adding != null -> ContactDraft.from(adding.first).let { d ->
                    d.copy(numbers = d.numbers + ContactNumber(adding.second, null, PhoneType.MOBILE))
                }
                else -> ContactDraft.new(prefill)
            }
            val target = existing ?: adding?.first
            ContactEditScreen(
                initial = initial,
                photoUri = target?.photoUri,
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
        } else if (speedPick != null) {
            val digit = speedPick ?: 1
            BackHandler { speedPick = null }
            fun save(n: String) {
                scope.launch { settings.setSpeedDial(digit, n) }
                speedChoice = null
                speedPick = null
            }
            ContactPickerScreen(
                title = stringResource(R.string.speed_dial_pick_title, digit),
                subtitle = stringResource(R.string.speed_dial_pick_hint, digit),
                contacts = contacts,
                onBack = { speedPick = null },
                onPick = { c ->
                    if (c.numbers.size == 1) save(c.numbers.first().number) else speedChoice = c
                },
            )
            speedChoice?.let { c ->
                AlertDialog(
                    onDismissRequest = { speedChoice = null },
                    containerColor = Numio.colors.background,
                    title = { Text(c.name, color = Numio.colors.text) },
                    text = {
                        Column {
                            c.numbers.forEach { n ->
                                Text(
                                    listOfNotNull(n.label, PhoneData.format(context, n.number)).joinToString(" · "),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Numio.colors.text,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(role = Role.Button) { save(n.number) }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                )
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { speedChoice = null }) {
                            Text(stringResource(R.string.cancel), color = Numio.colors.text)
                        }
                    },
                )
            }
        } else if (picker != null) {
            val number = picker.orEmpty()
            BackHandler { picker = null }
            ContactPickerScreen(
                title = stringResource(R.string.add_to_existing_title),
                subtitle = PhoneData.format(context, number),
                contacts = contacts,
                onBack = { picker = null },
                onPick = { c ->
                    picker = null
                    editor = "add:${c.id}:$number"
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
                    onAddContact = { sheet = "s:$number" },
                    canBlock = canBlock,
                    blocked = isBlocked(number),
                    onToggleBlock = { setBlocked(listOf(number), !isBlocked(number)) },
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
                        canBlock = canBlock,
                        blocked = contact.numbers.isNotEmpty() && contact.numbers.all { isBlocked(it.number) },
                        onToggleBlock = {
                            val all = contact.numbers.map { it.number }
                            setBlocked(all, !all.all { isBlocked(it) })
                        },
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
                keypadTones = keypadTones,
                onKeypadTones = { on -> scope.launch { settings.setKeypadTones(on) } },
                keypadVibrate = keypadVibrate,
                onKeypadVibrate = { on -> scope.launch { settings.setKeypadVibrate(on) } },
                speedDial = speedDial,
                contacts = contacts,
                onSetSpeedDial = { d ->
                    if (!hasContacts) onAllowContacts()
                    speedPick = d
                },
                onClearSpeedDial = { d -> scope.launch { settings.setSpeedDial(d, null) } },
                proximity = proximity,
                onProximity = { on -> scope.launch { settings.setProximity(on) } },
                canBlock = canBlock,
                blocked = blocked,
                onUnblock = { n -> setBlocked(listOf(n), false) },
                blockPrivate = blockPrivate,
                onBlockPrivate = { on -> scope.launch { settings.setBlockPrivate(on) } },
                blockUnknown = blockUnknown,
                onBlockUnknown = { on ->
                    if (on && !hasContacts) onAllowContacts()
                    scope.launch { settings.setBlockUnknown(on) }
                },
                blockedCodes = blockedCodes,
                onAddBlockedCode = { code -> scope.launch { settings.addBlockedCode(code) } },
                onRemoveBlockedCode = { code -> scope.launch { settings.removeBlockedCode(code) } },
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
                            onLongPress = { sheet = "n:$it" },
                        )
                        Tab.CONTACTS -> ContactsScreen(
                            hasPermission = hasContacts,
                            contacts = contacts,
                            onAllow = onAllowContacts,
                            onOpen = { detail = "c:${it.id}" },
                            onCall = onCall,
                            onAdd = { editor = "new" },
                            onLongPress = { sheet = "c:${it.id}" },
                        )
                        Tab.KEYPAD -> KeypadScreen(
                            dialRequest = dialRequest,
                            onDialRequestUsed = onDialRequestUsed,
                            notice = notice,
                            contacts = contacts,
                            lastDialled = entries.firstOrNull { it.kind == CallKind.OUTGOING }?.number,
                            speedDial = speedDial,
                            vibrate = keypadVibrate,
                            tones = keypadTones,
                            onCall = onCall,
                            onSaveNumber = { sheet = "s:$it" },
                            onSetUpSpeedDial = { d ->
                                if (!hasContacts) onAllowContacts()
                                speedPick = d
                            },
                        )
                    }
                }
                BottomTabs(selected = tab, onSelect = { tab = it })
            }
        }

        sheet?.let { open ->
            LongPressSheet(
                open = open,
                contacts = contacts,
                canPickContact = hasContacts,
                callName = { n -> groups.firstOrNull { PhoneNumbers.same(it.number, n) }?.name },
                onDismiss = { sheet = null },
                onCall = onCall,
                onMessage = onMessage,
                onCopy = { Clip.copyNumber(context, it) },
                onNewContact = { editor = "new:$it" },
                onAddToExisting = { picker = it },
                onViewContact = { detail = "c:${it.id}" },
                onEditContact = { editor = "edit:${it.id}" },
                canBlock = canBlock,
                isBlocked = ::isBlocked,
                onSetBlocked = ::setBlocked,
            )
        }
    }
}

/** What a long press offers, for a call, a contact, or a number to save. */
@Composable
private fun LongPressSheet(
    open: String,
    contacts: List<Contact>,
    canPickContact: Boolean,
    callName: (String) -> String?,
    onDismiss: () -> Unit,
    onCall: (String) -> Unit,
    onMessage: (String) -> Unit,
    onCopy: (String) -> Unit,
    onNewContact: (String) -> Unit,
    onAddToExisting: (String) -> Unit,
    onViewContact: (Contact) -> Unit,
    onEditContact: (Contact) -> Unit,
    canBlock: Boolean,
    isBlocked: (String) -> Boolean,
    onSetBlocked: (List<String>, Boolean) -> Unit,
) {
    val context = LocalContext.current
    fun blockAction(numbers: List<String>): SheetAction? {
        if (!canBlock || numbers.isEmpty()) return null
        val all = numbers.all(isBlocked)
        val label = context.getString(if (all) R.string.unblock else R.string.block)
        return SheetAction(R.drawable.ic_block, label) { onSetBlocked(numbers, !all) }
    }
    val kind = open.substringBefore(':')
    val value = open.substringAfter(':')

    fun saveActions(number: String) = buildList {
        add(SheetAction(R.drawable.ic_plus, context.getString(R.string.create_new_contact)) { onNewContact(number) })
        if (canPickContact && contacts.isNotEmpty()) {
            add(SheetAction(R.drawable.ic_person_add, context.getString(R.string.add_to_existing)) { onAddToExisting(number) })
        }
    }

    when (kind) {
        "c" -> {
            val contact = contacts.firstOrNull { it.id.toString() == value }
            if (contact == null) {
                LaunchedEffect(open) { onDismiss() }
                return
            }
            val first = contact.numbers.firstOrNull()?.number
            val actions = buildList {
                if (first != null) {
                    add(SheetAction(R.drawable.ic_call, context.getString(R.string.call)) { onCall(first) })
                    add(SheetAction(R.drawable.ic_message, context.getString(R.string.send_message)) { onMessage(first) })
                }
                if (contact.numbers.size == 1 && first != null) {
                    add(SheetAction(R.drawable.ic_copy, context.getString(R.string.copy_number)) { onCopy(first) })
                } else {
                    contact.numbers.forEach { n ->
                        val shown = PhoneData.format(context, n.number)
                        add(SheetAction(R.drawable.ic_copy, context.getString(R.string.copy_this_number, shown)) { onCopy(n.number) })
                    }
                }
                add(SheetAction(R.drawable.ic_pencil, context.getString(R.string.edit_contact)) { onEditContact(contact) })
                blockAction(contact.numbers.map { it.number })?.let { add(it) }
            }
            val subtitle = first?.let { PhoneData.format(context, it) }
            ActionSheet(contact.name, subtitle, contact.photoUri, actions, onDismiss)
        }
        "s" -> {
            val shown = PhoneData.format(context, value)
            ActionSheet(shown, null, null, saveActions(value), onDismiss)
        }
        else -> {
            val number = value
            val contact = contacts.firstOrNull { c -> c.numbers.any { PhoneNumbers.same(it.number, number) } }
            val shown = PhoneData.format(context, number)
            val title = contact?.name ?: callName(number)?.takeIf { it.isNotBlank() } ?: shown
            val actions = buildList {
                add(SheetAction(R.drawable.ic_call, context.getString(R.string.call)) { onCall(number) })
                add(SheetAction(R.drawable.ic_message, context.getString(R.string.send_message)) { onMessage(number) })
                add(SheetAction(R.drawable.ic_copy, context.getString(R.string.copy_number)) { onCopy(number) })
                if (contact != null) {
                    add(SheetAction(R.drawable.ic_contacts, context.getString(R.string.view_contact)) { onViewContact(contact) })
                } else {
                    addAll(saveActions(number))
                }
                if (number.isNotBlank()) blockAction(listOf(number))?.let { add(it) }
            }
            ActionSheet(title, if (title != shown) shown else null, contact?.photoUri, actions, onDismiss)
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
