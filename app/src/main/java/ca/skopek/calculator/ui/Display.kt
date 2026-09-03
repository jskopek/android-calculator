package ca.skopek.calculator.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import ca.skopek.calculator.DisplayState
import ca.skopek.calculator.R
import ca.skopek.calculator.engine.EvalError
import kotlinx.coroutines.launch

/**
 * The line being typed, at the bottom of the tape. While typing it shows the expression with a
 * live preview underneath; after "=" the expression moves up and the result springs into the big
 * line. Swipe left across it to backspace; a long swipe clears it.
 */
@Composable
fun Display(
    state: DisplayState,
    onSwipeBackspace: () -> Unit,
    onSwipeClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val secondaryStyle = MaterialTheme.typography.headlineSmall
    val primaryStyle = MaterialTheme.typography.displayMedium
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    val backspaceThreshold = with(density) { 56.dp.toPx() }
    val clearThreshold = with(density) { 200.dp.toPx() }

    Column(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        total += amount
                        // Rubber-band: the line follows the finger, but only part of the way.
                        val target = if (total < 0) total * 0.35f else total * 0.1f
                        scope.launch { drag.snapTo(target) }
                    },
                    onDragEnd = {
                        when {
                            total <= -clearThreshold -> {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onSwipeClear()
                            }
                            total <= -backspaceThreshold -> {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onSwipeBackspace()
                            }
                        }
                        scope.launch { drag.animateTo(0f, Motion.rubberBand()) }
                    },
                    onDragCancel = { scope.launch { drag.animateTo(0f, Motion.rubberBand()) } },
                )
            }
            .graphicsLayer { translationX = drag.value },
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        if (state.secondaryAbove) {
            ScrollingText(state.secondary.orEmpty(), secondaryStyle, colors.onSurfaceVariant)
        }
        // Digits typed just appear; a result springs up into place.
        AnimatedContent(
            targetState = state.secondaryAbove to state.primary,
            contentKey = { it.first },
            transitionSpec = {
                if (targetState.first) {
                    (slideInVertically(Motion.settle()) { it / 2 } + fadeIn(Motion.settle()))
                        .togetherWith(fadeOut())
                        .using(SizeTransform(clip = false))
                } else {
                    fadeIn().togetherWith(fadeOut()).using(SizeTransform(clip = false))
                }
            },
            label = "primaryLine",
        ) { (_, text) ->
            ScrollingText(text, primaryStyle, if (state.error != null) colors.error else colors.onSurface)
        }
        if (!state.secondaryAbove) {
            val (text, color) = when (state.error) {
                EvalError.DIVIDE_BY_ZERO -> stringResource(R.string.error_divide_by_zero) to colors.error
                EvalError.INCOMPLETE -> stringResource(R.string.error_incomplete) to colors.error
                null -> state.secondary.orEmpty() to colors.onSurfaceVariant
            }
            ScrollingText(text, secondaryStyle, color)
        }
    }
}

/** Single-line text that stays right-aligned and keeps its end (the most recent input) visible. */
@Composable
internal fun ScrollingText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(scrollState, reverseScrolling = true),
        )
    }
}
