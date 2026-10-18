package com.example.anjiannotes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.inputmethod.InputMethodManager
import android.widget.EdgeEffect
import android.widget.EditText
import android.widget.OverScroller
import android.widget.Toast
import android.view.View
import android.net.Uri
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.anjiannotes.data.FolderEntity
import com.example.anjiannotes.data.NoteEntity
import com.example.anjiannotes.data.NotePositionStore
import com.example.anjiannotes.ui.screenHorizontalPadding
import com.example.anjiannotes.ui.rememberIsWatchWindow
import com.example.anjiannotes.ui.contentMaxWidth
import com.example.anjiannotes.ui.contentWidthCap
import com.example.anjiannotes.ui.EditorSeed
import com.example.anjiannotes.ui.MarkdownPreview
import com.example.anjiannotes.ui.LocalMarkdownFontFamily
import com.example.anjiannotes.ui.MarkdownSyntaxHint
import com.example.anjiannotes.ui.NoteFormatMode
import com.example.anjiannotes.ui.RememberNoteReadingPosition
import com.example.anjiannotes.ui.extractLinkAt
import com.example.anjiannotes.ui.findSearchMatchRanges
import com.example.anjiannotes.ui.linkifyPlainText
import com.example.anjiannotes.ui.searchHighlightColors
import com.example.anjiannotes.ui.theme.motionSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun NoteDetailPage(
    note: NoteEntity?,
    seed: EditorSeed,
    initialFolderId: Long,
    initialContentCursor: Int?,
    initialPinned: Boolean,
    folders: List<FolderEntity>,
    positionStore: NotePositionStore,
    // 编辑器协程作用域由宿主（NotesApp）提供：双栏 pane 切换选中时详情组合会销毁，
    // 提升后的作用域保证防抖窗内的最后编辑仍能落盘。单栏整页导航下由默认值创建，
    // 语义与旧版一致（离开组合即取消）。
    editorScope: CoroutineScope = rememberCoroutineScope(),
    // 宿主请求详情「冲刷并离开」的令牌：每次自增触发一次 finalizeAndLeave(forceLeave = true)，
    // 用于双栏切换选中与窗口尺寸类变化（Promote/Demote）时的有序交接。
    leaveRequest: Int = 0,
    onBack: (Long, Long) -> Unit,
    onSave: (Long, String, String, Long, Boolean, Boolean, Boolean, Long, Long) -> Deferred<Long>,
    onDelete: (NoteEntity) -> Unit,
    onDiscardEmptyDraft: (Long) -> Unit,
    onExport: (String, String, Boolean) -> Unit,
    onCopyToFolder: (NoteEntity, Long) -> Unit
) {
    val context = LocalContext.current
    val isNewNote = note == null
    val noteId = note?.id
    val noteTitle = note?.title
    val noteContent = note?.content
    val noteColor = note?.color
    val notePinned = note?.isPinned
    val noteTopPinned = note?.isTopPinned
    val noteFolderId = note?.folderId
    var title by remember(noteId, seed) { mutableStateOf(noteTitle ?: seed.title) }
    var contentValue by remember(noteId, seed) { mutableStateOf(TextFieldValue(noteContent ?: seed.content)) }
    val content = contentValue.text
    var color by remember(noteId) { mutableStateOf(noteColor ?: NoteColors.first()) }
    var pinned by remember(noteId, initialPinned) { mutableStateOf(notePinned ?: initialPinned) }
    var topPinned by remember(noteId) { mutableStateOf(noteTopPinned ?: false) }
    var selectedFolderId by remember(noteId, initialFolderId) { mutableStateOf(noteFolderId ?: initialFolderId) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showCopyFolderPicker by remember { mutableStateOf(false) }
    var showDetailMenu by remember(noteId, seed) { mutableStateOf(false) }
    var focusMode by remember(noteId, seed) { mutableStateOf(false) }
    var formatMode by remember(noteId, seed) {
        mutableStateOf(
            if (note != null) {
                if (note.isMarkdown) NoteFormatMode.MARKDOWN else NoteFormatMode.PLAIN
            } else {
                seed.formatMode
            }
        )
    }
    var latestDraft by remember(noteId, seed, initialFolderId, initialPinned) {
        val initialContent = noteContent ?: seed.content
        mutableStateOf(
            NoteDraftSnapshot(
                title = noteTitle ?: seed.title,
                content = initialContent,
                color = noteColor ?: NoteColors.first(),
                isPinned = notePinned ?: initialPinned,
                isTopPinned = noteTopPinned ?: false,
                isMarkdown = formatMode.resolvesToMarkdown(initialContent),
                folderId = noteFolderId ?: initialFolderId
            )
        )
    }
    var detailMode by remember(noteId, seed) {
        mutableStateOf(if (note == null) DetailMode.EDIT else DetailMode.PREVIEW)
    }
    var focusTarget by remember(noteId, seed) {
        mutableStateOf(InlineEditTarget.CONTENT)
    }
    var requestedContentSelection by remember(noteId, seed) { mutableStateOf<TextRange?>(null) }
    var savedNoteId by remember(noteId, seed) { mutableStateOf(noteId ?: 0L) }
    val draftCreatedAt = remember(noteId, seed) { note?.createdAt ?: System.currentTimeMillis() }
    val seedHasContent = isNewNote && (seed.title.isNotBlank() || seed.content.isNotBlank())
    var editRevision by remember(noteId, seed) { mutableStateOf(if (seedHasContent) 1 else 0) }
    var savedRevision by remember(noteId, seed) { mutableStateOf(0) }
    var hasUserEdited by remember(noteId, seed) { mutableStateOf(seedHasContent) }
    var autoSaveState by remember(noteId, seed) { mutableStateOf(AutoSaveState.IDLE) }
    var saveError by remember(noteId, seed) { mutableStateOf<String?>(null) }
    var leavingInProgress by remember(noteId, seed) { mutableStateOf(false) }
    var saveWorker by remember(noteId, seed) { mutableStateOf<Job?>(null) }
    var debounceJob by remember(noteId, seed) { mutableStateOf<Job?>(null) }
    var nativeTitleFocusRequest by remember(noteId, seed) { mutableStateOf(-1) }
    var nativeContentFocusRequest by remember(noteId, seed) { mutableStateOf(-1) }
    var nativeContentEditor by remember(noteId, seed) { mutableStateOf<NativeNoteEditText?>(null) }
    var nativeEditorScrollVersion by remember(noteId, seed) { mutableStateOf(0) }
    val keyboard = LocalSoftwareKeyboardController.current
    val markdownActive = formatMode.resolvesToMarkdown(content)
    val folderName = folders.firstOrNull { it.id == selectedFolderId }?.name ?: "默认收藏夹"
    val characterCount = title.length + content.length
    val detailScrollState = rememberScrollState()
    var pendingScrollRestore by remember(noteId, seed) { mutableStateOf<Int?>(null) }
    // 预览页正文搜索状态：开关、关键字、当前命中序号（全局）与命中总数。
    // 搜索状态仅属于当前详情页实例；使用无键 remember，避免大型详情函数的 FIR 数据流图膨胀。
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActiveOrdinal by remember { mutableStateOf(0) }
    var searchTotal by remember { mutableStateOf(0) }

    LaunchedEffect(detailMode, focusTarget) {
        if (detailMode == DetailMode.EDIT) {
            when (focusTarget) {
                InlineEditTarget.TITLE -> nativeTitleFocusRequest += 1
                InlineEditTarget.CONTENT -> {
                    requestedContentSelection?.let { selection ->
                        contentValue = contentValue.copy(
                            selection = TextRange(
                                selection.start.coerceIn(0, contentValue.text.length),
                                selection.end.coerceIn(0, contentValue.text.length)
                            )
                        )
                        requestedContentSelection = null
                    }
                    nativeContentFocusRequest += 1
                }
            }
            pendingScrollRestore?.let { scrollPosition ->
                // 焦点建立会触发文本控件的可见区域校正；下一帧再恢复预览态的阅读位置。
                withFrameNanos { }
                detailScrollState.scrollTo(scrollPosition)
                pendingScrollRestore = null
            }
        } else {
            keyboard?.hide()
        }
    }

    fun enterEdit(target: InlineEditTarget, cursorPosition: Int? = null) {
        focusMode = false
        if (detailMode == DetailMode.PREVIEW) pendingScrollRestore = detailScrollState.value
        if (target == InlineEditTarget.CONTENT) {
            requestedContentSelection = when {
                cursorPosition != null -> TextRange(cursorPosition)
                savedNoteId > 0L -> positionStore.load(savedNoteId).let { saved ->
                    TextRange(saved.selectionStart, saved.selectionEnd)
                }
                else -> null
            }
        }
        focusTarget = target
        detailMode = DetailMode.EDIT
    }

    fun editorLog(message: String) {
        if (BuildConfig.DEBUG) Log.d("NoteEditor", message)
    }

    fun startSaveWorker() {
        if (!hasUserEdited || savedRevision >= editRevision || saveWorker?.isActive == true) return
        saveWorker = editorScope.launch(start = CoroutineStart.UNDISPATCHED) {
            while (hasUserEdited && savedRevision < editRevision) {
                val revisionToSave = editRevision
                // latestDraft 在 TextField 的 onValueChange 内同步更新，不依赖下一次组合重绘。
                val draftToSave = latestDraft
                autoSaveState = AutoSaveState.SAVING
                saveError = null
                editorLog(
                    "save start revision=$revisionToSave id=$savedNoteId contentLength=${draftToSave.content.length} " +
                        if (savedNoteId == 0L) "creating new note" else "updating note"
                )
                try {
                    val id = onSave(
                        savedNoteId,
                        draftToSave.title,
                        draftToSave.content,
                        draftToSave.color,
                        draftToSave.isPinned,
                        draftToSave.isTopPinned,
                        draftToSave.isMarkdown,
                        draftToSave.folderId,
                        draftCreatedAt
                    ).await()
                    savedNoteId = id
                    savedRevision = revisionToSave
                    autoSaveState = AutoSaveState.SAVED
                    editorLog("save completed revision=$revisionToSave id=$id")
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    saveError = error.message ?: "保存失败"
                    autoSaveState = AutoSaveState.IDLE
                    editorLog("save failed revision=$revisionToSave message=${saveError}")
                    return@launch
                }
            }
        }
    }

    fun scheduleAutoSave() {
        debounceJob?.cancel()
        if (savedNoteId == 0L) {
            // 第一处有效编辑立即创建草稿；后续内容全部进入同一个顺序保存队列。
            startSaveWorker()
        } else {
            debounceJob = editorScope.launch {
                delay(700)
                editorLog("debounce save")
                startSaveWorker()
            }
        }
    }

    fun markEdited(
        titleOverride: String = title,
        contentOverride: String = contentValue.text
    ) {
        // 在输入回调内保存最新值；即使用户立刻侧滑返回，保存队列也不会读取旧快照。
        latestDraft = NoteDraftSnapshot(
            title = titleOverride,
            content = contentOverride,
            color = color,
            isPinned = pinned,
            isTopPinned = topPinned,
            isMarkdown = formatMode.resolvesToMarkdown(contentOverride),
            folderId = selectedFolderId
        )
        hasUserEdited = true
        editRevision++
        saveError = null
        editorLog("content changed dirty=true revision=$editRevision contentLength=${contentOverride.length}")
        scheduleAutoSave()
    }

    LaunchedEffect(seedHasContent) {
        if (seedHasContent) scheduleAutoSave()
    }

    fun syncNativeContentBeforePreview() {
        val editor = nativeContentEditor ?: return
        val currentText = editor.text?.toString().orEmpty()
        if (currentText == contentValue.text) return
        val selectionStart = editor.selectionStart.coerceIn(0, currentText.length)
        val selectionEnd = editor.selectionEnd.coerceIn(0, currentText.length)
        contentValue = TextFieldValue(currentText, TextRange(selectionStart, selectionEnd))
        if (savedNoteId > 0L) positionStore.saveSelection(savedNoteId, selectionStart, selectionEnd)
        // AndroidView 的最后一次输入可能尚未触发 Compose 重组；切换预览前以视图
        // 当前文本作为最终来源，同步更新草稿和保存队列。
        markEdited(contentOverride = currentText)
    }

    suspend fun finalizePendingChanges(): Boolean {
        debounceJob?.cancel()
        // 有有效内容的新建页离开前仍经由已有单一保存队列写入一次 Room；这里不创建
        // 第二套保存机制。空草稿（标题与正文均无有效字符）不再强制写入，避免产生空笔记。
        if (savedRevision >= editRevision) {
            val editorText = nativeContentEditor?.text
            val finalContent = if (editorText != null) editorText.toString() else contentValue.text
            if (shouldForceFinalDraftSave(isNewNote, savedNoteId, title, finalContent)) {
                latestDraft = NoteDraftSnapshot(
                    title = title,
                    content = finalContent,
                    color = color,
                    isPinned = pinned,
                    isTopPinned = topPinned,
                    isMarkdown = formatMode.resolvesToMarkdown(finalContent),
                    folderId = selectedFolderId
                )
                hasUserEdited = true
                editRevision = savedRevision + 1
            }
        }
        editorLog("final save before leave dirty=${hasUserEdited && savedRevision < editRevision}")
        while (hasUserEdited && savedRevision < editRevision) {
            startSaveWorker()
            saveWorker?.join()
            if (saveError != null) return false
        }
        return true
    }

    fun finalizeAndLeave(forceLeave: Boolean = false) {
        // 返回时始终从原生 EditText 同步最后一帧用户输入，不依赖下一次 Compose 重组或 debounce。
        if (detailMode == DetailMode.EDIT) syncNativeContentBeforePreview()
        if (leavingInProgress) {
            editorLog("back ignored while final save is in progress")
            return
        }
        editorLog("back requested mode=$detailMode isNew=$isNewNote forceLeave=$forceLeave")
        leavingInProgress = true
        keyboard?.hide()
        editorScope.launch {
            if (!finalizePendingChanges()) {
                editorLog("back cancelled because final save failed")
                leavingInProgress = false
                return@launch
            }
            if (isNewNote) {
                if (isNewDraftEmpty(latestDraft.title, latestDraft.content)) {
                    // 空草稿不留在列表里：从未写入过就直接返回；输入后又清空的
                    // 情况已落过库，离开前删除这条空记录，避免出现“未命名空笔记”。
                    if (savedNoteId > 0L) {
                        editorLog("new draft empty; discard saved empty note id=$savedNoteId")
                        onDiscardEmptyDraft(savedNoteId)
                    } else {
                        editorLog("new draft empty; nothing persisted; leaving detail")
                    }
                    onBack(selectedFolderId, 0L)
                } else {
                    // 新建笔记第一次返回即在 Room 写入确认后回到主页，不再中转预览页。
                    editorLog("new draft saved; leaving detail")
                    onBack(selectedFolderId, savedNoteId)
                }
            } else if (detailMode == DetailMode.EDIT && !forceLeave) {
                // 已有笔记保留原有交互：第一次返回仅结束编辑，第二次返回离开详情。
                // forceLeave（双栏切换选中/窗口尺寸类变化）跳过停留直接冲刷离开，
                // 后续流向由宿主的 DetailExitIntent 决定，不经过用户二次确认。
                detailMode = DetailMode.PREVIEW
                editorLog("final save confirmed; switched to preview")
                withFrameNanos { }
                leavingInProgress = false
            } else {
                editorLog("final save confirmed; leaving detail")
                onBack(selectedFolderId, savedNoteId)
            }
        }
    }

    // 宿主请求的冲刷离开（leaveRequest 令牌）：双栏下切换选中另一篇笔记、或窗口
    // 尺寸类变化引发 Promote/Demote 时，由宿主自增令牌触发；详情完成最终保存后
    // 经 onBack 交还控制权，由宿主应用意图。令牌归零（初始态）不触发。
    LaunchedEffect(leaveRequest) {
        if (leaveRequest > 0) {
            editorLog("leave request token=$leaveRequest; finalizing")
            finalizeAndLeave(forceLeave = true)
        }
    }

    RememberNoteReadingPosition(
        noteId = savedNoteId,
        isPreview = detailMode == DetailMode.PREVIEW,
        scrollState = detailScrollState,
        positionStore = positionStore
    )

    LaunchedEffect(autoSaveState) {
        if (autoSaveState == AutoSaveState.SAVED) {
            delay(1_400)
            if (autoSaveState == AutoSaveState.SAVED) autoSaveState = AutoSaveState.IDLE
        }
    }

    LaunchedEffect(searchTotal) {
        if (searchTotal > 0 && searchActiveOrdinal >= searchTotal) searchActiveOrdinal = 0
    }

    fun moveSearchMatch(step: Int) {
        if (searchTotal <= 0) return
        searchActiveOrdinal = ((searchActiveOrdinal + step) % searchTotal + searchTotal) % searchTotal
    }

    fun toggleDetailMode() {
        if (detailMode == DetailMode.PREVIEW) {
            enterEdit(InlineEditTarget.CONTENT)
        } else {
            // 右上角“预览”不会经过系统返回逻辑，必须在此同步 AndroidView
            // 内尚未完成 Compose 重组的最后输入，避免预览读取旧草稿。
            syncNativeContentBeforePreview()
            startSaveWorker()
            detailMode = DetailMode.PREVIEW
            keyboard?.hide()
        }
    }

    fun openPreviewLink(link: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // 后台切换只催促同一保存队列；离开详情页仍以 handleBack 的最终确认为准。
                editorLog("lifecycle stop: request pending save")
                startSaveWorker()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val latestFinalizeAndLeave by rememberUpdatedState(::finalizeAndLeave)
    // 专注阅读使用分层返回：第一次返回只恢复普通阅读，第二次才离开笔记。
    BackHandler(enabled = true) {
        if (focusMode) focusMode = false else latestFinalizeAndLeave(false)
    }

    // 首次进入的轻量淡入：graphicsLayer alpha 只触发重绘、不触发重组与重新测量，
    // 且恒定向 1f 收敛，离开详情页时无退场成本。
    var pageEntered by remember(noteId, seed) { mutableStateOf(false) }
    LaunchedEffect(note?.id, seed) { pageEntered = true }
    // 当前动效档位（最低档 = 最快）：时长与曲线统一由此给出。
    val motion = motionSpec()
    val pageEnterAlpha by animateFloatAsState(
        targetValue = if (pageEntered) 1f else 0f,
        animationSpec = tween(durationMillis = motion.medium, easing = motion.easing),
        label = "detail-enter-alpha"
    )

    Scaffold(
        modifier = Modifier.graphicsLayer { alpha = pageEnterAlpha },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedVisibility(
                visible = !focusMode,
                enter = fadeIn(animationSpec = tween(motion.medium, easing = motion.easing)),
                // 退场统一走微反馈时长：收起比展开更快，符合「最快档」的节奏。
                exit = fadeOut(animationSpec = tween(motion.micro, easing = motion.easing))
            ) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    TextButton(onClick = ::finalizeAndLeave, modifier = Modifier.size(48.dp)) {
                        Text("←", fontSize = 28.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Text(
                        "$characterCount 字",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    when {
                        saveError != null -> Text("保存失败", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 4.dp))
                        autoSaveState == AutoSaveState.SAVING -> Text("保存中…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
                        else -> AnimatedVisibility(
                            // 「✓ 已保存」快速缩放入场、随即淡出：只走 alpha/scale，不触发重新布局。
                            visible = autoSaveState == AutoSaveState.SAVED,
                            enter = fadeIn(animationSpec = tween(motion.fast, easing = motion.easing)) +
                                scaleIn(animationSpec = tween(motion.fast, easing = motion.easing), initialScale = 0.85f),
                            exit = fadeOut(animationSpec = tween(motion.micro, easing = motion.easing))
                        ) {
                            Text("✓", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                        }
                    }
                    if (detailMode == DetailMode.EDIT) {
                        TextButton(onClick = { nativeContentEditor?.performNativeUndo() }) { Text("↶") }
                        TextButton(onClick = { nativeContentEditor?.performNativeRedo() }) { Text("↷") }
                        TextButton(onClick = ::toggleDetailMode) { Text("预览") }
                    }
                    if (detailMode == DetailMode.PREVIEW && !focusMode) {
                        TextButton(
                            onClick = {
                                searchVisible = !searchVisible
                                if (!searchVisible) {
                                    searchQuery = ""
                                    searchActiveOrdinal = 0
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text("🔍", fontSize = 19.sp, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    Box {
                        TextButton(onClick = { showDetailMenu = true }, modifier = Modifier.size(48.dp)) {
                            Text("⋯", fontSize = 26.sp, color = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(expanded = showDetailMenu, onDismissRequest = { showDetailMenu = false }) {
                            if (detailMode == DetailMode.PREVIEW) {
                                DropdownMenuItem(
                                    text = { Text("编辑") },
                                    onClick = { showDetailMenu = false; enterEdit(InlineEditTarget.CONTENT) }
                                )
                                DropdownMenuItem(
                                    text = { Text("专注阅读") },
                                    onClick = { focusMode = true; showDetailMenu = false }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (markdownActive) "切换为纯文本" else "切换为 Markdown") },
                                onClick = {
                                    formatMode = formatMode.next()
                                    markEdited()
                                    showDetailMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("移动收藏夹") },
                                onClick = { showFolderPicker = true; showDetailMenu = false }
                            )
                            if (note != null) {
                                DropdownMenuItem(
                                    text = { Text("复制全文") },
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val fullText = buildString {
                                            if (title.isNotBlank()) append(title.trimEnd()).append("\n\n")
                                            append(content)
                                        }
                                        clipboard.setPrimaryClip(ClipData.newPlainText("安笺笔记", fullText))
                                        Toast.makeText(context, "已复制全文", Toast.LENGTH_SHORT).show()
                                        showDetailMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("复制到另一个收藏夹") },
                                    onClick = { showCopyFolderPicker = true; showDetailMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("导出笔记") },
                                    onClick = {
                                        if (detailMode == DetailMode.EDIT) syncNativeContentBeforePreview()
                                        val exportText = nativeContentEditor?.text
                                        val exportContent = if (exportText != null) exportText.toString() else content
                                        onExport(
                                            title,
                                            exportContent,
                                            formatMode.resolvesToMarkdown(exportContent)
                                        )
                                        showDetailMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (pinned) "取消星标" else "加入星标") },
                                onClick = { pinned = !pinned; markEdited(); showDetailMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(if (topPinned) "取消置顶" else "置顶") },
                                onClick = { topPinned = !topPinned; markEdited(); showDetailMenu = false }
                            )
                            if (note != null) {
                                DropdownMenuItem(
                                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showDetailMenu = false; onDelete(note) }
                                )
                            }
                        }
                    }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
            if (detailMode == DetailMode.PREVIEW && searchVisible && !focusMode) {
                NoteSearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        searchActiveOrdinal = 0
                    },
                    matchIndex = searchActiveOrdinal,
                    matchTotal = searchTotal,
                    onPrevious = { moveSearchMatch(-1) },
                    onNext = { moveSearchMatch(1) },
                    onClose = {
                        searchVisible = false
                        searchQuery = ""
                        searchActiveOrdinal = 0
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = screenHorizontalPadding(), vertical = 4.dp)
                )
            }
            // contentAlignment = TopCenter：Medium/Expanded 下正文列封顶后水平居中，
            // 滚动条用显式 align(CenterEnd) 不受居中影响；手机竖屏与手表不封顶，表现不变。
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = screenHorizontalPadding()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = if (detailMode == DetailMode.EDIT) {
                        Modifier.fillMaxSize().contentWidthCap(contentMaxWidth())
                    } else {
                        Modifier.fillMaxSize().contentWidthCap(contentMaxWidth()).verticalScroll(detailScrollState)
                    },
                    // 编辑模式保留 18dp 让标题与正文输入框之间有舒适工作距离；
                    // 预览模式收紧到 10dp，让文章标题与首段连接更紧，与 MarkdownPreview
                    // 内部 BlockSpacing=12dp 形成统一的阅读节奏。
                    verticalArrangement = Arrangement.spacedBy(
                        if (detailMode == DetailMode.EDIT) 18.dp else 10.dp
                    )
                ) {
            if (detailMode == DetailMode.EDIT) {
                NativeNoteTitleEditor(
                    value = title,
                    externalModelKey = note?.let { it.id to it.updatedAt } ?: seed,
                    focusRequest = nativeTitleFocusRequest,
                    modifier = Modifier.fillMaxWidth(),
                    textColor = MaterialTheme.colorScheme.onBackground,
                    onValueChange = {
                        title = it
                        markEdited(titleOverride = it)
                    }
                )
            } else {
                val compact = rememberIsWatchWindow()
                Text(
                    title.ifBlank { "未命名笔记" },
                    // 小屏收小标题字号：手表一屏容纳更多正文。
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (compact) 22.sp else 28.sp,
                        lineHeight = if (compact) 30.sp else 37.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        onClick = {},
                        onDoubleClick = { enterEdit(InlineEditTarget.TITLE) }
                    )
                )
            }
            if (detailMode == DetailMode.EDIT) {
                if (markdownActive) MarkdownSyntaxHint()
                NativeNoteEditor(
                    value = contentValue,
                    externalModelKey = note?.let { it.id to it.updatedAt } ?: seed,
                    focusRequest = nativeContentFocusRequest,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textColor = MaterialTheme.colorScheme.onBackground,
                    onViewReady = { nativeContentEditor = it },
                    onScrollChanged = { nativeEditorScrollVersion++ },
                    onSelectionChange = { start, end ->
                        if (savedNoteId > 0L) positionStore.saveSelection(savedNoteId, start, end)
                    },
                    onValueChange = { value ->
                        contentValue = value
                        markEdited(contentOverride = value.text)
                    }
                )
            } else if (markdownActive) {
                // 阅读内容直接融入页面背景，避免出现像浮在页面上的纯白方块。
                MarkdownPreview(
                    content,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                    onLinkClick = ::openPreviewLink,
                    onDoubleClickAt = { position -> enterEdit(InlineEditTarget.CONTENT, position) },
                    initialSourceOffset = initialContentCursor,
                    searchQuery = searchQuery,
                    searchActiveOrdinal = searchActiveOrdinal,
                    onSearchMatchCountChange = { matchCount -> searchTotal = matchCount }
                )
            } else {
                var plainTextLayout by remember(content) { mutableStateOf<TextLayoutResult?>(null) }
                val linkColor = MaterialTheme.colorScheme.primary
                val previewText = remember(content, linkColor) {
                    linkifyPlainText(content.ifBlank { "空白笔记" }, linkColor)
                }
                // 纯文本笔记的正文搜索：命中计数、高亮与滚动复用页面滚动状态。
                val plainMatches = remember(content, searchQuery, searchVisible) {
                    if (!searchVisible || searchQuery.isBlank()) {
                        emptyList()
                    } else {
                        findSearchMatchRanges(content, searchQuery)
                    }
                }
                val searchColors = searchHighlightColors()
                val highlightedPreviewText = remember(previewText, plainMatches, searchActiveOrdinal, searchColors) {
                    if (plainMatches.isEmpty()) {
                        previewText
                    } else {
                        buildAnnotatedString {
                            append(previewText)
                            plainMatches.forEachIndexed { matchIndex, range ->
                                addStyle(
                                    SpanStyle(
                                        background = if (matchIndex == searchActiveOrdinal) {
                                            searchColors.second
                                        } else {
                                            searchColors.first
                                        }
                                    ),
                                    range.first,
                                    range.last + 1
                                )
                            }
                        }
                    }
                }
                LaunchedEffect(plainMatches.size) {
                    searchTotal = plainMatches.size
                }
                LaunchedEffect(searchActiveOrdinal, plainMatches, plainTextLayout) {
                    val range = plainMatches.getOrNull(searchActiveOrdinal) ?: return@LaunchedEffect
                    val layout = plainTextLayout ?: return@LaunchedEffect
                    val layoutTextLength = layout.layoutInput.text.length
                    if (layoutTextLength == 0) return@LaunchedEffect
                    val offset = range.first.coerceIn(0, layoutTextLength - 1)
                    val line = layout.getLineForOffset(offset)
                    val target = layout.getLineTop(line).roundToInt()
                    detailScrollState.scrollTo((target - 96).coerceAtLeast(0))
                }
                LaunchedEffect(initialContentCursor, plainTextLayout, content) {
                    val cursor = initialContentCursor ?: return@LaunchedEffect
                    val layout = plainTextLayout ?: return@LaunchedEffect
                    val target = layout.getBoundingBox(cursor.coerceIn(0, content.length)).top.roundToInt()
                    detailScrollState.scrollTo((target - 96).coerceAtLeast(0))
                }
                Text(
                    text = highlightedPreviewText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = LocalMarkdownFontFamily.current,
                        fontSize = 17.sp,
                        lineHeight = 30.sp
                    ),
                    onTextLayout = { plainTextLayout = it },
                    modifier = Modifier.fillMaxWidth().pointerInput(content) {
                        detectTapGestures(
                            onTap = { offset ->
                                val link = plainTextLayout?.let { layout ->
                                    extractLinkAt(content, layout.getOffsetForPosition(offset))
                                }
                                if (link != null) openPreviewLink(link)
                            },
                            onDoubleTap = { offset ->
                                plainTextLayout?.let { layout ->
                                    enterEdit(InlineEditTarget.CONTENT, layout.getOffsetForPosition(offset))
                                }
                            }
                        )
                    }
                )
            }
            if (detailMode != DetailMode.EDIT) {
                Spacer(Modifier.height(32.dp))
            }
                }
                if (detailMode == DetailMode.EDIT) {
                    nativeContentEditor?.let { editor ->
                        DraggableNativeEditorScrollbar(
                            editor = editor,
                            scrollVersion = nativeEditorScrollVersion,
                            thumbColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                        )
                    }
                } else if (!focusMode) {
                    DraggableDetailScrollbar(
                        scrollState = detailScrollState,
                        thumbColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                    )
                }
            }
            }
        }
    }
    if (showFolderPicker) {
        FolderPickerDialog(
            folders = folders,
            selectedFolderId = selectedFolderId,
            title = "移动收藏夹",
            onDismiss = { showFolderPicker = false },
            onSelected = { folderId ->
                selectedFolderId = folderId
                markEdited()
                showFolderPicker = false
            }
        )
    }
    if (showCopyFolderPicker && note != null) {
        FolderPickerDialog(
            folders = folders.filterNot { it.id == selectedFolderId },
            selectedFolderId = Long.MIN_VALUE,
            title = "复制到另一个收藏夹",
            onDismiss = { showCopyFolderPicker = false },
            onSelected = { folderId ->
                onCopyToFolder(
                    note.copy(
                        title = title,
                        content = content,
                        color = color,
                        isPinned = pinned,
                        isTopPinned = topPinned,
                        isMarkdown = markdownActive,
                        folderId = folderId,
                        updatedAt = System.currentTimeMillis()
                    ),
                    folderId
                )
                showCopyFolderPicker = false
            }
        )
    }
}


internal class NativeNoteTitleEditText(context: Context) : EditText(context) {
    var applyingModelText: Boolean = false
    var appliedExternalModelKey: Any? = null

    fun applyExternalModelText(modelText: String, modelKey: Any?) {
        applyingModelText = true
        try {
            setText(modelText)
            setSelection(modelText.length)
            appliedExternalModelKey = modelKey
        } finally {
            applyingModelText = false
        }
    }
}

@Composable
internal fun NativeNoteTitleEditor(
    value: String,
    externalModelKey: Any?,
    focusRequest: Int,
    modifier: Modifier = Modifier,
    textColor: Color,
    onValueChange: (String) -> Unit
) {
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            NativeNoteTitleEditText(viewContext).apply {
                background = null
                setPadding(0, 0, 0, 0)
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                typeface = ResourcesCompat.getFont(viewContext, R.font.noto_sans_sc_semibold)
                isSingleLine = true
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                applyExternalModelText(value, externalModelKey)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (!applyingModelText) latestOnValueChange(s?.toString().orEmpty())
                    }
                    override fun afterTextChanged(s: Editable?) = Unit
                })
            }
        },
        update = { view ->
            if (shouldApplyExternalModelText(view.appliedExternalModelKey, externalModelKey)) {
                view.applyExternalModelText(value, externalModelKey)
            }
            view.setTextColor(textColor.toArgb())
            val lastRequest = view.getTag() as? Int ?: -1
            if (focusRequest >= 0 && focusRequest != lastRequest) {
                view.setTag(focusRequest)
                view.requestFocus()
                view.post {
                    (view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                        ?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
    )
}

/**
 * 编辑期间 Compose 文本可能暂时落后于原生输入；仅在明确切换笔记或导入种子时允许模型回写。
 */
internal fun shouldApplyExternalModelText(
    appliedExternalModelKey: Any?,
    externalModelKey: Any?
): Boolean = appliedExternalModelKey != externalModelKey

internal class NativeNoteEditText(context: Context) : EditText(context) {
    /** 仅在外部模型主动替换编辑内容时为 true，防止 TextWatcher 把回填误判为用户输入。 */
    var applyingModelText: Boolean = false
    /** 当前 View 已接收的外部模型身份；普通 Compose 重组不会改变它。 */
    var appliedExternalModelKey: Any? = null
    var lastFocusRequest: Int = -1
    var onSelectionChange: ((Int, Int) -> Unit)? = null
    var onScrollPositionChanged: (() -> Unit)? = null
    private var scrollCallbackPosted = false

    // 原生 EditText 的文本拖动不会自动提供可靠的 fling，这里补上连续惯性滚动。
    private val scroller = OverScroller(context)
    private val viewConfig = ViewConfiguration.get(context)
    private val minFlingVelocity = viewConfig.scaledMinimumFlingVelocity
    private val maxFlingVelocity = viewConfig.scaledMaximumFlingVelocity.toFloat()
    private var velocityTracker: VelocityTracker? = null
    private var lastTouchY = 0f
    private var isBeingDragged = false
    private val edgeEffectTop = EdgeEffect(context)
    private val edgeEffectBottom = EdgeEffect(context)

    init {
        isNestedScrollingEnabled = false
    }

    /** 键盘弹出后，光标行底部与键盘上沿之间保留的空隙，避免文字贴着键盘边缘。 */
    private val keyboardCursorGapPx = (8 * resources.displayMetrics.density).toInt()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Manifest 为 adjustResize：键盘弹出会直接压缩编辑器高度，编辑器底边即键盘顶边。
        // 高度变小时延迟到布局稳定后核对一次光标可见性——只有光标真的落到底边之下才滚动，
        // 原本就在可视区内的光标保持不动（不打扰用户已调好的阅读位置）。
        if (h < oldh) post { keepCursorAboveKeyboard() }
    }

    /**
     * 光标可见性核对（键盘弹出后调用一次）：
     * - 光标行底部低于可视区底边（会被键盘遮挡）→ 上滚到刚好露出，并留出空隙；
     * - 光标原本完整可见 → 不做任何滚动。
     *
     * 幂等：滚动后光标即可见，后续布局回调不会重复滚动。
     */
    private fun keepCursorAboveKeyboard() {
        val textLayout = layout ?: return
        val cursor = selectionStart
        if (cursor < 0) return
        val line = textLayout.getLineForOffset(cursor)
        val cursorBottom = textLayout.getLineBottom(line)
        val visibleBottom = scrollY + height - compoundPaddingBottom
        if (cursorBottom <= visibleBottom) return
        scrollBy(0, cursorBottom - visibleBottom + keyboardCursorGapPx)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (!scrollCallbackPosted) {
            scrollCallbackPosted = true
            postOnAnimation {
                scrollCallbackPosted = false
                onScrollPositionChanged?.invoke()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) scroller.abortAnimation()
                velocityTracker = velocityTracker ?: VelocityTracker.obtain()
                velocityTracker?.clear()
                velocityTracker?.addMovement(event)
                lastTouchY = event.y
                isBeingDragged = false
                edgeEffectTop.onRelease()
                edgeEffectBottom.onRelease()
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val dy = event.y - lastTouchY
                if (!isBeingDragged && abs(dy) > viewConfig.scaledTouchSlop) isBeingDragged = true
                lastTouchY = event.y
                if (isBeingDragged) parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000, maxFlingVelocity)
                val velocity = velocityTracker?.yVelocity
                val vy = velocity?.toInt() ?: 0
                if (event.actionMasked == MotionEvent.ACTION_UP && isBeingDragged && abs(vy) > minFlingVelocity) {
                    val scrollRange = (computeVerticalScrollRange() - height).coerceAtLeast(0)
                    scroller.fling(0, scrollY, 0, -vy, 0, 0, 0, scrollRange, 0, computeVerticalScrollExtent() / 3)
                    postInvalidateOnAnimation()
                }
                edgeEffectTop.onRelease()
                edgeEffectBottom.onRelease()
                velocityTracker?.recycle()
                velocityTracker = null
                isBeingDragged = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (scroller.isFinished) return
        if (scroller.computeScrollOffset()) {
            scrollTo(0, scroller.currY)
            val maxScroll = (computeVerticalScrollRange() - height).coerceAtLeast(0)
            if (scroller.currY <= 0) {
                edgeEffectTop.onAbsorb(-scroller.currVelocity.toInt())
                scroller.abortAnimation()
            } else if (scroller.currY >= maxScroll) {
                edgeEffectBottom.onAbsorb(scroller.currVelocity.toInt())
                scroller.abortAnimation()
            }
            postInvalidateOnAnimation()
        }
    }

    fun abortFling() {
        if (!scroller.isFinished) scroller.abortAnimation()
    }

    fun verticalScrollRange(): Int = computeVerticalScrollRange()
    fun verticalScrollExtent(): Int = computeVerticalScrollExtent()

    fun performNativeUndo() {
        onTextContextMenuItem(android.R.id.undo)
    }

    fun performNativeRedo() {
        onTextContextMenuItem(android.R.id.redo)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (!applyingModelText) onSelectionChange?.invoke(selStart, selEnd)
    }

    fun applyExternalModelText(modelText: String, modelKey: Any?) {
        applyingModelText = true
        try {
            setText(modelText)
            appliedExternalModelKey = modelKey
        } finally {
            applyingModelText = false
        }
    }
}

@Composable
internal fun NativeNoteEditor(
    value: TextFieldValue,
    externalModelKey: Any?,
    focusRequest: Int,
    modifier: Modifier = Modifier,
    textColor: Color,
    onViewReady: (NativeNoteEditText) -> Unit = {},
    onScrollChanged: () -> Unit = {},
    onSelectionChange: (Int, Int) -> Unit = { _, _ -> },
    onValueChange: (TextFieldValue) -> Unit
) {
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnScrollChanged by rememberUpdatedState(onScrollChanged)
    val latestOnSelectionChange by rememberUpdatedState(onSelectionChange)
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            NativeNoteEditText(viewContext).apply {
                background = null
                setPadding(0, 0, 0, 0)
                includeFontPadding = false
                gravity = Gravity.TOP or Gravity.START
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                typeface = ResourcesCompat.getFont(viewContext, R.font.noto_sans_sc_regular)
                minLines = 6
                maxLines = Int.MAX_VALUE
                isSingleLine = false
                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                // 初始文本属于明确的外部模型装配；之后用户输入始终以 view.text 为实时来源。
                applyExternalModelText(value.text, externalModelKey)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (applyingModelText) return
                        val changed = s?.toString().orEmpty()
                        if (BuildConfig.DEBUG) {
                            Log.d("NativeNoteEditor", "user TextWatcher nativeTextLength=${changed.length}")
                        }
                        val selectionStart = selectionStart.coerceIn(0, changed.length)
                        val selectionEnd = selectionEnd.coerceIn(0, changed.length)
                        // 用户输入、粘贴与输入法提交均从这里同步到 Compose，再进入既有保存流程。
                        latestOnValueChange(TextFieldValue(changed, TextRange(selectionStart, selectionEnd)))
                    }
                    override fun afterTextChanged(s: Editable?) = Unit
                })
            }
        },
        update = { view ->
            onViewReady(view)
                            view.onSelectionChange = latestOnSelectionChange
                view.onScrollPositionChanged = latestOnScrollChanged
                val viewText = view.text?.toString().orEmpty()

            if (BuildConfig.DEBUG) {
                Log.d(
                    "NativeNoteEditor",
                    "update viewTextLength=${viewText.length} modelTextLength=${value.text.length} " +
                        "externalChanged=${view.appliedExternalModelKey != externalModelKey}"
                )
            }
            // value.text 在编辑期间只来自 TextWatcher 的异步 Compose 回调，可能暂时落后于
            // 原生输入。只有笔记/导入种子确实切换时，才允许模型主动回填 View。
            if (shouldApplyExternalModelText(view.appliedExternalModelKey, externalModelKey)) {
                view.applyExternalModelText(value.text, externalModelKey)
            } else if (BuildConfig.DEBUG && viewText.length > value.text.length) {
                Log.d("NativeNoteEditor", "skip stale model write: native text is newer")
            }
            view.setTextColor(textColor.toArgb())
            if (focusRequest >= 0 && focusRequest != view.lastFocusRequest) {
                view.lastFocusRequest = focusRequest
                view.requestFocus()
                val start = value.selection.start.coerceIn(0, view.length())
                val end = value.selection.end.coerceIn(0, view.length())
                view.setSelection(start, end)
                view.post {
                    (view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                        ?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
    )
}


@Composable
internal fun DraggableNativeEditorScrollbar(
    editor: NativeNoteEditText,
    scrollVersion: Int,
    thumbColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thumbWidthPx = with(density) { 4.dp.toPx() }
    val thumbHeightPx = with(density) { 42.dp.toPx() }
    val scrollRange = (editor.verticalScrollRange() - editor.verticalScrollExtent()).coerceAtLeast(0)
    val canScroll = scrollRange > 0
    var dragging by remember(editor) { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .width(12.dp)
            .pointerInput(editor, scrollRange) {
                if (!canScroll) return@pointerInput
                fun scrollToPointer(positionY: Float) {
                    val trackHeight = (size.height - thumbHeightPx).coerceAtLeast(1f)
                    val fraction = (positionY - thumbHeightPx / 2f) / trackHeight
                    editor.scrollTo(0, (fraction.coerceIn(0f, 1f) * scrollRange).roundToInt())
                }
                detectDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        scrollToPointer(offset.y)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        scrollToPointer(change.position.y)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                )
            }
    ) {
        // 仅引用版本号以保证原生滚动回调触发后重新绘制滑条位置。
        if (scrollVersion < 0 || !canScroll) return@Canvas
        val actualThumbHeight = thumbHeightPx.coerceAtMost(size.height)
        val scrollFraction = (editor.scrollY.toFloat() / scrollRange).coerceIn(0f, 1f)
        val thumbY = (size.height - actualThumbHeight) * scrollFraction
        drawRoundRect(
            color = thumbColor.copy(alpha = if (dragging) 0.72f else 0.52f),
            topLeft = Offset(size.width - thumbWidthPx, thumbY),
            size = Size(thumbWidthPx, actualThumbHeight),
            cornerRadius = CornerRadius(thumbWidthPx, thumbWidthPx)
        )
    }
}

@Composable
internal fun DraggableDetailScrollbar(
    scrollState: ScrollState,
    thumbColor: Color,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val canScroll = scrollState.maxValue > 0
    var dragging by remember { mutableStateOf(false) }
    val thumbWidthPx = with(density) { 4.dp.toPx() }
    val thumbHeightPx = with(density) { 42.dp.toPx() }

    fun scrollToFraction(fraction: Float) {
        val targetValue = (fraction.coerceIn(0f, 1f) * scrollState.maxValue).roundToInt()
        scope.launch { scrollState.scrollTo(targetValue) }
    }

    Canvas(
        modifier = modifier
            .width(12.dp)
            .pointerInput(canScroll, scrollState.maxValue) {
                if (!canScroll) return@pointerInput
                fun scrollToPointer(positionY: Float) {
                    scrollToFraction(positionY / size.height.coerceAtLeast(1))
                }
                detectDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        scrollToPointer(offset.y)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        scrollToPointer(change.position.y)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                )
            }
    ) {
        if (!canScroll || (!scrollState.isScrollInProgress && !dragging)) return@Canvas

        val viewportHeight = size.height
        val actualThumbHeight = thumbHeightPx.coerceAtMost(viewportHeight)
        val scrollFraction = (scrollState.value.toFloat() / scrollState.maxValue).coerceIn(0f, 1f)
        val thumbY = (viewportHeight - actualThumbHeight) * scrollFraction

        drawRoundRect(
            color = thumbColor.copy(alpha = 0.52f),
            topLeft = Offset(size.width - thumbWidthPx, thumbY),
            size = Size(thumbWidthPx, actualThumbHeight),
            cornerRadius = CornerRadius(thumbWidthPx, thumbWidthPx)
        )
    }
}
