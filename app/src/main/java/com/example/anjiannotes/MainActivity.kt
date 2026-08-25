package com.example.anjiannotes

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.anjiannotes.ui.markdownToPlainText
import com.example.anjiannotes.ui.readTextImport
import com.example.anjiannotes.ui.theme.AnJianTheme
import com.example.anjiannotes.ui.theme.AppearanceMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AnJianApplication
        val factory = NotesViewModelFactory(
            NotesRepository(app.database, app.database.noteDao(), app.database.folderDao()),
            app.webDavConfigStore,
            app.webDavBackupClient
        )
        setContent {
            val appearanceMode by app.appearancePreferences.mode.collectAsStateWithLifecycle()
            AnJianTheme(
                appearanceMode = appearanceMode,
                systemDarkTheme = isSystemInDarkTheme()
            ) {
                val notesViewModel: NotesViewModel = viewModel(factory = factory)
                NotesApp(
                    viewModel = notesViewModel,
                    appearanceMode = appearanceMode,
                    onAppearanceChange = app.appearancePreferences::setMode
                )
            }
        }
    }
}

private enum class InlineEditTarget { TITLE, CONTENT }
private enum class DetailMode { PREVIEW, EDIT }
private enum class AutoSaveState { IDLE, SAVING, SAVED }

