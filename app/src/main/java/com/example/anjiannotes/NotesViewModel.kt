package com.example.anjiannotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.anjiannotes.data.BackupCodec
import com.example.anjiannotes.data.DEFAULT_FOLDER_ID
import com.example.anjiannotes.data.FolderEntity
import com.example.anjiannotes.data.NoteEntity
import com.example.anjiannotes.data.NotesRepository
import com.example.anjiannotes.data.PlainTextBackupCodec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NotesViewModel(private val repository: NotesRepository) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedFolderId = MutableStateFlow(DEFAULT_FOLDER_ID)

    val query: StateFlow<String> = searchQuery.asStateFlow()
    val activeFolderId: StateFlow<Long> = selectedFolderId.asStateFlow()
    val folders: StateFlow<List<FolderEntity>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = combine(
        searchQuery.debounce(80),
        selectedFolderId
    ) { query, folderId -> query to folderId }
        .flatMapLatest { (query, folderId) -> repository.observeNotes(query, folderId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.ensureDefaultFolder() }
    }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun selectFolder(folderId: Long) {
        selectedFolderId.value = folderId
    }

    fun createFolder(name: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val id = repository.createFolder(name)
                selectedFolderId.value = id
                name.trim()
            }.onSuccess(onSuccess)
                .onFailure { onFailure(it.message ?: "收藏夹创建失败，请重试") }
        }
    }

    fun createBackup(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { BackupCodec.encode(repository.exportSnapshot()) }
                .onSuccess(onSuccess)
                .onFailure { onFailure(it.message ?: "备份导出失败") }
        }
    }

    fun createTextBackup(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { PlainTextBackupCodec.encode(repository.exportSnapshot()) }
                .onSuccess(onSuccess)
                .onFailure { onFailure(it.message ?: "TXT 备份导出失败") }
        }
    }

    fun restoreBackup(rawBackup: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        restoreSnapshot(
            decode = { BackupCodec.decode(rawBackup) },
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun restoreTextBackup(rawBackup: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        restoreSnapshot(
            decode = { PlainTextBackupCodec.decode(rawBackup) },
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    private fun restoreSnapshot(
        decode: () -> com.example.anjiannotes.data.BackupSnapshot,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val snapshot = decode()
                repository.restoreSnapshot(snapshot)
                selectedFolderId.value = DEFAULT_FOLDER_ID
                searchQuery.value = ""
                "已恢复 ${snapshot.folders.size} 个收藏夹和 ${snapshot.notes.size} 条笔记"
            }.onSuccess(onSuccess)
                .onFailure { onFailure(it.message ?: "备份导入失败，请确认文件完整") }
        }
    }

    fun saveNote(
        id: Long,
        title: String,
        content: String,
        color: Long,
        pinned: Boolean,
        markdown: Boolean,
        folderId: Long,
        createdAt: Long = System.currentTimeMillis()
    ) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            repository.save(
                NoteEntity(
                    id = id,
                    title = title.trim(),
                    content = content.trim(),
                    color = color,
                    createdAt = createdAt,
                    updatedAt = System.currentTimeMillis(),
                    isPinned = pinned,
                    isMarkdown = markdown,
                    folderId = folderId
                )
            )
        }
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long) {
        viewModelScope.launch { repository.moveToFolder(noteId, folderId) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}

class NotesViewModelFactory(private val repository: NotesRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(NotesViewModel::class.java))
        return NotesViewModel(repository) as T
    }
}
