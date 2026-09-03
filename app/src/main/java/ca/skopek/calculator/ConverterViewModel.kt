package ca.skopek.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.skopek.calculator.data.ConverterSelection
import ca.skopek.calculator.data.CurrencyRates
import ca.skopek.calculator.data.CurrencyRepository
import ca.skopek.calculator.data.SettingsRepository
import ca.skopek.calculator.engine.Key
import ca.skopek.calculator.engine.NumberFormatter
import ca.skopek.calculator.engine.NumberInput
import ca.skopek.calculator.engine.units.Currencies
import ca.skopek.calculator.engine.units.CurrencyInfo
import ca.skopek.calculator.engine.units.ConversionUnit
import ca.skopek.calculator.engine.units.UnitCatalog
import ca.skopek.calculator.engine.units.UnitCategory
import ca.skopek.calculator.engine.units.UnitConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

enum class ConverterField { FROM, TO }

data class UnitValue(val unit: ConversionUnit, val text: String)

/** Currency-specific bits of the converter state; null for other categories. */
data class CurrencyStatus(
    val ratesAvailable: Boolean,
    val updating: Boolean,
    val note: String,
    val providerUrl: String?,
    /** Codes currently shown in the picker, in display order. */
    val favorites: List<String>,
    /** Everything the rate provider offers, for the "choose currencies" dialog. */
    val available: List<CurrencyInfo>,
)

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
    val currency: CurrencyStatus? = null,
) {
    val activeUnit: ConversionUnit get() = if (activeField == ConverterField.FROM) fromUnit else toUnit
    val activeText: String get() = if (activeField == ConverterField.FROM) fromText else toText
}

class ConverterViewModel(application: Application) : AndroidViewModel(application) {
    private val formatter = NumberFormatter()
    private val settings = SettingsRepository(application)
    private val currencyRepository = CurrencyRepository(application)

    private var favorites: List<String> = settings.currencyFavorites ?: Currencies.defaultFavorites
    private lateinit var currencyCategory: UnitCategory
    private lateinit var currencyStatus: CurrencyStatus
    private val categories: List<UnitCategory> get() = UnitCatalog.categories + currencyCategory

    private var category: UnitCategory
    private var fromUnit: ConversionUnit
    private var toUnit: ConversionUnit
    private var activeField = ConverterField.FROM
    private var input = "1"

    private val _uiState: MutableStateFlow<ConverterUiState>
    val uiState: StateFlow<ConverterUiState>

    init {
        rebuildCurrency(currencyRepository.rates.value, updating = false, error = null)
        val saved = settings.converterSelection
        category = saved?.let { s -> categories.firstOrNull { it.id == s.categoryId } } ?: categories.first()
        fromUnit = saved?.let { category.unit(it.fromUnitId) } ?: category.defaultFrom
        toUnit = saved?.let { category.unit(it.toUnitId) } ?: category.defaultTo
        input = settings.converterInput ?: "1"
        _uiState = MutableStateFlow(build())
        uiState = _uiState.asStateFlow()

        viewModelScope.launch {
            combine(currencyRepository.rates, currencyRepository.updating, currencyRepository.error) { r, u, e -> Triple(r, u, e) }
                .collect { (rates, updating, error) ->
                    rebuildCurrency(rates, updating, error)
                    if (isCurrency) reselectCurrencyUnits()
                    publish()
                }
        }
        if (isCurrency) refreshRates()
    }

    private val isCurrency: Boolean get() = category.id == Currencies.CATEGORY_ID

    /** Called whenever the converter screen becomes visible: fetch fresh rates once a day. */
    fun onShown() {
        if (isCurrency) refreshRates()
    }

    private fun refreshRates() {
        viewModelScope.launch { currencyRepository.refreshIfStale() }
    }

    fun selectCategory(id: String) {
        val next = categories.firstOrNull { it.id == id } ?: return
        if (next == category) return
        category = next
        fromUnit = next.defaultFrom
        toUnit = next.defaultTo
        activeField = ConverterField.FROM
        if (isCurrency) refreshRates()
        publish()
    }

    /** Sets the number to convert (used when opening the converter from the calculator). */
    fun setInput(text: String) {
        val value = text.toBigDecimalOrNull() ?: return
        input = formatter.toInputText(value)
        activeField = ConverterField.FROM
        publish()
    }

