package ca.skopek.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.skopek.calculator.data.HistoryEntry
import ca.skopek.calculator.data.HistoryRepository
import ca.skopek.calculator.data.SettingsRepository
import ca.skopek.calculator.engine.Calculation
import ca.skopek.calculator.engine.CalculatorEngine
import ca.skopek.calculator.engine.CalculatorState
import ca.skopek.calculator.engine.EvalError
import ca.skopek.calculator.engine.Key
import ca.skopek.calculator.engine.NumberFormatter
import ca.skopek.calculator.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the display area shows. */
data class DisplayState(
    /** The big line: the expression being typed, or the result after "=". */
    val primary: String,
    /** The small line: the live preview while typing, or the evaluated expression after "=". */
    val secondary: String?,
    /** True after "=": the secondary line (the expression) sits above the result. */
    val secondaryAbove: Boolean,
    val error: EvalError?,
)

data class CalculatorUiState(
    val display: DisplayState,
    val history: List<HistoryEntry>,
    val themeMode: ThemeMode,
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val formatter = NumberFormatter()
    private val engine = CalculatorEngine(formatter)
    private val historyRepository = HistoryRepository(application)
    private val settings = SettingsRepository(application)

    val decimalSeparator: Char get() = formatter.decimalSeparator

    private var state = CalculatorState()
    private var lastExpression: String? = null

    private val display = MutableStateFlow(render())
    private val history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    private val themeMode = MutableStateFlow(settings.themeMode)

    val uiState: StateFlow<CalculatorUiState> =
        combine(display, history, themeMode) { d, h, t -> CalculatorUiState(d, h, t) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                CalculatorUiState(display.value, history.value, themeMode.value),
            )

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { historyRepository.load() }
            // Anything calculated before the load finished goes after the stored entries.
            history.update { current -> loaded + current }
        }
    }

    fun onKey(key: Key) {
        val result = engine.press(state, key)
        if (key == Key.Equals && result.state.justEvaluated && !state.justEvaluated) {
            lastExpression = result.calculation?.let { formatter.formatExpression(it.expression) }
        }
        result.calculation?.let(::record)
        state = result.state
        display.value = render()
    }

    fun useHistoryEntry(entry: HistoryEntry) {
        state = engine.insertNumber(state, entry.resultValue)
        display.value = render()
    }

    fun clearHistory() {
        history.value = emptyList()
        persistHistory()
    }

    fun setThemeMode(mode: ThemeMode) {
        settings.themeMode = mode
        themeMode.value = mode
    }

    private fun record(calculation: Calculation) {
        val now = System.currentTimeMillis()
        val entry = HistoryEntry(
            id = maxOf(now, (history.value.lastOrNull()?.id ?: 0L) + 1),
            expression = formatter.formatExpression(calculation.expression),
            result = formatter.formatResult(calculation.result),
            resultValue = formatter.toInputText(calculation.result),
            timestamp = now,
        )
        history.update { (it + entry).takeLast(HistoryRepository.MAX_ENTRIES) }
        persistHistory()
    }

    private fun persistHistory() {
        val snapshot = history.value
        viewModelScope.launch(Dispatchers.IO) { historyRepository.save(snapshot) }
    }

    private fun render(): DisplayState {
        val expression = formatter.formatExpression(state.tokens).ifEmpty { "0" }
        return when {
            state.error != null -> DisplayState(expression, null, secondaryAbove = false, error = state.error)
            state.justEvaluated -> DisplayState(expression, lastExpression, secondaryAbove = true, error = null)
            else -> {
                val preview = engine.preview(state)?.let(formatter::formatResult)
                DisplayState(expression, preview, secondaryAbove = false, error = null)
            }
        }
    }
}
