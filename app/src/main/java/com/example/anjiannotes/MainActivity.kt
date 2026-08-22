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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anjiannotes.data.NoteEntity
import com.example.anjiannotes.data.NotesRepository
import com.example.anjiannotes.ui.EditorSeed
import com.example.anjiannotes.ui.ImportReadResult
import com.example.anjiannotes.ui.MarkdownPreview
import com.example.anjiannotes.ui.MarkdownSyntaxHint
import com.example.anjiannotes.ui.NoteFormatMode
import com.example.anjiannotes.ui.extractFirstLink
import com.example.anjiannotes.ui.formatForFileName
import com.example.anjiannotes.ui.markdownToPlainText
import com.example.anjiannotes.ui.readTextImport
import com.example.anjiannotes.ui.theme.AnJianTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AnJianApplication
        val factory = NotesViewModelFactory(NotesRepository(app.database.noteDao()))
        setContent {
            AnJianTheme {
                val notesViewModel: NotesViewModel = viewModel(factory = factory)
                NotesApp(notesViewModel)
            }
        }
    }
}

private sealed interface AppPage {
    data object List : AppPage
    data class Detail(val note: NoteEntity?, val seed: EditorSeed = EditorSeed()) : AppPage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesApp(viewModel: NotesViewModel) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf<AppPage>(AppPage.List) }
    var showSearch by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    fun openImported(note: EditorSeed) {
        page = AppPage.Detail(note = null, seed = note)
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

    AnimatedContent(targetState = page, transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(80)) }, label = "page") { currentPage ->
        when (currentPage) {
            AppPage.List -> NotesListPage(
                notes = notes,
                query = query,
                showSearch = showSearch,
                onSearchToggle = {
                    showSearch = !showSearch
                    if (!showSearch) viewModel.setSearchQuery("")
                },
                onSearchChange = viewModel::setSearchQuery,
                createMenuExpanded = showCreateMenu,
                onCreateMenuExpanded = { showCreateMenu = it },
                onNewNote = { page = AppPage.Detail(note = null) },
                onImportFile = { fileLauncher.launch(arrayOf("text/plain", "text/markdown", "text/x-markdown", "text/*")) },
                onImportClipboard = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val recentText = clipboard.primaryClip?.let { clip ->
                        (0 until clip.itemCount)
                            .asSequence()
                            .mapNotNull { index -> clip.getItemAt(index).coerceToText(context)?.toString() }
                            .firstOrNull { it.isNotBlank() }
                    }.orEmpty()
                    page = AppPage.Detail(note = null, seed = EditorSeed("剪切板笔记", recentText, NoteFormatMode.AUTO))
                },
                onOpenNote = { note -> page = AppPage.Detail(note = note) }
            )
            is AppPage.Detail -> NoteDetailPage(
                note = currentPage.note,
                seed = currentPage.seed,
                onBack = { page = AppPage.List },
                onSave = { id, title, content, tags, color, pinned, markdown, createdAt ->
                    viewModel.saveNote(id, title, content, tags, color, pinned, markdown, createdAt)
                    page = AppPage.List
                },
                onDelete = { note -> noteToDelete = note }
            )
        }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesListPage(
    notes: List<NoteEntity>,
    query: String,
    showSearch: Boolean,
    onSearchToggle: () -> Unit,
    onSearchChange: (String) -> Unit,
    createMenuExpanded: Boolean,
    onCreateMenuExpanded: (Boolean) -> Unit,
    onNewNote: () -> Unit,
    onImportFile: () -> Unit,
    onImportClipboard: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("安笺", fontWeight = FontWeight.Bold)
                        Text("轻写，轻放", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = { TextButton(onClick = onSearchToggle) { Text(if (showSearch) "收起" else "搜索") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            CreateMenu(
                expanded = createMenuExpanded,
                onExpandedChange = onCreateMenuExpanded,
                onNewNote = onNewNote,
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
                    label = { Text("搜索标题、正文或标签") },
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
                    item { Spacer(Modifier.height(84.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CreateMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNewNote: () -> Unit,
    onImportFile: () -> Unit,
    onImportClipboard: () -> Unit
) {
    Box {
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) { Text("+", fontSize = 30.sp, fontWeight = FontWeight.Light) }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            CreateMenuItem("新建笔记", "输入内容，自动识别 Markdown 与文本", onClick = { onExpandedChange(false); onNewNote() })
            CreateMenuItem("导入文件", "支持 .md、.markdown、.txt 与 UTF-8 文本", onClick = { onExpandedChange(false); onImportFile() })
            CreateMenuItem("从剪切板导入", "读取当前最近一条剪切板文本", onClick = { onExpandedChange(false); onImportClipboard() })
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NoteDetailPage(
    note: NoteEntity?,
    seed: EditorSeed,
    onBack: () -> Unit,
    onSave: (Long, String, String, String, Long, Boolean, Boolean, Long) -> Unit,
    onDelete: (NoteEntity) -> Unit
) {
    val context = LocalContext.current
    var title by remember(note?.id, seed) { mutableStateOf(note?.title ?: seed.title) }
    var content by remember(note?.id, seed) { mutableStateOf(note?.content ?: seed.content) }
    var tags by remember(note?.id) { mutableStateOf(note?.tags.orEmpty()) }
    var color by remember(note?.id) { mutableStateOf(note?.color ?: NoteColors.first()) }
    var pinned by remember(note?.id) { mutableStateOf(note?.isPinned ?: false) }
    var formatMode by remember(note?.id, seed) { mutableStateOf(if (note != null) if (note.isMarkdown) NoteFormatMode.MARKDOWN else NoteFormatMode.PLAIN else seed.formatMode) }
    var editing by remember(note?.id, seed) {
        mutableStateOf(note == null)
    }
    var pendingLink by remember { mutableStateOf<String?>(null) }
    val markdownActive = formatMode.resolvesToMarkdown(content)
    val formatDetail = if (formatMode == NoteFormatMode.AUTO) if (markdownActive) "自动：Markdown" else "自动：纯文本" else formatMode.label

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (note == null) "新建笔记" else "笔记详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                actions = {
                    if (markdownActive) {
                        TextButton(onClick = { editing = !editing }) { Text(if (editing) "预览" else "编辑") }
                    }
                    if (editing) TextButton(onClick = {
                        onSave(note?.id ?: 0, title, content, tags, color, pinned, markdownActive, note?.createdAt ?: System.currentTimeMillis())
                    }) { Text("保存", fontWeight = FontWeight.SemiBold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (editing) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                AssistChip(onClick = { formatMode = formatMode.next() }, label = { Text("格式：$formatDetail") })
                if (markdownActive) MarkdownSyntaxHint()
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (markdownActive) "Markdown 正文" else "正文") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp),
                    minLines = 10
                )
                PlainTextLinkHint(content = content, onLinkLongPress = { pendingLink = it })
                OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签（用逗号分隔）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("笔记颜色", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoteColors.forEach { option ->
                        Box(
                            modifier = Modifier.size(if (color == option) 32.dp else 26.dp).clip(CircleShape).background(Color(option)).clickable { color = option },
                            contentAlignment = Alignment.Center
                        ) { if (color == option) Text("✓", color = Color(0xFF514A42), fontWeight = FontWeight.Bold) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (note != null) TextButton(onClick = { onDelete(note) }) { Text("删除", color = MaterialTheme.colorScheme.error) } else Spacer(Modifier.width(1.dp))
                    TextButton(onClick = { pinned = !pinned }) { Text(if (pinned) "取消置顶" else "置顶") }
                }
            } else {
                Text(title.ifBlank { "未命名笔记" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("Markdown") })
                    if (pinned) AssistChip(onClick = {}, label = { Text("已置顶") })
                    Text(formatDate(note?.updatedAt ?: System.currentTimeMillis()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                if (markdownActive) {
                    MarkdownPreview(
                        content,
                        modifier = Modifier.fillMaxWidth(),
                        onLinkLongPress = { pendingLink = it },
                        onDoubleClick = { editing = true }
                    )
                } else {
                    Text(
                        text = content.ifBlank { "空白笔记" },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onDoubleClick = { editing = true })
                    )
                    PlainTextLinkHint(content = content, onLinkLongPress = { pendingLink = it })
                }
                if (tags.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        tags.split(',').filter { it.isNotBlank() }.forEach { tag -> AssistChip(onClick = {}, label = { Text("#$tag") }) }
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
        }
    }
    pendingLink?.let { link ->
        LinkActionDialog(
            link = link,
            onDismiss = { pendingLink = null },
            onOpen = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                pendingLink = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlainTextLinkHint(content: String, onLinkLongPress: (String) -> Unit) {
    val link = remember(content) { extractFirstLink(content) }
    if (link != null) {
        Surface(
            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { onLinkLongPress(link) }),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("检测到链接：长按此处后可选择跳转\n$link", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LinkActionDialog(link: String, onDismiss: () -> Unit, onOpen: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("打开链接？") },
        text = { Text("链接不会因点击而自动跳转。确认后将使用系统浏览器打开：\n$link") },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(onClick = onOpen) { Text("跳转") } }
    )
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
    val summary = remember(note.id, note.content, note.isMarkdown) { if (note.isMarkdown) markdownToPlainText(note.content) else note.content }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = Color(note.color)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(note.title.ifBlank { "未命名笔记" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note.isMarkdown) Text("MD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                if (note.isPinned) Text("置顶", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (summary.isNotBlank()) Text(summary, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDate(note.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
