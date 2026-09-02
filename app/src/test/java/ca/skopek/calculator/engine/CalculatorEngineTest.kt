package ca.skopek.calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CalculatorEngineTest {
    private val formatter = NumberFormatter(Locale.US)
    private val engine = CalculatorEngine(formatter)

    /** Feeds a compact key script: digits, ".", the four operators as + - * and /, "%", "~" (toggle sign), "<" (backspace), "C", "=". */
    private fun run(script: String): Pair<CalculatorState, List<Calculation>> {
        var state = CalculatorState()
        val calculations = mutableListOf<Calculation>()
        for (ch in script) {
            val key = when (ch) {
                in '0'..'9' -> Key.Digit(ch)
                '.' -> Key.Decimal
                '+' -> Key.Operation(Operator.ADD)
                '-' -> Key.Operation(Operator.SUBTRACT)
                '*' -> Key.Operation(Operator.MULTIPLY)
                '/' -> Key.Operation(Operator.DIVIDE)
                '%' -> Key.Percent
                '~' -> Key.ToggleSign
                '<' -> Key.Backspace
                'C' -> Key.Clear
                '=' -> Key.Equals
                ' ' -> continue
                else -> error("Unknown key $ch")
            }
            val result = engine.press(state, key)
            state = result.state
            result.calculation?.let { calculations += it }
        }
        return state to calculations
    }

    private fun display(state: CalculatorState) = formatter.formatExpression(state.tokens)

    private fun resultOf(script: String): String {
        val (state, _) = run(script)
        assertTrue("expected evaluated state for '$script'", state.justEvaluated)
        return display(state)
    }

    @Test
    fun addition() = assertEquals("5", resultOf("2+3="))

    @Test
    fun precedence() = assertEquals("33", resultOf("12+7*3="))

    @Test
    fun leftToRightForSamePrecedence() = assertEquals("1", resultOf("8/4/2="))

    @Test
    fun decimalArithmeticIsExact() = assertEquals("0.3", resultOf("0.1+0.2="))

    @Test
    fun division() = assertEquals("2.5", resultOf("5/2="))

    @Test
    fun repeatingDecimalIsRounded() = assertEquals("0.333333333333", resultOf("1/3="))

    @Test
    fun percentOfLeftOperandForAddition() = assertEquals("55", resultOf("50+10%="))

    @Test
    fun percentOfLeftOperandForSubtraction() = assertEquals("180", resultOf("200-10%="))

    @Test
    fun percentIsPlainFractionForMultiplication() = assertEquals("5", resultOf("50*10%="))

    @Test
    fun lonePercent() = assertEquals("0.5", resultOf("50%="))

    @Test
    fun negativeNumbers() = assertEquals("-6", resultOf("2*3~="))

    @Test
    fun startWithMinus() = assertEquals("-1", resultOf("-3+2="))

    @Test
    fun negativeAfterMultiply() = assertEquals("-10", resultOf("5*-2="))

    @Test
    fun repeatedOperatorReplacesPrevious() {
        val (state, _) = run("5+*")
        assertEquals("5 ×", display(state))
    }

    @Test
    fun trailingOperatorIsIgnoredOnEquals() = assertEquals("5", resultOf("5+="))

    @Test
    fun divideByZeroShowsError() {
        val (state, calculations) = run("5/0=")
        assertEquals(EvalError.DIVIDE_BY_ZERO, state.error)
        assertEquals("5 ÷ 0", display(state))
        assertTrue(calculations.isEmpty())
    }

    @Test
    fun errorIsClearedByNextKey() {
        val (state, _) = run("5/0=<")
        assertNull(state.error)
        assertEquals("5 ÷", display(state))
    }

    @Test
    fun backspaceEditsNumbersAndOperators() {
        val (state, _) = run("12+3<<")
        assertEquals("12", display(state))
    }

    @Test
    fun backspaceRemovesPercentFirst() {
        val (state, _) = run("50%<")
        assertEquals("50", display(state))
    }

    @Test
    fun backspaceAfterResultClears() {
        val (state, _) = run("1+1=<")
        assertEquals("", display(state))
    }

    @Test
    fun leadingZeroIsReplaced() {
        val (state, _) = run("007")
        assertEquals("7", display(state))
    }

    @Test
    fun onlyOneDecimalPoint() {
        val (state, _) = run("1.2.3")
        assertEquals("1.23", display(state))
    }

    @Test
    fun decimalAfterOperatorStartsWithZero() {
        val (state, _) = run("1+.5")
        assertEquals("1 + 0.5", display(state))
    }

    @Test
    fun digitAfterResultStartsFresh() {
        val (state, _) = run("1+1=7")
        assertEquals("7", display(state))
    }

    @Test
    fun operatorAfterResultContinuesFromResult() = assertEquals("6", resultOf("1+1=*3="))

    @Test
    fun grouping() {
        val (state, _) = run("1234567.891")
        assertEquals("1,234,567.891", display(state))
    }

    @Test
    fun historyRecordsExpressionAndResult() {
        val (_, calculations) = run("12+7*3=")
        assertEquals(1, calculations.size)
        assertEquals("12 + 7 × 3", formatter.formatExpression(calculations[0].expression))
        assertEquals("33", formatter.formatResult(calculations[0].result))
    }

    @Test
    fun plainNumberEqualsIsNotRecorded() {
        val (state, calculations) = run("42=")
        assertTrue(calculations.isEmpty())
        assertTrue(state.justEvaluated)
    }

    @Test
    fun previewShowsRunningResult() {
        val (state, _) = run("12+7*3")
        assertEquals("33", formatter.formatResult(engine.preview(state)!!))
    }

    @Test
    fun previewIsHiddenWithoutCalculation() {
        assertNull(engine.preview(run("12").first))
        assertNull(engine.preview(run("12+").first))
        assertNull(engine.preview(run("1+1=").first))
    }

    @Test
    fun insertNumberReplacesCurrentNumber() {
        val state = engine.insertNumber(run("5+3").first, "33")
        assertEquals("5 + 33", display(state))
    }

    @Test
    fun insertNumberAfterResultStartsFresh() {
        val state = engine.insertNumber(run("1+1=").first, "33")
        assertEquals("33", display(state))
        assertNotNull(engine.preview(engine.press(engine.press(state, Key.Operation(Operator.ADD)).state, Key.Digit('1')).state))
    }

    @Test
    fun inputDigitLimit() {
        val (state, _) = run("12345678901234567890")
        assertEquals("123,456,789,012,345", display(state))
    }
}
