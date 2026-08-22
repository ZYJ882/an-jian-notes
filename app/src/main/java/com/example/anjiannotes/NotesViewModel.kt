package com.example.anjiannotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.anjiannotes.data.NoteEntity
import com.example.anjiannotes.data.NotesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NotesViewModel(private val repository: NotesRepository) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    val query: StateFlow<String> = searchQuery.asStateFlow()

    val notes: StateFlow<List<NoteEntity>> = searchQuery
        .debounce(80)
        .flatMapLatest(repository::observeNotes)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun saveNote(
        id: Long,
        title: String,
        content: String,
        rawTags: String,
        color: Long,
        pinned: Boolean,
        markdown: Boolean,
        createdAt: Long = System.currentTimeMillis()
    ) {
        val cleanedTags = rawTags.split(',', '，')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .joinToString(",")
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            repository.save(
                NoteEntity(
                    id = id,
                    title = title.trim(),
                    content = content.trim(),
                    tags = cleanedTags,
                    color = color,
                    createdAt = createdAt,
                    updatedAt = System.currentTimeMillis(),
                    isPinned = pinned,
                    isMarkdown = markdown
                )
            )
        }
    }

    fun togglePinned(note: NoteEntity) {
        viewModelScope.launch {
            repository.save(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
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
