package com.example.anjiannotes.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val tags: String = "",
    val color: Long = 0xFFFFF8F0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Dao
interface NoteDao {
    @Query(
        """
        SELECT * FROM notes
        WHERE :query = ''
           OR title LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun observeNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

class NotesRepository(private val noteDao: NoteDao) {
    fun observeNotes(query: String): Flow<List<NoteEntity>> = noteDao.observeNotes(query.trim())
    suspend fun getById(id: Long): NoteEntity? = noteDao.getById(id)
    suspend fun save(note: NoteEntity): Long = noteDao.upsert(note)
    suspend fun delete(id: Long) = noteDao.deleteById(id)
}
