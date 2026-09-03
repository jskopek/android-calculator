package ca.skopek.calculator.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Turns numbers and expressions into display text. Internally numbers always use '.' as the
 * decimal separator; the locale only affects how they are shown.
 */
class NumberFormatter(locale: Locale = Locale.getDefault()) {
    private val symbols = DecimalFormatSymbols.getInstance(locale)

    val decimalSeparator: Char get() = symbols.decimalSeparator
    private val groupingSeparator: Char get() = symbols.groupingSeparator

    /** Rounds a result for display / reuse. 12 significant digits is plenty for a basic calculator. */
    fun normalize(value: BigDecimal): BigDecimal {
        val rounded = value.round(MathContext(MAX_SIGNIFICANT_DIGITS, RoundingMode.HALF_UP)).stripTrailingZeros()
        return if (rounded.signum() == 0) BigDecimal.ZERO else rounded
    }

    /** Raw text suitable for storing in a [Token.Num] (no grouping, '.' decimal separator). */
    fun toInputText(value: BigDecimal): String = normalize(value).toPlainString()

    /** Formats a result for display, switching to scientific notation for very large or small values. */
    fun formatResult(value: BigDecimal): String {
        val v = normalize(value)
        if (v.signum() == 0) return "0"
        val exponent = v.precision() - v.scale() - 1
        if (exponent >= SCIENTIFIC_ABOVE_EXPONENT || exponent <= SCIENTIFIC_BELOW_EXPONENT) {
            val mantissa = v.movePointLeft(exponent).round(MathContext(SCIENTIFIC_DIGITS, RoundingMode.HALF_UP))
                .stripTrailingZeros()
            val exponentSign = if (exponent < 0) "-" else ""
            return formatInput(mantissa.toPlainString()) + "E" + exponentSign + kotlin.math.abs(exponent)
        }
        return formatInput(v.toPlainString())
    }

    /**
     * Money formatting: two decimals for amounts of one or more, otherwise four significant digits
     * so small unit values (1 JPY in USD) stay meaningful.
     */
    fun formatCurrency(value: BigDecimal): String {
        if (value.signum() == 0) return "0"
        return if (value.abs() >= BigDecimal.ONE) {
            formatInput(value.setScale(2, RoundingMode.HALF_UP).toPlainString())
        } else {
            formatResult(value.round(MathContext(4, RoundingMode.HALF_UP)))
        }
    }

    /**
     * Formats number text exactly as typed, only adding grouping separators to the integer part and
     * swapping in the locale's decimal separator. "1234.50" -> "1,234.50", "-" -> "-", "0." -> "0.".
     */
    fun formatInput(text: String): String {
        if (text.isEmpty()) return ""
        val negative = text.startsWith("-")
        val unsigned = if (negative) text.substring(1) else text
        val dot = unsigned.indexOf('.')
        val integerPart = if (dot >= 0) unsigned.substring(0, dot) else unsigned
        val fractionPart = if (dot >= 0) unsigned.substring(dot + 1) else null
        val builder = StringBuilder()
        if (negative) builder.append('-')
        integerPart.forEachIndexed { i, ch ->
            val remaining = integerPart.length - i
            if (i > 0 && remaining % 3 == 0) builder.append(groupingSeparator)
            builder.append(ch)
        }
        if (fractionPart != null) {
            builder.append(decimalSeparator)
            builder.append(fractionPart)
        }
        return builder.toString()
    }

    fun formatToken(token: Token): String = when (token) {
        is Token.Num -> {
            val number = formatNumberText(token.text)
            if (token.percent) "$number%" else number
        }
        is Token.Op -> token.operator.symbol.toString()
    }

    /** Formats a full expression, e.g. "12 + 7 × 3". */
    fun formatExpression(tokens: List<Token>): String =
        tokens.joinToString(separator = " ") { formatToken(it) }

    private fun formatNumberText(text: String): String {
        // Anything short enough to have been typed by hand is shown verbatim. Longer text can only
        // come from a result (e.g. "100000000000000000000"), which is formatted so huge or tiny
        // values collapse into scientific notation.
        val value = text.toBigDecimalOrNull()
        return if (value != null && text.length > MAX_INPUT_DIGITS + 2) formatResult(value) else formatInput(text)
    }

    companion object {
        const val MAX_SIGNIFICANT_DIGITS = 12
        const val MAX_INPUT_DIGITS = 15
        private const val SCIENTIFIC_ABOVE_EXPONENT = 15
        private const val SCIENTIFIC_BELOW_EXPONENT = -9
        private const val SCIENTIFIC_DIGITS = 8
    }
}
