package ca.skopek.calculator.engine

import java.math.BigDecimal

/**
 * Pure, side-effect free editing logic for the calculator. Given the current [CalculatorState] and
 * a [Key], it produces the next state (and a [Calculation] when "=" completed something).
 */
class CalculatorEngine(private val formatter: NumberFormatter = NumberFormatter()) {

    fun press(state: CalculatorState, key: Key): KeyResult {
        val s = state.copy(error = null)
        return when (key) {
            is Key.Digit -> KeyResult(appendDigit(s, key.digit))
            Key.Decimal -> KeyResult(appendDecimal(s))
            is Key.Operation -> KeyResult(applyOperator(s, key.operator))
            Key.Percent -> KeyResult(applyPercent(s))
            Key.ToggleSign -> KeyResult(toggleSign(s))
            Key.Backspace -> KeyResult(backspace(s))
            Key.Clear -> KeyResult(CalculatorState())
            Key.Equals -> evaluate(s)
        }
    }

    /** Inserts a complete number (for example a result picked from history) at the cursor. */
    fun insertNumber(state: CalculatorState, text: String): CalculatorState {
        val token = Token.Num(text)
        val last = state.tokens.lastOrNull()
        return when {
            state.justEvaluated -> CalculatorState(listOf(token))
            last is Token.Num -> state.copy(tokens = state.tokens.replaceLast(token), error = null)
            else -> state.copy(tokens = state.tokens + token, error = null)
        }
    }

    /** The live result of the expression typed so far, or null when there is nothing to show. */
    fun preview(state: CalculatorState): BigDecimal? {
        if (state.justEvaluated || state.error != null) return null
        val expression = state.tokens.trimIncomplete()
        if (!expression.isCalculation()) return null
        return (Evaluator.evaluate(expression) as? Evaluator.Result.Success)?.value
    }

    private fun appendDigit(s: CalculatorState, digit: Char): CalculatorState {
        val last = s.tokens.lastOrNull()
        if (s.justEvaluated || (last is Token.Num && last.percent)) {
            return CalculatorState(listOf(Token.Num(digit.toString())))
        }
        if (last is Token.Num) {
            val text = last.text
            val newText = when {
                text == "0" -> digit.toString()
                text == "-0" -> "-$digit"
                text.count { it.isDigit() } >= NumberFormatter.MAX_INPUT_DIGITS -> return s
                else -> text + digit
            }
            return s.copy(tokens = s.tokens.replaceLast(Token.Num(newText)))
        }
        return s.copy(tokens = s.tokens + Token.Num(digit.toString()))
    }

    private fun appendDecimal(s: CalculatorState): CalculatorState {
        val last = s.tokens.lastOrNull()
        if (s.justEvaluated || (last is Token.Num && last.percent)) {
            return CalculatorState(listOf(Token.Num("0.")))
        }
        if (last is Token.Num) {
            val text = last.text
            val newText = when {
                text.contains('.') -> return s
                text == "-" -> "-0."
                text.isEmpty() -> "0."
                else -> "$text."
            }
            return s.copy(tokens = s.tokens.replaceLast(Token.Num(newText)))
        }
        return s.copy(tokens = s.tokens + Token.Num("0."))
    }

    private fun applyOperator(s: CalculatorState, operator: Operator): CalculatorState {
        if (s.justEvaluated) {
            return s.copy(tokens = s.tokens + Token.Op(operator), justEvaluated = false)
        }
        return when (val last = s.tokens.lastOrNull()) {
            null -> if (operator == Operator.SUBTRACT) s.copy(tokens = listOf(Token.Num("-"))) else s
            is Token.Op -> {
                // "5 × −" starts a negative number; any other repeated operator replaces the previous one.
                if (operator == Operator.SUBTRACT && !last.operator.isAdditive) {
                    s.copy(tokens = s.tokens + Token.Num("-"))
                } else {
                    s.copy(tokens = s.tokens.replaceLast(Token.Op(operator)))
                }
            }
            is Token.Num -> if (last.isComplete()) s.copy(tokens = s.tokens + Token.Op(operator)) else s
        }
    }

    private fun applyPercent(s: CalculatorState): CalculatorState {
        val last = s.tokens.lastOrNull() as? Token.Num ?: return s
        if (last.percent || !last.isComplete()) return s
        return s.copy(tokens = s.tokens.replaceLast(last.copy(percent = true)), justEvaluated = false)
    }

    private fun toggleSign(s: CalculatorState): CalculatorState {
        return when (val last = s.tokens.lastOrNull()) {
            null -> s.copy(tokens = listOf(Token.Num("-")))
            is Token.Op -> s.copy(tokens = s.tokens + Token.Num("-"))
            is Token.Num -> {
                val text = if (last.text.startsWith("-")) last.text.substring(1) else "-" + last.text
                s.copy(tokens = s.tokens.replaceLast(last.copy(text = text)))
            }
        }
    }

    private fun backspace(s: CalculatorState): CalculatorState {
        if (s.justEvaluated) return CalculatorState()
        return when (val last = s.tokens.lastOrNull()) {
            null -> s
            is Token.Op -> s.copy(tokens = s.tokens.dropLast(1))
            is Token.Num -> when {
                last.percent -> s.copy(tokens = s.tokens.replaceLast(last.copy(percent = false)))
                last.text.length <= 1 -> s.copy(tokens = s.tokens.dropLast(1))
                else -> s.copy(tokens = s.tokens.replaceLast(last.copy(text = last.text.dropLast(1))))
            }
        }
    }

    private fun evaluate(s: CalculatorState): KeyResult {
        if (s.justEvaluated) return KeyResult(s)
        val expression = s.tokens.trimIncomplete()
        if (expression.isEmpty()) return KeyResult(s)
        val value = when (val result = Evaluator.evaluate(expression)) {
            is Evaluator.Result.Success -> result.value
            is Evaluator.Result.Failure -> return KeyResult(s.copy(error = result.error))
        }
        val next = CalculatorState(tokens = listOf(Token.Num(formatter.toInputText(value))), justEvaluated = true)
        val calculation = if (expression.isCalculation()) Calculation(expression, value) else null
        return KeyResult(next, calculation)
    }

    private fun Token.Num.isComplete(): Boolean = text.any { it.isDigit() }

    private fun List<Token>.isCalculation(): Boolean =
        any { it is Token.Op } || any { it is Token.Num && it.percent }

    /** Drops trailing operators and unfinished numbers so "5 × −" evaluates as "5". */
    private fun List<Token>.trimIncomplete(): List<Token> {
        var tokens = this
        while (true) {
            val last = tokens.lastOrNull() ?: return tokens
            val incomplete = last is Token.Op || (last is Token.Num && !last.isComplete())
            if (!incomplete) return tokens
            tokens = tokens.dropLast(1)
        }
    }

    private fun List<Token>.replaceLast(token: Token): List<Token> = dropLast(1) + token
}
