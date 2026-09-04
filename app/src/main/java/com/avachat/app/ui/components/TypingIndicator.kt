package com.avachat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Three-dot typing indicator. Uses one infinite transition (cheap) and stops
 * automatically when removed from composition.
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typingAlpha"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            var phase by remember { mutableIntStateOf(0) }
            LaunchedEffect(index) {
                delay(index * 150L)
                phase = 1
            }
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = if (phase == 1) alpha else 0.25f),
                        shape = CircleShape
                    )
            )
        }
    }
}
