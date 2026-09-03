package ca.skopek.calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberInputTest {
    private fun type(script: String): String {
        var text = ""
        for (ch in script) {
            val key = when (ch) {
                in '0'..'9' -> Key.Digit(ch)
                '.' -> Key.Decimal
                '~' -> Key.ToggleSign
                '<' -> Key.Backspace
                'C' -> Key.Clear
                else -> error("unknown $ch")
            }
            text = NumberInput.apply(text, key)
        }
        return text
    }

    @Test
    fun digits() = assertEquals("123", type("123"))

    @Test
    fun leadingZero() = assertEquals("7", type("007"))

    @Test
    fun decimal() = assertEquals("0.5", type(".5"))

    @Test
    fun singleDecimal() = assertEquals("1.25", type("1.2.5"))

    @Test
    fun sign() = assertEquals("-12", type("12~"))

    @Test
    fun signOnEmpty() = assertEquals("-0.5", type("~.5"))

    @Test
    fun backspaceAndClear() {
        assertEquals("12", type("123<"))
        assertEquals("", type("123C"))
    }

    @Test
    fun digitLimit() = assertEquals("123456789012345", type("1234567890123456789"))
}
