package ca.skopek.calculator.data

/** One line on the tape: what was calculated and what came out. */
data class HistoryEntry(
    val id: Long,
    /** Formatted expression, e.g. "12 + 7 × 3". */
    val expression: String,
    /** Formatted result, e.g. "33". */
    val result: String,
    /** Raw result text that can be inserted back into the calculator. */
    val resultValue: String,
    val timestamp: Long,
)
