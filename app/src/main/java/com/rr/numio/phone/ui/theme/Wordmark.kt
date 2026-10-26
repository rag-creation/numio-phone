package com.rr.numio.phone.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width

// The "Numio." wordmark from getnumio.org: the N shape, "umio" in Chakra Petch Bold
// (SIL OFL 1.1, glyphs converted to outlines) and the dot. Kept as shapes, not a font,
// so it is crisp at any size and needs no extra font file.
private const val VIEW_W = 3078.71f
private const val VIEW_H = 712f
private const val N_PATH = "M 0.00,12.00 L 312.40,253.84 L 168.23,369.90 L 168.23,712.00 L 0.00,712.00 Z M 632.71,712.00 L 320.31,470.16 L 464.48,354.10 L 464.48,12.00 L 632.71,12.00 Z"
private const val UMIO_PATH = "M 747.71,607.00 V 214.00 H 879.71 V 566.00 L 913.71,600.00 H 993.71 L 1091.71,502.00 V 214.00 H 1223.71 V 712.00 H 1101.71 V 627.00 L 1015.71,712.00 H 852.71 Z M 1323.71,214.00 H 1445.71 V 296.00 L 1526.71,214.00 H 1652.71 L 1720.71,283.00 L 1788.71,214.00 H 1962.71 L 2069.71,322.00 V 712.00 H 1937.71 V 361.00 L 1903.71,326.00 H 1819.71 L 1766.71,379.00 V 712.00 H 1638.71 V 359.00 L 1606.71,326.00 H 1546.71 L 1455.71,418.00 V 712.00 H 1323.71 Z M 2159.71,110.00 V 46.00 L 2205.71,0.00 H 2275.71 L 2321.71,46.00 V 110.00 L 2275.71,156.00 H 2205.71 Z M 2174.71,214.00 H 2306.71 V 712.00 H 2174.71 Z M 2406.71,608.00 V 318.00 L 2509.71,214.00 H 2795.71 L 2898.71,318.00 V 608.00 L 2795.71,712.00 H 2509.71 Z M 2731.71,602.00 2766.71,567.00 V 359.00 L 2731.71,324.00 H 2573.71 L 2538.71,359.00 V 567.00 L 2573.71,602.00 Z"
private const val DOT_PATH = "M 2978.71,662.00 a 50 50 0 1 0 100 0 a 50 50 0 1 0 -100 0 Z"

@Composable
fun NumioWordmark(
    height: Dp = 15.dp,
    markColor: Color = Numio.colors.accent,
    textColor: Color = Numio.colors.textSoft,
    modifier: Modifier = Modifier,
) {
    val vector = remember(markColor, textColor) {
        ImageVector.Builder(
            name = "NumioWordmark",
            defaultWidth = (VIEW_W / VIEW_H * 24f).dp,
            defaultHeight = 24.dp,
            viewportWidth = VIEW_W,
            viewportHeight = VIEW_H,
        )
            .addPath(addPathNodes(N_PATH), fill = SolidColor(markColor))
            .addPath(addPathNodes(UMIO_PATH), fill = SolidColor(textColor))
            .addPath(addPathNodes(DOT_PATH), fill = SolidColor(markColor))
            .build()
    }
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .width(height * (VIEW_W / VIEW_H))
            .semantics { contentDescription = "Numio" },
    )
}
