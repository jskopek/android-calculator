package ca.skopek.calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class NumberFormatterTest {
    private val us = NumberFormatter(Locale.US)

    @Test
    fun stripsTrailingZeros() = assertEquals("2.5", us.formatResult(BigDecimal("2.500")))

    @Test
    fun zero() = assertEquals("0", us.formatResult(BigDecimal("0.000")))

    @Test
    fun negativeGrouping() = assertEquals("-1,234,567", us.formatResult(BigDecimal("-1234567")))

    @Test
    fun largeNumbersUseScientific() = assertEquals("1.2345679E20", us.formatResult(BigDecimal("123456789012345678901")))

    @Test
    fun exactlyFifteenIntegerDigitsStaysPlain() =
        assertEquals("999,999,999,999,000", us.formatResult(BigDecimal("999999999999000")))

    @Test
    fun tinyNumbersUseScientific() = assertEquals("1.5E-10", us.formatResult(BigDecimal("0.00000000015")))

    @Test
    fun inputKeepsTypedText() {
        assertEquals("1,234.50", us.formatInput("1234.50"))
        assertEquals("0.", us.formatInput("0."))
        assertEquals("-", us.formatInput("-"))
        assertEquals("-0.5", us.formatInput("-0.5"))
    }

    @Test
    fun localeSeparators() {
        val de = NumberFormatter(Locale.GERMANY)
        assertEquals("1.234,5", de.formatInput("1234.5"))
        assertEquals(',', de.decimalSeparator)
    }

    @Test
    fun percentToken() = assertEquals("10%", us.formatToken(Token.Num("10", percent = true)))
}
