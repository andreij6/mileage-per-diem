package com.aijones.mileage.core.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

val LocalPageAnimatedScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

enum class MotionDirection { Top, Bottom, Start, End }

@Composable
fun Modifier.staggeredEnter(
    direction: MotionDirection,
    delayMillis: Int = 0,
): Modifier {
    val scope = LocalPageAnimatedScope.current ?: return this
    val offsetPx = with(LocalDensity.current) { 36.dp.roundToPx() }
    val enterSpec = tween<IntOffset>(
        durationMillis = 320,
        delayMillis = delayMillis,
        easing = FastOutSlowInEasing,
    )
    val fadeEnterSpec = tween<Float>(
        durationMillis = 260,
        delayMillis = delayMillis,
    )
    val exitSpec = tween<IntOffset>(durationMillis = 200)
    val fadeExitSpec = tween<Float>(durationMillis = 160)
    val enter: EnterTransition = when (direction) {
        MotionDirection.Top -> slideInVertically(enterSpec) { -offsetPx } + fadeIn(fadeEnterSpec)
        MotionDirection.Bottom -> slideInVertically(enterSpec) { offsetPx } + fadeIn(fadeEnterSpec)
        MotionDirection.Start -> slideInHorizontally(enterSpec) { -offsetPx } + fadeIn(fadeEnterSpec)
        MotionDirection.End -> slideInHorizontally(enterSpec) { offsetPx } + fadeIn(fadeEnterSpec)
    }
    val exit: ExitTransition = when (direction) {
        MotionDirection.Top -> slideOutVertically(exitSpec) { -offsetPx / 2 } + fadeOut(fadeExitSpec)
        MotionDirection.Bottom -> slideOutVertically(exitSpec) { offsetPx / 2 } + fadeOut(fadeExitSpec)
        MotionDirection.Start -> slideOutHorizontally(exitSpec) { -offsetPx / 2 } + fadeOut(fadeExitSpec)
        MotionDirection.End -> slideOutHorizontally(exitSpec) { offsetPx / 2 } + fadeOut(fadeExitSpec)
    }
    return with(scope) { this@staggeredEnter.animateEnterExit(enter = enter, exit = exit) }
}
