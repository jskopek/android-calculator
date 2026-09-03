package ca.skopek.calculator.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.skopek.calculator.R
import ca.skopek.calculator.data.HistoryEntry

/**
 * The paper tape: every calculation is a line, newest at the bottom, right above the line being
 * typed. Tap a line to reuse its result; long-press for copy / delete.
 */
@Composable
fun Tape(
    entries: List<HistoryEntry>,
    onUse: (HistoryEntry) -> Unit,
    onCopy: (HistoryEntry) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    if (entries.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.titleSmall,
                fontStyle = FontStyle.Italic,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val newestFirst = remember(entries) { entries.asReversed() }
    // Keep the tape pinned to the newest line whenever something is added.
    LaunchedEffect(entries.size) { listState.animateScrollToItem(0) }
    val fade = colors.background

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = modifier.drawWithContent {
            drawContent()
            // The top of the tape fades out, as if it continues off the roll.
            drawRect(Brush.verticalGradient(0f to fade, 0.28f to Color.Transparent))
        },
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 48.dp),
    ) {
        items(newestFirst, key = { it.id }) { entry ->
            TapeLine(
                entry = entry,
                onUse = { onUse(entry) },
                onCopy = { onCopy(entry) },
                onDelete = { onDelete(entry) },
                modifier = Modifier.animateItem(
                    fadeInSpec = Motion.settle(),
                    placementSpec = Motion.settle(),
                    fadeOutSpec = Motion.settle(),
                ),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TapeLine(
    entry: HistoryEntry,
    onUse: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onUse,
                    onLongClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        menuOpen = true
                    },
                )
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = entry.expression,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
            Text(
                text = "= ${entry.result}",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                color = colors.onSurface,
                textAlign = TextAlign.End,
            )
        }
        DashedRule(color = colors.outlineVariant, modifier = Modifier.align(Alignment.BottomCenter))
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.use_result)) }, onClick = { menuOpen = false; onUse() })
            DropdownMenuItem(text = { Text(stringResource(R.string.copy)) }, onClick = { menuOpen = false; onCopy() })
            DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menuOpen = false; onDelete() })
        }
    }
}

@Composable
private fun DashedRule(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
        )
    }
}
