package com.rr.numio.phone.ui.settings

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rr.numio.phone.R
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.rr.numio.phone.data.Contact
import com.rr.numio.phone.data.PhoneData
import com.rr.numio.phone.data.PhoneNumbers
import com.rr.numio.phone.ui.RoundIconButton
import com.rr.numio.phone.ui.theme.Numio
import com.rr.numio.phone.ui.theme.ThemeMode
import com.rr.numio.phone.ui.theme.NumioWordmark

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    versionName: String,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onOpenDefaultApps: () -> Unit,
    canFullScreen: Boolean,
    onAllowFullScreen: () -> Unit,
    keypadTones: Boolean,
    onKeypadTones: (Boolean) -> Unit,
    keypadVibrate: Boolean,
    onKeypadVibrate: (Boolean) -> Unit,
    speedDial: Map<Int, String>,
    contacts: List<Contact>,
    onSetSpeedDial: (Int) -> Unit,
    onClearSpeedDial: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                icon = R.drawable.ic_back,
                description = stringResource(R.string.back),
                onClick = onBack,
            )
            Text(
                stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineSmall,
                color = Numio.colors.text,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            SectionLabel(stringResource(R.string.default_phone_app))
            Text(
                stringResource(if (isDefault) R.string.default_yes else R.string.default_no),
                style = MaterialTheme.typography.bodyMedium,
                color = Numio.colors.textSoft,
            )
            Spacer(Modifier.height(12.dp))
            if (isDefault) {
                if (!canFullScreen) {
                    Text(
                        stringResource(R.string.full_screen_off),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Numio.colors.text,
                    )
                    Spacer(Modifier.height(12.dp))
                    FilledButton(stringResource(R.string.allow_full_screen), onAllowFullScreen)
                    Spacer(Modifier.height(10.dp))
                }
                OutlineButton(stringResource(R.string.change_default), onOpenDefaultApps)
            } else {
                FilledButton(stringResource(R.string.set_default), onSetDefault)
            }

            SectionLabel(stringResource(R.string.keypad))
            SettingSwitch(stringResource(R.string.keypad_tones), stringResource(R.string.keypad_tones_body), keypadTones, onKeypadTones)
            Spacer(Modifier.height(8.dp))
            SettingSwitch(stringResource(R.string.keypad_vibrate), stringResource(R.string.keypad_vibrate_body), keypadVibrate, onKeypadVibrate)

            SectionLabel(stringResource(R.string.speed_dial))
            Text(
                stringResource(R.string.speed_dial_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Numio.colors.textSoft,
            )
            Spacer(Modifier.height(8.dp))
            (1..9).forEach { d ->
                val number = speedDial[d]
                val name = number?.let { n -> contacts.firstOrNull { c -> c.numbers.any { PhoneNumbers.same(it.number, n) } }?.name }
                SpeedDialRow(
                    digit = d,
                    title = when {
                        number == null && d == 1 -> stringResource(R.string.voicemail)
                        number == null -> stringResource(R.string.speed_dial_empty)
                        else -> name ?: PhoneData.format(context, number)
                    },
                    subtitle = if (number != null && name != null) PhoneData.format(context, number) else null,
                    set = number != null,
                    onClick = { onSetSpeedDial(d) },
                    onClear = { onClearSpeedDial(d) },
                )
            }

            SectionLabel(stringResource(R.string.appearance))
            ThemeChooser(selected = themeMode, onSelect = onThemeMode)

            SectionLabel(stringResource(R.string.privacy))
            Text(
                stringResource(R.string.privacy_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Numio.colors.textSoft,
            )

            SectionLabel(stringResource(R.string.about))
            NumioWordmark(height = 18.dp, textColor = Numio.colors.text)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.bodyLarge,
                color = Numio.colors.text,
            )
            Text(
                stringResource(R.string.version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = Numio.colors.textSoft,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FilledButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Numio.colors.accent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Numio.colors.onAccent)
    }
}

@Composable
private fun OutlineButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Numio.colors.ring, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Numio.colors.text)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Numio.colors.accentInk,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
    )
}

// Dark / Light / System as one quiet segmented row
@Composable
private fun ThemeChooser(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.DARK to R.string.theme_dark,
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.SYSTEM to R.string.theme_system,
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Numio.colors.surface)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (mode, label) ->
            val isSelected = mode == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Numio.colors.accent else Numio.colors.surface)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(mode) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Numio.colors.onAccent else Numio.colors.textSoft,
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Numio.colors.surface)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Numio.colors.text)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Numio.colors.textSoft)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Numio.colors.onAccent,
                checkedTrackColor = Numio.colors.accent,
                checkedBorderColor = Numio.colors.accent,
                uncheckedThumbColor = Numio.colors.textSoft,
                uncheckedTrackColor = Numio.colors.background,
                uncheckedBorderColor = Numio.colors.ring,
            ),
        )
    }
}

/** One speed dial key: its digit in a ring, who it calls, and × to clear it. */
@Composable
private fun SpeedDialRow(
    digit: Int,
    title: String,
    subtitle: String?,
    set: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .border(1.dp, if (set) Numio.colors.accent else Numio.colors.ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(digit.toString(), style = MaterialTheme.typography.titleMedium, color = Numio.colors.text)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (set) Numio.colors.text else Numio.colors.textSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Numio.colors.textSoft, maxLines = 1)
            }
        }
        if (set) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    stringResource(R.string.speed_dial_clear, digit),
                    tint = Numio.colors.textSoft,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
