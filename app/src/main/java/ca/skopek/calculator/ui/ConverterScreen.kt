package ca.skopek.calculator.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import ca.skopek.calculator.engine.Key
import ca.skopek.calculator.engine.units.ConversionUnit
import ca.skopek.calculator.engine.units.CurrencyInfo
import ca.skopek.calculator.engine.units.UnitCategory

/**
 * Unit converter. Two fields (from / to); tapping a field makes it the one the keypad edits and
 * the other side follows. Wide windows also show the value in every unit of the category.
 */
@Composable
fun ConverterScreen(
    windowSizeClass: WindowSizeClass,
    decimalSeparator: Char,
    onBack: () -> Unit,
    viewModel: ConverterViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { viewModel.onShown() }

    val twoPane = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val compactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(
                    text = stringResource(R.string.unit_converter),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            CategoryChips(
                categories = state.categories,
                selected = state.category,
                onSelect = { viewModel.selectCategory(it.id) },
            )

            if (twoPane) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    ConverterPane(
                        state = state,
                        decimalSeparator = decimalSeparator,
                        compactHeight = compactHeight,
                        viewModel = viewModel,
                        modifier = Modifier.weight(0.55f).fillMaxHeight(),
                    )
                    VerticalDivider()
                    AllUnitsList(
                        state = state,
                        onSelect = viewModel::showUnit,
                        modifier = Modifier.weight(0.45f).fillMaxHeight(),
                    )
                }
            } else {
                ConverterPane(
                    state = state,
                    decimalSeparator = decimalSeparator,
                    compactHeight = compactHeight,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
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
                label = { Text(category.name) },
            )
        }
    }
}

@Composable
private fun ConverterPane(
    state: ConverterUiState,
    decimalSeparator: Char,
    compactHeight: Boolean,
    viewModel: ConverterViewModel,
    modifier: Modifier = Modifier,
) {
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
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            UnitField(
                label = stringResource(R.string.converter_from),
                unit = state.fromUnit,
                units = state.category.units,
                valueText = state.fromText,
                active = state.activeField == ConverterField.FROM,
                onActivate = { viewModel.activateField(ConverterField.FROM) },
                onUnitSelected = { viewModel.selectUnit(ConverterField.FROM, it.id) },
                onEditUnits = onEditCurrencies,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilledTonalIconButton(onClick = viewModel::swapUnits) {
                    Icon(Icons.Outlined.SwapVert, contentDescription = stringResource(R.string.swap_units))
                }
            }
            UnitField(
                label = stringResource(R.string.converter_to),
                unit = state.toUnit,
                units = state.category.units,
                valueText = state.toText,
                active = state.activeField == ConverterField.TO,
                onActivate = { viewModel.activateField(ConverterField.TO) },
                onUnitSelected = { viewModel.selectUnit(ConverterField.TO, it.id) },
                onEditUnits = onEditCurrencies,
            )
            if (currency != null) {
                CurrencyStatusRow(status = currency, onEdit = { showCurrencyDialog = true })
            }
        }
        ConverterKeypad(
            decimalSeparator = decimalSeparator,
            compact = compactHeight,
            onKey = viewModel::onKey,
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (compactHeight) 1.6f else 1.2f)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun UnitField(
    label: String,
    unit: ConversionUnit,
    units: List<ConversionUnit>,
    valueText: String,
    active: Boolean,
    onActivate: () -> Unit,
    onUnitSelected: (ConversionUnit) -> Unit,
    onEditUnits: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) colors.surfaceContainerHigh else colors.surfaceContainerLow)
            .clickable(onClick = onActivate)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            UnitPicker(unit = unit, units = units, onSelect = onUnitSelected, onEdit = onEditUnits)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            ScrollingText(
                text = valueText,
                style = MaterialTheme.typography.displaySmall,
                color = if (active) colors.primary else colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = unit.symbol,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
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
            Text(unit.name)
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

@Composable
private fun AllUnitsList(
    state: ConverterUiState,
    onSelect: (ConversionUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.converter_equals, "${state.activeText} ${state.activeUnit.symbol}"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(state.allValues, key = { it.unit.id }) { value ->
                UnitValueRow(value = value, highlighted = value.unit == state.activeUnit, onClick = { onSelect(value.unit) })
            }
        }
    }
}

@Composable
private fun UnitValueRow(value: UnitValue, highlighted: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.unit.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${value.text} ${value.unit.symbol}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private class ConverterKeySpec(
    val key: Key,
    val style: KeyStyle,
    val label: String? = null,
    val icon: ImageVector? = null,
    val descriptionRes: Int? = null,
    /** Relative width; the "0" key spans several columns. */
    val span: Float = 1f,
)

/** Digits plus clear, backspace, sign and decimal, styled like the calculator keypad. */
@Composable
private fun ConverterKeypad(
    decimalSeparator: Char,
    compact: Boolean,
    onKey: (Key) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun digit(c: Char) = ConverterKeySpec(Key.Digit(c), KeyStyle.NUMBER, label = c.toString())
    val rows = listOf(
        listOf(digit('7'), digit('8'), digit('9'), ConverterKeySpec(Key.Backspace, KeyStyle.ACTION, icon = Icons.AutoMirrored.Outlined.Backspace, descriptionRes = R.string.key_backspace)),
        listOf(digit('4'), digit('5'), digit('6'), ConverterKeySpec(Key.Clear, KeyStyle.ACTION, label = "C")),
        listOf(digit('1'), digit('2'), digit('3'), ConverterKeySpec(Key.ToggleSign, KeyStyle.ACTION, label = "±", descriptionRes = R.string.key_toggle_sign)),
        listOf(
            digit('0').let { ConverterKeySpec(it.key, it.style, label = it.label, span = 3f) },
            ConverterKeySpec(Key.Decimal, KeyStyle.NUMBER, label = decimalSeparator.toString(), descriptionRes = R.string.key_decimal),
        ),
    )
    val gap = if (compact) 6.dp else 10.dp
    val view = LocalView.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        rows.forEach { row ->
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { spec ->
                    KeyButton(
                        label = spec.label,
                        icon = spec.icon,
                        style = spec.style,
                        compact = compact,
                        contentDescription = spec.descriptionRes?.let { stringResource(it) },
                        modifier = Modifier.weight(spec.span).fillMaxHeight(),
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
