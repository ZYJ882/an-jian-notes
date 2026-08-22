package com.example.anjiannotes

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anjiannotes.data.NoteEntity
import com.example.anjiannotes.data.NotesRepository
import com.example.anjiannotes.ui.EditorSeed
import com.example.anjiannotes.ui.ImportReadResult
import com.example.anjiannotes.ui.MarkdownPreview
import com.example.anjiannotes.ui.MarkdownSyntaxHint
import com.example.anjiannotes.ui.NoteFormatMode
import com.example.anjiannotes.ui.looksLikeMarkdown
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesApp(viewModel: NotesViewModel) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<NoteEntity?>(null) }
    var editorSeed by remember { mutableStateOf(EditorSeed()) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    fun openSeed(seed: EditorSeed) {
        selectedNote = null
        editorSeed = seed
        showEditor = true
    }

    val markdownFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        when (val result = readTextImport(context, uri)) {
            is ImportReadResult.Success -> openSeed(EditorSeed(result.note.title, result.note.content, NoteFormatMode.MARKDOWN))
            is ImportReadResult.Failure -> importError = result.message
        }
    }
    val textFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        when (val result = readTextImport(context, uri)) {
            is ImportReadResult.Success -> openSeed(EditorSeed(result.note.title, result.note.content, NoteFormatMode.PLAIN))
            is ImportReadResult.Failure -> importError = result.message
        }
    }

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
                actions = {
                    TextButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) viewModel.setSearchQuery("")
                    }) { Text(if (showSearch) "收起" else "搜索") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            CreateMenu(
                expanded = showCreateMenu,
                onExpandedChange = { showCreateMenu = it },
                onNewNote = { openSeed(EditorSeed()) },
                onImportMarkdown = { markdownFileLauncher.launch(arrayOf("text/markdown", "text/x-markdown", "text/plain")) },
                onImportText = { textFileLauncher.launch(arrayOf("text/plain")) },
                onImportClipboard = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                    if (text.isNullOrBlank()) {
                        importError = "剪切板中没有可导入的文本"
                    } else {
                        openSeed(EditorSeed("剪切板笔记", text, NoteFormatMode.AUTO))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            AnimatedVisibility(
                visible = showSearch,
                enter = expandVertically(animationSpec = tween(140)) + fadeIn(animationSpec = tween(100)),
                exit = shrinkVertically(animationSpec = tween(120)) + fadeOut(animationSpec = tween(80))
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    label = { Text("搜索标题、正文或标签") },
                    singleLine = true
                )
            }
            if (notes.isEmpty()) {
                EmptyNotes(query = query, onCreate = { openSeed(EditorSeed()) })
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            "共 ${notes.size} 条笔记",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    items(notes.size, key = { notes[it].id }) { index ->
                        val note = notes[index]
                        NoteCard(
                            note = note,
                            onOpen = { selectedNote = note; editorSeed = EditorSeed(); showEditor = true },
                            onTogglePinned = { viewModel.togglePinned(note) }
                        )
                    }
                    item { Spacer(Modifier.height(84.dp)) }
                }
            }
        }
    }

    if (showEditor) {
        NoteEditorDialog(
            note = selectedNote,
            seed = editorSeed,
            onDismiss = { showEditor = false },
            onSave = { id, title, content, tags, color, pinned, markdown, createdAt ->
                viewModel.saveNote(id, title, content, tags, color, pinned, markdown, createdAt)
                showEditor = false
            },
            onDelete = { selectedNote?.let { noteToDelete = it }; showEditor = false }
        )
    }
    noteToDelete?.let { note ->
        ConfirmDeleteDialog(
            onDismiss = { noteToDelete = null },
            onConfirm = { viewModel.deleteNote(note.id); noteToDelete = null }
        )
    }
    importError?.let { message ->
        ImportErrorDialog(message = message, onDismiss = { importError = null })
    }
}

