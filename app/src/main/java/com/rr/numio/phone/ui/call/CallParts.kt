package com.rr.numio.phone.ui.call

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.phone.R
import com.rr.numio.phone.ui.theme.Numio

// Thin light type used for names and the timer, like the keypad digits
val CallName = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 36.sp)
val CallTimer = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 22.sp,
    letterSpacing = 1.sp, fontFeatureSettings = "tnum",
)

/** Small amber capitals: "INCOMING CALL · SIM 1". */
@Composable
fun CallLabel(text: String, color: Color = Numio.colors.accentInk) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
        color = color,
    )
}

/** Caller's first letter, or a person icon for a bare number. */
@Composable
fun Avatar(title: String, hasName: Boolean, size: Dp, ringColor: Color, fill: Color = Color.Transparent) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (hasName) {
            Text(
                title.trim().take(1).uppercase(),
                style = CallName.copy(fontSize = (size.value * 0.4f).sp),
                color = Numio.colors.text,
            )
        } else {
            Icon(
                painterResource(R.drawable.ic_contacts),
                contentDescription = null,
                tint = Numio.colors.textSoft,
                modifier = Modifier.size(size * 0.42f),
            )
        }
    }
}

/**
 * Faint amber rings around the avatar. While ringing they slowly breathe outward;
 * on a call there is one still ring.
 */
@Composable
fun HaloRings(pulsing: Boolean, size: Dp, content: @Composable () -> Unit) {
    val accent = Numio.colors.accent
    val t = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "rings")
        val v by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
            label = "ringPhase",
        )
        v
    } else {
        0f
    }
    Box(
        Modifier
            .size(size)
            .drawBehind {
                val maxR = this.size.minDimension / 2f
                val stroke = Stroke(width = 1.dp.toPx())
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                if (pulsing) {
                    // Three rings moving outward, fading as they grow
                    for (i in 0 until 3) {
                        val phase = (t + i / 3f) % 1f
                        val r = maxR * (0.45f + 0.55f * phase)
                        drawCircle(accent.copy(alpha = 0.30f * (1f - phase)), r, center, style = stroke)
                    }
                } else {
                    drawCircle(accent.copy(alpha = 0.16f), maxR * 0.7f, center, style = stroke)
                }
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A Halo ring button with a label under it (Mute, Speaker…). */
@Composable
fun RingButton(
    @DrawableRes icon: Int,
    label: String,
    on: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 76.dp,
    onClick: () -> Unit,
) {
    val c = Numio.colors
    val tint = when {
        !enabled -> c.textFaint.copy(alpha = 0.5f)
        on -> c.accentInk
        else -> c.text
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (on) c.keyPressed else Color.Transparent)
                .border(1.dp, if (on) c.accent else c.ring, CircleShape)
                .clickable(enabled = enabled, role = Role.Switch, onClick = onClick)
                .semantics {
                    contentDescription = label
                    stateDescription = if (on) "On" else "Off"
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
            color = if (on) c.accentInk else c.textSoft,
        )
    }
}

/** Round filled button: amber Answer or red End. */
@Composable
fun BigRoundButton(
    @DrawableRes icon: Int,
    description: String,
    fill: Color,
    iconTint: Color,
    halo: Color? = null,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(96.dp)
            .then(if (halo != null) Modifier.border(1.dp, halo, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(fill)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
        }
    }
}
