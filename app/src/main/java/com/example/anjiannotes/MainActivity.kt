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

private data class NoteExportPayload(
    val fileName: String,
    val content: String
)

@Composable
private fun rememberNoteFontFamily(font: NoteFont, customPath: String?): FontFamily {
    return remember(font, customPath) {
        when (font) {
            NoteFont.DEFAULT -> NotoSansSC
            NoteFont.SONG -> FontFamily.Serif
            NoteFont.CUSTOM -> customPath?.let { path ->
                runCatching { FontFamily(Font(File(path))) }.getOrNull()
            } ?: NotoSansSC
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AnJianApplication
        setContent {
            val appearanceMode by app.appearancePreferences.mode.collectAsStateWithLifecycle()
            val noteFont by app.fontPreferences.font.collectAsStateWithLifecycle()
            val customFontPath by app.fontPreferences.customPath.collectAsStateWithLifecycle()
            var factory by remember { mutableStateOf<NotesViewModelFactory?>(null) }

            // 让 Activity 尽快交出主线程以绘制首帧；Room 与依赖组装在后台完成后再进入首页。
            LaunchedEffect(app) {
                factory = withContext(Dispatchers.Default) {
                    val database = app.database
                    NotesViewModelFactory(
                        NotesRepository(database, database.noteDao(), database.folderDao()),
                        app.webDavConfigStore,
                        app.webDavBackupClient,
                        app.folderSelectionPreferences,
                        app.appSortingPreferences
                    )
                }
            }

            AnJianTheme(
                appearanceMode = appearanceMode,
                systemDarkTheme = isSystemInDarkTheme()
            ) {
                val viewModelFactory = factory
                if (viewModelFactory == null) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
                } else {
                    val notesViewModel: NotesViewModel = viewModel(factory = viewModelFactory)
                    CompositionLocalProvider(LocalMarkdownFontFamily provides rememberNoteFontFamily(noteFont, customFontPath)) {
                        NotesApp(
                            viewModel = notesViewModel,
                            appearanceMode = appearanceMode,
                            noteFont = noteFont,
                            customFontPath = customFontPath,
                            externalFileUri = intent.data,
                            onAppearanceChange = app.appearancePreferences::setMode,
                            onFontChange = app.fontPreferences::setFont,
                            onCustomFontPath = app.fontPreferences::setCustomPath
                        )
                    }
                }
            }
        }
    }
}

internal enum class InlineEditTarget { TITLE, CONTENT }
internal enum class DetailMode { PREVIEW, EDIT }
internal enum class AutoSaveState { IDLE, SAVING, SAVED }

/** 导出文件名清洗：剔除各平台文件系统保留字符。预编译，避免每次导出重复编译。 */
internal val ExportUnsafeFileNamePattern = Regex("[\\\\/:*?\\\"<>|]")

/** 新建草稿是否为空：标题与正文均无有效字符（含纯空白）即视为空，不应留下空笔记。 */
internal fun isNewDraftEmpty(draftTitle: String, draftContent: String): Boolean =
    draftTitle.isBlank() && draftContent.isBlank()

/**
 * 新建草稿离开详情页的策略：仅有有效内容的新建页才需要强制写入一次；
 * 空草稿一律不落库（未写入过直接返回，已写入过的空记录则删除）。
 */
internal fun shouldForceFinalDraftSave(
    isNewNote: Boolean,
    savedNoteId: Long,
    draftTitle: String,
    draftContent: String
): Boolean = isNewNote && savedNoteId == 0L && !isNewDraftEmpty(draftTitle, draftContent)

/** 每次编辑事件同步生成的不可变保存快照，避免组合重绘前读取到旧文本。 */
internal data class NoteDraftSnapshot(
    val title: String,
    val content: String,
    val color: Long,
    val isPinned: Boolean,
    val isTopPinned: Boolean,
    val isMarkdown: Boolean,
    val folderId: Long
)

internal data class NoteListPreviewKey(
    val id: Long,
    val updatedAt: Long,
    val isMarkdown: Boolean
)

/**
 * 列表项离开并重新进入可视区时，Compose 可能需要重新组合该项。
 * 缓存已生成的轻量摘要，避免再次执行多轮 Markdown 正则替换。
 */
