package ca.skopek.calculator.engine.units

import ca.skopek.calculator.engine.NumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class CurrenciesTest {
    private val formatter = NumberFormatter(Locale.US)
    private val rates = mapOf(
        "USD" to BigDecimal("1"),
        "CAD" to BigDecimal("1.36"),
        "EUR" to BigDecimal("0.92"),
        "JPY" to BigDecimal("147.5"),
    )

    @Test
    fun buildsOnlyRequestedCurrenciesWithRates() {
        val category = Currencies.category(rates, listOf("CAD", "USD", "XXX", "EUR"), note = null)
        assertEquals(listOf("CAD", "USD", "EUR"), category.units.map { it.id })
        assertEquals("CAD", category.defaultFromId)
        assertEquals("USD", category.defaultToId)
        assertNull(category.note)
    }

    @Test
    fun convertsThroughUsd() {
        val category = Currencies.category(rates, listOf("USD", "CAD", "EUR", "JPY"), note = null)
        val cadToUsd = UnitConverter.convert(BigDecimal("136"), category.unit("CAD")!!, category.unit("USD")!!)
        assertEquals("100.00", formatter.formatCurrency(cadToUsd))
        val eurToCad = UnitConverter.convert(BigDecimal("92"), category.unit("EUR")!!, category.unit("CAD")!!)
        assertEquals("136.00", formatter.formatCurrency(eurToCad))
        val jpyToUsd = UnitConverter.convert(BigDecimal("1"), category.unit("JPY")!!, category.unit("USD")!!)
        assertEquals("0.00678", formatter.formatCurrency(jpyToUsd))
    }

    @Test
    fun namesAndFlags() {
        assertEquals("🇨🇦 Canadian dollar", Currencies.info("CAD").displayName)
        assertEquals("🇪🇺 Euro", Currencies.info("EUR").displayName)
        assertEquals("West African CFA franc", Currencies.info("XOF").displayName)
        assertEquals("ZZZ", Currencies.info("ZZZ").displayName)
    }

    @Test
    fun catalogHasUniqueCodesAndDefaultsExist() {
        assertEquals(Currencies.all.size, Currencies.all.map { it.code }.toSet().size)
        assertTrue(Currencies.defaultFavorites.all { code -> Currencies.all.any { it.code == code } })
    }

    @Test
    fun currencyFormatting() {
        assertEquals("1,234.57", formatter.formatCurrency(BigDecimal("1234.5678")))
        assertEquals("150.00", formatter.formatCurrency(BigDecimal("150")))
        assertEquals("0.7312", formatter.formatCurrency(BigDecimal("0.731245")))
        assertEquals("-2.50", formatter.formatCurrency(BigDecimal("-2.5")))
        assertEquals("0", formatter.formatCurrency(BigDecimal.ZERO))
    }
}