@Composable
private fun CreateMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNewNote: () -> Unit,
    onImportMarkdown: () -> Unit,
    onImportText: () -> Unit,
    onImportClipboard: () -> Unit
) {
    Box {
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text("+", fontSize = 30.sp, fontWeight = FontWeight.Light)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            CreateMenuItem("新建笔记", "手写内容，自动识别 Markdown", onClick = { onExpandedChange(false); onNewNote() })
            CreateMenuItem("导入 Markdown", "选择 .md 或 Markdown 文本", onClick = { onExpandedChange(false); onImportMarkdown() })
            CreateMenuItem("导入文本", "选择标准 .txt 文本文件", onClick = { onExpandedChange(false); onImportText() })
            CreateMenuItem("从剪切板导入", "读取当前剪切板中的文本", onClick = { onExpandedChange(false); onImportClipboard() })
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
private fun NoteCard(note: NoteEntity, onOpen: () -> Unit, onTogglePinned: () -> Unit) {
    val summary = remember(note.id, note.content, note.isMarkdown) {
        if (note.isMarkdown) markdownToPlainText(note.content) else note.content
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(120))
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = Color(note.color)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = note.title.ifBlank { "未命名笔记" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.isMarkdown) {
                    Text("MD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                }
                TextButton(onClick = onTogglePinned, modifier = Modifier.wrapContentWidth()) { Text(if (note.isPinned) "已置顶" else "置顶") }
            }
            if (summary.isNotBlank()) {
                Text(summary, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (note.tags.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    note.tags.split(',').filter { it.isNotBlank() }.forEach { tag ->
                        AssistChip(onClick = onOpen, label = { Text("#$tag") })
                    }
                }
            }
            Text(formatDate(note.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NoteEditorDialog(
    note: NoteEntity?,
    seed: EditorSeed,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, Long, Boolean, Boolean, Long) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember(note?.id, seed) { mutableStateOf(note?.title ?: seed.title) }
    var content by remember(note?.id, seed) { mutableStateOf(note?.content ?: seed.content) }
    var tags by remember(note?.id) { mutableStateOf(note?.tags.orEmpty()) }
    var color by remember(note?.id) { mutableStateOf(note?.color ?: NoteColors.first()) }
    var pinned by remember(note?.id) { mutableStateOf(note?.isPinned ?: false) }
    var formatMode by remember(note?.id, seed) {
        mutableStateOf(if (note != null) if (note.isMarkdown) NoteFormatMode.MARKDOWN else NoteFormatMode.PLAIN else seed.formatMode)
    }
    var previewMode by remember(note?.id, seed) { mutableStateOf(false) }
    val markdownActive = formatMode.resolvesToMarkdown(content)
    val formatDetail = if (formatMode == NoteFormatMode.AUTO) if (markdownActive) "识别为 Markdown" else "识别为纯文本" else formatMode.label

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (note == null) "新建笔记" else "编辑笔记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { pinned = !pinned }) { Text(if (pinned) "取消置顶" else "置顶") }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { formatMode = formatMode.next(); previewMode = false },
                        label = { Text("格式：$formatDetail") }
                    )
                    if (markdownActive) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { previewMode = false }) { Text("编辑", fontWeight = if (!previewMode) FontWeight.Bold else FontWeight.Normal) }
                        TextButton(onClick = { previewMode = true }) { Text("预览", fontWeight = if (previewMode) FontWeight.Bold else FontWeight.Normal) }
                    }
                }
                if (markdownActive && !previewMode) MarkdownSyntaxHint()
                if (markdownActive && previewMode) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 176.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        MarkdownPreview(content, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text(if (markdownActive) "Markdown 正文" else "正文") },
                        modifier = Modifier.fillMaxWidth().height(176.dp),
                        minLines = 6
                    )
                }
                OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签（用逗号分隔）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("笔记颜色", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoteColors.forEach { option ->
                        Box(
                            modifier = Modifier
                                .size(if (color == option) 32.dp else 26.dp)
                                .clip(CircleShape)
                                .background(Color(option))
                                .clickable { color = option },
                            contentAlignment = Alignment.Center
                        ) { if (color == option) Text("✓", color = Color(0xFF514A42), fontWeight = FontWeight.Bold) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (note != null) TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) } else Spacer(Modifier.width(1.dp))
                    Row {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        ElevatedButton(onClick = {
                            onSave(note?.id ?: 0, title, content, tags, color, pinned, markdownActive, note?.createdAt ?: System.currentTimeMillis())
                        }) { Text("保存") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("删除这条笔记？", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("删除后无法恢复。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = onConfirm) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun ImportErrorDialog(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("导入未完成", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("知道了") }
                }
            }
        }
    }
}

private val NoteColors = listOf(0xFFF5F0E8, 0xFFF5ECEB, 0xFFEDF3F5, 0xFFEEF5F0, 0xFFF5F1E3)
private fun formatDate(time: Long): String = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Date(time))