/** 每次编辑事件同步生成的不可变保存快照，避免组合重绘前读取到旧文本。 */
private data class NoteDraftSnapshot(
    val title: String,
    val content: String,
    val color: Long,
    val isPinned: Boolean,
    val isMarkdown: Boolean,
    val folderId: Long
)

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

    fun writableFolderId(): Long =
        activeFolderId.takeUnless { it == STARRED_FOLDER_ID } ?: DEFAULT_FOLDER_ID

    fun openImported(note: EditorSeed) {
        page = AppPage.Detail(note = null, seed = note, folderId = writableFolderId())
    }

    val backupSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = pendingBackupPayload
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer -> writer.write(payload) }
                    ?: error("无法写入所选位置")
            }.onSuccess {
                feedbackMessage = "备份已导出"
            }.onFailure {
                feedbackMessage = "备份导出失败：${it.message ?: "无法写入文件"}"
            }
        }
        pendingBackupPayload = null
    }

    val textBackupSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val payload = pendingTextBackupPayload
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer -> writer.write(payload) }
                    ?: error("无法写入所选位置")
            }.onSuccess {
                feedbackMessage = "TXT 明文备份已导出"
            }.onFailure {
                feedbackMessage = "TXT 备份导出失败：${it.message ?: "无法写入文件"}"
            }
        }
        pendingTextBackupPayload = null
    }

    val backupOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("无法读取所选文件")
            }.onSuccess { raw ->
                pendingRestorePayload = raw
            }.onFailure {
                feedbackMessage = "备份文件读取失败：${it.message ?: "无法读取文件"}"
            }
        }
    }

    val textBackupOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("无法读取所选文件")
            }.onSuccess { raw ->
                pendingTextRestorePayload = raw
            }.onFailure {
                feedbackMessage = "TXT 备份文件读取失败：${it.message ?: "无法读取文件"}"
            }
        }
    }

    val markdownZipSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val payload = pendingMarkdownZipPayload
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream -> stream.write(payload) }
                    ?: error("无法写入所选位置")
            }.onSuccess {
                feedbackMessage = "Markdown ZIP 备份已导出"
            }.onFailure {
                feedbackMessage = "ZIP 备份导出失败：${it.message ?: "无法写入文件"}"
            }
        }
        pendingMarkdownZipPayload = null
    }

    val markdownZipOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("无法读取所选文件")
            }.onSuccess { payload ->
                pendingMarkdownZipRestorePayload = payload
            }.onFailure {
                feedbackMessage = "ZIP 备份读取失败：${it.message ?: "无法读取文件"}"
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        when (val result = readTextImport(context, uri)) {
            is ImportReadResult.Success -> {
                val sourceName = uri.lastPathSegment.orEmpty()
                openImported(EditorSeed(result.note.title, result.note.content, formatForFileName(sourceName)))
            }
            is ImportReadResult.Failure -> importError = result.message
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
            onSearchToggle = {
                showSearch = !showSearch
                if (!showSearch) viewModel.setSearchQuery("")
            },
            onSearchChange = viewModel::setSearchQuery,
            onFolderSelected = viewModel::selectFolder,
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
            onDeleteFolder = { folderToDelete = it }
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
            onBack = { page = AppPage.List },
            onSave = { id, title, content, color, pinned, markdown, folderId, createdAt ->
                viewModel.saveNote(id, title, content, color, pinned, markdown, folderId, createdAt)
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
    onOpenNote: (NoteEntity) -> Unit,
    onToggleStar: (NoteEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val activeFolderName = folders.firstOrNull { it.id == activeFolderId }?.name ?: "全部笔记"

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
                        Column {
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
                        Column {
                            Text(activeFolderName, fontWeight = FontWeight.Bold)
                            Text("轻写，轻放", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        TextButton(onClick = {
                            scope.launch {
                                if (drawerState.currentValue == DrawerValue.Closed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Text("☰", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        TextButton(onClick = onSearchToggle) {
                            Text(if (showSearch) "×" else "⌕", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)
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
                if (showSearch) {
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
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item { Text("共 ${notes.size} 条笔记", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp)) }
                        items(notes.size, key = { notes[it].id }) { index ->
                            NoteCard(
                                note = notes[index],
                                onOpen = { onOpenNote(notes[index]) },
                                onToggleStar = { onToggleStar(notes[index]) }
                            )
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }
                }
            }
        }
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
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除收藏夹？") },
        text = {
            Text("“${folder.name}”中的笔记会移动到默认收藏夹，星标状态保持不变。此收藏夹随后将被删除。")
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(onClick = onConfirm) { Text("删除", color = MaterialTheme.colorScheme.error) } }
    )
}

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
            verticalArrangement = Arrangement.spacedBy(22.dp)
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
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable(onClick = item.onClick)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(item.symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(item.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                            modifier = Modifier.padding(start = 68.dp)
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
private fun RestoreTextBackupDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("恢复 TXT 明文备份？") },
        text = { Text("恢复会以 TXT 备份中的收藏夹和笔记替换当前本地数据，此操作无法撤销。") },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确认恢复") } }
    )
}

@Composable
private fun RestoreBackupDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("恢复备份？") },
        text = { Text("恢复会以备份文件中的收藏夹和笔记替换当前本地数据，此操作无法撤销。") },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确认恢复") } }
    )
}

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
                        elevation = 8.dp,
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
                            elevation = 7.dp,
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
    onBack: () -> Unit,
    onSave: suspend (Long, String, String, Long, Boolean, Boolean, Long, Long) -> Long,
    onDelete: (NoteEntity) -> Unit
) {
    val context = LocalContext.current
    var title by remember(note?.id, seed) { mutableStateOf(note?.title ?: seed.title) }
    var contentValue by remember(note?.id, seed) { mutableStateOf(TextFieldValue(note?.content ?: seed.content)) }
    val content = contentValue.text
    var color by remember(note?.id) { mutableStateOf(note?.color ?: NoteColors.first()) }
    var pinned by remember(note?.id) { mutableStateOf(note?.isPinned ?: false) }
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
                isMarkdown = formatMode.resolvesToMarkdown(initialContent),
                folderId = note?.folderId ?: initialFolderId
            )
        )
    }
    var detailMode by remember(note?.id, seed) {
        mutableStateOf(if (note == null) DetailMode.EDIT else DetailMode.PREVIEW)
    }
    var focusTarget by remember(note?.id, seed) {
        mutableStateOf(if (note == null) InlineEditTarget.CONTENT else InlineEditTarget.CONTENT)
    }
    var requestedContentCursor by remember(note?.id, seed) { mutableStateOf<Int?>(null) }
    var savedNoteId by remember(note?.id, seed) { mutableStateOf(note?.id ?: 0L) }
    val draftCreatedAt = remember(note?.id, seed) { note?.createdAt ?: System.currentTimeMillis() }
    val seedHasContent = note == null && (seed.title.isNotBlank() || seed.content.isNotBlank())
    var editRevision by remember(note?.id, seed) { mutableStateOf(if (seedHasContent) 1 else 0) }
    var savedRevision by remember(note?.id, seed) { mutableStateOf(0) }
    var hasUserEdited by remember(note?.id, seed) { mutableStateOf(seedHasContent) }
    var autoSaveState by remember(note?.id, seed) { mutableStateOf(AutoSaveState.IDLE) }
    var saveError by remember(note?.id, seed) { mutableStateOf<String?>(null) }
    var leavingInProgress by remember(note?.id, seed) { mutableStateOf(false) }
    var saveWorker by remember(note?.id, seed) { mutableStateOf<Job?>(null) }
    var debounceJob by remember(note?.id, seed) { mutableStateOf<Job?>(null) }
    val editorScope = rememberCoroutineScope()
    val titleFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val markdownActive = formatMode.resolvesToMarkdown(content)
    val formatDetail = if (formatMode == NoteFormatMode.AUTO) if (markdownActive) "自动：Markdown" else "自动：纯文本" else formatMode.label
    val folderName = folders.firstOrNull { it.id == selectedFolderId }?.name ?: "默认收藏夹"
    val detailScrollState = rememberScrollState()
    var pendingScrollRestore by remember(note?.id, seed) { mutableStateOf<Int?>(null) }

    LaunchedEffect(detailMode, focusTarget) {
        if (detailMode == DetailMode.EDIT) {
            when (focusTarget) {
                InlineEditTarget.TITLE -> titleFocus.requestFocus()
                InlineEditTarget.CONTENT -> {
                    requestedContentCursor?.let { position ->
                        contentValue = contentValue.copy(selection = TextRange(position.coerceIn(0, contentValue.text.length)))
                        requestedContentCursor = null
                    }
                    contentFocus.requestFocus()
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
        saveWorker = editorScope.launch {
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
                        draftToSave.isMarkdown,
                        draftToSave.folderId,
                        draftCreatedAt
                    )
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

    suspend fun flushPendingChanges(): Boolean {
        debounceJob?.cancel()
        editorLog("final save before leave dirty=${hasUserEdited && savedRevision < editRevision}")
        while (hasUserEdited && savedRevision < editRevision) {
            startSaveWorker()
            saveWorker?.join()
            if (saveError != null) return false
        }
        return true
    }

    fun handleBack() {
        if (leavingInProgress) {
            editorLog("back ignored while final save is in progress")
            return
        }
        editorLog("back requested mode=$detailMode")
        leavingInProgress = true
        keyboard?.hide()
        editorScope.launch {
            if (!flushPendingChanges()) {
                editorLog("back cancelled because final save failed")
                leavingInProgress = false
                return@launch
            }
            if (detailMode == DetailMode.EDIT) {
                // 第一次返回仅在最终保存完成后切换预览，页面仍保持在同一篇笔记。
                detailMode = DetailMode.PREVIEW
                editorLog("final save confirmed; switched to preview")
                withFrameNanos { }
                leavingInProgress = false
            } else {
                // 预览态再次返回时也会经过同一最终保存确认，再离开详情页。
                editorLog("final save confirmed; leaving detail")
                onBack()
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
        if (detailMode == DetailMode.PREVIEW) enterEdit(InlineEditTarget.CONTENT) else handleBack()
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

    val latestFinishEditing by rememberUpdatedState(::handleBack)
    // BackHandler 通过 AndroidX 回调覆盖物理返回键、三键导航及系统返回手势。
    BackHandler(enabled = true) { latestFinishEditing() }

    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
        )
    ) {
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
                    TextButton(onClick = ::handleBack) {
                        Text(if (detailMode == DetailMode.EDIT) "预览" else "返回")
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
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(detailScrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
            if (detailMode == DetailMode.EDIT) {
                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        markEdited(titleOverride = it)
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true
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
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 3
                ) {
                    AssistChip(
                        onClick = {
                            if (detailMode == DetailMode.EDIT) {
                                formatMode = formatMode.next()
                                markEdited()
                            }
                        },
                        label = { Text("格式：$formatDetail") }
                    )
                    AssistChip(onClick = { showFolderPicker = true }, label = { Text("收藏夹：$folderName") })
                    if (pinned) {
                        AssistChip(
                            onClick = { pinned = !pinned; markEdited() },
                            label = { Text("已星标") }
                        )
                    }

                    Text(formatDate(note?.updatedAt ?: System.currentTimeMillis()), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            if (detailMode == DetailMode.EDIT) {
                if (markdownActive) MarkdownSyntaxHint()
                BasicTextField(
                    value = contentValue,
                    onValueChange = { value ->
                        contentValue = value
                        markEdited(contentOverride = value.text)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp).focusRequester(contentFocus),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    minLines = 6
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (note != null) TextButton(onClick = { onDelete(note) }) { Text("删除", color = MaterialTheme.colorScheme.error) } else Spacer(Modifier.width(1.dp))
                    TextButton(onClick = { pinned = !pinned; markEdited() }) { Text(if (pinned) "取消星标" else "加入星标") }
                }
            } else if (markdownActive) {
                MarkdownPreview(
                    content,
                    modifier = Modifier.fillMaxWidth(),
                    onLinkClick = ::openPreviewLink,
                    onDoubleClickAt = { position -> enterEdit(InlineEditTarget.CONTENT, position) },
                    onClick = {}
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
private fun NoteCard(
    note: NoteEntity,
    onOpen: () -> Unit,
    onToggleStar: () -> Unit
) {
    val summary = remember(note.id, note.content, note.isMarkdown) {
        if (note.isMarkdown) markdownToPlainText(note.content) else note.content
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp).heightIn(min = 122.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                note.title.ifBlank { "未命名笔记" },
                style = MaterialTheme.typography.titleLarge,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (note.isMarkdown) {
                    Text("MD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f), modifier = Modifier.padding(end = 12.dp))
                }
                Text(
                    if (note.isPinned) "★" else "☆",
                    fontSize = 22.sp,
                    color = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onToggleStar)
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除这条笔记？") },
        text = { Text("删除后无法恢复。") },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(onClick = onConfirm) { Text("删除", color = MaterialTheme.colorScheme.error) } }
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


private fun formatDate(time: Long): String = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Date(time))
