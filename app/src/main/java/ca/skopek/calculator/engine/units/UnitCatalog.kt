package ca.skopek.calculator.engine.units

import java.math.BigDecimal
import java.math.MathContext

/** The built-in unit categories. Factors are exact definitions where one exists. */
object UnitCatalog {
    private fun unit(id: String, name: String, symbol: String, factor: String, offset: String = "0") =
        ConversionUnit(id, name, symbol, BigDecimal(factor), BigDecimal(offset))

    private val fiveNinths: BigDecimal = BigDecimal(5).divide(BigDecimal(9), MathContext.DECIMAL64)

    val length = UnitCategory(
        id = "length",
        name = "Length",
        units = listOf(
            unit("mm", "Millimeters", "mm", "0.001"),
            unit("cm", "Centimeters", "cm", "0.01"),
            unit("m", "Meters", "m", "1"),
            unit("km", "Kilometers", "km", "1000"),
            unit("in", "Inches", "in", "0.0254"),
            unit("ft", "Feet", "ft", "0.3048"),
            unit("yd", "Yards", "yd", "0.9144"),
            unit("mi", "Miles", "mi", "1609.344"),
            unit("nmi", "Nautical miles", "nmi", "1852"),
        ),
        defaultFromId = "cm",
        defaultToId = "in",
    )

    val area = UnitCategory(
        id = "area",
        name = "Area",
        units = listOf(
            unit("mm2", "Square millimeters", "mm²", "0.000001"),
            unit("cm2", "Square centimeters", "cm²", "0.0001"),
            unit("m2", "Square meters", "m²", "1"),
            unit("ha", "Hectares", "ha", "10000"),
            unit("km2", "Square kilometers", "km²", "1000000"),
            unit("in2", "Square inches", "in²", "0.00064516"),
            unit("ft2", "Square feet", "ft²", "0.09290304"),
            unit("yd2", "Square yards", "yd²", "0.83612736"),
            unit("ac", "Acres", "ac", "4046.8564224"),
            unit("mi2", "Square miles", "mi²", "2589988.110336"),
        ),
        defaultFromId = "m2",
        defaultToId = "ft2",
    )

    val temperature = UnitCategory(
        id = "temperature",
        name = "Temperature",
        units = listOf(
            ConversionUnit("c", "Celsius", "°C", BigDecimal.ONE),
            ConversionUnit("f", "Fahrenheit", "°F", fiveNinths, BigDecimal("-32")),
            ConversionUnit("k", "Kelvin", "K", BigDecimal.ONE, BigDecimal("-273.15")),
        ),
        defaultFromId = "c",
        defaultToId = "f",
    )

    val volume = UnitCategory(
        id = "volume",
        name = "Volume",
        units = listOf(
            unit("ml", "Milliliters", "mL", "0.001"),
            unit("l", "Liters", "L", "1"),
            unit("m3", "Cubic meters", "m³", "1000"),
            unit("tsp", "Teaspoons (US)", "tsp", "0.00492892159375"),
            unit("tbsp", "Tablespoons (US)", "tbsp", "0.01478676478125"),
            unit("floz", "Fluid ounces (US)", "fl oz", "0.0295735295625"),
            unit("cup", "Cups (US)", "cup", "0.2365882365"),
            unit("pt", "Pints (US)", "pt", "0.473176473"),
            unit("qt", "Quarts (US)", "qt", "0.946352946"),
            unit("gal", "Gallons (US)", "gal", "3.785411784"),
            unit("galuk", "Gallons (UK)", "gal (UK)", "4.54609"),
            unit("in3", "Cubic inches", "in³", "0.016387064"),
            unit("ft3", "Cubic feet", "ft³", "28.316846592"),
        ),
        defaultFromId = "l",
        defaultToId = "gal",
    )

    val mass = UnitCategory(
        id = "mass",
        name = "Mass",
        units = listOf(
            unit("mg", "Milligrams", "mg", "0.000001"),
            unit("g", "Grams", "g", "0.001"),
            unit("kg", "Kilograms", "kg", "1"),
            unit("t", "Metric tons", "t", "1000"),
            unit("oz", "Ounces", "oz", "0.028349523125"),
            unit("lb", "Pounds", "lb", "0.45359237"),
            unit("st", "Stones", "st", "6.35029318"),
            unit("ton", "Tons (US)", "ton", "907.18474"),
        ),
        defaultFromId = "kg",
        defaultToId = "lb",
    )

    val data = UnitCategory(
        id = "data",
        name = "Data",
        units = listOf(
            unit("bit", "Bits", "b", "0.125"),
            unit("byte", "Bytes", "B", "1"),
            unit("kb", "Kilobytes", "kB", "1000"),
            unit("mb", "Megabytes", "MB", "1000000"),
            unit("gb", "Gigabytes", "GB", "1000000000"),
            unit("tb", "Terabytes", "TB", "1000000000000"),
            unit("kib", "Kibibytes", "KiB", "1024"),
            unit("mib", "Mebibytes", "MiB", "1048576"),
            unit("gib", "Gibibytes", "GiB", "1073741824"),
            unit("tib", "Tebibytes", "TiB", "1099511627776"),
        ),
        defaultFromId = "gb",
        defaultToId = "mb",
    )

    val speed = UnitCategory(
        id = "speed",
        name = "Speed",
        units = listOf(
            unit("mps", "Meters per second", "m/s", "1"),
            unit("kmh", "Kilometers per hour", "km/h", "0.2777777777777778"),
            unit("mph", "Miles per hour", "mph", "0.44704"),
            unit("fps", "Feet per second", "ft/s", "0.3048"),
            unit("kn", "Knots", "kn", "0.5144444444444444"),
        ),
        defaultFromId = "kmh",
        defaultToId = "mph",
    )

    val time = UnitCategory(
        id = "time",
        name = "Time",
        units = listOf(
            unit("ms", "Milliseconds", "ms", "0.001"),
            unit("s", "Seconds", "s", "1"),
            unit("min", "Minutes", "min", "60"),
            unit("h", "Hours", "h", "3600"),
            unit("d", "Days", "d", "86400"),
            unit("wk", "Weeks", "wk", "604800"),
            unit("mo", "Months (average)", "mo", "2629746"),
            unit("yr", "Years (average)", "yr", "31556952"),
        ),
        defaultFromId = "h",
        defaultToId = "min",
    )

    val categories: List<UnitCategory> = listOf(length, area, temperature, volume, mass, data, speed, time)

    fun category(id: String): UnitCategory? = categories.firstOrNull { it.id == id }
}
