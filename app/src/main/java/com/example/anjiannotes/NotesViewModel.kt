package com.example.anjiannotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.anjiannotes.data.ALL_FOLDERS_ID
import com.example.anjiannotes.data.BackupCodec
import com.example.anjiannotes.data.DEFAULT_FOLDER_ID
import com.example.anjiannotes.data.FolderSelectionPreferences
import com.example.anjiannotes.data.FolderEntity
import com.example.anjiannotes.data.MarkdownZipBackupCodec
import com.example.anjiannotes.data.NoteEntity
import com.example.anjiannotes.data.NotesRepository
import com.example.anjiannotes.data.PlainTextBackupCodec
import com.example.anjiannotes.data.STARRED_FOLDER
import com.example.anjiannotes.data.WebDavBackupClient
import com.example.anjiannotes.data.WebDavConfig
import com.example.anjiannotes.data.WebDavConfigStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NotesViewModel(
    private val repository: NotesRepository,
    private val webDavConfigStore: WebDavConfigStore,
    private val webDavBackupClient: WebDavBackupClient,
    private val folderSelectionPreferences: FolderSelectionPreferences
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedFolderId = MutableStateFlow(folderSelectionPreferences.load())
    private val configuredWebDav = MutableStateFlow(webDavConfigStore.load())

    val query: StateFlow<String> = searchQuery.asStateFlow()
    val activeFolderId: StateFlow<Long> = selectedFolderId.asStateFlow()
    val isGlobalSearch: StateFlow<Boolean> = selectedFolderId
        .map { it == ALL_FOLDERS_ID }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), selectedFolderId.value == ALL_FOLDERS_ID)
    val webDavConfig: StateFlow<WebDavConfig?> = configuredWebDav.asStateFlow()
    val folders: StateFlow<List<FolderEntity>> = repository.observeFolders()
        .map { folders -> listOf(STARRED_FOLDER) + folders }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = combine(
        searchQuery.debounce(180),
        selectedFolderId
    ) { query, folderId -> query to folderId }
        .flatMapLatest { (query, folderId) -> repository.observeNotes(query, folderId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.ensureDefaultFolder() }
    }

    private fun <T> launchResult(
        fallbackMessage: String,
        onSuccess: (T) -> Unit,
        onFailure: (String) -> Unit,
        action: suspend () -> T
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { runCatching { action() } }
            result.onSuccess(onSuccess)
                .onFailure { onFailure(it.message ?: fallbackMessage) }
        }
    }

    private fun <T> launchDeferred(action: suspend () -> T): Deferred<T> {
        val result = CompletableDeferred<T>()
        viewModelScope.launch {
            withContext(Dispatchers.Default) { runCatching { action() } }
                .onSuccess(result::complete)
                .onFailure(result::completeExceptionally)
        }
        return result
    }

    private fun persistWebDavConfig(config: WebDavConfig): WebDavConfig = config.normalized().also { normalized ->
        webDavConfigStore.save(normalized)
        configuredWebDav.value = normalized
    }

    private fun runBulkNoteAction(
        ids: Collection<Long>,
        onComplete: (Int) -> Unit,
        action: suspend (Collection<Long>) -> Int
    ) {
        viewModelScope.launch {
            onComplete(withContext(Dispatchers.Default) { action(ids) })
        }
    }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun selectFolder(folderId: Long) {
        selectedFolderId.value = folderId
        if (folderId != ALL_FOLDERS_ID) folderSelectionPreferences.save(folderId)
    }

    fun openGlobalSearch() {
        selectedFolderId.value = ALL_FOLDERS_ID
        searchQuery.value = ""
    }

    fun closeGlobalSearch() {
        if (selectedFolderId.value == ALL_FOLDERS_ID) {
            selectedFolderId.value = folderSelectionPreferences.load()
        }
    }

    fun createFolder(name: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        launchResult("收藏夹创建失败，请重试", onSuccess, onFailure) {
            val id = repository.createFolder(name)
            selectFolder(id)
            name.trim()
        }
    }

    fun saveWebDavConfig(config: WebDavConfig, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        launchResult("WebDAV 配置保存失败", { onSuccess() }, onFailure) {
            persistWebDavConfig(config)
        }
    }

    fun syncWebDav(
        config: WebDavConfig,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        launchResult("WebDAV 备份失败", onSuccess, onFailure) {
            val normalized = persistWebDavConfig(config)
            val result = webDavBackupClient.syncIncremental(
                config = normalized,
                snapshot = repository.exportSnapshot(),
                appVersion = BuildConfig.VERSION_NAME
            )
            "WebDAV 备份完成：上传 ${result.uploadedNotes} 条，跳过 ${result.skippedNotes} 条未变化笔记"
        }
    }

    fun createBackup(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        launchResult("备份导出失败", onSuccess, onFailure) {
            BackupCodec.encode(repository.exportSnapshot())
        }
    }

    fun createMarkdownZipBackup(onSuccess: (ByteArray) -> Unit, onFailure: (String) -> Unit) {
        launchResult("Markdown ZIP 备份导出失败", onSuccess, onFailure) {
            MarkdownZipBackupCodec.encode(repository.exportSnapshot(), BuildConfig.VERSION_NAME)
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
        launchResult("TXT 备份导出失败", onSuccess, onFailure) {
            PlainTextBackupCodec.encode(repository.exportSnapshot())
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
        launchResult("备份导入失败，请确认文件完整", onSuccess, onFailure) {
            val snapshot = decode()
            repository.restoreSnapshot(snapshot)
            selectedFolderId.value = DEFAULT_FOLDER_ID
            searchQuery.value = ""
            "已恢复 ${snapshot.folders.size} 个收藏夹和 ${snapshot.notes.size} 条笔记"
        }
    }

    /**
     * 可等待的单次写入。编辑页以单一保存队列顺序调用它，只有 Room 确认写入后才会
     * 切换预览或离开详情页。正文不做 trim，确保 Markdown 换行、空格和代码块等
     * 原始书写内容不会被悄悄改写；即使最终标题和正文都为空，也会保存已编辑笔记的最终状态。
     */
    suspend fun saveNote(
        id: Long,
        title: String,
        content: String,
        color: Long,
        pinned: Boolean,
        topPinned: Boolean,
        markdown: Boolean,
        folderId: Long,
        createdAt: Long = System.currentTimeMillis()
    ): Long = repository.save(
        NoteEntity(
            id = id,
            title = title,
            content = content,
            color = color,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis(),
            isPinned = pinned,
            isTopPinned = topPinned,
            isMarkdown = markdown,
            folderId = folderId
        )
    )

    /**
     * 详情页把一次保存交给 ViewModel 作用域后立即得到可等待结果。
     * 页面在系统返回期间被提前销毁时，写入任务仍会继续完成；仍在详情页时则可 await 该结果，
     * 用于确认首次草稿已取得稳定的数据库 ID。
     */
    fun queueSaveNote(
        id: Long,
        title: String,
        content: String,
        color: Long,
        pinned: Boolean,
        topPinned: Boolean,
        markdown: Boolean,
        folderId: Long,
        createdAt: Long = System.currentTimeMillis()
    ): Deferred<Long> = launchDeferred {
        saveNote(id, title, content, color, pinned, topPinned, markdown, folderId, createdAt)
    }

    fun copyNoteToFolder(
        note: NoteEntity,
        folderId: Long,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        launchResult("复制笔记失败", { onSuccess() }, onFailure) {
            repository.save(
                note.copy(
                    id = 0L,
                    folderId = folderId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            Unit
        }
    }

    fun toggleStar(note: NoteEntity) {
        viewModelScope.launch {
            repository.save(note.copy(isPinned = !note.isPinned))
        }
    }

    fun toggleTopPin(note: NoteEntity) {
        viewModelScope.launch {
            repository.save(note.copy(isTopPinned = !note.isTopPinned))
        }
    }

    fun setTopPinnedNotes(ids: Collection<Long>, isTopPinned: Boolean, onComplete: (Int) -> Unit) {
        runBulkNoteAction(ids, onComplete) { repository.setTopPinnedMany(it, isTopPinned) }
    }

    fun deleteFolder(
        folderId: Long,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        launchResult("收藏夹删除失败，请重试", onSuccess, onFailure) {
            repository.deleteFolder(folderId)
            if (selectedFolderId.value == folderId) selectFolder(DEFAULT_FOLDER_ID)
            "收藏夹和其中笔记已删除"
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun deleteNotes(ids: Collection<Long>, onComplete: (Int) -> Unit) {
        runBulkNoteAction(ids, onComplete, repository::deleteMany)
    }

    fun starNotes(ids: Collection<Long>, onComplete: (Int) -> Unit) {
        runBulkNoteAction(ids, onComplete, repository::starMany)
    }
}

class NotesViewModelFactory(
    private val repository: NotesRepository,
    private val webDavConfigStore: WebDavConfigStore,
    private val webDavBackupClient: WebDavBackupClient,
    private val folderSelectionPreferences: FolderSelectionPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(NotesViewModel::class.java))
        return NotesViewModel(repository, webDavConfigStore, webDavBackupClient, folderSelectionPreferences) as T
    }
}
