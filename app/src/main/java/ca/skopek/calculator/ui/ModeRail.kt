package ca.skopek.calculator.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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

/** What sits beside (or above) the calculator: the paper tape, or the converter. */
enum class Panel { TAPE, CONVERT }

/** Two words along the edge. The oxblood underline slides to the active one. */
@Composable
fun ModeRail(active: Panel, onSelect: (Panel) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Panel.entries.forEach { panel ->
            val selected = panel == active
            val emphasis by animateFloatAsState(if (selected) 1f else 0f, Motion.settle(), label = "railEmphasis")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(panel) },
                    )
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(panel.labelRes),
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

val Panel.labelRes: Int
    get() = when (this) {
        Panel.TAPE -> R.string.mode_tape
        Panel.CONVERT -> R.string.mode_convert
    }
