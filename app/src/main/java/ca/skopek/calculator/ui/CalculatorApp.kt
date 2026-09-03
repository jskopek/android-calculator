package ca.skopek.calculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.skopek.calculator.CalculatorUiState
import ca.skopek.calculator.ConverterViewModel
import ca.skopek.calculator.DisplayState
import ca.skopek.calculator.R
import ca.skopek.calculator.data.HistoryEntry
import ca.skopek.calculator.engine.Key
import ca.skopek.calculator.ui.theme.ThemeMode

/**
 * Root layout. Adapts to the window:
 *  - Compact width (phone, or the cover screen of a foldable): calculator only, history in a bottom sheet.
 *  - Medium/Expanded width (unfolded inner screen, landscape, tablets): history tape beside the calculator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorApp(
    uiState: CalculatorUiState,
    windowSizeClass: WindowSizeClass,
    decimalSeparator: Char,
    onKey: (Key) -> Unit,
    onHistorySelect: (HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onConverterOpenChange: (Boolean) -> Unit,
) {
    val converterViewModel: ConverterViewModel = viewModel()
    if (uiState.converterOpen) {
        ConverterScreen(
            windowSizeClass = windowSizeClass,
            decimalSeparator = decimalSeparator,
            onBack = { onConverterOpenChange(false) },
            viewModel = converterViewModel,
        )
        return
    }
    val openConverter = {
        uiState.display.currentValue?.let(converterViewModel::setInput)
        onConverterOpenChange(true)
    }

    val twoPane = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val compactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    var showHistorySheet by rememberSaveable { mutableStateOf(false) }

    // Unfolding while the sheet is open: the side pane takes over, so drop the sheet.
    LaunchedEffect(twoPane) { if (twoPane) showHistorySheet = false }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (twoPane) {
            Row(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                HistoryPane(
                    entries = uiState.history,
                    onSelect = onHistorySelect,
                    onClear = onClearHistory,
                    modifier = Modifier.weight(0.42f).fillMaxHeight().padding(top = 8.dp),
                )
                VerticalDivider()
                CalculatorPane(
                    display = uiState.display,
                    themeMode = uiState.themeMode,
                    decimalSeparator = decimalSeparator,
                    compactHeight = compactHeight,
                    showHistoryButton = false,
                    onShowHistory = {},
                    onOpenConverter = openConverter,
                    onKey = onKey,
                    onThemeChange = onThemeChange,
                    modifier = Modifier.weight(0.58f).fillMaxHeight(),
                )
            }
        } else {
            CalculatorPane(
                display = uiState.display,
                themeMode = uiState.themeMode,
                decimalSeparator = decimalSeparator,
                compactHeight = compactHeight,
                showHistoryButton = true,
                onShowHistory = { showHistorySheet = true },
                onOpenConverter = openConverter,
                onKey = onKey,
                onThemeChange = onThemeChange,
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            )

            if (showHistorySheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showHistorySheet = false },
                    sheetState = sheetState,
                ) {
                    HistoryPane(
                        entries = uiState.history,
                        onSelect = { entry ->
                            onHistorySelect(entry)
                            showHistorySheet = false
                        },
                        onClear = onClearHistory,
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatorPane(
    display: DisplayState,
    themeMode: ThemeMode,
    decimalSeparator: Char,
    compactHeight: Boolean,
    showHistoryButton: Boolean,
    onShowHistory: () -> Unit,
    onOpenConverter: () -> Unit,
    onKey: (Key) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TopActions(
            themeMode = themeMode,
            showHistoryButton = showHistoryButton,
            onShowHistory = onShowHistory,
            onOpenConverter = onOpenConverter,
            onThemeChange = onThemeChange,
        )
        Display(
            state = display,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        Keypad(
            decimalSeparator = decimalSeparator,
            onKey = onKey,
            compact = compactHeight,
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (compactHeight) 2.2f else 1.6f)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun TopActions(
    themeMode: ThemeMode,
    showHistoryButton: Boolean,
    onShowHistory: () -> Unit,
    onOpenConverter: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        IconButton(onClick = onOpenConverter) {
            Icon(Icons.Outlined.SwapHoriz, contentDescription = stringResource(R.string.unit_converter))
        }
        if (showHistoryButton) {
            IconButton(onClick = onShowHistory) {
                Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.history))
            }
        }
        Box {
            var menuOpen by remember { mutableStateOf(false) }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Text(
                    text = stringResource(R.string.theme),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(stringResource(mode.labelRes)) },
                        leadingIcon = { Icon(mode.icon, contentDescription = null) },
                        trailingIcon = {
                            if (mode == themeMode) Icon(Icons.Outlined.Check, contentDescription = null)
                        },
                        onClick = {
                            onThemeChange(mode)
                            menuOpen = false
                        },
                    )
                }
            }
        }
    }
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

private val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
        ThemeMode.LIGHT -> Icons.Outlined.LightMode
        ThemeMode.DARK -> Icons.Outlined.DarkMode
    }
