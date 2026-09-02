package ca.skopek.calculator.engine

import java.math.BigDecimal
import java.math.MathContext

/**
 * Evaluates a token list with standard precedence (× ÷ before + −) using exact decimal arithmetic.
 *
 * Percent follows the convention of most basic calculators:
 *  - `50 + 10%` is 55 (the percent is taken of the left operand for + and −)
 *  - `50 × 10%` is 5 (the percent is simply divided by 100 for × and ÷)
 *  - `50%` on its own is 0.5
 */
object Evaluator {
    sealed interface Result {
        data class Success(val value: BigDecimal) : Result
        data class Failure(val error: EvalError) : Result
    }

    private val divisionContext = MathContext.DECIMAL64

    fun evaluate(tokens: List<Token>): Result {
        if (tokens.isEmpty()) return Result.Failure(EvalError.INCOMPLETE)
        return try {
            val parser = Parser(tokens)
            val value = parser.parseExpression()
            if (parser.hasMore()) Result.Failure(EvalError.INCOMPLETE) else Result.Success(value)
        } catch (e: EvaluationException) {
            Result.Failure(e.error)
        }
    }

    private class EvaluationException(val error: EvalError) : RuntimeException()

    /** A term that is a lone percent number keeps its raw value so the caller can apply "% of left operand". */
    private data class Term(val value: BigDecimal, val lonePercent: Boolean)

    private class Parser(private val tokens: List<Token>) {
        private var index = 0

        fun hasMore(): Boolean = index < tokens.size

        fun parseExpression(): BigDecimal {
            val first = parseTerm()
            var acc = if (first.lonePercent) first.value.movePointLeft(2) else first.value
            while (hasMore()) {
                val op = nextOperator()
                if (!op.isAdditive) throw EvaluationException(EvalError.INCOMPLETE)
                index++
                val term = parseTerm()
                val rhs = if (term.lonePercent) acc.multiply(term.value).movePointLeft(2) else term.value
                acc = if (op == Operator.ADD) acc.add(rhs) else acc.subtract(rhs)
            }
            return acc
        }

        private fun parseTerm(): Term {
            val (firstValue, firstPercent) = parseNumber()
            var acc = firstValue
            var lonePercent = firstPercent
            while (hasMore()) {
                val op = nextOperator()
                if (op.isAdditive) break
                index++
                if (lonePercent) {
                    acc = acc.movePointLeft(2)
                    lonePercent = false
                }
                val (rawValue, percent) = parseNumber()
                val value = if (percent) rawValue.movePointLeft(2) else rawValue
                acc = when (op) {
                    Operator.MULTIPLY -> acc.multiply(value)
                    Operator.DIVIDE -> {
                        if (value.signum() == 0) throw EvaluationException(EvalError.DIVIDE_BY_ZERO)
                        acc.divide(value, divisionContext)
                    }
                    else -> throw EvaluationException(EvalError.INCOMPLETE)
                }
            }
            return Term(acc, lonePercent)
        }

        private fun nextOperator(): Operator =
            (tokens[index] as? Token.Op)?.operator ?: throw EvaluationException(EvalError.INCOMPLETE)

        private fun parseNumber(): Pair<BigDecimal, Boolean> {
            val token = tokens.getOrNull(index) as? Token.Num ?: throw EvaluationException(EvalError.INCOMPLETE)
            index++
            val value = token.text.toBigDecimalOrNull() ?: throw EvaluationException(EvalError.INCOMPLETE)
            return value to token.percent
        }
    }
}
