package com.example.langpic.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/**
 * Entrance animation: slides up + fades in with a delay offset.
 * Use for staggered list items.
 */
@Composable
fun StaggeredEntrance(
    visible: Boolean = true,
    delayMs: Int = 0,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(durationMillis = 500, delayMillis = delayMs, easing = FastOutSlowInEasing),
        ) + fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = delayMs)),
    ) {
        content()
    }
}

/**
 * Continuous bounce animation. Returns a scale factor that oscillates between 1.0 and scaleTo.
 */
@Composable
fun rememberBounceScale(
    scaleTo: Float = 1.08f,
    durationMs: Int = 800,
): Float {
    val transition = rememberInfiniteTransition(label = "bounce")
    return transition.animateFloat(
        initialValue = 1f,
        targetValue = scaleTo,
        animationSpec = infiniteRepeatable(tween(durationMs), repeatMode = RepeatMode.Reverse),
        label = "bounceScale",
    ).value
}

/**
 * "Pop-in" scale animation for newly appearing elements.
 */
@Composable
fun PopIn(
    visible: Boolean = true,
    delayMs: Int = 0,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.3f,
            animationSpec = tween(durationMillis = 400, delayMillis = delayMs, easing = FastOutSlowInEasing),
        ) + fadeIn(animationSpec = tween(300, delayMillis = delayMs)),
    ) {
        content()
    }
}
