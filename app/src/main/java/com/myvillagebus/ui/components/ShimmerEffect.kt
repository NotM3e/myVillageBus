package com.myvillagebus.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Tworzy animowany shimmer brush
 */
@Composable
fun shimmerBrush(
    shimmerColors: List<Color> = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    )
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")

    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 500f, translateAnimation - 500f),
        end = Offset(translateAnimation, translateAnimation)
    )
}

/**
 * Placeholder box z shimmer efektem
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

/**
 * Placeholder karta przewoźnika
 */
@Composable
fun CarrierCardPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Nazwa + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Nazwa przewoźnika
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(24.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opis
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Badge placeholder
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(28.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Divider placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            )

            // Info rows + button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Info po lewej
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Przycisk po prawej
                ShimmerBox(
                    modifier = Modifier
                        .width(100.dp)
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}