package ca.skopek.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ca.skopek.calculator.data.ConverterSelection
import ca.skopek.calculator.data.SettingsRepository
import ca.skopek.calculator.engine.Key
import ca.skopek.calculator.engine.NumberFormatter
import ca.skopek.calculator.engine.NumberInput
import ca.skopek.calculator.engine.units.ConversionUnit
import ca.skopek.calculator.engine.units.UnitCatalog
import ca.skopek.calculator.engine.units.UnitCategory
import ca.skopek.calculator.engine.units.UnitConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal

enum class ConverterField { FROM, TO }

data class UnitValue(val unit: ConversionUnit, val text: String)

data class ConverterUiState(
    val categories: List<UnitCategory>,
    val category: UnitCategory,
    val fromUnit: ConversionUnit,
    val toUnit: ConversionUnit,
    val fromText: String,
    val toText: String,
    /** Which side the keypad edits; the other side shows the converted value. */
    val activeField: ConverterField,
    /** The value being edited expressed in every unit of the category. */
    val allValues: List<UnitValue>,
) {
    val activeUnit: ConversionUnit get() = if (activeField == ConverterField.FROM) fromUnit else toUnit
    val activeText: String get() = if (activeField == ConverterField.FROM) fromText else toText
}

class ConverterViewModel(application: Application) : AndroidViewModel(application) {
    private val formatter = NumberFormatter()
    private val settings = SettingsRepository(application)

    // Static categories today; a currency category with live rates can be appended here later.
    private val categories: List<UnitCategory> = UnitCatalog.categories

    private var category: UnitCategory
    private var fromUnit: ConversionUnit
    private var toUnit: ConversionUnit
    private var activeField = ConverterField.FROM
    private var input = "1"

    private val _uiState: MutableStateFlow<ConverterUiState>
    val uiState: StateFlow<ConverterUiState>

    init {
        val saved = settings.converterSelection
        category = saved?.let { s -> categories.firstOrNull { it.id == s.categoryId } } ?: categories.first()
        fromUnit = saved?.let { category.unit(it.fromUnitId) } ?: category.defaultFrom
        toUnit = saved?.let { category.unit(it.toUnitId) } ?: category.defaultTo
        _uiState = MutableStateFlow(build())
        uiState = _uiState.asStateFlow()
    }

    fun selectCategory(id: String) {
        val next = categories.firstOrNull { it.id == id } ?: return
        if (next == category) return
        category = next
        fromUnit = next.defaultFrom
        toUnit = next.defaultTo
        activeField = ConverterField.FROM
        publish()
    }

    fun selectUnit(field: ConverterField, unitId: String) {
        val unit = category.unit(unitId) ?: return
        when (field) {
            ConverterField.FROM -> fromUnit = unit
            ConverterField.TO -> toUnit = unit
        }
        publish()
    }

    /** From the "all units" list: show the tapped unit on the inactive side. */
    fun showUnit(unit: ConversionUnit) {
        when (activeField) {
            ConverterField.FROM -> toUnit = unit
            ConverterField.TO -> fromUnit = unit
        }
        publish()
    }

    fun activateField(field: ConverterField) {
        if (field == activeField) return
        // The value shown on that side becomes the editable input.
        input = formatter.toInputText(convertedValue())
        activeField = field
        publish()
    }

    fun swapUnits() {
        val previousFrom = fromUnit
        fromUnit = toUnit
        toUnit = previousFrom
        publish()
    }

    fun onKey(key: Key) {
        input = NumberInput.apply(input, key)
        publish()
    }

    private fun activeValue(): BigDecimal = input.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun convertedValue(): BigDecimal {
        val (from, to) = if (activeField == ConverterField.FROM) fromUnit to toUnit else toUnit to fromUnit
        return UnitConverter.convert(activeValue(), from, to)
    }

    private fun publish() {
        settings.converterSelection = ConverterSelection(category.id, fromUnit.id, toUnit.id)
        _uiState.value = build()
    }

    private fun build(): ConverterUiState {
        val typed = formatter.formatInput(input.ifEmpty { "0" })
        val converted = formatter.formatResult(convertedValue())
        val activeUnit = if (activeField == ConverterField.FROM) fromUnit else toUnit
        val value = activeValue()
        return ConverterUiState(
            categories = categories,
            category = category,
            fromUnit = fromUnit,
            toUnit = toUnit,
            fromText = if (activeField == ConverterField.FROM) typed else converted,
            toText = if (activeField == ConverterField.TO) typed else converted,
            activeField = activeField,
            allValues = category.units.map { unit ->
                UnitValue(unit, formatter.formatResult(UnitConverter.convert(value, activeUnit, unit)))
            },
        )
    }
}
