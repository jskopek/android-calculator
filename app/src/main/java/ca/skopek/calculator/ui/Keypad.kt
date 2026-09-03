package ca.skopek.calculator.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.skopek.calculator.R
import ca.skopek.calculator.engine.Key
import ca.skopek.calculator.engine.Operator
import ca.skopek.calculator.ui.theme.Fonts

internal enum class KeyStyle { NUMBER, OPERATOR, ACTION, EQUALS }

private class KeySpec(
    val key: Key,
    val style: KeyStyle,
    val label: String? = null,
    val icon: ImageVector? = null,
    /** String resource for accessibility; null means the label already says it. */
    val descriptionRes: Int? = null,
)

private fun digit(c: Char) = KeySpec(Key.Digit(c), KeyStyle.NUMBER, label = c.toString())

private fun keypadLayout(decimalSeparator: Char): List<List<KeySpec>> = listOf(
    listOf(
        KeySpec(Key.Clear, KeyStyle.ACTION, label = "AC"),
        KeySpec(Key.Backspace, KeyStyle.ACTION, icon = Icons.AutoMirrored.Outlined.Backspace, descriptionRes = R.string.key_backspace),
        KeySpec(Key.Percent, KeyStyle.ACTION, label = "%", descriptionRes = R.string.key_percent),
        KeySpec(Key.Operation(Operator.DIVIDE), KeyStyle.OPERATOR, label = "÷", descriptionRes = R.string.key_divide),
    ),
    listOf(digit('7'), digit('8'), digit('9'), KeySpec(Key.Operation(Operator.MULTIPLY), KeyStyle.OPERATOR, label = "×", descriptionRes = R.string.key_multiply)),
    listOf(digit('4'), digit('5'), digit('6'), KeySpec(Key.Operation(Operator.SUBTRACT), KeyStyle.OPERATOR, label = "−", descriptionRes = R.string.key_subtract)),
    listOf(digit('1'), digit('2'), digit('3'), KeySpec(Key.Operation(Operator.ADD), KeyStyle.OPERATOR, label = "+", descriptionRes = R.string.key_add)),
    listOf(
        KeySpec(Key.ToggleSign, KeyStyle.NUMBER, label = "±", descriptionRes = R.string.key_toggle_sign),
        digit('0'),
        KeySpec(Key.Decimal, KeyStyle.NUMBER, label = decimalSeparator.toString(), descriptionRes = R.string.key_decimal),
        KeySpec(Key.Equals, KeyStyle.EQUALS, label = "=", descriptionRes = R.string.key_equals),
    ),
)

/**
 * The 4×5 grid of keys. Rows and columns share space with weights so the keypad fills whatever
 * area it is given, which is what makes it comfortable on both the cover and inner screens of a
 * folding phone.
 */
@Composable
fun Keypad(
    decimalSeparator: Char,
    onKey: (Key) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val layout = keypadLayout(decimalSeparator)
    val gap: Dp = if (compact) 6.dp else 8.dp
    val view = LocalView.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        layout.forEach { row ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.forEach { spec ->
                    KeyButton(
                        label = spec.label,
                        icon = spec.icon,
                        style = spec.style,
                        compact = compact,
                        contentDescription = spec.descriptionRes?.let { stringResource(it) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onKey(spec.key)
                        },
                    )
                }
            }
        }
    }
}

/**
 * One paper key. Scales down while pressed and springs back with a small overshoot on release.
 * Shared by the calculator keypad and the converter keypad.
 */
@Composable
internal fun KeyButton(
    label: String?,
    icon: ImageVector?,
    style: KeyStyle,
    compact: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = Motion.keyPress(),
        label = "keyScale",
    )
    val (container, content, border) = when (style) {
        KeyStyle.NUMBER -> Triple(colors.surface, colors.onSurface, BorderStroke(1.dp, colors.outlineVariant))
        KeyStyle.ACTION -> Triple(colors.tertiaryContainer, colors.onTertiaryContainer, BorderStroke(1.dp, colors.outlineVariant))
        KeyStyle.OPERATOR -> Triple(colors.secondary, colors.onSecondary, null)
        KeyStyle.EQUALS -> Triple(colors.primary, colors.onPrimary, null)
    }
    val semantics = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(semantics),
        shape = RoundedCornerShape(if (compact) 12.dp else 16.dp),
        color = container,
        contentColor = content,
        border = border,
        interactionSource = interaction,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 20.dp else 24.dp),
                )
                else -> Text(
                    text = label.orEmpty(),
                    fontFamily = Fonts.mono,
                    fontSize = if (compact) 20.sp else 26.sp,
                    fontWeight = if (style == KeyStyle.NUMBER) FontWeight.Normal else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}
