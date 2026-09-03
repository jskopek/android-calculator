package ca.skopek.calculator.ui

import android.view.HapticFeedbackConstants
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val gap: Dp = if (compact) 6.dp else 10.dp
    val view = LocalView.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        layout.forEach { row ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.forEach { spec ->
                    CalculatorKey(
                        spec = spec,
                        compact = compact,
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

@Composable
private fun CalculatorKey(
    spec: KeySpec,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    KeyButton(
        label = spec.label,
        icon = spec.icon,
        style = spec.style,
        compact = compact,
        contentDescription = spec.descriptionRes?.let { stringResource(it) },
        modifier = modifier,
        onClick = onClick,
    )
}

/** One pill-shaped key. Shared by the calculator keypad and the converter keypad. */
@OptIn(ExperimentalMaterial3Api::class)
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
    val (container, content) = when (style) {
        KeyStyle.NUMBER -> colors.surfaceContainerHigh to colors.onSurface
        KeyStyle.OPERATOR -> colors.secondaryContainer to colors.onSecondaryContainer
        KeyStyle.ACTION -> colors.tertiaryContainer to colors.onTertiaryContainer
        KeyStyle.EQUALS -> colors.primary to colors.onPrimary
    }
    val semantics = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Surface(
        onClick = onClick,
        modifier = modifier.then(semantics),
        shape = RoundedCornerShape(percent = 50),
        color = container,
        contentColor = content,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 22.dp else 28.dp),
                )
                else -> Text(
                    text = label.orEmpty(),
                    fontSize = if (compact) 22.sp else 30.sp,
                    fontWeight = if (style == KeyStyle.NUMBER) FontWeight.Normal else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}
