package com.myvillagebus.utils

import androidx.compose.runtime.*
import kotlinx.coroutines.delay

/**
 * Debounced click handler - ignores rapid clicks
 *
 * @param delayMillis Minimum time between clicks (default 500ms)
 * @param onClick Action to perform
 */
@Composable
fun rememberDebouncedClick(
    delayMillis: Long = 500L,
    onClick: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableStateOf(0L) }

    return remember {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= delayMillis) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }
}