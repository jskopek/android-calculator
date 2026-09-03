package ca.skopek.calculator.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/** The one motion vocabulary: everything springs, overshoots a little, and settles. */
object Motion {
    fun <T> keyPress() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    fun <T> settle() = spring<T>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    fun <T> sheet() = spring<T>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    fun <T> rubberBand() = spring<T>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
}
