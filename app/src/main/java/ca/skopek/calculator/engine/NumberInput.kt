package ca.skopek.calculator.engine

/**
 * Editing rules for a single number typed on a keypad (used by the unit converter). The text is
 * the raw number with '.' as decimal separator; "" means nothing typed yet.
 */
object NumberInput {
    fun apply(text: String, key: Key): String = when (key) {
        is Key.Digit -> when {
            text == "0" -> key.digit.toString()
            text == "-0" -> "-${key.digit}"
            text.count { it.isDigit() } >= NumberFormatter.MAX_INPUT_DIGITS -> text
            else -> text + key.digit
        }
        Key.Decimal -> when {
            text.contains('.') -> text
            text.isEmpty() -> "0."
            text == "-" -> "-0."
            else -> "$text."
        }
        Key.ToggleSign -> if (text.startsWith("-")) text.substring(1) else "-$text"
        Key.Backspace -> text.dropLast(1)
        Key.Clear -> ""
        else -> text
    }
}
