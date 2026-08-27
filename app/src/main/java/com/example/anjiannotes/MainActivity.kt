package com.example.anjiannotes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
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
import com.example.anjiannotes.data.NotesRepository
import com.example.anjiannotes.data.STARRED_FOLDER_ID
import com.example.anjiannotes.data.WebDavConfig
import com.example.anjiannotes.ui.EditorSeed
import com.example.anjiannotes.ui.ImportReadResult
import com.example.anjiannotes.ui.MarkdownPreview
import com.example.anjiannotes.ui.MarkdownSyntaxHint
import com.example.anjiannotes.ui.NoteFormatMode
import com.example.anjiannotes.ui.extractFirstLink
import com.example.anjiannotes.ui.extractLinkAt
import com.example.anjiannotes.ui.formatForFileName
import com.example.anjiannotes.ui.linkifyPlainText
import com.example.anjiannotes.ui.markdownToListPreview
import com.example.anjiannotes.ui.markdownToPlainText
import com.example.anjiannotes.ui.plainTextToListPreview
import com.example.anjiannotes.ui.readTextImport
import com.example.anjiannotes.ui.theme.AnJianTheme
import com.example.anjiannotes.ui.theme.AppearanceMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AnJianApplication
        setContent {
            val appearanceMode by app.appearancePreferences.mode.collectAsStateWithLifecycle()
            var factory by remember { mutableStateOf<NotesViewModelFactory?>(null) }

            // 让 Activity 尽快交出主线程以绘制首帧；Room 与依赖组装在后台完成后再进入首页。
            LaunchedEffect(app) {
                factory = withContext(Dispatchers.Default) {
                    val database = app.database
                    NotesViewModelFactory(
                        NotesRepository(database, database.noteDao(), database.folderDao()),
                        app.webDavConfigStore,
                        app.webDavBackupClient,
                        app.folderSelectionPreferences
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
                    NotesApp(
                        viewModel = notesViewModel,
                        appearanceMode = appearanceMode,
                        onAppearanceChange = app.appearancePreferences::setMode
                    )
                }
            }
        }
    }
}

private enum class InlineEditTarget { TITLE, CONTENT }
private enum class DetailMode { PREVIEW, EDIT }
private enum class AutoSaveState { IDLE, SAVING, SAVED }

/** 新建草稿离开详情页时必须至少写入一次；已有笔记仍沿用原有的脏数据判断。 */
internal fun shouldForceFinalDraftSave(isNewNote: Boolean, savedNoteId: Long): Boolean =
    isNewNote && savedNoteId == 0L

/** 每次编辑事件同步生成的不可变保存快照，避免组合重绘前读取到旧文本。 */
private data class NoteDraftSnapshot(
    val title: String,
    val content: String,
    val color: Long,
    val isPinned: Boolean,
    val isTopPinned: Boolean,
    val isMarkdown: Boolean,
    val folderId: Long
)

private data class NoteListPreviewKey(
    val id: Long,
    val updatedAt: Long,
    val isMarkdown: Boolean
)

/**
 * 列表项离开并重新进入可视区时，Compose 可能需要重新组合该项。
 * 缓存已生成的轻量摘要，避免再次执行多轮 Markdown 正则替换。
 */
private object NoteListPreviewCache {
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

private val NoteDateFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
}

private sealed interface AppPage {
    data object List : AppPage
    data class Detail(
        val note: NoteEntity?,
        val seed: EditorSeed = EditorSeed(),
        val folderId: Long = DEFAULT_FOLDER_ID
    ) : AppPage
    data object Settings : AppPage
    data object Appearance : AppPage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesApp(
    viewModel: NotesViewModel,
    appearanceMode: AppearanceMode,
    onAppearanceChange: (AppearanceMode) -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val activeFolderId by viewModel.activeFolderId.collectAsStateWithLifecycle()
    val isGlobalSearch by viewModel.isGlobalSearch.collectAsStateWithLifecycle()
    val webDavConfig by viewModel.webDavConfig.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf<AppPage>(AppPage.List) }
    var showSearch by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
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
    var showWebDavDialog by remember { mutableStateOf(false) }
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

