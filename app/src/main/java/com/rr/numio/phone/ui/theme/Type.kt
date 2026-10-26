package com.rr.numio.phone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rr.numio.phone.R

// Inter (SIL OFL 1.1, see licenses/Inter-OFL.txt). Malayalam, Arabic and emoji
// fall back to the phone's system fonts.
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val base = Typography()

val PhoneTypography = Typography(
    headlineSmall = base.headlineSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleMedium = base.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Inter),
    bodyMedium = base.bodyMedium.copy(fontFamily = Inter),
    bodySmall = base.bodySmall.copy(fontFamily = Inter),
    labelLarge = base.labelLarge.copy(fontFamily = Inter),
    labelMedium = base.labelMedium.copy(fontFamily = Inter),
    labelSmall = base.labelSmall.copy(fontFamily = Inter, letterSpacing = 1.2.sp),
)

// Numbers: tabular figures so digits don't jump around while typing
val NumberDisplay = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 36.sp,
    letterSpacing = 1.sp,
    fontFeatureSettings = "tnum",
)

val KeyDigit = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 30.sp,
    fontFeatureSettings = "tnum",
)

val KeyLetters = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    letterSpacing = 1.5.sp,
)
