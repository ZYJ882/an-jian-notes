package com.example.anjiannotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.anjiannotes.data.BackupCodec
import com.example.anjiannotes.data.DEFAULT_FOLDER_ID
import com.example.anjiannotes.data.FolderEntity
import com.example.anjiannotes.data.MarkdownZipBackupCodec
import com.example.anjiannotes.data.NoteEntity
import com.example.anjiannotes.data.NotesRepository
import com.example.anjiannotes.data.PlainTextBackupCodec
import com.example.anjiannotes.data.WebDavBackupClient
import com.example.anjiannotes.data.WebDavConfig
import com.example.anjiannotes.data.WebDavConfigStore
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
class NotesViewModel(
    private val repository: NotesRepository,
    private val webDavConfigStore: WebDavConfigStore,
    private val webDavBackupClient: WebDavBackupClient
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedFolderId = MutableStateFlow(DEFAULT_FOLDER_ID)
    private val configuredWebDav = MutableStateFlow(webDavConfigStore.load())

    val query: StateFlow<String> = searchQuery.asStateFlow()
    val activeFolderId: StateFlow<Long> = selectedFolderId.asStateFlow()
    val webDavConfig: StateFlow<WebDavConfig?> = configuredWebDav.asStateFlow()
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

    fun saveWebDavConfig(config: WebDavConfig, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        runCatching {
            val normalized = config.normalized()
            webDavConfigStore.save(normalized)
            configuredWebDav.value = normalized
        }.onSuccess { onSuccess() }
            .onFailure { onFailure(it.message ?: "WebDAV 配置保存失败") }
    }

    fun syncWebDav(
        config: WebDavConfig,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val normalized = config.normalized()
                webDavConfigStore.save(normalized)
                configuredWebDav.value = normalized
                val result = webDavBackupClient.syncIncremental(
                    config = normalized,
                    snapshot = repository.exportSnapshot(),
                    appVersion = BuildConfig.VERSION_NAME
                )
                "WebDAV 备份完成：上传 ${result.uploadedNotes} 条，跳过 ${result.skippedNotes} 条未变化笔记"
            }.onSuccess(onSuccess)
                .onFailure { onFailure(it.message ?: "WebDAV 备份失败") }
        }
    }

    fun createBackup(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { BackupCodec.encode(repository.exportSnapshot()) }
                .onSuccess(onSuccess)
                .onFailure { onFailure(it.message ?: "备份导出失败") }
        }
    }

    fun createMarkdownZipBackup(onSuccess: (ByteArray) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                MarkdownZipBackupCodec.encode(repository.exportSnapshot(), BuildConfig.VERSION_NAME)
            }.onSuccess(onSuccess)
                .onFailure { onFailure(it.message ?: "Markdown ZIP 备份导出失败") }
        }
    }

    fun restoreMarkdownZipBackup(payload: ByteArray, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        restoreSnapshot(
            decode = { MarkdownZipBackupCodec.decode(payload) },
            onSuccess = onSuccess,
            onFailure = onFailure
        )
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

    /**
     * 编辑页以防抖方式频繁调用该方法。正文不做 trim，确保 Markdown 换行、
     * 空格和代码块等原始书写内容不会在自动保存时被悄悄改写。
     */
    fun saveNote(
        id: Long,
        title: String,
        content: String,
        color: Long,
        pinned: Boolean,
        markdown: Boolean,
        folderId: Long,
        createdAt: Long = System.currentTimeMillis(),
        onSaved: (Long) -> Unit = {}
    ) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val savedId = repository.save(
                NoteEntity(
                    id = id,
                    title = title,
                    content = content,
                    color = color,
                    createdAt = createdAt,
                    updatedAt = System.currentTimeMillis(),
                    isPinned = pinned,
                    isMarkdown = markdown,
                    folderId = folderId
                )
            )
            onSaved(savedId)
        }
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long) {
        viewModelScope.launch { repository.moveToFolder(noteId, folderId) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}

class NotesViewModelFactory(
    private val repository: NotesRepository,
    private val webDavConfigStore: WebDavConfigStore,
    private val webDavBackupClient: WebDavBackupClient
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(NotesViewModel::class.java))
        return NotesViewModel(repository, webDavConfigStore, webDavBackupClient) as T
    }
}