    fun openImported(note: EditorSeed) {
        page = AppPage.Detail(note = null, seed = note, folderId = writableFolderId())
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
                        openImported(EditorSeed(result.note.title, result.note.content, result.note.formatMode))
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
                }
            },
            onSearchChange = viewModel::setSearchQuery,
            onFolderSelected = viewModel::selectFolder,
            onOpenGlobalSearch = {
                viewModel.openGlobalSearch()
                showSearch = true
            },
            onCreateFolder = { showNewFolderDialog = true },
            onOpenSettings = { page = AppPage.Settings },
            createMenuExpanded = showCreateMenu,
            onCreateMenuExpanded = { showCreateMenu = it },
            onNewNote = { page = AppPage.Detail(note = null, folderId = writableFolderId()) },
            onImportFile = { fileLauncher.launch(arrayOf("text/plain", "text/markdown", "text/x-markdown", "text/*")) },
            onImportClipboard = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val recentText = clipboard.primaryClip?.let { clip ->
                    (0 until clip.itemCount)
                        .asSequence()
                        .mapNotNull { index -> clip.getItemAt(index).coerceToText(context)?.toString() }
                        .firstOrNull { it.isNotBlank() }
                }.orEmpty()
                page = AppPage.Detail(
                    note = null,
                    seed = EditorSeed("剪切板笔记", recentText, NoteFormatMode.AUTO),
                    folderId = writableFolderId()
                )
            },
            onOpenNote = { note -> page = AppPage.Detail(note = note, folderId = note.folderId) },
            onToggleStar = viewModel::toggleStar,
            onToggleTopPin = viewModel::toggleTopPin,
            onDeleteFolder = { folderToDelete = it },
            onBatchDelete = { ids ->
                viewModel.deleteNotes(ids) { deletedCount ->
                    feedbackMessage = "已删除 $deletedCount 条笔记"
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
            }
        )
        AppPage.Settings -> SettingsPage(
            onBack = { page = AppPage.List },
            onOpenAppearance = { page = AppPage.Appearance },
            appearanceMode = appearanceMode,
            onBackupClick = { showBackupMenu = true },
            onWebDavClick = { showWebDavDialog = true },
            webDavConfigured = webDavConfig != null
        )
        AppPage.Appearance -> AppearanceSettingsPage(
            appearanceMode = appearanceMode,
            onBack = { page = AppPage.Settings },
            onSelect = onAppearanceChange
        )
        is AppPage.Detail -> NoteDetailPage(
            note = currentPage.note,
            seed = currentPage.seed,
            initialFolderId = currentPage.folderId,
            folders = folders.filterNot { it.id == STARRED_FOLDER_ID },
            onBack = { returnedFolderId, returnedNoteId ->
                if (currentPage.note == null && returnedNoteId > 0L) {
                    // “已保存”仅会在 Room 写入完成后显示；返回时定位到新笔记所在收藏夹，
                    // 并清除旧搜索条件，避免首个草稿因列表筛选而看似消失。
                    if (activeFolderId != returnedFolderId) viewModel.selectFolder(returnedFolderId)
                    if (query.isNotBlank()) viewModel.setSearchQuery("")
                } else if (activeFolderId == STARRED_FOLDER_ID && returnedFolderId != STARRED_FOLDER_ID) {
                    viewModel.selectFolder(returnedFolderId)
                }
                page = AppPage.List
            },
            onSave = { id, title, content, color, pinned, topPinned, markdown, folderId, createdAt ->
                viewModel.queueSaveNote(id, title, content, color, pinned, topPinned, markdown, folderId, createdAt)
            },
            onDelete = { note -> noteToDelete = note }
        )
    }

    noteToDelete?.let { note ->
        ConfirmDeleteDialog(
            onDismiss = { noteToDelete = null },
            onConfirm = { viewModel.deleteNote(note.id); noteToDelete = null; page = AppPage.List }
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
    feedbackMessage?.let { message ->
        FeedbackDialog(message = message, onDismiss = { feedbackMessage = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesListPage(
    notes: List<NoteEntity>,
    folders: List<FolderEntity>,
    activeFolderId: Long,
    query: String,
    showSearch: Boolean,
    globalSearch: Boolean,
    onSearchToggle: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFolderSelected: (Long) -> Unit,
    onOpenGlobalSearch: () -> Unit,
    onCreateFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    createMenuExpanded: Boolean,
    onCreateMenuExpanded: (Boolean) -> Unit,
    onNewNote: () -> Unit,
    onImportFile: () -> Unit,
    onImportClipboard: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onToggleStar: (NoteEntity) -> Unit,
    onToggleTopPin: (NoteEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onBatchDelete: (Set<Long>) -> Unit,
    onBatchStar: (Set<Long>) -> Unit,
    onBatchTopPin: (Set<Long>, Boolean) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
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
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.76f)) {
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
                        TextButton(
                            onClick = {
                                onOpenGlobalSearch()
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Text("⌕", fontSize = 25.sp, color = MaterialTheme.colorScheme.onSurface)
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
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            folder.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (folder.id != DEFAULT_FOLDER_ID && folder.id != STARRED_FOLDER_ID) {
                                            TextButton(onClick = { onDeleteFolder(folder) }) {
                                                Text("删除")
                                            }
                                        }
                                    }
                                },
                                selected = folder.id == activeFolderId,
                                onClick = {
                                    onFolderSelected(folder.id)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.fillMaxWidth()
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
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)
            ) {
                if (showSearch && !selectionMode) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onSearchChange,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        label = { Text("搜索标题或正文") },
                        singleLine = true
                    )
                }
                if (notes.isEmpty()) {
                    EmptyNotes(query = query, onCreate = onNewNote)
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = noteListState,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 12.dp)
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
                                    note = note,
                                    selected = note.id in selectedNoteIds,
                                    selectionMode = selectionMode,
                                    onOpen = {
                                        if (selectionMode) toggleSelection(note.id) else onOpenNote(note)
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
private fun FolderNameDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建收藏夹") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("收藏夹名称") },
                singleLine = true
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("创建") } }
    )
}

@Composable
private fun FolderDeleteDialog(
    folder: FolderEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) = ConfirmationDialog(
    title = "删除收藏夹？",
    message = "将删除“${folder.name}”及其中全部笔记。此操作不可恢复。",
    confirmLabel = "删除",
    destructive = true,
    onDismiss = onDismiss,
    onConfirm = onConfirm
)

@Composable
private fun FolderPickerDialog(
    folders: List<FolderEntity>,
    selectedFolderId: Long,
    onDismiss: () -> Unit,
    onSelected: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动到收藏夹") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                folders.forEach { folder ->
                    TextButton(
                        onClick = { onSelected(folder.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (folder.id == selectedFolderId) "${folder.name} ✓" else folder.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPage(
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    appearanceMode: AppearanceMode,
    onBackupClick: () -> Unit,
    onWebDavClick: () -> Unit,
    webDavConfigured: Boolean
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SettingsGroup(
                title = "外观",
                items = listOf(
                    SettingsEntry("◐", "外观", "当前：${appearanceMode.label}", onOpenAppearance)
                )
            )
            SettingsGroup(
                title = "数据与备份",
                items = listOf(
                    SettingsEntry("↥", "本地备份与恢复", "Markdown ZIP，可在其他应用中直接阅读", onBackupClick),
                    SettingsEntry(
                        "⌁",
                        "WebDAV 增量备份",
                        if (webDavConfigured) "已配置 · 仅上传发生变化的笔记" else "配置服务器后上传 Markdown 与元数据",
                        onWebDavClick
                    )
                )
            )
            SettingsGroup(
                title = "关于",
                items = listOf(
                    SettingsEntry("i", "安装", "安笺 ${BuildConfig.VERSION_NAME} · 离线笔记", onClick = {})
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettingsPage(
    appearanceMode: AppearanceMode,
    onBack: () -> Unit,
    onSelect: (AppearanceMode) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("外观", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "选择主题后会立即应用到当前页面和所有组件。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            AppearanceOption(
                symbol = "☀",
                title = "浅色",
                subtitle = "柔和浅灰背景，适合明亮环境",
                selected = appearanceMode == AppearanceMode.LIGHT,
                onClick = { onSelect(AppearanceMode.LIGHT) }
            )
            AppearanceOption(
                symbol = "☾",
                title = "深色",
                subtitle = "低亮度深灰层级，适合暗光阅读",
                selected = appearanceMode == AppearanceMode.DARK,
                onClick = { onSelect(AppearanceMode.DARK) }
            )
            AppearanceOption(
                symbol = "⚙",
                title = "跟随系统",
                subtitle = "随 Android 系统的浅色与深色模式实时变化",
                selected = appearanceMode == AppearanceMode.SYSTEM,
                onClick = { onSelect(AppearanceMode.SYSTEM) }
            )
        }
    }
}

@Composable
private fun AppearanceOption(
    symbol: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.76f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

private data class SettingsEntry(
    val symbol: String,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
private fun SettingsGroup(title: String, items: List<SettingsEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f))
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable(onClick = item.onClick)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.width(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(item.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f))
                    }
                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                            modifier = Modifier.padding(start = 52.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebDavBackupDialog(
    initialConfig: WebDavConfig?,
    onDismiss: () -> Unit,
    onSave: (WebDavConfig) -> Unit,
    onSync: (WebDavConfig) -> Unit
) {
    var endpoint by remember(initialConfig) { mutableStateOf(initialConfig?.endpoint.orEmpty()) }
    var username by remember(initialConfig) { mutableStateOf(initialConfig?.username.orEmpty()) }
    var password by remember(initialConfig) { mutableStateOf(initialConfig?.password.orEmpty()) }
    var remoteDirectory by remember(initialConfig) { mutableStateOf(initialConfig?.remoteDirectory ?: "an-jian-backup") }
    fun currentConfig() = WebDavConfig(endpoint, username, password, remoteDirectory)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WebDAV 增量备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("服务器上会保存 notes/*.md、folders.json 与 metadata.json。密码使用设备加密密钥保存在本机。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://dav.example.com/remote.php/dav/files/name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("用户名") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码或应用专用密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = remoteDirectory,
                    onValueChange = { remoteDirectory = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("远程备份目录") },
                    singleLine = true
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onSave(currentConfig()) }) { Text("仅保存") }
                TextButton(onClick = { onSync(currentConfig()) }) { Text("保存并备份", fontWeight = FontWeight.SemiBold) }
            }
        }
    )
}

@Composable
private fun BackupMenuDialog(
    onDismiss: () -> Unit,
    onExportZip: () -> Unit,
    onImportZip: () -> Unit,
    onExportText: () -> Unit,
    onImportText: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("备份与恢复") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("备份会创建可跨平台阅读的 ZIP：每篇笔记均为独立 Markdown 文件，同时包含收藏夹与备份元数据。恢复会替换当前本地数据。")
                TextButton(onClick = onExportZip, modifier = Modifier.fillMaxWidth()) { Text("导出 Markdown ZIP 备份", modifier = Modifier.fillMaxWidth()) }
                TextButton(onClick = onImportZip, modifier = Modifier.fillMaxWidth()) { Text("导入 Markdown ZIP 并完整恢复", modifier = Modifier.fillMaxWidth()) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))
                Text("TXT 明文备份保留为轻量、直接可读的本地备份格式。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onExportText, modifier = Modifier.fillMaxWidth()) { Text("导出 TXT 明文备份", modifier = Modifier.fillMaxWidth()) }
                TextButton(onClick = onImportText, modifier = Modifier.fillMaxWidth()) { Text("导入 TXT 明文并完整恢复", modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun RestoreTextBackupDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) = ConfirmationDialog(
    title = "恢复 TXT 明文备份？",
    message = "恢复会以 TXT 备份中的收藏夹和笔记替换当前本地数据，此操作无法撤销。",
    confirmLabel = "确认恢复",
    onDismiss = onDismiss,
    onConfirm = onConfirm
)

@Composable
private fun RestoreBackupDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) = ConfirmationDialog(
    title = "恢复备份？",
    message = "恢复会以备份文件中的收藏夹和笔记替换当前本地数据，此操作无法撤销。",
    confirmLabel = "确认恢复",
    onDismiss = onDismiss,
    onConfirm = onConfirm
)

@Composable
private fun FeedbackDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } }
    )
}

