package ca.skopek.calculator.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.skopek.calculator.CalculatorUiState
import ca.skopek.calculator.ConverterViewModel
import ca.skopek.calculator.R
import ca.skopek.calculator.data.HistoryEntry
import ca.skopek.calculator.engine.Key
import ca.skopek.calculator.ui.theme.ThemeMode

/**
 * Root of the app. The calculator (current line + keypad) never changes. Beside it on a wide
 * screen, or above it on a phone, is a panel that is either the paper tape or the converter.
 * Swipe the panel sideways, swipe the keypad up or down, or tap the rail to switch.
 */
@Composable
fun CalculatorApp(
    uiState: CalculatorUiState,
    windowSizeClass: WindowSizeClass,
    decimalSeparator: Char,
    onKey: (Key) -> Unit,
    onInsertValue: (String) -> Unit,
    onHistorySelect: (HistoryEntry) -> Unit,
    onHistoryDelete: (HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onConverterOpenChange: (Boolean) -> Unit,
) {
    val converterViewModel: ConverterViewModel = viewModel()
    val panel = if (uiState.converterOpen) Panel.CONVERT else Panel.TAPE
    val view = LocalView.current
    val selectPanel: (Panel) -> Unit = { target ->
        if (target != panel) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onConverterOpenChange(target == Panel.CONVERT)
        }
    }
    // The converter follows whatever the calculator currently shows.
    LaunchedEffect(uiState.display.currentValue) { converterViewModel.setValue(uiState.display.currentValue) }
    BackHandler(enabled = panel == Panel.CONVERT) { selectPanel(Panel.TAPE) }

    val twoPane = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val compactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    val clipboard = LocalClipboardManager.current
    val onCopy: (HistoryEntry) -> Unit = { clipboard.setText(AnnotatedString(it.result)) }

    val panelContent: @Composable (Modifier) -> Unit = { modifier ->
        SwitchingPanel(
            panel = panel,
            onSelect = selectPanel,
            modifier = modifier,
        ) { shown ->
            when (shown) {
                Panel.TAPE -> Tape(
                    entries = uiState.history,
                    onUse = onHistorySelect,
                    onCopy = onCopy,
                    onDelete = onHistoryDelete,
                    modifier = Modifier.fillMaxSize(),
                )
                Panel.CONVERT -> ConverterPanel(
                    viewModel = converterViewModel,
                    onUseValue = onInsertValue,
                    tall = twoPane,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
    val keypadModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp)
        .swipeVertically(
            onUp = { selectPanel(Panel.CONVERT) },
            onDown = { selectPanel(Panel.TAPE) },
        )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (twoPane) {
            Row(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
                    PanelHeader(panel, uiState.themeMode, onThemeChange, onClearHistory)
                    panelContent(Modifier.weight(1f).fillMaxWidth())
                    ModeRail(active = panel, onSelect = selectPanel)
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
                        modifier = keypadModifier.weight(if (compactHeight) 2.2f else 1.6f).padding(bottom = 16.dp),
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                PanelHeader(panel, uiState.themeMode, onThemeChange, onClearHistory)
                panelContent(Modifier.weight(1f).fillMaxWidth())
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
                    modifier = keypadModifier.weight(if (compactHeight) 2.2f else 1.35f),
                )
                ModeRail(active = panel, onSelect = selectPanel)
            }
        }
    }
}

/** Slides between the tape and the converter, and takes a sideways swipe to switch. */
@Composable
private fun SwitchingPanel(
    panel: Panel,
    onSelect: (Panel) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Panel) -> Unit,
) {
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    Box(
        modifier = modifier.pointerInput(Unit) {
            var total = 0f
            var fired = false
            detectHorizontalDragGestures(
                onDragStart = {
                    total = 0f
                    fired = false
                },
                onHorizontalDrag = { change, amount ->
                    total += amount
                    if (!fired && total < -threshold) {
                        fired = true
                        change.consume()
                        onSelect(Panel.CONVERT)
                    } else if (!fired && total > threshold) {
                        fired = true
                        change.consume()
                        onSelect(Panel.TAPE)
                    }
                },
            )
        },
    ) {
        AnimatedContent(
            targetState = panel,
            transitionSpec = {
                val forward = targetState == Panel.CONVERT
                val enter = slideInHorizontally(Motion.sheet()) { if (forward) it else -it } + fadeIn()
                val exit = slideOutHorizontally(Motion.sheet()) { if (forward) -it / 3 else it / 3 } + fadeOut()
                enter.togetherWith(exit)
            },
            label = "panel",
        ) { shown -> content(shown) }
    }
}

/** Swipe up across the keypad opens the converter; swipe down brings the tape back. */
@Composable
private fun Modifier.swipeVertically(onUp: () -> Unit, onDown: () -> Unit): Modifier {
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
                    onUp()
                } else if (!fired && total > threshold) {
                    fired = true
                    change.consume()
                    onDown()
                }
            },
        )
    }
}

@Composable
private fun PanelHeader(
    panel: Panel,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onClearHistory: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = panel,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "title",
        ) { shown ->
            Text(
                text = stringResource(shown.labelRes),
                style = MaterialTheme.typography.titleLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
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
