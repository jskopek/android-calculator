package ca.skopek.calculator.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ca.skopek.calculator.R
import androidx.compose.foundation.background

enum class Mode { CALCULATOR, CONVERT, CURRENCY }

/** The three words along the bottom edge. The oxblood underline slides to the active one. */
@Composable
fun ModeRail(active: Mode, onSelect: (Mode) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Mode.entries.forEach { mode ->
            val selected = mode == active
            val emphasis by animateFloatAsState(if (selected) 1f else 0f, Motion.settle(), label = "railEmphasis")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(mode) },
                    )
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(mode.labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .height(2.dp)
                        .width(28.dp)
                        .graphicsLayer { scaleX = 0.3f + 0.7f * emphasis }
                        .alpha(emphasis)
                        .background(colors.primary),
                )
            }
        }
    }
}

private val Mode.labelRes: Int
    get() = when (this) {
        Mode.CALCULATOR -> R.string.mode_calculator
        Mode.CONVERT -> R.string.mode_convert
        Mode.CURRENCY -> R.string.mode_currency
    }
