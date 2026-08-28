package com.example.anjiannotes.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.example.anjiannotes.data.NotePositionStore
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect

/**
 * 读取和保存详情预览态的滚动位置。
 *
 * 位置恢复只发生一次；后续滚动以 450ms 节流写入偏好，避免高频滚动触发同步 I/O。
 */
@OptIn(FlowPreview::class)
@Composable
fun RememberNoteReadingPosition(
    noteId: Long,
    isPreview: Boolean,
    scrollState: ScrollState,
    positionStore: NotePositionStore
) {
    var restored by remember(noteId) { mutableStateOf(false) }
    val latestPreview by rememberUpdatedState(isPreview)

    LaunchedEffect(noteId, isPreview, positionStore) {
        if (noteId > 0L && isPreview && !restored) {
            withFrameNanos { }
            scrollState.scrollTo(positionStore.load(noteId).readingScrollPx)
            restored = true
        }
    }

    LaunchedEffect(noteId, positionStore) {
        if (noteId <= 0L) return@LaunchedEffect
        snapshotFlow { latestPreview to scrollState.value }
            .distinctUntilChanged()
            .debounce(450)
            .collect { (preview, scrollPx) ->
                if (preview) positionStore.saveReadingScroll(noteId, scrollPx)
            }
    }
}
