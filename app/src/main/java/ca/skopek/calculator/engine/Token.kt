package ca.skopek.calculator.engine

/** Binary operators supported by the basic calculator, with their display symbols. */
enum class Operator(val symbol: Char) {
    ADD('+'),
    SUBTRACT('−'),
    MULTIPLY('×'),
    DIVIDE('÷');

    val isAdditive: Boolean get() = this == ADD || this == SUBTRACT
}

/** One element of the expression the user is building. */
sealed interface Token {
    /**
     * A number as typed by the user. [text] is the raw, unformatted text and may be a partial
     * number while editing (for example "-", "0." or "12.").
     */
    data class Num(val text: String, val percent: Boolean = false) : Token

    data class Op(val operator: Operator) : Token
}

/** A key press on the keypad. */
sealed interface Key {
    data class Digit(val digit: Char) : Key
    data object Decimal : Key
    data class Operation(val operator: Operator) : Key
    data object Percent : Key
    data object ToggleSign : Key
    data object Backspace : Key
    data object Clear : Key
    data object Equals : Key
}

/** The full editing state of the calculator. Immutable; every key press produces a new one. */
data class CalculatorState(
    val tokens: List<Token> = emptyList(),
    /** True right after "=" was pressed: digits start a fresh expression, operators continue from the result. */
    val justEvaluated: Boolean = false,
    val error: EvalError? = null,
)

enum class EvalError { DIVIDE_BY_ZERO, INCOMPLETE }

/** A completed calculation, emitted when "=" produces a result worth recording in history. */
data class Calculation(val expression: List<Token>, val result: java.math.BigDecimal)

data class KeyResult(val state: CalculatorState, val calculation: Calculation? = null)
