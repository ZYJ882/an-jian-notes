package com.example.anjiannotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.anjiannotes.data.NoteEntity
import com.example.anjiannotes.data.NotesDatabase
import com.example.anjiannotes.data.NotesRepository
import com.example.anjiannotes.ui.theme.AnJianTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(applicationContext, NotesDatabase::class.java, "an_jian_notes.db").build()
        val factory = NotesViewModelFactory(NotesRepository(database.noteDao()))
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
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("安笺", fontWeight = FontWeight.Bold)
                        Text("记录此刻，整理思绪", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) viewModel.setSearchQuery("")
                    }) { Text(if (showSearch) "关闭" else "搜索") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { selectedNote = null; showEditor = true }) {
                Text("新建", modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    label = { Text("搜索标题、正文或标签") },
                    singleLine = true
                )
            }
            if (notes.isEmpty()) {
                EmptyNotes(query = query, onCreate = { selectedNote = null; showEditor = true })
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { Text("共 ${notes.size} 条笔记", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(notes.size, key = { notes[it].id }) { index ->
                        val note = notes[index]
                        NoteCard(
                            note = note,
                            onOpen = { selectedNote = note; showEditor = true },
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
            onDismiss = { showEditor = false },
            onSave = { id, title, content, tags, color, pinned, createdAt ->
                viewModel.saveNote(id, title, content, tags, color, pinned, createdAt)
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
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
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
                TextButton(onClick = onTogglePinned, modifier = Modifier.wrapContentWidth()) { Text(if (note.isPinned) "已置顶" else "置顶") }
            }
            if (note.content.isNotBlank()) {
                Text(note.content, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, Long, Boolean, Long) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var tags by remember(note?.id) { mutableStateOf(note?.tags.orEmpty()) }
    var color by remember(note?.id) { mutableStateOf(note?.color ?: NoteColors.first()) }
    var pinned by remember(note?.id) { mutableStateOf(note?.isPinned ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (note == null) "新建笔记" else "编辑笔记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { pinned = !pinned }) { Text(if (pinned) "取消置顶" else "置顶") }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("正文") },
                    modifier = Modifier.fillMaxWidth().height(176.dp),
                    minLines = 6
                )
                OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签（用逗号分隔）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("笔记颜色", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoteColors.forEach { option ->
                        Box(
                            modifier = Modifier
                                .size(if (color == option) 32.dp else 26.dp)
                                .clip(CircleShape)
                                .background(Color(option))
                                .clickable { color = option },
                            contentAlignment = Alignment.Center
                        ) { if (color == option) Text("✓", color = Color(0xFF3B332A), fontWeight = FontWeight.Bold) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (note != null) TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) } else Spacer(Modifier.width(1.dp))
                    Row {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        ElevatedButton(onClick = { onSave(note?.id ?: 0, title, content, tags, color, pinned, note?.createdAt ?: System.currentTimeMillis()) }) { Text("保存") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
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

private val NoteColors = listOf(0xFFFFF8F0, 0xFFFFF0F0, 0xFFF1F8FF, 0xFFF0FAF3, 0xFFFFF8D9)
private fun formatDate(time: Long): String = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Date(time))
