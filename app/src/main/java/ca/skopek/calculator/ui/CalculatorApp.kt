package ca.skopek.calculator.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.skopek.calculator.CalculatorUiState
import ca.skopek.calculator.ConverterViewModel
import ca.skopek.calculator.R
import ca.skopek.calculator.data.HistoryEntry
import ca.skopek.calculator.engine.Key
import ca.skopek.calculator.engine.units.Currencies
import ca.skopek.calculator.ui.theme.ThemeMode

/**
 * Root of the app. Two modes: the calculator (a paper tape with the keypad beneath it) and the
 * converter, which springs up over it like a sheet. The mode rail along the bottom, or a swipe
 * up from the keypad, moves between them.
 */
@Composable
fun CalculatorApp(
    uiState: CalculatorUiState,
    windowSizeClass: WindowSizeClass,
    decimalSeparator: Char,
    onKey: (Key) -> Unit,
    onHistorySelect: (HistoryEntry) -> Unit,
    onHistoryDelete: (HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onConverterOpenChange: (Boolean) -> Unit,
) {
    val converterViewModel: ConverterViewModel = viewModel()
    val converterState by converterViewModel.uiState.collectAsStateWithLifecycle()
    val mode = when {
        !uiState.converterOpen -> Mode.CALCULATOR
        converterState.category.id == Currencies.CATEGORY_ID -> Mode.CURRENCY
        else -> Mode.CONVERT
    }
    val selectMode: (Mode) -> Unit = { target ->
        when (target) {
            Mode.CALCULATOR -> onConverterOpenChange(false)
            Mode.CONVERT, Mode.CURRENCY -> {
                if (!uiState.converterOpen) uiState.display.currentValue?.let(converterViewModel::setInput)
                if (target == Mode.CURRENCY) converterViewModel.showCurrency() else converterViewModel.showUnits()
                onConverterOpenChange(true)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = uiState.converterOpen,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically(Motion.sheet()) { it } + fadeIn())
                        .togetherWith(slideOutVertically { -it / 8 } + fadeOut())
                } else {
                    (slideInVertically { -it / 8 } + fadeIn())
                        .togetherWith(slideOutVertically(Motion.sheet()) { it } + fadeOut())
                }
            },
            label = "mode",
        ) { converterOpen ->
            if (converterOpen) {
                ConverterScreen(
                    windowSizeClass = windowSizeClass,
                    decimalSeparator = decimalSeparator,
                    mode = mode,
                    onSelectMode = selectMode,
                    viewModel = converterViewModel,
                )
            } else {
                CalculatorScreen(
                    uiState = uiState,
                    windowSizeClass = windowSizeClass,
                    decimalSeparator = decimalSeparator,
                    onKey = onKey,
                    onHistorySelect = onHistorySelect,
                    onHistoryDelete = onHistoryDelete,
                    onClearHistory = onClearHistory,
                    onThemeChange = onThemeChange,
                    onSelectMode = selectMode,
                )
            }
        }
    }
}

@Composable
private fun CalculatorScreen(
    uiState: CalculatorUiState,
    windowSizeClass: WindowSizeClass,
    decimalSeparator: Char,
    onKey: (Key) -> Unit,
    onHistorySelect: (HistoryEntry) -> Unit,
    onHistoryDelete: (HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onSelectMode: (Mode) -> Unit,
) {
    val twoPane = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val compactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    val clipboard = LocalClipboardManager.current
    val onCopy: (HistoryEntry) -> Unit = { clipboard.setText(AnnotatedString(it.result)) }

    if (twoPane) {
        Row(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
                TapeHeader(
                    themeMode = uiState.themeMode,
                    onThemeChange = onThemeChange,
                    onClearHistory = onClearHistory,
                )
                Tape(
                    entries = uiState.history,
                    onUse = onHistorySelect,
                    onCopy = onCopy,
                    onDelete = onHistoryDelete,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
                Display(
                    state = uiState.display,
                    onSwipeBackspace = { onKey(Key.Backspace) },
                    onSwipeClear = { onKey(Key.Clear) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                Keypad(
                    decimalSeparator = decimalSeparator,
                    onKey = onKey,
                    compact = compactHeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (compactHeight) 2.2f else 1.6f)
                        .padding(horizontal = 12.dp)
                        .swipeUpToOpen { onSelectMode(Mode.CONVERT) },
                )
                ModeRail(active = Mode.CALCULATOR, onSelect = onSelectMode)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            TapeHeader(
                themeMode = uiState.themeMode,
                onThemeChange = onThemeChange,
                onClearHistory = onClearHistory,
            )
            Tape(
                entries = uiState.history,
                onUse = onHistorySelect,
                onCopy = onCopy,
                onDelete = onHistoryDelete,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Display(
                state = uiState.display,
                onSwipeBackspace = { onKey(Key.Backspace) },
                onSwipeClear = { onKey(Key.Clear) },
                modifier = Modifier.fillMaxWidth(),
            )
            Keypad(
                decimalSeparator = decimalSeparator,
                onKey = onKey,
                compact = compactHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (compactHeight) 2.2f else 1.35f)
                    .padding(horizontal = 12.dp)
                    .swipeUpToOpen { onSelectMode(Mode.CONVERT) },
            )
            ModeRail(active = Mode.CALCULATOR, onSelect = onSelectMode)
        }
    }
}

/** A swipe up across the keypad opens the converter sheet. Taps still go to the keys. */
@Composable
private fun Modifier.swipeUpToOpen(onOpen: () -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    return pointerInput(Unit) {
        var total = 0f
        var fired = false
        detectVerticalDragGestures(
            onDragStart = {
                total = 0f
                fired = false
            },
            onVerticalDrag = { change, amount ->
                total += amount
                if (!fired && total < -threshold) {
                    fired = true
                    change.consume()
                    onOpen()
                }
            },
        )
    }
}

@Composable
private fun TapeHeader(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onClearHistory: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.tape),
            style = MaterialTheme.typography.titleLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        Box {
            var menuOpen by remember { mutableStateOf(false) }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreHoriz, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Text(
                    text = stringResource(R.string.theme),
                    style = MaterialTheme.typography.labelMedium,
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
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clear_tape)) },
                    leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                    onClick = {
                        onClearHistory()
                        menuOpen = false
                    },
                )
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