internal object NoteListPreviewCache {
    private const val MAX_ENTRIES = 128
    private val values = object : LinkedHashMap<NoteListPreviewKey, String>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<NoteListPreviewKey, String>?): Boolean =
            size > MAX_ENTRIES
    }

    fun get(note: NoteEntity): String {
        val key = NoteListPreviewKey(note.id, note.updatedAt, note.isMarkdown)
        return values[key] ?: createPreview(note).also { values[key] = it }
    }

    private fun createPreview(note: NoteEntity): String = if (note.isMarkdown) {
        markdownToListPreview(note.content)
    } else {
        plainTextToListPreview(note.content)
    }
}

internal val NoteDateFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
}

internal sealed interface AppPage {
    data object List : AppPage
    data class Detail(
        val note: NoteEntity?,
        val seed: EditorSeed = EditorSeed(),
        val folderId: Long = DEFAULT_FOLDER_ID,
        val initialContentCursor: Int? = null,
        val initialPinned: Boolean = false
    ) : AppPage
    data object Settings : AppPage
    data object Appearance : AppPage
}

/**
 * 双栏模式下详情 pane 的离开意图。
 *
 * 详情页的离开必须经过自身的最终保存确认（finalizeAndLeave），因此「切换选中 /
 * 提升为整页 / 降级回双栏」都由宿主先记录意图、再请求离开，详情保存完成后回调
 * onBack 时统一应用，避免组合直接销毁导致防抖窗内的最后编辑丢失。
 */
internal sealed interface DetailExitIntent {
    /** 切换到另一篇笔记（双栏内点选）。 */
    data class Switch(val target: AppPage.Detail) : DetailExitIntent

    /** 窗口缩窄（旋转/分屏/合上折叠屏）：把双栏选中态提升为整页导航。 */
    data object Promote : DetailExitIntent

