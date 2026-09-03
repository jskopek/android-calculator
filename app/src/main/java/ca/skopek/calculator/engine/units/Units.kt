package ca.skopek.calculator.engine.units

import java.math.BigDecimal
import java.math.MathContext

/**
 * A unit expressed relative to its category's base unit:
 * `base = (value + offset) × factor` and `value = base ÷ factor − offset`.
 * Plain units only need a factor; temperature scales also use an offset.
 */
data class ConversionUnit(
    val id: String,
    val name: String,
    val symbol: String,
    val factor: BigDecimal,
    val offset: BigDecimal = BigDecimal.ZERO,
) {
    fun toBase(value: BigDecimal): BigDecimal = value.add(offset).multiply(factor)

    fun fromBase(base: BigDecimal): BigDecimal = base.divide(factor, MathContext.DECIMAL64).subtract(offset)
}

/**
 * A group of units that convert between each other. Static categories come from [UnitCatalog];
 * categories with live data (currencies) can be built at runtime with the same class.
 */
data class UnitCategory(
    val id: String,
    val name: String,
    val units: List<ConversionUnit>,
    val defaultFromId: String,
    val defaultToId: String,
    /** Optional footnote, e.g. when exchange rates were last updated. */
    val note: String? = null,
) {
    fun unit(id: String): ConversionUnit? = units.firstOrNull { it.id == id }
    val defaultFrom: ConversionUnit get() = unit(defaultFromId) ?: units.first()
    val defaultTo: ConversionUnit get() = unit(defaultToId) ?: units.last()
}

object UnitConverter {
    fun convert(value: BigDecimal, from: ConversionUnit, to: ConversionUnit): BigDecimal =
        if (from == to) value else to.fromBase(from.toBase(value))
}
