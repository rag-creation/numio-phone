package com.rr.numio.phone.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rr.numio.phone.R
import com.rr.numio.phone.data.ContactDraft
import com.rr.numio.phone.data.ContactNumber
import com.rr.numio.phone.data.PhoneType
import com.rr.numio.phone.ui.common.DetailTopBar
import com.rr.numio.phone.ui.common.PersonAvatar
import com.rr.numio.phone.ui.theme.Numio

/** Add or edit a contact. Saving is done by the caller; [saving] greys out the button meanwhile. */
@Composable
fun ContactEditScreen(
    initial: ContactDraft,
    photoUri: String?,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: (ContactDraft) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var starred by rememberSaveable { mutableStateOf(initial.starred) }
    // Numbers as two parallel lists so they survive rotation with rememberSaveable
    var numbers by rememberSaveable { mutableStateOf(initial.numbers.map { it.number }.ifEmpty { listOf("") }) }
    var types by rememberSaveable { mutableStateOf(initial.numbers.map { it.type }.ifEmpty { listOf(PhoneType.MOBILE) }) }
    // Custom labels ("Dubai SIM") and where each number came from, so saving keeps them
    var labels by rememberSaveable {
        mutableStateOf(initial.numbers.map { if (it.type == PhoneType.CUSTOM) it.label else null }.ifEmpty { listOf(null) })
    }
    var origins by rememberSaveable { mutableStateOf(initial.numbers.map { it.from }.ifEmpty { listOf(null) }) }
    var confirmDelete by remember { mutableStateOf(false) }

    val draft = ContactDraft(
        initial.id,
        name,
        numbers.indices.map { i -> ContactNumber(numbers[i], labels[i], types[i], origins[i]) },
        starred,
    )
    val isNew = initial.id == null

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        DetailTopBar(onBack = onCancel, backLabel = stringResource(R.string.cancel)) {
            Text(
                stringResource(if (isNew) R.string.new_contact else R.string.edit_contact),
                style = MaterialTheme.typography.titleMedium,
                color = Numio.colors.text,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                PersonAvatar(name.ifBlank { null }, photoUri, 96.dp, ring = if (starred) Numio.colors.accent else Numio.colors.ring)
            }

            FieldLabel(stringResource(R.string.name))
            Field(
                value = name,
                onChange = { name = it },
                hint = stringResource(R.string.name_hint),
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )

            FieldLabel(stringResource(R.string.numbers))
            numbers.forEachIndexed { i, number ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        Field(
                            value = number,
                            onChange = { v -> numbers = numbers.toMutableList().also { it[i] = v } },
                            hint = stringResource(R.string.number_hint),
                            keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            numeric = true,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Tap to cycle Mobile → Home → Work → Other (and back to the number's own label, if it had one)
                    Text(
                        typeName(types[i], labels[i]),
                        style = MaterialTheme.typography.labelLarge,
                        color = Numio.colors.accentInk,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Numio.colors.ring, RoundedCornerShape(16.dp))
                            .clickable(role = Role.Button) { types = types.toMutableList().also { it[i] = PhoneType.next(it[i], labels[i]) } }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                    if (numbers.size > 1) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) {
                                    numbers = numbers.toMutableList().also { it.removeAt(i) }
                                    types = types.toMutableList().also { it.removeAt(i) }
                                    labels = labels.toMutableList().also { it.removeAt(i) }
                                    origins = origins.toMutableList().also { it.removeAt(i) }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.remove_number), tint = Numio.colors.textSoft, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) {
                        numbers = numbers + ""
                        types = types + PhoneType.MOBILE
                        labels = labels + null
                        origins = origins + null
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_plus), null, tint = Numio.colors.accentInk, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_number), style = MaterialTheme.typography.labelLarge, color = Numio.colors.accentInk)
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, if (starred) Numio.colors.accent else Numio.colors.ring, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Switch) { starred = !starred }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_star), null, tint = if (starred) Numio.colors.accentInk else Numio.colors.textSoft, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(if (starred) R.string.in_favourites else R.string.add_to_favourites),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Numio.colors.text,
                )
            }

            if (onDelete != null) {
                Spacer(Modifier.height(28.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) { confirmDelete = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_trash), null, tint = Numio.colors.danger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_contact), style = MaterialTheme.typography.labelLarge, color = Numio.colors.danger)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Save, always in reach above the keyboard
        val enabled = draft.canSave && !saving
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) Numio.colors.accent else Numio.colors.surface)
                .clickable(enabled = enabled, role = Role.Button) { onSave(draft) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(if (saving) R.string.saving else R.string.save),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Numio.colors.onAccent else Numio.colors.textFaint,
            )
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Numio.colors.background,
            title = { Text(stringResource(R.string.delete_title, draft.savedName), color = Numio.colors.text) },
            text = { Text(stringResource(R.string.delete_body), color = Numio.colors.textSoft) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = Numio.colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel), color = Numio.colors.text)
                }
            },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Numio.colors.accentInk,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun typeName(type: Int, customLabel: String?): String {
    if (type == PhoneType.CUSTOM && !customLabel.isNullOrBlank()) return customLabel
    return stringResource(
        when (type) {
            PhoneType.HOME -> R.string.type_home
            PhoneType.WORK -> R.string.type_work
            PhoneType.OTHER -> R.string.type_other
            else -> R.string.type_mobile
        },
    )
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    keyboard: KeyboardOptions,
    numeric: Boolean = false,
) {
    val style: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = Numio.colors.text,
        fontFeatureSettings = if (numeric) "tnum" else null,
    )
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Numio.colors.ring, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) Text(hint, style = style.copy(color = Numio.colors.textFaint))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(Numio.colors.accent),
            keyboardOptions = keyboard,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
        )
    }
}