    /** 窗口展宽（旋转回来/展开折叠屏）：把整页导航降级回双栏选中态。 */
    data object Demote : DetailExitIntent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesApp(
    viewModel: NotesViewModel,
    appearanceMode: AppearanceMode,
    noteFont: NoteFont,
    customFontPath: String?,
    externalFileUri: Uri?,
    onAppearanceChange: (AppearanceMode) -> Unit,
    onFontChange: (NoteFont) -> Unit,
    onCustomFontPath: (String) -> Unit
) {
    val context = LocalContext.current
    val notePositionStore = remember(context.applicationContext) {
        (context.applicationContext as AnJianApplication).notePositionStore
    }
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val noteListSortByOpen by viewModel.noteListSortByRecentOpen.collectAsStateWithLifecycle()
    val activeFolderId by viewModel.activeFolderId.collectAsStateWithLifecycle()
    val isGlobalSearch by viewModel.isGlobalSearch.collectAsStateWithLifecycle()
    val webDavConfig by viewModel.webDavConfig.collectAsStateWithLifecycle()
    val webDavTask by viewModel.webDavTask.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf<AppPage>(AppPage.List) }
    // 双栏（平板 ≥840dp）：selectedDetail 为详情 pane 当前选中目标（null = 空态占位）。
    // 切换选中/窗口变化引起的详情离开都先记录意图再请求离开（见 DetailExitIntent），
    // 由详情自身完成最终保存后于 onBack 统一应用，避免组合销毁丢失防抖窗内的编辑。
    val twoPane = rememberTwoPane()
    var selectedDetail by remember { mutableStateOf<AppPage.Detail?>(null) }
    var pendingDetailIntent by remember { mutableStateOf<DetailExitIntent?>(null) }
    var detailLeaveRequest by remember { mutableStateOf(0) }
    // 编辑器保存作用域提升到 NotesApp：详情 pane 离开组合时，未落盘的防抖保存不随之取消
    // （queueSaveNote 本身在 viewModelScope 执行，写入与组合生命周期解耦）。
    val appEditorScope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var folderMenuFor by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToRename by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToSort by remember { mutableStateOf<FolderEntity?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showBackupMenu by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var pendingBackupPayload by remember { mutableStateOf<String?>(null) }
    var pendingRestorePayload by remember { mutableStateOf<String?>(null) }
    var pendingTextBackupPayload by remember { mutableStateOf<String?>(null) }
    var pendingTextRestorePayload by remember { mutableStateOf<String?>(null) }
    var pendingMarkdownZipPayload by remember { mutableStateOf<ByteArray?>(null) }
    var pendingMarkdownZipRestorePayload by remember { mutableStateOf<ByteArray?>(null) }
    var pendingNoteExport by remember { mutableStateOf<NoteExportPayload?>(null) }
    var showWebDavDialog by remember { mutableStateOf(false) }
    var pendingWebDavRestore by remember { mutableStateOf<WebDavRemoteSnapshot?>(null) }
    val fileIoScope = rememberCoroutineScope()

    fun <T> runFileOperation(
        failurePrefix: String,
        operation: () -> T,
        onSuccess: (T) -> Unit
    ) {
        fileIoScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching(operation) }
            result.onSuccess(onSuccess)
                .onFailure { feedbackMessage = "$failurePrefix：${it.message ?: "无法读写文件"}" }
        }
    }

    fun writableFolderId(): Long = when (activeFolderId) {
        STARRED_FOLDER_ID, ALL_FOLDERS_ID -> DEFAULT_FOLDER_ID
        else -> activeFolderId
    }

    /** 打开详情目标：单栏走整页导航；双栏先冲刷当前详情的未保存内容再切换选中。 */
    fun openDetailTarget(target: AppPage.Detail) {
        if (!twoPane) {
            page = target
            return
        }
        val current = selectedDetail
        // 同一篇已入库笔记的重复点选视为无变化：pane 已在展示该笔记，重新赋值会因
        // seed 变化重置详情 remember 状态，把编辑中的内容覆盖为旧快照。
        // 新草稿（note == null）之间或草稿与笔记之间一律走冲刷路径。
        val sameNote = current != null && target.note != null && current.note?.id == target.note?.id
        if (current != null && !sameNote) {
            pendingDetailIntent = DetailExitIntent.Switch(target)
            detailLeaveRequest += 1
        } else if (current == null) {
            selectedDetail = target
            pendingDetailIntent = null
        }
    }

    /** 详情离开后的善后与意图应用：整页导航与双栏 pane 共用。 */
    fun handleDetailBack(wasNewNote: Boolean, returnedFolderId: Long, returnedNoteId: Long) {
        if (wasNewNote && returnedNoteId > 0L) {
            // “已保存”仅会在 Room 写入完成后显示；返回时定位到新笔记所在收藏夹，
            // 并清除旧搜索条件，避免首个草稿因列表筛选而看似消失。
            if (activeFolderId != STARRED_FOLDER_ID && activeFolderId != returnedFolderId) {
                viewModel.selectFolder(returnedFolderId)
            }
            if (query.isNotBlank()) viewModel.setSearchQuery("")
        } else if (activeFolderId == STARRED_FOLDER_ID && returnedFolderId != STARRED_FOLDER_ID) {
            viewModel.selectFolder(returnedFolderId)
        }
        when (val intent = pendingDetailIntent) {
            null -> {
                // 详情自身发起离开（系统返回/返回箭头/专注模式退出后的返回）
                selectedDetail = null
                if (!twoPane) page = AppPage.List
            }
            is DetailExitIntent.Switch -> {
                // 与 Demote 同理：目标快照刷新为列表最新实体，避免展示点选瞬间的旧内容。
                val target = intent.target
                val latest = target.note?.id?.let { id -> notes.firstOrNull { it.id == id } }
                selectedDetail = target.copy(note = latest ?: target.note)
                page = AppPage.List
            }
            DetailExitIntent.Promote -> {
                // 新草稿不提升为整页：草稿已入库并出现在列表，点选即可打开；
                // 若用空 seed 重建详情会用空白内容覆盖已保存草稿的显示。
                val target = selectedDetail
                if (target != null && target.note != null) {
                    page = target
                } else {
                    selectedDetail = null
                    page = AppPage.List
                }
            }
            DetailExitIntent.Demote -> {
                // 整页导航持有的 NoteEntity 是打开时的快照，编辑已由 finalizeAndLeave
                // 落库；从当前列表 Flow 取最新实体替换，避免 pane 显示编辑前的旧内容。
                // 列表被筛选不含该笔记时回退旧快照（内容已在库中，不影响正确性）。
                val detail = page as? AppPage.Detail
                val latest = detail?.note?.id?.let { id -> notes.firstOrNull { it.id == id } }
                selectedDetail = detail?.copy(note = latest ?: detail.note)
                page = AppPage.List
            }
        }
        pendingDetailIntent = null
        detailLeaveRequest = 0
    }

    fun saveImportedDirectly(note: EditorSeed) {
        val folderId = writableFolderId()
        val pinned = activeFolderId == STARRED_FOLDER_ID
        fileIoScope.launch {
            runCatching {
                viewModel.queueSaveNote(
                    id = 0L,
                    title = note.title,
                    content = note.content,
                    color = 0xFFF5F0E8,
                    pinned = pinned,
                    topPinned = false,
                    markdown = note.formatMode.resolvesToMarkdown(note.content),
                    folderId = folderId
                ).await()
            }.onSuccess {
                feedbackMessage = "已导入 1 条笔记"
            }.onFailure {
                feedbackMessage = "导入保存失败：${it.message ?: "无法保存笔记"}"
            }
        }
    }

    LaunchedEffect(externalFileUri) {
        val selectedUri = externalFileUri ?: return@LaunchedEffect
        runFileOperation("打开文件失败", { readTextImport(context, selectedUri) }) { result ->
            when (result) {
                is ImportReadResult.Success -> {
                    val folderId = writableFolderId()
                    val pinned = activeFolderId == STARRED_FOLDER_ID
                    fileIoScope.launch {
                        runCatching {
                            val createdAt = System.currentTimeMillis()
                            val markdown = result.note.formatMode.resolvesToMarkdown(result.note.content)
                            val id = viewModel.queueSaveNote(
                                id = 0L,
                                title = result.note.title,
                                content = result.note.content,
                                color = 0xFFF5F0E8,
                                pinned = pinned,
                                topPinned = false,
                                markdown = markdown,
                                folderId = folderId,
                                createdAt = createdAt
                            ).await()
                            page = AppPage.Detail(
                                note = NoteEntity(
                                    id = id,
                                    title = result.note.title,
                                    content = result.note.content,
                                    color = 0xFFF5F0E8,
                                    createdAt = createdAt,
                                    updatedAt = createdAt,
                                    isPinned = pinned,
                                    isMarkdown = markdown,
                                    folderId = folderId
                                ),
                                folderId = folderId
                            )
                        }.onFailure {
                            feedbackMessage = "打开文件保存失败：${it.message ?: "无法保存笔记"}"
                        }
                    }
                }
                is ImportReadResult.Failure -> importError = result.message
            }
        }
    }

    val backupSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = pendingBackupPayload
        pendingBackupPayload = null
        if (uri != null && payload != null) {
            runFileOperation("备份导出失败", {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
                    ?: error("无法写入所选位置")
            }) {
                feedbackMessage = "备份已导出"
            }
        }
    }

    val noteExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/*")
    ) { uri ->
        val payload = pendingNoteExport
        pendingNoteExport = null
        if (uri != null && payload != null) {
            runFileOperation("笔记导出失败", {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(payload.content)
                } ?: error("无法写入所选位置")
            }) {
                feedbackMessage = "笔记已导出"
            }
        }
    }
    fun launchNoteExport(title: String, content: String, markdown: Boolean) {
        val extension = if (markdown) "md" else "txt"
        val safeTitle = title.trim()
            .ifBlank { "未命名笔记" }
            .replace(ExportUnsafeFileNamePattern, "_")
            .take(80)
            .ifBlank { "未命名笔记" }
        pendingNoteExport = NoteExportPayload(
            fileName = "$safeTitle.$extension",
            content = content
        )
        noteExportLauncher.launch("$safeTitle.$extension")
    }

    val textBackupSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val payload = pendingTextBackupPayload
        pendingTextBackupPayload = null
        if (uri != null && payload != null) {
            runFileOperation("TXT 备份导出失败", {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
                    ?: error("无法写入所选位置")
            }) {
                feedbackMessage = "TXT 明文备份已导出"
            }
        }
    }

    val backupOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selectedUri ->
            runFileOperation("备份文件读取失败", {
                context.contentResolver.openInputStream(selectedUri)?.bufferedReader()?.use { it.readText() }
                    ?: error("无法读取所选文件")
            }) { raw ->
                pendingRestorePayload = raw
            }
        }
    }

    val textBackupOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selectedUri ->
            runFileOperation("TXT 备份文件读取失败", {
                context.contentResolver.openInputStream(selectedUri)?.bufferedReader()?.use { it.readText() }
                    ?: error("无法读取所选文件")
            }) { raw ->
                pendingTextRestorePayload = raw
            }
        }
    }

    val markdownZipSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val payload = pendingMarkdownZipPayload
        pendingMarkdownZipPayload = null
        if (uri != null && payload != null) {
            runFileOperation("ZIP 备份导出失败", {
                context.contentResolver.openOutputStream(uri)?.use { it.write(payload) }
                    ?: error("无法写入所选位置")
            }) {
                feedbackMessage = "Markdown ZIP 备份已导出"
            }
        }
    }

    val markdownZipOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selectedUri ->
            runFileOperation("ZIP 备份读取失败", {
                context.contentResolver.openInputStream(selectedUri)?.use { it.readBytes() }
                    ?: error("无法读取所选文件")
            }) { payload ->
                pendingMarkdownZipRestorePayload = payload
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selectedUri ->
            runFileOperation("导入失败", { readTextImport(context, selectedUri) }) { result ->
                when (result) {
                    is ImportReadResult.Success -> {
                        saveImportedDirectly(EditorSeed(result.note.title, result.note.content, result.note.formatMode))
                    }
                    is ImportReadResult.Failure -> importError = result.message
                }
            }
        }
    }

    // 详情页自身必须独占系统返回事件，以便先执行自动保存；根页面仅处理设置层级的返回。
    BackHandler(enabled = page is AppPage.Settings || page is AppPage.Appearance) {
        page = if (page is AppPage.Appearance) AppPage.Settings else AppPage.List
    }

    // 双栏与整页随窗口互转：缩窄（旋转/退出分屏/合上折叠屏）把选中态提升为整页导航，
    // 展宽（旋转回来/进入分屏/展开折叠屏）把整页降级回双栏。两者都先请求详情完成
    // 最终保存（leaveRequest 令牌），保存完成后由 handleDetailBack 应用意图。
    LaunchedEffect(twoPane) {
        if (!twoPane && page is AppPage.List && selectedDetail != null) {
            pendingDetailIntent = DetailExitIntent.Promote
            detailLeaveRequest += 1
        } else if (twoPane && page is AppPage.Detail) {
            pendingDetailIntent = DetailExitIntent.Demote
            detailLeaveRequest += 1
        }
    }

    when (val currentPage = page) {
        AppPage.List -> NotesListPage(
            notes = notes,
            folders = folders,
            activeFolderId = activeFolderId,
            query = query,
            showSearch = showSearch,
            globalSearch = isGlobalSearch,
            onSearchToggle = {
                showSearch = !showSearch
                if (!showSearch) {
                    viewModel.setSearchQuery("")
                    if (isGlobalSearch) viewModel.closeGlobalSearch()
                } else {
                    // 顶部搜索面向全部笔记，不受当前收藏夹限制；收藏夹仍可通过抽屉单独浏览。
                    viewModel.openGlobalSearch()
                }
            },
            onSearchChange = viewModel::setSearchQuery,
            onFolderSelected = viewModel::selectFolder,
            onCreateFolder = { showNewFolderDialog = true },
            onOpenSettings = { page = AppPage.Settings },
            createMenuExpanded = showCreateMenu,
            onCreateMenuExpanded = { showCreateMenu = it },
            onNewNote = {
                openDetailTarget(
                    AppPage.Detail(
                        note = null,
                        folderId = writableFolderId(),
                        initialPinned = activeFolderId == STARRED_FOLDER_ID
                    )
                )
            },
            onImportFile = { fileLauncher.launch(arrayOf("text/plain", "text/markdown", "text/x-markdown", "text/*")) },
            onImportClipboard = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val recentText = clipboard.primaryClip?.let { clip ->
                    (0 until clip.itemCount)
                        .asSequence()
                        .mapNotNull { index -> clip.getItemAt(index).coerceToText(context)?.toString() }
                        .firstOrNull { it.isNotBlank() }
                }.orEmpty()
                if (recentText.isBlank()) {
                    feedbackMessage = "剪贴板没有可导入的文本"
                } else {
                    saveImportedDirectly(EditorSeed("", recentText, NoteFormatMode.AUTO))
                }
            },
            onOpenNote = { note, contentCursor ->
                // 打开即刷新“最近打开”时间戳，供“笔记按最近打开置顶”排序使用。
                viewModel.noteOpened(note.id)
                openDetailTarget(
                    AppPage.Detail(note = note, folderId = note.folderId, initialContentCursor = contentCursor)
                )
            },
            onToggleStar = viewModel::toggleStar,
            onToggleTopPin = viewModel::toggleTopPin,
            onFolderLongPress = { folderMenuFor = it },
            onBatchDelete = { ids ->
                viewModel.deleteNotes(ids) { deletedCount ->
                    feedbackMessage = "已删除 $deletedCount 条笔记"
                    // 双栏下被选中的笔记也在删除集合内：取消选中，避免 pane 展示已删除笔记。
                    // 残留竞态：若删除前 700ms 内还在编辑该笔记，迟到的防抖保存（upsert）
                    // 理论上会复活已删除行；需要「输入后立即长按+删除+确认」四步在防抖
                    // 窗口内完成，实际不可达，故不为它引入第二套取消机制。
                    val selectedId = selectedDetail?.note?.id
                    if (selectedId != null && selectedId in ids) {
                        selectedDetail = null
                        pendingDetailIntent = null
                        detailLeaveRequest = 0
                    }
                }
            },
            onBatchStar = { ids ->
                viewModel.starNotes(ids) { starredCount ->
                    feedbackMessage = "已加入星标 $starredCount 条笔记"
                }
            },
            onBatchTopPin = { ids, topPinned ->
                viewModel.setTopPinnedNotes(ids, topPinned) { changedCount ->
                    feedbackMessage = if (topPinned) "已置顶 $changedCount 条笔记" else "已取消置顶 $changedCount 条笔记"
                }
            },
            twoPane = twoPane,
            paneSelectedNoteId = selectedDetail?.note?.id,
            detailPane = if (twoPane) {
                {
                    val detail = selectedDetail
                    if (detail == null) {
                        DetailPaneEmptyState(
                            onCreate = {
                                openDetailTarget(
                                    AppPage.Detail(
                                        note = null,
                                        folderId = writableFolderId(),
                                        initialPinned = activeFolderId == STARRED_FOLDER_ID
                                    )
                                )
                            }
                        )
                    } else {
                        NoteDetailPage(
                            note = detail.note,
                            seed = detail.seed,
                            initialFolderId = detail.folderId,
                            initialContentCursor = detail.initialContentCursor,
                            initialPinned = detail.initialPinned,
                            folders = folders.filterNot { it.id == STARRED_FOLDER_ID },
                            positionStore = notePositionStore,
                            editorScope = appEditorScope,
                            leaveRequest = detailLeaveRequest,
                            onBack = { returnedFolderId, returnedNoteId ->
                                handleDetailBack(detail.note == null, returnedFolderId, returnedNoteId)
                            },
                            onSave = { id, title, content, color, pinned, topPinned, markdown, folderId, createdAt ->
                                viewModel.queueSaveNote(id, title, content, color, pinned, topPinned, markdown, folderId, createdAt)
                            },
                            onDelete = { note -> noteToDelete = note },
                            onDiscardEmptyDraft = { noteId -> viewModel.deleteNote(noteId) },
                            onExport = { title, content, markdown -> launchNoteExport(title, content, markdown) },
                            onCopyToFolder = { note, folderId ->
                                viewModel.copyNoteToFolder(
                                    note = note,
                                    folderId = folderId,
                                    onSuccess = { feedbackMessage = "已复制到收藏夹" },
                                    onFailure = { feedbackMessage = "复制失败：$it" }
                                )
                            }
                        )
                    }
                }
            } else null
        )
        AppPage.Settings -> SettingsPage(
            onBack = { page = AppPage.List },
            onOpenAppearance = { page = AppPage.Appearance },
            appearanceMode = appearanceMode,
            noteListSortByOpen = noteListSortByOpen,
            onNoteListSortByOpenChange = viewModel::setNoteListSortByRecentOpen,
            onBackupClick = { showBackupMenu = true },
            onWebDavClick = { showWebDavDialog = true },
            webDavConfigured = webDavConfig != null
        )
        AppPage.Appearance -> AppearanceSettingsPage(
            appearanceMode = appearanceMode,
            noteFont = noteFont,
            customFontPath = customFontPath,
            onBack = { page = AppPage.Settings },
            onSelect = onAppearanceChange,
            onFontSelect = onFontChange,
            onCustomFontPath = onCustomFontPath
        )
        is AppPage.Detail -> NoteDetailPage(
            note = currentPage.note,
            seed = currentPage.seed,
            initialFolderId = currentPage.folderId,
            initialContentCursor = currentPage.initialContentCursor,
            initialPinned = currentPage.initialPinned,
            folders = folders.filterNot { it.id == STARRED_FOLDER_ID },
            positionStore = notePositionStore,
            editorScope = appEditorScope,
            leaveRequest = detailLeaveRequest,
            onBack = { returnedFolderId, returnedNoteId ->
                handleDetailBack(currentPage.note == null, returnedFolderId, returnedNoteId)
            },
            onSave = { id, title, content, color, pinned, topPinned, markdown, folderId, createdAt ->
                viewModel.queueSaveNote(id, title, content, color, pinned, topPinned, markdown, folderId, createdAt)
            },
            onDelete = { note -> noteToDelete = note },
            onDiscardEmptyDraft = { noteId -> viewModel.deleteNote(noteId) },
            onExport = { title, content, markdown -> launchNoteExport(title, content, markdown) },
            onCopyToFolder = { note, folderId ->
                viewModel.copyNoteToFolder(
                    note = note,
                    folderId = folderId,
                    onSuccess = { feedbackMessage = "已复制到收藏夹" },
                    onFailure = { feedbackMessage = "复制失败：$it" }
                )
            }
        )
    }

    noteToDelete?.let { note ->
        ConfirmDeleteDialog(
            onDismiss = { noteToDelete = null },
            onConfirm = {
                viewModel.deleteNote(note.id)
                noteToDelete = null
                page = AppPage.List
                // 双栏下删除的正是 pane 选中笔记：清选中回空态，避免详情页继续
                // 展示已删除内容（且后续编辑会经 upsert 复活该行）。残留竞态与
                // 批量删除同一论证：删除前 700ms 内的迟到保存实际不可达。
                if (selectedDetail?.note?.id == note.id) {
                    selectedDetail = null
                    pendingDetailIntent = null
                    detailLeaveRequest = 0
                }
            }
        )
    }
    folderToDelete?.let { folder ->
        FolderDeleteDialog(
            folder = folder,
            onDismiss = { folderToDelete = null },
            onConfirm = {
                viewModel.deleteFolder(
                    folderId = folder.id,
                    onSuccess = { message ->
                        folderToDelete = null
                        feedbackMessage = message
                    },
                    onFailure = { message ->
                        folderToDelete = null
                        feedbackMessage = message
                    }
                )
            }
        )
    }
    folderMenuFor?.let { menuFolder ->
        FolderActionsMenuDialog(
            folder = menuFolder,
            onDismiss = { folderMenuFor = null },
            onRename = {
                folderMenuFor = null
                folderToRename = menuFolder
            },
            onSort = {
                folderMenuFor = null
                folderToSort = menuFolder
            },
            onDelete = {
                folderMenuFor = null
                folderToDelete = menuFolder
            }
        )
    }
    folderToRename?.let { renameTarget ->
        FolderRenameDialog(
            folder = renameTarget,
            onDismiss = { folderToRename = null },
            onConfirm = { name ->
                folderToRename = null
                viewModel.renameFolder(
                    folderId = renameTarget.id,
                    name = name,
                    onSuccess = { newName -> feedbackMessage = "已重命名为「$newName」" },
                    onFailure = { message -> feedbackMessage = message }
                )
            }
        )
    }
    folderToSort?.let { sortTarget ->
        FolderSortDialog(
            folderId = sortTarget.id,
            baseOrder = folders.filterNot { it.id == STARRED_FOLDER_ID },
            onDismiss = { folderToSort = null },
            onTop = { viewModel.moveFolderToTop(sortTarget.id) },
            onUp = { viewModel.moveFolderUp(sortTarget.id) },
            onDown = { viewModel.moveFolderDown(sortTarget.id) }
        )
    }
    importError?.let { message ->
        ImportErrorDialog(message = message, onDismiss = { importError = null })
    }
    if (showNewFolderDialog) {
        FolderNameDialog(
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(
                    name = name,
                    onSuccess = { createdName ->
                        showNewFolderDialog = false
                        feedbackMessage = "已创建并切换到收藏夹：$createdName"
                    },
                    onFailure = { message -> feedbackMessage = message }
                )
            }
        )
    }
    if (showBackupMenu) {
        BackupMenuDialog(
            onDismiss = { showBackupMenu = false },
            onExportZip = {
                showBackupMenu = false
                viewModel.createMarkdownZipBackup(
                    onSuccess = { backup ->
                        pendingMarkdownZipPayload = backup
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        markdownZipSaveLauncher.launch("AnJian_Backup_$timestamp.zip")
                    },
                    onFailure = { message -> feedbackMessage = message }
                )
            },
            onImportZip = {
                showBackupMenu = false
                markdownZipOpenLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
            },
            onExportText = {
                showBackupMenu = false
                viewModel.createTextBackup(
                    onSuccess = { backup ->
                        pendingTextBackupPayload = backup
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        textBackupSaveLauncher.launch("AnJian_Text_Backup_$timestamp.txt")
                    },
                    onFailure = { message -> feedbackMessage = message }
                )
            },
            onImportText = {
                showBackupMenu = false
                textBackupOpenLauncher.launch(arrayOf("text/plain"))
            }
        )
    }
    pendingMarkdownZipRestorePayload?.let { backup ->
        RestoreBackupDialog(
            onDismiss = { pendingMarkdownZipRestorePayload = null },
            onConfirm = {
                viewModel.restoreMarkdownZipBackup(
                    payload = backup,
                    onSuccess = { message ->
                        pendingMarkdownZipRestorePayload = null
                        feedbackMessage = message
                    },
                    onFailure = { message ->
                        pendingMarkdownZipRestorePayload = null
                        feedbackMessage = message
                    }
                )
            }
        )
    }
    pendingRestorePayload?.let { backup ->
        RestoreBackupDialog(
            onDismiss = { pendingRestorePayload = null },
            onConfirm = {
                viewModel.restoreBackup(
                    rawBackup = backup,
                    onSuccess = { message ->
                        pendingRestorePayload = null
                        feedbackMessage = message
                    },
                    onFailure = { message ->
                        pendingRestorePayload = null
                        feedbackMessage = message
                    }
                )
            }
        )
    }
    pendingTextRestorePayload?.let { backup ->
        RestoreTextBackupDialog(
            onDismiss = { pendingTextRestorePayload = null },
            onConfirm = {
                viewModel.restoreTextBackup(
                    rawBackup = backup,
                    onSuccess = { message ->
                        pendingTextRestorePayload = null
                        feedbackMessage = message
                    },
                    onFailure = { message ->
                        pendingTextRestorePayload = null
                        feedbackMessage = message
                    }
                )
            }
        )
    }
    if (showWebDavDialog) {
        WebDavBackupDialog(
            initialConfig = webDavConfig,
            onDismiss = { showWebDavDialog = false },
            onSave = { config ->
                viewModel.saveWebDavConfig(
                    config = config,
                    onSuccess = {
                        showWebDavDialog = false
                        feedbackMessage = "WebDAV 配置已安全保存"
                    },
                    onFailure = { message -> feedbackMessage = message }
                )
            },
            task = webDavTask,
            onTest = { config ->
                viewModel.testWebDav(
                    config = config,
                    onSuccess = { feedbackMessage = "WebDAV 连接正常" },
                    onFailure = { message -> feedbackMessage = message }
                )
            },
            onRestore = { config ->
                viewModel.previewWebDavRestore(
                    config = config,
                    onSuccess = { pendingWebDavRestore = it },
                    onFailure = { message -> feedbackMessage = message }
                )
            },
            onSync = { config ->
                viewModel.syncWebDav(
                    config = config,
                    onSuccess = { message ->
                        showWebDavDialog = false
                        feedbackMessage = message
                    },
                    onFailure = { message -> feedbackMessage = message }
                )
            }
        )
    }
    pendingWebDavRestore?.let { remote ->
        WebDavRestorePreviewDialog(
            remote = remote,
            onDismiss = {
                pendingWebDavRestore = null
                if (webDavTask == WebDavTask.RESTORE) viewModel.cancelWebDavRestorePreview()
            },
            onConfirm = {
                pendingWebDavRestore = null
                viewModel.confirmWebDavRestore(
                    remote = remote,
                    onSuccess = { message -> showWebDavDialog = false; feedbackMessage = message },
                    onFailure = { message -> feedbackMessage = message }
                )
            }
        )
    }
    feedbackMessage?.let { message ->
        FeedbackDialog(message = message, onDismiss = { feedbackMessage = null })
    }
}
