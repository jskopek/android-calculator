package ca.skopek.calculator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ca.skopek.calculator.R

/** Instrument Serif for words, IBM Plex Mono for numbers. */
object Fonts {
    val serif = FontFamily(
        Font(R.font.instrument_serif_regular, FontWeight.Normal),
        Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
    )
    val mono = FontFamily(
        Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    )
}

private const val TABULAR = "tnum"

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Fonts.mono, fontWeight = FontWeight.Medium, fontSize = 48.sp, letterSpacing = (-0.5).sp, fontFeatureSettings = TABULAR),
    displayMedium = TextStyle(fontFamily = Fonts.mono, fontWeight = FontWeight.Medium, fontSize = 40.sp, letterSpacing = (-0.5).sp, fontFeatureSettings = TABULAR),
    displaySmall = TextStyle(fontFamily = Fonts.mono, fontWeight = FontWeight.Medium, fontSize = 34.sp, fontFeatureSettings = TABULAR),
    headlineMedium = TextStyle(fontFamily = Fonts.mono, fontSize = 26.sp, fontFeatureSettings = TABULAR),
    headlineSmall = TextStyle(fontFamily = Fonts.mono, fontSize = 22.sp, fontFeatureSettings = TABULAR),
    titleLarge = TextStyle(fontFamily = Fonts.serif, fontSize = 26.sp),
    titleMedium = TextStyle(fontFamily = Fonts.serif, fontSize = 21.sp),
    titleSmall = TextStyle(fontFamily = Fonts.serif, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = Fonts.mono, fontSize = 16.sp, fontFeatureSettings = TABULAR),
    bodyMedium = TextStyle(fontFamily = Fonts.mono, fontSize = 14.sp, fontFeatureSettings = TABULAR),
    bodySmall = TextStyle(fontFamily = Fonts.mono, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Fonts.mono, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Fonts.mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = Fonts.mono, fontSize = 11.sp, letterSpacing = 0.5.sp),
)
