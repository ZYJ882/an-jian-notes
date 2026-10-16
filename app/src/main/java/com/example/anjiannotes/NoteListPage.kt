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
import android.os.Bundle
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anjiannotes.data.ALL_FOLDERS_ID
import com.example.anjiannotes.data.DEFAULT_FOLDER_ID
import com.example.anjiannotes.data.FolderEntity
import com.example.anjiannotes.data.NoteEntity
import com.example.anjiannotes.data.findSearchMatch
import com.example.anjiannotes.data.parseSearchTerms
import com.example.anjiannotes.data.NotePositionStore
import com.example.anjiannotes.data.NotesRepository
import com.example.anjiannotes.data.STARRED_FOLDER_ID
import com.example.anjiannotes.data.WebDavConfig
import com.example.anjiannotes.data.WebDavRemoteSnapshot
import com.example.anjiannotes.ui.DialogAction
import com.example.anjiannotes.ui.DialogActionButtons
import com.example.anjiannotes.ui.DialogContentColumn
import com.example.anjiannotes.ui.screenHorizontalPadding
import com.example.anjiannotes.ui.rememberIsWatchWindow
import com.example.anjiannotes.ui.contentMaxWidth
import com.example.anjiannotes.ui.contentWidthCap
import com.example.anjiannotes.ui.drawerMaxWidth
import com.example.anjiannotes.ui.listPaneWidth
import com.example.anjiannotes.ui.rememberTwoPane
import com.example.anjiannotes.ui.EditorSeed
import com.example.anjiannotes.ui.ImportReadResult
import com.example.anjiannotes.ui.MarkdownPreview
import com.example.anjiannotes.ui.LocalMarkdownFontFamily
import com.example.anjiannotes.ui.MarkdownSyntaxHint
import com.example.anjiannotes.ui.NoteFormatMode
import com.example.anjiannotes.ui.RememberNoteReadingPosition
import com.example.anjiannotes.ui.extractFirstLink
import com.example.anjiannotes.ui.extractLinkAt
import com.example.anjiannotes.ui.findSearchMatchRanges
import com.example.anjiannotes.ui.formatForFileName
import com.example.anjiannotes.ui.linkifyPlainText
import com.example.anjiannotes.ui.markdownToListPreview
import com.example.anjiannotes.ui.markdownToPlainText
import com.example.anjiannotes.ui.plainTextToListPreview
import com.example.anjiannotes.ui.readTextImport
import com.example.anjiannotes.ui.searchHighlightColors
import com.example.anjiannotes.ui.theme.AnJianTheme
import com.example.anjiannotes.ui.theme.AppearanceMode
import com.example.anjiannotes.ui.theme.FontPreferences
import com.example.anjiannotes.ui.theme.MotionTokens
import com.example.anjiannotes.ui.theme.NoteFont
import com.example.anjiannotes.ui.theme.NotoSansSC
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesListPage(
    notes: List<NoteEntity>,
    folders: List<FolderEntity>,
    activeFolderId: Long,
    query: String,
    showSearch: Boolean,
    globalSearch: Boolean,
    onSearchToggle: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFolderSelected: (Long) -> Unit,
    onCreateFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    createMenuExpanded: Boolean,
    onCreateMenuExpanded: (Boolean) -> Unit,
    onNewNote: () -> Unit,
    onImportFile: () -> Unit,
    onImportClipboard: () -> Unit,
    onOpenNote: (NoteEntity, Int?) -> Unit,
    onToggleStar: (NoteEntity) -> Unit,
    onToggleTopPin: (NoteEntity) -> Unit,
    onFolderLongPress: (FolderEntity) -> Unit,
    onBatchDelete: (Set<Long>) -> Unit,
    onBatchStar: (Set<Long>) -> Unit,
    onBatchTopPin: (Set<Long>, Boolean) -> Unit,
    // Expanded 双栏：列表占左 pane（宽度由 listPaneWidth 按窗口尺寸类给出），
    // detailPane 为右侧详情槽；单选栏下三者保持默认值，布局与旧版完全一致。
    twoPane: Boolean = false,
    paneSelectedNoteId: Long? = null,
    detailPane: (@Composable () -> Unit)? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // 搜索关键词只按查询串解析一次：列表内所有卡片共享同一份 terms，
    // 避免每敲一个字符就对每张可见卡片重复执行 split。
    val searchTerms = remember(query) { parseSearchTerms(query) }
    val activeFolderName = if (globalSearch) "全局搜索" else folders.firstOrNull { it.id == activeFolderId }?.name ?: "全部笔记"
    val noteListState = rememberLazyListState()
    var selectedNoteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingBatchDeleteIds by remember { mutableStateOf<Set<Long>?>(null) }
    val selectionMode = selectedNoteIds.isNotEmpty()
    val allSelectedNotesAreTopPinned = selectedNoteIds.isNotEmpty() &&
        notes.filter { it.id in selectedNoteIds }.all { it.isTopPinned }

    fun toggleSelection(noteId: Long) {
        selectedNoteIds = if (noteId in selectedNoteIds) selectedNoteIds - noteId else selectedNoteIds + noteId
    }

    BackHandler(enabled = selectionMode) { selectedNoteIds = emptySet() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.76f).contentWidthCap(drawerMaxWidth())) {
                Column(modifier = Modifier.fillMaxSize().padding(vertical = 20.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("安", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("我的收藏夹", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("安笺 · 离线笔记", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable {
                            onCreateFolder()
                            scope.launch { drawerState.close() }
                        },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "＋  新建收藏夹",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(folders.size, key = { folders[it].id }) { index ->
                            val folder = folders[index]
                            FolderDrawerItem(
                                label = folder.name,
                                selected = folder.id == activeFolderId,
                                onClick = {
                                    onFolderSelected(folder.id)
                                    scope.launch { drawerState.close() }
                                },
                                onLongClick = if (folder.id != STARRED_FOLDER_ID) {
                                    { onFolderLongPress(folder) }
                                } else null
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))
                    NavigationDrawerItem(
                        label = { Text("⚙  设置", style = MaterialTheme.typography.labelLarge) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onOpenSettings()
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        if (selectionMode) {
                            Text("已选择 ${selectedNoteIds.size} 条", fontWeight = FontWeight.SemiBold)
                        } else {
                            Column {
                                Text(activeFolderName, fontWeight = FontWeight.Bold)
                                Text("轻写，轻放", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = {
                        TextButton(onClick = {
                            if (selectionMode) {
                                selectedNoteIds = emptySet()
                            } else {
                                scope.launch {
                                    if (drawerState.currentValue == DrawerValue.Closed) drawerState.open() else drawerState.close()
                                }
                            }
                        }) {
                            Text(if (selectionMode) "取消" else "☰", fontSize = if (selectionMode) 15.sp else 24.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        if (selectionMode) {
                            // 批量操作按钮随宽度滚动：小屏上一行放不下「全选/星标/置顶/删除」时
                            // 横向滑动而非直接裁掉（与详情页顶栏一致的保护）。
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { selectedNoteIds = notes.mapTo(linkedSetOf()) { it.id } },
                                    enabled = notes.isNotEmpty() && selectedNoteIds.size < notes.size
                                ) { Text("全选") }
                                TextButton(onClick = {
                                    val ids = selectedNoteIds
                                    selectedNoteIds = emptySet()
                                    onBatchStar(ids)
                                }) { Text("星标") }
                                TextButton(onClick = {
                                    val ids = selectedNoteIds
                                    selectedNoteIds = emptySet()
                                    onBatchTopPin(ids, !allSelectedNotesAreTopPinned)
                                }) { Text(if (allSelectedNotesAreTopPinned) "取消置顶" else "置顶") }
                                TextButton(onClick = { pendingBatchDeleteIds = selectedNoteIds }) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            TextButton(onClick = onSearchToggle) {
                                Text(if (showSearch) "×" else "⌕", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = {
                QuickCaptureBar(
                    expanded = createMenuExpanded,
                    onExpandedChange = onCreateMenuExpanded,
                    onStartWriting = onNewNote,
                    onImportFile = onImportFile,
                    onImportClipboard = onImportClipboard
                )
            }
        ) { padding ->
            // 列表主体（搜索框 + 列表/空态）：单栏与双栏共用同一份内容，
            // 双栏时装入左侧固定宽度 pane，单栏时外套行长封顶。
            val listBody: @Composable ColumnScope.() -> Unit = {
                if (showSearch && !selectionMode) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onSearchChange,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        label = { Text("搜索标题或正文", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true
                    )
                }
                if (notes.isEmpty()) {
                    EmptyNotes(query = query, onCreate = onNewNote)
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = noteListState,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text(
                                    text = if (globalSearch && query.isNotBlank()) "全局匹配 ${notes.size} 条" else "共 ${notes.size} 条笔记",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            items(
                                items = notes,
                                key = { it.id },
                                contentType = { "note" }
                            ) { note ->
                                NoteCard(
                                    // 位置变化（星标/置顶/删除/排序）时平滑滑动就位；
                                    // spring 阻尼比 1.0 无回弹，刚度 Medium 保证快速收敛。
                                    modifier = Modifier.animateItem(
                                        placementSpec = spring(stiffness = Spring.StiffnessMedium)
                                    ),
                                    note = note,
                                    // 批量选择与双栏 pane 选中都算「选中」：前者显示复选框，
                                    // 后者高亮当前阅读项，两种状态视觉可区分且互不干扰。
                                    selected = note.id in selectedNoteIds || note.id == paneSelectedNoteId,
                                    selectionMode = selectionMode,
                                    searchTerms = searchTerms,
                                    onOpen = {
                                        if (selectionMode) toggleSelection(note.id) else onOpenNote(note, note.findSearchMatch(searchTerms)?.contentOffset)
                                    },
                                    onLongPress = { toggleSelection(note.id) },
                                    onToggleStar = { onToggleStar(note) },
                                    onToggleTopPin = { onToggleTopPin(note) }
                                )
                            }
                            item { Spacer(Modifier.height(20.dp)) }
                        }
                        DraggableNoteListScrollbar(
                            listState = noteListState,
                            thumbColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                        )
                    }
                }
            }
            if (twoPane && detailPane != null) {
                // Expanded 双栏：左列表、右详情，中间 1dp 分隔线（outlineVariant 降透明
                // 度避免割裂感）。列表宽度由窗口尺寸类档位决定（listPaneWidth，400dp），
                // 详情 pane 取剩余空间；详情页内部仍按 contentMaxWidth 做行长封顶，
                // 长文本不会横贯整块平板。Medium 档（600–839dp）不分栏：列表 400dp
                // 加详情会互相挤压，维持单栏封顶居中更稳妥。
                Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier
                            .width(listPaneWidth())
                            .fillMaxHeight()
                            .padding(horizontal = screenHorizontalPadding()),
                        content = listBody
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        detailPane()
                    }
                }
            } else {
                // 单栏：宽屏（Medium/Expanded）内容封顶并居中，长文本行不横贯平板，
                // 阅读区保持 60–75 字符行长；手机竖屏与手表 widthIn 为恒等变换，布局零变化。
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .contentWidthCap(contentMaxWidth())
                            .padding(horizontal = screenHorizontalPadding()),
                        content = listBody
                    )
                }
            }
        }
    }

    pendingBatchDeleteIds?.let { ids ->
        BatchDeleteDialog(
            count = ids.size,
            onDismiss = { pendingBatchDeleteIds = null },
            onConfirm = {
                pendingBatchDeleteIds = null
                selectedNoteIds = emptySet()
                onBatchDelete(ids)
            }
        )
    }
}

@Composable
internal fun EmptyNotes(query: String, onCreate: () -> Unit) {
    // 空状态淡入：只走 alpha，不引入布局动画。
    // key 取 query.isBlank() 而非 query：搜索时每敲一字 query 都会变，
    // 用 query 作 key 会导致淡入反复重播（闪烁）；这里只在
    // 「无笔记」与「无匹配」两种空状态互相切换时才重播一次。
    var visible by remember(query.isBlank()) { mutableStateOf(false) }
    LaunchedEffect(query.isBlank()) { visible = true }
    val compact = rememberIsWatchWindow()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.Easing)),
        exit = fadeOut(animationSpec = tween(MotionTokens.DurationMicro, easing = MotionTokens.Easing))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (compact) 20.dp else 28.dp,
                        vertical = if (compact) 16.dp else 24.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
                ) {
                    Text(
                        if (query.isBlank()) "从一条笔记开始" else "没有匹配的笔记",
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (query.isBlank()) "点击底部输入栏，随手记录此刻想法。" else "尝试换个关键词，或清除搜索条件。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (query.isBlank()) TextButton(onClick = onCreate) { Text("写下第一条") }
                }
            }
        }
    }
}

/**
 * 双栏模式右侧详情 pane 的空状态：尚未选中任何笔记时的占位。
 *
 * 与 EmptyNotes 的区别：列表空态引导「写第一条」，此处列表已有内容，
 * 引导「从左侧选一条开始读」，同时保留新建入口（空列表时也能直接写）。

 */
@Composable
internal fun DetailPaneEmptyState(onCreate: () -> Unit) {
    val compact = rememberIsWatchWindow()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (compact) 20.dp else 28.dp,
                    vertical = if (compact) 16.dp else 24.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
            ) {
                Text(
                    "选择左侧笔记开始阅读",
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "也可以直接新建一条，写入的内容会自动保存。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onCreate) { Text("写新的") }
            }
        }
    }
}


@Composable
internal fun DraggableNoteListScrollbar(
    listState: LazyListState,
    thumbColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val canScroll by remember(listState) {
        derivedStateOf { listState.canScrollBackward || listState.canScrollForward }
    }
    val scrolling by remember(listState) {
        derivedStateOf { listState.isScrollInProgress }
    }
    var dragging by remember { mutableStateOf(false) }
    var dragPixelsToScroll by remember { mutableStateOf(0f) }
    val thumbWidthPx = with(density) { 4.dp.toPx() }
    val thumbHeightPx = with(density) { 42.dp.toPx() }
    val itemSpacingPx = with(density) { 5.dp.toPx() }
    val thumbAlpha = if (canScroll && (scrolling || dragging)) 0.58f else 0f

    Canvas(
        modifier = modifier
            .width(12.dp)
            .graphicsLayer { alpha = thumbAlpha }
            .pointerInput(listState) {
                detectDragGestures(
                    onDragStart = onDragStart@{
                        val layoutInfo = listState.layoutInfo
                        val visibleItems = layoutInfo.visibleItemsInfo
                        val totalItems = layoutInfo.totalItemsCount
                        if (visibleItems.isEmpty() || visibleItems.size >= totalItems) {
                            dragging = false
                            dragPixelsToScroll = 0f
                            return@onDragStart
                        }
                        dragging = true
                        val viewportHeight = size.height.toFloat()
                        if (viewportHeight > 0f) {
                            val itemExtent = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size + itemSpacingPx
                            val estimatedContentHeight = itemExtent * totalItems
                            val trackHeight = (viewportHeight - thumbHeightPx).coerceAtLeast(1f)
                            val scrollRange = (estimatedContentHeight - viewportHeight).coerceAtLeast(1f)
                            dragPixelsToScroll = scrollRange / trackHeight
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (dragPixelsToScroll > 0f) {
                            change.consume()
                            listState.dispatchRawDelta(dragAmount.y * dragPixelsToScroll)
                        }
                    },
                    onDragEnd = {
                        dragging = false
                        dragPixelsToScroll = 0f
                    },
                    onDragCancel = {
                        dragging = false
                        dragPixelsToScroll = 0f
                    }
                )
            }
    ) {
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val totalItems = layoutInfo.totalItemsCount
        if (visibleItems.isEmpty() || visibleItems.size >= totalItems) return@Canvas

        val viewportHeight = size.height
        val itemExtent = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size + itemSpacingPx
        val estimatedContentHeight = itemExtent * totalItems
        val scrollRange = (estimatedContentHeight - viewportHeight).coerceAtLeast(1f)
        val currentOffset = listState.firstVisibleItemIndex * itemExtent + listState.firstVisibleItemScrollOffset
        val estimatedFraction = (currentOffset / scrollRange).coerceIn(0f, 1f)
        val scrollFraction = when {
            !listState.canScrollBackward -> 0f
            !listState.canScrollForward -> 1f
            else -> estimatedFraction
        }
        val actualThumbHeight = thumbHeightPx.coerceAtMost(viewportHeight)
        val thumbY = (viewportHeight - actualThumbHeight) * scrollFraction

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(size.width - thumbWidthPx, thumbY),
            size = Size(thumbWidthPx, actualThumbHeight),
            cornerRadius = CornerRadius(thumbWidthPx, thumbWidthPx)
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NoteCard(
    note: NoteEntity,
    selected: Boolean,
    selectionMode: Boolean,
    searchTerms: List<String>,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleTopPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compact = rememberIsWatchWindow()
    val searchMatch = remember(note.id, note.updatedAt, searchTerms) { note.findSearchMatch(searchTerms) }
    val summary = remember(note.id, note.updatedAt, note.isMarkdown, searchMatch) {
        searchMatch?.snippet?.ifBlank { NoteListPreviewCache.get(note) } ?: NoteListPreviewCache.get(note)
    }
    val formattedDate = remember(note.updatedAt) { formatDate(note.updatedAt) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f) else MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.58f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            // 小屏收紧卡片内边距与最小高度：手表一屏要放下更多条目。
            modifier = Modifier
                .padding(
                    horizontal = if (compact) 12.dp else 16.dp,
                    vertical = if (compact) 10.dp else 14.dp
                )
                .heightIn(min = if (compact) 84.dp else 104.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp)
        ) {
            Text(
                note.title.ifBlank { "未命名笔记" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (summary.isNotBlank()) {
                Text(
                    if (searchMatch?.contentOffset != null) "命中：$summary" else summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.84f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f)
                )
                if (note.isMarkdown) {
                    Text("MD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f), modifier = Modifier.padding(end = 12.dp))
                }
                if (selectionMode) {
                    Text(
                        if (selected) "✓" else "○",
                        fontSize = 22.sp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(6.dp)
                    )
                } else {
                    Text(
                        text = if (note.isTopPinned) "↑ 置顶" else "↑",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (note.isTopPinned) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (note.isTopPinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (note.isTopPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else Color.Transparent
                            )
                            .clickable(onClick = onToggleTopPin)
                            .padding(
                                horizontal = if (note.isTopPinned) 9.dp else 7.dp,
                                vertical = 5.dp
                            )
                    )
                    Text(
                        if (note.isPinned) "★" else "☆",
                        fontSize = 22.sp,
                        color = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onToggleStar)
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}


@Composable
internal fun BatchDeleteDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) = ConfirmationDialog(
    title = "删除 $count 条笔记？",
    message = "批量删除后无法恢复。",
    confirmLabel = "删除",
    destructive = true,
    onDismiss = onDismiss,
    onConfirm = onConfirm
)

internal val NoteColors = listOf(0xFFF5F0E8, 0xFFF5ECEB, 0xFFEDF3F5, 0xFFEEF5F0, 0xFFF5F1E3)

internal fun formatDate(time: Long): String = requireNotNull(NoteDateFormatter.get()).format(Date(time))
