package ca.skopek.calculator.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.skopek.calculator.ConverterField
import ca.skopek.calculator.ConverterUiState
import ca.skopek.calculator.ConverterViewModel
import ca.skopek.calculator.CurrencyStatus
import ca.skopek.calculator.R
import ca.skopek.calculator.UnitValue
import ca.skopek.calculator.engine.units.ConversionUnit
import ca.skopek.calculator.engine.units.CurrencyInfo
import ca.skopek.calculator.engine.units.UnitCategory

/**
 * The converter panel. It has no keypad: "from" is whatever the calculator currently shows, and
 * tapping the converted number sends it back to the calculator.
 */
@Composable
fun ConverterPanel(
    viewModel: ConverterViewModel,
    onUseValue: (String) -> Unit,
    tall: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.category.id) { viewModel.onShown() }

    var showCurrencyDialog by remember { mutableStateOf(false) }
    val currency = state.currency
    val onEditCurrencies: (() -> Unit)? = if (currency != null) ({ showCurrencyDialog = true }) else null

    if (showCurrencyDialog && currency != null) {
        CurrencyPickerDialog(
            available = currency.available,
            selected = currency.favorites,
            onToggle = viewModel::toggleFavorite,
            onDismiss = { showCurrencyDialog = false },
        )
    }

    Column(modifier = modifier) {
        CategoryChips(
            categories = state.categories,
            selected = state.category,
            onSelect = { viewModel.selectCategory(it.id) },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (tall) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            UnitField(
                label = stringResource(R.string.converter_from),
                unit = state.fromUnit,
                units = state.category.units,
                valueText = state.fromText,
                emphasized = false,
                onTap = null,
                onUnitSelected = { viewModel.selectUnit(ConverterField.FROM, it.id) },
                onEditUnits = onEditCurrencies,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilledTonalIconButton(onClick = viewModel::swapUnits) {
                    Icon(Icons.Outlined.SwapVert, contentDescription = stringResource(R.string.swap_units))
                }
            }
            val toValue = state.toValue
            UnitField(
                label = stringResource(R.string.converter_to),
                unit = state.toUnit,
                units = state.category.units,
                valueText = state.toText,
                emphasized = true,
                onTap = toValue?.let { value -> { onUseValue(value) } },
                onUnitSelected = { viewModel.selectUnit(ConverterField.TO, it.id) },
                onEditUnits = onEditCurrencies,
            )
            if (currency != null) {
                CurrencyStatusRow(status = currency, onEdit = { showCurrencyDialog = true })
            }
        }
        if (tall) {
            AllUnitsList(
                state = state,
                onSelect = viewModel::showUnit,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<UnitCategory>,
    selected: UnitCategory,
    onSelect: (UnitCategory) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(category.name, style = MaterialTheme.typography.titleSmall) },
            )
        }
    }
}

@Composable
private fun UnitField(
    label: String,
    unit: ConversionUnit,
    units: List<ConversionUnit>,
    valueText: String,
    emphasized: Boolean,
    onTap: (() -> Unit)?,
    onUnitSelected: (ConversionUnit) -> Unit,
    onEditUnits: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val useResult = stringResource(R.string.use_result)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (emphasized) colors.surfaceContainerHigh else colors.surfaceContainerLow)
            .then(
                if (onTap != null) {
                    Modifier.clickable(onClickLabel = useResult) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onTap()
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            UnitPicker(unit = unit, units = units, onSelect = onUnitSelected, onEdit = onEditUnits)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            ScrollingText(
                text = valueText,
                style = if (emphasized) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
                color = if (emphasized) colors.primary else colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = unit.symbol,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 5.dp),
            )
        }
    }
}

@Composable
private fun UnitPicker(
    unit: ConversionUnit,
    units: List<ConversionUnit>,
    onSelect: (ConversionUnit) -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    Box {
        var open by remember { mutableStateOf(false) }
        TextButton(onClick = { open = true }) {
            Text(unit.name, style = MaterialTheme.typography.titleSmall)
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            units.forEach { candidate ->
                DropdownMenuItem(
                    text = { Text("${candidate.name} (${candidate.symbol})") },
                    trailingIcon = {
                        if (candidate == unit) Icon(Icons.Outlined.Check, contentDescription = null)
                    },
                    onClick = {
                        onSelect(candidate)
                        open = false
                    },
                )
            }
            if (onEdit != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.currency_choose) + "…") },
                    onClick = {
                        open = false
                        onEdit()
                    },
                )
            }
        }
    }
}

@Composable
private fun CurrencyStatusRow(status: CurrencyStatus, onEdit: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status.updating) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = status.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (status.providerUrl != null) Modifier.clickable { uriHandler.openUri(status.providerUrl) } else Modifier,
                ),
        )
        TextButton(onClick = onEdit) { Text(stringResource(R.string.currency_choose)) }
    }
}

@Composable
private fun AllUnitsList(
    state: ConverterUiState,
    onSelect: (ConversionUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.converter_equals, "${state.fromText} ${state.fromUnit.symbol}"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(state.allValues, key = { it.unit.id }) { value ->
                UnitValueRow(
                    value = value,
                    highlighted = value.unit == state.toUnit,
                    dimmed = value.unit == state.fromUnit,
                    onClick = { onSelect(value.unit) },
                )
            }
        }
    }
}

@Composable
private fun UnitValueRow(value: UnitValue, highlighted: Boolean, dimmed: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val color = when {
        highlighted -> colors.primary
        dimmed -> colors.onSurfaceVariant
        else -> colors.onSurface
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.unit.name,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${value.text} ${value.unit.symbol}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                color = color,
            )
        }
        HorizontalDivider(color = colors.outlineVariant)
    }
}

/** Checklist of every currency the rate provider offers; ticked ones appear in the pickers. */
@Composable
private fun CurrencyPickerDialog(
    available: List<CurrencyInfo>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(available, query) {
        val q = query.trim()
        if (q.isEmpty()) available
        else available.filter { it.code.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency_choose)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.currency_search)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(top = 8.dp)) {
                    items(filtered, key = { it.code }) { info ->
                        val checked = info.code in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(info.code) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = info.displayName, modifier = Modifier.weight(1f))
                            Text(
                                text = info.code,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}
