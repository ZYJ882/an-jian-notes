package com.example.anjiannotes.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 仅识别短按和双击，但不消费指针事件。
 * 长按与拖动完全交给 SelectionContainer 的系统文本选择处理。
 */
fun Modifier.nonConsumingTapGestures(
    onTap: ((Offset) -> Unit)? = null,
    onDoubleTap: ((Offset) -> Unit)? = null
): Modifier = pointerInput(onTap, onDoubleTap) {
    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        val firstUp = awaitUpOrCancellation(firstDown.id)
        if (firstUp == null) return@awaitEachGesture
        if (firstUp.uptimeMillis - firstDown.uptimeMillis >= viewConfiguration.longPressTimeoutMillis) {
            return@awaitEachGesture
        }

        val secondDown = awaitNextDownWithin(viewConfiguration.doubleTapTimeoutMillis)
        if (secondDown == null) {
            onTap?.invoke(firstUp.position)
            return@awaitEachGesture
        }
        val secondUp = awaitUpOrCancellation(secondDown.id)
        if (secondUp != null && secondUp.uptimeMillis - secondDown.uptimeMillis < viewConfiguration.longPressTimeoutMillis) {
            onDoubleTap?.invoke(secondUp.position)
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitUpOrCancellation(pointerId: PointerId): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == pointerId } ?: return null
        if (change.changedToUpIgnoreConsumed()) return change
        if (change.position != change.previousPosition) return null
    }
}

private suspend fun AwaitPointerEventScope.awaitNextDownWithin(timeoutMillis: Long): PointerInputChange? {
    return withTimeoutOrNull(timeoutMillis) {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() }
            if (down != null) return@withTimeoutOrNull down
        }
        error("unreachable")
    }
}
