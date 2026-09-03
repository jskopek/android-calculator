package ca.skopek.calculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Paper and ink": warm paper, near-black ink, one oxblood accent.
private val Paper = Color(0xFFF6F1E8)
private val PaperDeep = Color(0xFFEFE8DB)
private val PaperDeeper = Color(0xFFE6DED0)
private val Ink = Color(0xFF1D1916)
private val InkMuted = Color(0xFF7A716A)
private val Rule = Color(0xFFE2DACE)
private val RuleStrong = Color(0xFFB8AE9F)
private val Oxblood = Color(0xFF8A2F2A)

// "Slate and chalk": the same design after dark.
private val Slate = Color(0xFF1E1C1A)
private val SlateDeep = Color(0xFF26231F)
private val SlateDeeper = Color(0xFF302C28)
private val Chalk = Color(0xFFECE6DA)
private val ChalkMuted = Color(0xFFA39B90)
private val DarkRule = Color(0xFF3A3733)
private val DarkRuleStrong = Color(0xFF5C554E)
private val OxbloodLight = Color(0xFFD0655C)

private val LightColors = lightColorScheme(
    primary = Oxblood,
    onPrimary = Paper,
    primaryContainer = Color(0xFFF3DAD6),
    onPrimaryContainer = Color(0xFF3B0F0C),
    secondary = Ink,
    onSecondary = Paper,
    secondaryContainer = PaperDeeper,
    onSecondaryContainer = Ink,
    tertiary = InkMuted,
    onTertiary = Paper,
    tertiaryContainer = PaperDeep,
    onTertiaryContainer = Color(0xFF5A524B),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color(0xFFFBF8F2),
    surfaceContainerLow = Color(0xFFF2ECE1),
    surfaceContainer = PaperDeep,
    surfaceContainerHigh = Color(0xFFEAE2D3),
    surfaceContainerHighest = PaperDeeper,
    outline = RuleStrong,
    outlineVariant = Rule,
    error = Color(0xFF9B2C2C),
    onError = Paper,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    scrim = Color(0xFF1D1916),
)

private val DarkColors = darkColorScheme(
    primary = OxbloodLight,
    onPrimary = Color(0xFF2A0C0A),
    primaryContainer = Color(0xFF5E1F1B),
    onPrimaryContainer = Color(0xFFF6D9D5),
    secondary = Chalk,
    onSecondary = Slate,
    secondaryContainer = DarkRule,
    onSecondaryContainer = Chalk,
    tertiary = ChalkMuted,
    onTertiary = Slate,
    tertiaryContainer = SlateDeep,
    onTertiaryContainer = ChalkMuted,
    background = Slate,
    onBackground = Chalk,
    surface = Slate,
    onSurface = Chalk,
    surfaceVariant = SlateDeep,
    onSurfaceVariant = ChalkMuted,
    surfaceContainerLowest = Color(0xFF181614),
    surfaceContainerLow = Color(0xFF22201D),
    surfaceContainer = SlateDeep,
    surfaceContainerHigh = SlateDeeper,
    surfaceContainerHighest = Color(0xFF3A3632),
    outline = DarkRuleStrong,
    outlineVariant = DarkRule,
    error = Color(0xFFE07A70),
    onError = Color(0xFF2A0C0A),
    inverseSurface = Chalk,
    inverseOnSurface = Slate,
    scrim = Color(0xFF000000),
)

/** Resolves the user's theme preference to an actual light/dark choice. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun CalculatorTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
