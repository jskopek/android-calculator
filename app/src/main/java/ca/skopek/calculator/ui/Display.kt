package ca.skopek.calculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.skopek.calculator.DisplayState
import ca.skopek.calculator.R
import ca.skopek.calculator.engine.EvalError

/**
 * The read-out above the keypad. While typing it shows the expression with a live preview of the
 * result underneath; after "=" the expression moves up and the result takes the big line.
 */
@Composable
fun Display(state: DisplayState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val secondaryStyle = MaterialTheme.typography.headlineSmall.tabular()
    val primaryStyle = MaterialTheme.typography.displayMedium.tabular().copy(fontWeight = FontWeight.Normal)

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        if (state.secondaryAbove) {
            ScrollingText(state.secondary.orEmpty(), secondaryStyle, colors.onSurfaceVariant)
        }
        ScrollingText(state.primary, primaryStyle, if (state.error != null) colors.error else colors.onSurface)
        if (!state.secondaryAbove) {
            val (text, color) = when (state.error) {
                EvalError.DIVIDE_BY_ZERO -> stringResource(R.string.error_divide_by_zero) to colors.error
                EvalError.INCOMPLETE -> stringResource(R.string.error_incomplete) to colors.error
                null -> state.secondary.orEmpty() to colors.onSurfaceVariant
            }
            ScrollingText(text, secondaryStyle, color)
        }
    }
}

/** Single-line text that stays right-aligned and keeps its end (the most recent input) visible. */
@Composable
internal fun ScrollingText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(scrollState, reverseScrolling = true),
        )
    }
}

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
