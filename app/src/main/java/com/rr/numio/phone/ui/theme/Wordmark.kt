package com.rr.numio.phone.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rr.numio.phone.R

// The official "Numio." wordmark, exactly as shipped in the brand files (two PNGs:
// cream letters for dark screens, dark letters for light screens). Never redrawn or
// recoloured. The parameters are kept so existing calls still work: textColor only
// decides which of the two official versions fits the screen, markColor is ignored
// because the N and the dot are always official Numio amber.
private const val ASPECT = 1156f / 268f

@Suppress("UNUSED_PARAMETER")
@Composable
fun NumioWordmark(
    height: Dp = 15.dp,
    markColor: Color = Numio.colors.accent,
    textColor: Color = Numio.colors.textSoft,
    modifier: Modifier = Modifier,
) {
    val onDark = textColor.luminance() > 0.5f
    Image(
        painter = painterResource(if (onDark) R.drawable.numio_wordmark_on_dark else R.drawable.numio_wordmark_on_light),
        contentDescription = "Numio",
        contentScale = ContentScale.Fit,
        modifier = modifier.height(height).width(height * ASPECT),
    )
}
