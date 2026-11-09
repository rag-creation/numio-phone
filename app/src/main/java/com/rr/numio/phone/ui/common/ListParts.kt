package com.rr.numio.phone.ui.common

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.phone.R
import com.rr.numio.phone.ui.theme.Numio
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val photoCache = LruCache<String, ImageBitmap>(80)

// Bumped when photos are forgotten, so avatars on screen load again
private var photoEpoch by mutableIntStateOf(0)

/** Forget loaded photos, after a contact's photo was changed (its address stays the same). */
fun clearPhotoCache() {
    photoCache.evictAll()
    photoEpoch++
}

/** Contact photo thumbnail, read from the phone on a background thread and cached. */
@Composable
fun rememberPhoto(uri: String?): ImageBitmap? {
    val context = LocalContext.current
    val epoch = photoEpoch
    val photo by produceState(uri?.let { photoCache.get(it) }, uri, epoch) {
        if (uri == null) {
            value = null
            return@produceState
        }
        photoCache.get(uri)?.let {
            value = it
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            try {
                // Read the size first, so a big gallery picture is loaded small (avatars are tiny)
                val u = Uri.parse(uri)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 360) sample *= 2
                context.contentResolver.openInputStream(u)?.use { s ->
                    BitmapFactory.decodeStream(s, null, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                }?.also { photoCache.put(uri, it) }
            } catch (e: Exception) {
                null
            }
        }
    }
    return photo
}

/** Ring avatar: contact photo, else first letter, else a person icon. */
@Composable
fun PersonAvatar(name: String?, photoUri: String?, size: Dp, ring: Color = Numio.colors.ring) {
    val photo = rememberPhoto(photoUri)
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            photo != null -> Image(
                photo, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape),
            )
            !name.isNullOrBlank() && name.trim().first().isLetter() -> Text(
                name.trim().take(1).uppercase(),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                fontSize = (size.value * 0.42f).sp,
                color = Numio.colors.text,
            )
            else -> Icon(
                painterResource(R.drawable.ic_contacts), null,
                tint = Numio.colors.textSoft, modifier = Modifier.size(size * 0.45f),
            )
        }
    }
}

/** Small ring icon button used at the end of list rows (call, message). */
@Composable
fun RingIconButton(@DrawableRes icon: Int, description: String, size: Dp = 44.dp, tint: Color = Numio.colors.text, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, Numio.colors.ring, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

/** Amber small-caps label above a group of rows. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Numio.colors.accentInk,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Explains why a permission is needed, with one button to allow it. Nothing is asked until tapped. */
@Composable
fun PermissionPrompt(@DrawableRes icon: Int, title: String, body: String, button: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(icon), null, tint = Numio.colors.accentInk, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Numio.colors.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Numio.colors.textSoft, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Numio.colors.accent)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 28.dp, vertical = 14.dp),
        ) {
            Text(button, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Numio.colors.onAccent)
        }
    }
}

/** Back arrow row at the top of a details page. */
@Composable
fun DetailTopBar(onBack: () -> Unit, backLabel: String, end: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = backLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_back), null, tint = Numio.colors.textSoft, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.weight(1f))
        end()
    }
}
