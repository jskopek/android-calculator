package ca.skopek.calculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ca.skopek.calculator.R
import ca.skopek.calculator.data.HistoryEntry

/**
 * Ticker-tape style history: every calculation is a line showing the full expression and its
 * result, newest at the bottom, like the paper roll on an adding machine. Tapping a line inserts
 * its result into the current expression.
 */
@Composable
fun HistoryPane(
    entries: List<HistoryEntry>,
    onSelect: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (entries.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.clear_history))
                }
            }
        }

        if (entries.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            val listState = rememberLazyListState()
            val newestFirst = remember(entries) { entries.asReversed() }
            // Keep the tape pinned to the newest line whenever something is added.
            LaunchedEffect(entries.size) { listState.animateScrollToItem(0) }

            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                items(newestFirst, key = { it.id }) { entry ->
                    HistoryRow(entry = entry, onClick = { onSelect(entry) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val useResult = stringResource(R.string.use_result)
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = useResult, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = entry.expression,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
            Text(
                text = "= ${entry.result}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            )
        }
    }
}
