package ca.skopek.calculator.engine.units

import ca.skopek.calculator.engine.NumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class UnitConverterTest {
    private val formatter = NumberFormatter(Locale.US)

    private fun convert(category: UnitCategory, value: String, from: String, to: String): String {
        val result = UnitConverter.convert(BigDecimal(value), category.unit(from)!!, category.unit(to)!!)
        return formatter.formatResult(result)
    }

    @Test
    fun acresToSquareMeters() = assertEquals("4,046.8564224", convert(UnitCatalog.area, "1", "ac", "m2"))

    @Test
    fun squareMetersToSquareFeet() = assertEquals("107.639104167", convert(UnitCatalog.area, "10", "m2", "ft2"))

    @Test
    fun milesToKilometers() = assertEquals("1.609344", convert(UnitCatalog.length, "1", "mi", "km"))

    @Test
    fun inchesToCentimeters() = assertEquals("15.24", convert(UnitCatalog.length, "6", "in", "cm"))

    @Test
    fun celsiusToFahrenheit() {
        assertEquals("212", convert(UnitCatalog.temperature, "100", "c", "f"))
        assertEquals("32", convert(UnitCatalog.temperature, "0", "c", "f"))
    }

    @Test
    fun fahrenheitToCelsius() {
        assertEquals("100", convert(UnitCatalog.temperature, "212", "f", "c"))
        assertEquals("-40", convert(UnitCatalog.temperature, "-40", "f", "c"))
        assertEquals("37", convert(UnitCatalog.temperature, "98.6", "f", "c"))
    }

    @Test
    fun kelvin() {
        assertEquals("273.15", convert(UnitCatalog.temperature, "0", "c", "k"))
        assertEquals("-459.67", convert(UnitCatalog.temperature, "0", "k", "f"))
    }

    @Test
    fun gallonsToLiters() = assertEquals("3.785411784", convert(UnitCatalog.volume, "1", "gal", "l"))

    @Test
    fun poundsToKilograms() = assertEquals("0.45359237", convert(UnitCatalog.mass, "1", "lb", "kg"))

    @Test
    fun gibibytesToMegabytes() = assertEquals("1,073.741824", convert(UnitCatalog.data, "1", "gib", "mb"))

    @Test
    fun bitsToBytes() = assertEquals("1", convert(UnitCatalog.data, "8", "bit", "byte"))

    @Test
    fun mphToKmh() = assertEquals("96.56064", convert(UnitCatalog.speed, "60", "mph", "kmh"))

    @Test
    fun knotsToKmh() = assertEquals("1.852", convert(UnitCatalog.speed, "1", "kn", "kmh"))

    @Test
    fun hoursToMinutes() = assertEquals("90", convert(UnitCatalog.time, "1.5", "h", "min"))

    @Test
    fun sameUnitIsIdentity() = assertEquals("42", convert(UnitCatalog.length, "42", "m", "m"))

    @Test
    fun roundTripsAreStable() {
        for (category in UnitCatalog.categories) {
            for (from in category.units) for (to in category.units) {
                val there = UnitConverter.convert(BigDecimal("123.456"), from, to)
                val back = UnitConverter.convert(there, to, from)
                assertEquals("${category.id}: ${from.id} -> ${to.id}", "123.456", formatter.formatResult(back))
            }
        }
    }

    @Test
    fun catalogIsConsistent() {
        for (category in UnitCatalog.categories) {
            assertTrue(category.units.size >= 3)
            assertEquals(category.units.size, category.units.map { it.id }.toSet().size)
            assertNotNull(category.unit(category.defaultFromId))
            assertNotNull(category.unit(category.defaultToId))
        }
        assertEquals(UnitCatalog.categories.size, UnitCatalog.categories.map { it.id }.toSet().size)
    }
}