@Composable
private fun QuickCaptureBar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onStartWriting: () -> Unit,
    onImportFile: () -> Unit,
    onImportClipboard: () -> Unit
) {
    val pillShape = RoundedCornerShape(30.dp)
    val shadowColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = pillShape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
                    .clip(pillShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .clickable(onClick = onStartWriting)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "写点什么…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(
                            elevation = 5.dp,
                            shape = CircleShape,
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        )
                        .clickable { onExpandedChange(!expanded) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⋮", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                    CreateMenuItem("新建笔记", "打开完整编辑页开始书写", onClick = { onExpandedChange(false); onStartWriting() })
                    CreateMenuItem("导入文件", "支持 .md、.markdown、.txt 与 UTF-8 文本", onClick = { onExpandedChange(false); onImportFile() })
                    CreateMenuItem("从剪切板导入", "读取当前最近一条剪切板文本", onClick = { onExpandedChange(false); onImportClipboard() })
                }
            }
        }
    }
}

@Composable
private fun CreateMenuItem(title: String, subtitle: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun NoteDetailPage(
    note: NoteEntity?,
    seed: EditorSeed,
    initialFolderId: Long,
    folders: List<FolderEntity>,
    onBack: (Long, Long) -> Unit,
    onSave: (Long, String, String, Long, Boolean, Boolean, Boolean, Long, Long) -> Deferred<Long>,
    onDelete: (NoteEntity) -> Unit
) {
    val context = LocalContext.current
    val isNewNote = note == null
    var title by remember(note?.id, seed) { mutableStateOf(note?.title ?: seed.title) }
    var contentValue by remember(note?.id, seed) { mutableStateOf(TextFieldValue(note?.content ?: seed.content)) }
    val content = contentValue.text
    var color by remember(note?.id) { mutableStateOf(note?.color ?: NoteColors.first()) }
    var pinned by remember(note?.id) { mutableStateOf(note?.isPinned ?: false) }
    var topPinned by remember(note?.id) { mutableStateOf(note?.isTopPinned ?: false) }
    var selectedFolderId by remember(note?.id, initialFolderId) { mutableStateOf(note?.folderId ?: initialFolderId) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var formatMode by remember(note?.id, seed) {
        mutableStateOf(
            if (note != null) {
                if (note.isMarkdown) NoteFormatMode.MARKDOWN else NoteFormatMode.PLAIN
            } else {
                seed.formatMode
            }
        )
    }
    var latestDraft by remember(note?.id, seed, initialFolderId) {
        val initialContent = note?.content ?: seed.content
        mutableStateOf(
            NoteDraftSnapshot(
                title = note?.title ?: seed.title,
                content = initialContent,
                color = note?.color ?: NoteColors.first(),
                isPinned = note?.isPinned ?: false,
                isTopPinned = note?.isTopPinned ?: false,
                isMarkdown = formatMode.resolvesToMarkdown(initialContent),
                folderId = note?.folderId ?: initialFolderId
            )
        )
    }
    var detailMode by remember(note?.id, seed) {
        mutableStateOf(if (note == null) DetailMode.EDIT else DetailMode.PREVIEW)
    }
    var focusTarget by remember(note?.id, seed) {
        mutableStateOf(InlineEditTarget.CONTENT)
    }
    var requestedContentCursor by remember(note?.id, seed) { mutableStateOf<Int?>(null) }
    var savedNoteId by remember(note?.id, seed) { mutableStateOf(note?.id ?: 0L) }
    val draftCreatedAt = remember(note?.id, seed) { note?.createdAt ?: System.currentTimeMillis() }
    val seedHasContent = isNewNote && (seed.title.isNotBlank() || seed.content.isNotBlank())
    var editRevision by remember(note?.id, seed) { mutableStateOf(if (seedHasContent) 1 else 0) }
    var savedRevision by remember(note?.id, seed) { mutableStateOf(0) }
    var hasUserEdited by remember(note?.id, seed) { mutableStateOf(seedHasContent) }
    var autoSaveState by remember(note?.id, seed) { mutableStateOf(AutoSaveState.IDLE) }
    var saveError by remember(note?.id, seed) { mutableStateOf<String?>(null) }
    var leavingInProgress by remember(note?.id, seed) { mutableStateOf(false) }
    var saveWorker by remember(note?.id, seed) { mutableStateOf<Job?>(null) }
    var debounceJob by remember(note?.id, seed) { mutableStateOf<Job?>(null) }
    val editorScope = rememberCoroutineScope()
    var nativeTitleFocusRequest by remember(note?.id, seed) { mutableStateOf(-1) }
    var nativeContentFocusRequest by remember(note?.id, seed) { mutableStateOf(-1) }
    var nativeContentEditor by remember(note?.id, seed) { mutableStateOf<NativeNoteEditText?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val markdownActive = formatMode.resolvesToMarkdown(content)
    val folderName = folders.firstOrNull { it.id == selectedFolderId }?.name ?: "默认收藏夹"
    val detailScrollState = rememberScrollState()
    var pendingScrollRestore by remember(note?.id, seed) { mutableStateOf<Int?>(null) }

    LaunchedEffect(detailMode, focusTarget) {
        if (detailMode == DetailMode.EDIT) {
            when (focusTarget) {
                InlineEditTarget.TITLE -> nativeTitleFocusRequest += 1
                InlineEditTarget.CONTENT -> {
                    requestedContentCursor?.let { position ->
                        contentValue = contentValue.copy(selection = TextRange(position.coerceIn(0, contentValue.text.length)))
                        requestedContentCursor = null
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
        if (detailMode == DetailMode.PREVIEW) pendingScrollRestore = detailScrollState.value
        if (target == InlineEditTarget.CONTENT) requestedContentCursor = cursorPosition
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
        // AndroidView 的最后一次输入可能尚未触发 Compose 重组；切换预览前以视图
        // 当前文本作为最终来源，同步更新草稿和保存队列。
        markEdited(contentOverride = currentText)
    }

    suspend fun finalizePendingChanges(): Boolean {
        debounceJob?.cancel()
        // 无论是否输入过，新建页离开前都必须经由已有单一保存队列写入一次 Room。
        // 这里不创建第二套保存机制，仅让现有 worker 获得一个需要保存的最终 revision。
        if (shouldForceFinalDraftSave(isNewNote, savedNoteId) && savedRevision >= editRevision) {
            latestDraft = NoteDraftSnapshot(
                title = title,
                content = nativeContentEditor?.text?.toString() ?: contentValue.text,
                color = color,
                isPinned = pinned,
                isTopPinned = topPinned,
                isMarkdown = formatMode.resolvesToMarkdown(nativeContentEditor?.text?.toString() ?: contentValue.text),
                folderId = selectedFolderId
            )
            hasUserEdited = true
            editRevision = savedRevision + 1
        }
        editorLog("final save before leave dirty=${hasUserEdited && savedRevision < editRevision}")
        while (hasUserEdited && savedRevision < editRevision) {
            startSaveWorker()
            saveWorker?.join()
            if (saveError != null) return false
        }
        return true
    }

    fun finalizeAndLeave() {
        // 返回时始终从原生 EditText 同步最后一帧用户输入，不依赖下一次 Compose 重组或 debounce。
        if (detailMode == DetailMode.EDIT) syncNativeContentBeforePreview()
        if (leavingInProgress) {
            editorLog("back ignored while final save is in progress")
            return
        }
        editorLog("back requested mode=$detailMode isNew=$isNewNote")
        leavingInProgress = true
        keyboard?.hide()
        editorScope.launch {
            if (!finalizePendingChanges()) {
                editorLog("back cancelled because final save failed")
                leavingInProgress = false
                return@launch
            }
            if (isNewNote) {
                // 新建笔记第一次返回即在 Room 写入确认后回到主页，不再中转预览页。
                editorLog("new draft saved; leaving detail")
                onBack(selectedFolderId, savedNoteId)
            } else if (detailMode == DetailMode.EDIT) {
                // 已有笔记保留原有交互：第一次返回仅结束编辑，第二次返回离开详情。
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

    LaunchedEffect(autoSaveState) {
        if (autoSaveState == AutoSaveState.SAVED) {
            delay(1_400)
            if (autoSaveState == AutoSaveState.SAVED) autoSaveState = AutoSaveState.IDLE
        }
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
    // BackHandler 通过 AndroidX 回调覆盖物理返回键、三键导航及系统返回手势。
    BackHandler(enabled = true) { latestFinalizeAndLeave() }

    Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (note == null) "新建笔记" else "笔记详情", fontWeight = FontWeight.SemiBold)
                        when {
                            saveError != null -> Text("保存失败", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            autoSaveState == AutoSaveState.SAVING -> Text("保存中…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            autoSaveState == AutoSaveState.SAVED -> Text("已保存 ✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = ::finalizeAndLeave) {
                        Text("返回")
                    }
                },
                actions = {
                    TextButton(onClick = ::toggleDetailMode) {
                        Text(if (detailMode == DetailMode.PREVIEW) "编辑" else "预览")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Column(
                    modifier = if (detailMode == DetailMode.EDIT) {
                        Modifier.fillMaxSize().padding(end = 12.dp)
                    } else {
                        Modifier.fillMaxSize().padding(end = 12.dp).verticalScroll(detailScrollState)
                    },
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
            if (detailMode == DetailMode.EDIT) {
                NativeNoteTitleEditor(
                    value = title,
                    focusRequest = nativeTitleFocusRequest,
                    modifier = Modifier.fillMaxWidth(),
                    textColor = MaterialTheme.colorScheme.onBackground,
                    onValueChange = {
                        title = it
                        markEdited(titleOverride = it)
                    }
                )
            } else {
                Text(
                    title.ifBlank { "未命名笔记" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        onClick = {},
                        onDoubleClick = { enterEdit(InlineEditTarget.TITLE) }
                    )
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (markdownActive) "Markdown" else "纯文本",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                if (detailMode == DetailMode.EDIT) {
                                    formatMode = formatMode.next()
                                    markEdited()
                                }
                            }
                            .padding(vertical = 4.dp)
                    )
                    Text(" · ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { showFolderPicker = true }
                            .padding(vertical = 4.dp)
                    )
                    if (pinned) {
                        Text(
                            text = " · ★",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { pinned = !pinned; markEdited() }
                                .padding(vertical = 4.dp)
                        )
                    }
                    if (topPinned) {
                        Text(
                            text = " · ↑",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    formatDate(note?.updatedAt ?: System.currentTimeMillis()),
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f))
            if (detailMode == DetailMode.EDIT) {
                if (markdownActive) MarkdownSyntaxHint()
                NativeNoteEditor(
                    value = contentValue,
                    externalModelKey = note?.let { it.id to it.updatedAt } ?: seed,
                    focusRequest = nativeContentFocusRequest,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textColor = MaterialTheme.colorScheme.onBackground,
                    onViewReady = { nativeContentEditor = it },
                    onValueChange = { value ->
                        contentValue = value
                        markEdited(contentOverride = value.text)
                    }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (note != null) TextButton(onClick = { onDelete(note) }) { Text("删除", color = MaterialTheme.colorScheme.error) } else Spacer(Modifier.width(1.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(onClick = { topPinned = !topPinned; markEdited() }) {
                            Text("置顶", color = if (topPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { pinned = !pinned; markEdited() }) {
                            Text("星标", color = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (markdownActive) {
                MarkdownPreview(
                    content,
                    modifier = Modifier.fillMaxWidth(),
                    onLinkClick = ::openPreviewLink,
                    onDoubleClickAt = { position -> enterEdit(InlineEditTarget.CONTENT, position) }
                )
            } else {
                var plainTextLayout by remember(content) { mutableStateOf<TextLayoutResult?>(null) }
                val linkColor = MaterialTheme.colorScheme.primary
                val previewText = remember(content, linkColor) {
                    linkifyPlainText(content.ifBlank { "空白笔记" }, linkColor)
                }
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyLarge,
                    onTextLayout = { plainTextLayout = it },
                    modifier = Modifier.fillMaxWidth().pointerInput(content) {
                        detectTapGestures(
                            onTap = { offset ->
                                plainTextLayout?.let { layout ->
                                    extractLinkAt(content, layout.getOffsetForPosition(offset))?.let(::openPreviewLink)
                                }
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
            Spacer(Modifier.height(48.dp))
                }
                if (detailMode == DetailMode.PREVIEW) {
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
    if (showFolderPicker) {
        FolderPickerDialog(
            folders = folders,
            selectedFolderId = selectedFolderId,
            onDismiss = { showFolderPicker = false },
            onSelected = { folderId ->
                selectedFolderId = folderId
                markEdited()
                showFolderPicker = false
            }
        )
    }
}

@Composable
private fun NativeNoteTitleEditor(
    value: String,
    focusRequest: Int,
    modifier: Modifier = Modifier,
    textColor: Color,
    onValueChange: (String) -> Unit
) {
    val latestValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            EditText(viewContext).apply {
                background = null
                setPadding(0, 0, 0, 0)
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))
                isSingleLine = true
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        val changed = s?.toString().orEmpty()
                        if (changed != latestValue) latestOnValueChange(changed)
                    }
                    override fun afterTextChanged(s: Editable?) = Unit
                })
            }
        },
        update = { view ->
            if (view.text.toString() != value) {
                view.setText(value)
                view.setSelection(value.length)
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

private class NativeNoteEditText(context: Context) : EditText(context) {
    /** 仅在外部模型主动替换编辑内容时为 true，防止 TextWatcher 把回填误判为用户输入。 */
    var applyingModelText: Boolean = false
    /** 当前 View 已接收的外部模型身份；普通 Compose 重组不会改变它。 */
    var appliedExternalModelKey: Any? = null
    var lastFocusRequest: Int = -1

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
private fun NativeNoteEditor(
    value: TextFieldValue,
    externalModelKey: Any?,
    focusRequest: Int,
    modifier: Modifier = Modifier,
    textColor: Color,
    onViewReady: (NativeNoteEditText) -> Unit = {},
    onValueChange: (TextFieldValue) -> Unit
) {
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            NativeNoteEditText(viewContext).apply {
                background = null
                setPadding(0, 0, 0, 0)
                includeFontPadding = false
                gravity = Gravity.TOP or Gravity.START
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                minLines = 6
                maxLines = Int.MAX_VALUE
                isSingleLine = false
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
private fun EmptyNotes(query: String, onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (query.isBlank()) "从一条笔记开始" else "没有匹配的笔记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun DraggableDetailScrollbar(
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

@Composable
private fun DraggableNoteListScrollbar(
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
    val itemSpacingPx = with(density) { 10.dp.toPx() }
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
private fun NoteCard(
    note: NoteEntity,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleTopPin: () -> Unit
) {
    val summary = remember(note.id, note.updatedAt, note.isMarkdown) {
        NoteListPreviewCache.get(note)
    }
    val formattedDate = remember(note.updatedAt) { formatDate(note.updatedAt) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f) else MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.58f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).heightIn(min = 104.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
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
                    summary,
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
private fun BatchDeleteDialog(
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

@Composable
private fun ConfirmDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) = ConfirmationDialog(
    title = "删除这条笔记？",
    message = "删除后无法恢复。",
    confirmLabel = "删除",
    destructive = true,
    onDismiss = onDismiss,
    onConfirm = onConfirm
)

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified)
            }
        }
    )
}

@Composable
private fun ImportErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入未完成") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } }
    )
}

private val NoteColors = listOf(0xFFF5F0E8, 0xFFF5ECEB, 0xFFEDF3F5, 0xFFEEF5F0, 0xFFF5F1E3)


private fun formatDate(time: Long): String = requireNotNull(NoteDateFormatter.get()).format(Date(time))
