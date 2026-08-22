package com.example.anjiannotes

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anjiannotes.data.DEFAULT_FOLDER_ID
import com.example.anjiannotes.data.FolderEntity
import com.example.anjiannotes.data.NoteEntity
import com.example.anjiannotes.data.NotesRepository
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
import com.example.anjiannotes.ui.previewLinkColor
import com.example.anjiannotes.ui.readTextImport
import com.example.anjiannotes.ui.theme.AnJianTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AnJianApplication
        val factory = NotesViewModelFactory(NotesRepository(app.database, app.database.noteDao(), app.database.folderDao()))
        setContent {
            AnJianTheme {
                val notesViewModel: NotesViewModel = viewModel(factory = factory)
                NotesApp(notesViewModel)
            }
        }
    }
}

private enum class InlineEditTarget { TITLE, CONTENT }
private enum class DetailMode { PREVIEW, EDIT }

private sealed interface AppPage {
    data object List : AppPage
    data class Detail(
        val note: NoteEntity?,
        val seed: EditorSeed = EditorSeed(),
        val folderId: Long = DEFAULT_FOLDER_ID
    ) : AppPage
    data object Settings : AppPage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesApp(viewModel: NotesViewModel) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val activeFolderId by viewModel.activeFolderId.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf<AppPage>(AppPage.List) }
    var showSearch by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showBackupMenu by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var pendingBackupPayload by remember { mutableStateOf<String?>(null) }
    var pendingRestorePayload by remember { mutableStateOf<String?>(null) }
    var pendingTextBackupPayload by remember { mutableStateOf<String?>(null) }
    var pendingTextRestorePayload by remember { mutableStateOf<String?>(null) }

    fun openImported(note: EditorSeed) {
        page = AppPage.Detail(note = null, seed = note, folderId = activeFolderId)
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

    BackHandler(enabled = page !is AppPage.List) { page = AppPage.List }

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
            onNewNote = { page = AppPage.Detail(note = null, folderId = activeFolderId) },
            onImportFile = { fileLauncher.launch(arrayOf("text/plain", "text/markdown", "text/x-markdown", "text/*")) },
            onImportClipboard = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val recentText = clipboard.primaryClip?.let { clip ->
                    (0 until clip.itemCount)
                        .asSequence()
                        .mapNotNull { index -> clip.getItemAt(index).coerceToText(context)?.toString() }
                        .firstOrNull { it.isNotBlank() }
                }.orEmpty()
                page = AppPage.Detail(note = null, seed = EditorSeed("剪切板笔记", recentText, NoteFormatMode.AUTO), folderId = activeFolderId)
            },
            onOpenNote = { note -> page = AppPage.Detail(note = note, folderId = note.folderId) }
        )
        AppPage.Settings -> SettingsPage(
            onBack = { page = AppPage.List },
            onBackupClick = { showBackupMenu = true }
        )
        is AppPage.Detail -> NoteDetailPage(
            note = currentPage.note,
            seed = currentPage.seed,
            initialFolderId = currentPage.folderId,
            folders = folders,
            onBack = { page = AppPage.List },
            onSave = { id, title, content, color, pinned, markdown, folderId, createdAt ->
                viewModel.saveNote(id, title, content, color, pinned, markdown, folderId, createdAt)
                if (currentPage.note == null) page = AppPage.List
            },
            onDelete = { note -> noteToDelete = note },
            onMoveFolder = viewModel::moveNoteToFolder
        )
    }

    noteToDelete?.let { note ->
        ConfirmDeleteDialog(
            onDismiss = { noteToDelete = null },
            onConfirm = { viewModel.deleteNote(note.id); noteToDelete = null; page = AppPage.List }
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
            onExportJson = {
                showBackupMenu = false
                viewModel.createBackup(
                    onSuccess = { backup ->
                        pendingBackupPayload = backup
                        backupSaveLauncher.launch("an-jian-backup-${System.currentTimeMillis()}.json")
                    },
                    onFailure = { message -> feedbackMessage = message }
                )
            },
            onImportJson = {
                showBackupMenu = false
                backupOpenLauncher.launch(arrayOf("application/json", "text/plain"))
            },
            onExportText = {
                showBackupMenu = false
                viewModel.createTextBackup(
                    onSuccess = { backup ->
                        pendingTextBackupPayload = backup
                        textBackupSaveLauncher.launch("an-jian-backup-${System.currentTimeMillis()}.txt")
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
    onOpenNote: (NoteEntity) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val activeFolderName = folders.firstOrNull { it.id == activeFolderId }?.name ?: "全部笔记"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.82f)) {
                Column(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Text("安笺", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("我的收藏夹", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            NavigationDrawerItem(
                                label = { Text("＋ 新建收藏夹") },
                                selected = false,
                                onClick = {
                                    onCreateFolder()
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                        items(folders.size, key = { folders[it].id }) { index ->
                            val folder = folders[index]
                            NavigationDrawerItem(
                                label = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                selected = folder.id == activeFolderId,
                                onClick = {
                                    onFolderSelected(folder.id)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text("⚙  设置") },
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
                            Text("☰", fontSize = 24.sp)
                        }
                    },
                    actions = {
                        TextButton(onClick = onSearchToggle) { Text(if (showSearch) "收起" else "搜索") }
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
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
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
                            NoteCard(note = notes[index], onOpen = { onOpenNote(notes[index]) })
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
    onBackupClick: () -> Unit
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("数据", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onBackupClick),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("备份与恢复", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("导出或恢复全部收藏夹与笔记，支持 JSON 和 TXT 明文备份。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("使用说明", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("安笺", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("离线保存，轻写轻放。笔记内容仅保存在本机，建议定期使用备份与恢复功能保存副本。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupMenuDialog(
    onDismiss: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onExportText: () -> Unit,
    onImportText: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("备份与恢复") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("备份包含全部收藏夹和笔记。恢复会替换当前本地数据。")
                TextButton(onClick = onExportJson, modifier = Modifier.fillMaxWidth()) { Text("导出 JSON 备份", modifier = Modifier.fillMaxWidth()) }
                TextButton(onClick = onImportJson, modifier = Modifier.fillMaxWidth()) { Text("导入 JSON 备份", modifier = Modifier.fillMaxWidth()) }
                HorizontalDivider()
                TextButton(onClick = onExportText, modifier = Modifier.fillMaxWidth()) { Text("导出 TXT 明文备份", modifier = Modifier.fillMaxWidth()) }
                TextButton(onClick = onImportText, modifier = Modifier.fillMaxWidth()) { Text("导入 TXT 明文备份", modifier = Modifier.fillMaxWidth()) }
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
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.weight(1f).clickable(onClick = onStartWriting),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    "写点什么…",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                TextButton(onClick = { onExpandedChange(!expanded) }) {
                    Text("⋮", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
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
    onSave: (Long, String, String, Long, Boolean, Boolean, Long, Long) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onMoveFolder: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    var title by remember(note?.id, seed) { mutableStateOf(note?.title ?: seed.title) }
    var contentValue by remember(note?.id, seed) { mutableStateOf(TextFieldValue(note?.content ?: seed.content)) }
    val content = contentValue.text
    var color by remember(note?.id) { mutableStateOf(note?.color ?: NoteColors.first()) }
    var pinned by remember(note?.id) { mutableStateOf(note?.isPinned ?: false) }
    var selectedFolderId by remember(note?.id, initialFolderId) { mutableStateOf(note?.folderId ?: initialFolderId) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var formatMode by remember(note?.id, seed) { mutableStateOf(if (note != null) if (note.isMarkdown) NoteFormatMode.MARKDOWN else NoteFormatMode.PLAIN else seed.formatMode) }
    var detailMode by remember(note?.id, seed) {
        mutableStateOf(if (note == null) DetailMode.EDIT else DetailMode.PREVIEW)
    }
    var focusTarget by remember(note?.id, seed) {
        mutableStateOf(if (note == null) InlineEditTarget.CONTENT else InlineEditTarget.CONTENT)
    }
    var requestedContentCursor by remember(note?.id, seed) { mutableStateOf<Int?>(null) }
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

    fun toggleDetailMode() {
        if (detailMode == DetailMode.PREVIEW) {
            enterEdit(InlineEditTarget.CONTENT)
        } else {
            detailMode = DetailMode.PREVIEW
        }
    }

    fun openPreviewLink(link: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
    }

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
                title = { Text(if (note == null) "新建笔记" else "笔记详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                actions = {
                    TextButton(onClick = ::toggleDetailMode) {
                        Text(if (detailMode == DetailMode.PREVIEW) "编辑" else "预览")
                    }
                    if (detailMode == DetailMode.EDIT) {
                        TextButton(onClick = {
                            onSave(note?.id ?: 0, title, content, color, pinned, markdownActive, selectedFolderId, note?.createdAt ?: System.currentTimeMillis())
                        }) { Text("保存", fontWeight = FontWeight.SemiBold) }
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
                    onValueChange = { title = it },
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
                        onClick = { if (detailMode == DetailMode.EDIT) formatMode = formatMode.next() },
                        label = { Text("格式：$formatDetail") }
                    )
                    AssistChip(onClick = { showFolderPicker = true }, label = { Text("收藏夹：$folderName") })
                    if (pinned) AssistChip(onClick = {}, label = { Text("已置顶") })
                    Text(formatDate(note?.updatedAt ?: System.currentTimeMillis()), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            if (detailMode == DetailMode.EDIT) {
                if (markdownActive) MarkdownSyntaxHint()
                BasicTextField(
                    value = contentValue,
                    onValueChange = { value -> contentValue = value },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp).focusRequester(contentFocus),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    minLines = 6
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (note != null) TextButton(onClick = { onDelete(note) }) { Text("删除", color = MaterialTheme.colorScheme.error) } else Spacer(Modifier.width(1.dp))
                    TextButton(onClick = { pinned = !pinned }) { Text(if (pinned) "取消置顶" else "置顶") }
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
                val linkColor = previewLinkColor(isSystemInDarkTheme())
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
                if (note != null) onMoveFolder(note.id, folderId)
                showFolderPicker = false
            }
        )
    }
}

@Composable
private fun EmptyNotes(query: String, onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (query.isBlank()) "还没有笔记" else "没有匹配的笔记", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(if (query.isBlank()) "从一条简短记录开始吧。" else "尝试换个关键词，或清除搜索条件。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (query.isBlank()) ElevatedButton(onClick = onCreate) { Text("写下第一条") }
        }
    }
}

@Composable
private fun NoteCard(note: NoteEntity, onOpen: () -> Unit) {
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (note.isMarkdown) {
                    Text("MD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                }
                Text(
                    if (note.isPinned) "★" else "☆",
                    fontSize = 22.sp,
                    color = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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