    fun selectUnit(field: ConverterField, unitId: String) {
        val unit = category.unit(unitId) ?: return
        // Choosing the unit already shown on the other side swaps the pair instead of duplicating it.
        when (field) {
            ConverterField.FROM -> {
                if (unit == toUnit) toUnit = fromUnit
                fromUnit = unit
            }
            ConverterField.TO -> {
                if (unit == fromUnit) fromUnit = toUnit
                toUnit = unit
            }
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

    /** Flips the units. The number in the "from" field stays put and is now in the other unit. */
    fun swapUnits() {
        if (activeField == ConverterField.TO) {
            input = formatter.toInputText(convertedValue())
            activeField = ConverterField.FROM
        }
        val previousFrom = fromUnit
        fromUnit = toUnit
        toUnit = previousFrom
        publish()
    }

    fun onKey(key: Key) {
        input = NumberInput.apply(input, key)
        publish()
    }

    /** Adds or removes a currency from the picker. At least two must remain. */
    fun toggleFavorite(code: String) {
        val next = if (code in favorites) favorites - code else favorites + code
        if (next.size < 2) return
        favorites = next
        settings.currencyFavorites = next
        rebuildCurrency(currencyRepository.rates.value, currencyStatus.updating, null)
        if (isCurrency) reselectCurrencyUnits()
        publish()
    }

    private fun rebuildCurrency(rates: CurrencyRates?, updating: Boolean, error: String?) {
        val app = getApplication<Application>()
        val ordered = Currencies.all.map { it.code }.filter { it in favorites } +
            favorites.filter { code -> Currencies.all.none { it.code == code } }
        val note = when {
            updating -> app.getString(R.string.currency_updating)
            rates == null && error != null -> app.getString(R.string.currency_update_failed, error)
            rates == null -> app.getString(R.string.currency_no_rates)
            else -> {
                val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(rates.updatedAt))
                val base = app.getString(R.string.currency_rates_by, rates.providerName, date)
                if (error != null) app.getString(R.string.currency_update_failed, error) + "\n" + base else base
            }
        }
        val rateMap = rates?.rates ?: ordered.associateWith { BigDecimal.ONE }
        currencyCategory = Currencies.category(
            rates = rateMap,
            codes = ordered,
            note = note,
            preferredFrom = localCurrencyCode(),
            preferredTo = Currencies.BASE,
        )
        val availableCodes = rates?.rates?.keys ?: Currencies.all.map { it.code }.toSet()
        currencyStatus = CurrencyStatus(
            ratesAvailable = rates != null,
            updating = updating,
            note = note,
            providerUrl = rates?.providerUrl,
            favorites = ordered,
            available = availableCodes.map(Currencies::info).sortedBy { it.name.lowercase() },
        )
    }

    /** After the currency category is rebuilt, keep the same units if they still exist. */
    private fun reselectCurrencyUnits() {
        category = currencyCategory
        fromUnit = category.unit(fromUnit.id) ?: category.defaultFrom
        toUnit = category.unit(toUnit.id) ?: category.defaultTo
        if (toUnit == fromUnit) toUnit = category.units.firstOrNull { it != fromUnit } ?: toUnit
    }

    private fun localCurrencyCode(): String = try {
        Currency.getInstance(Locale.getDefault()).currencyCode
    } catch (e: Exception) {
        Currencies.BASE
    }

    private fun activeValue(): BigDecimal = input.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun convertedValue(): BigDecimal {
        val (from, to) = if (activeField == ConverterField.FROM) fromUnit to toUnit else toUnit to fromUnit
        return UnitConverter.convert(activeValue(), from, to)
    }

    private fun formatValue(value: BigDecimal): String =
        if (isCurrency) formatter.formatCurrency(value) else formatter.formatResult(value)

    private fun publish() {
        settings.converterSelection = ConverterSelection(category.id, fromUnit.id, toUnit.id)
        settings.converterInput = input
        _uiState.value = build()
    }

    private fun build(): ConverterUiState {
        val ratesMissing = isCurrency && !currencyStatus.ratesAvailable
        val typed = formatter.formatInput(input.ifEmpty { "0" })
        val converted = if (ratesMissing) "—" else formatValue(convertedValue())
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
                val text = if (ratesMissing) "—" else formatValue(UnitConverter.convert(value, activeUnit, unit))
                UnitValue(unit, text)
            },
            currency = if (isCurrency) currencyStatus else null,
        )
    }
}
