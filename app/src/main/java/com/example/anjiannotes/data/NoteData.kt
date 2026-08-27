package com.example.anjiannotes.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

const val DEFAULT_FOLDER_ID = 1L
/** 仅用于界面筛选的内置收藏夹，不会写入 folders 表或改变笔记原有 folderId。 */
const val STARRED_FOLDER_ID = -1L

val STARRED_FOLDER = FolderEntity(
    id = STARRED_FOLDER_ID,
    name = "星标笔记",
    createdAt = Long.MIN_VALUE,
    sortOrder = Long.MIN_VALUE
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val color: Long = 0xFFF5F0E8,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isMarkdown: Boolean = false,
    val folderId: Long = DEFAULT_FOLDER_ID
)

@Dao
interface NoteDao {
    @Query(
        """
        SELECT * FROM notes
        WHERE (
            (:showStarred = 1 AND isPinned = 1)
            OR (:showStarred = 0 AND folderId = :folderId)
        )
          AND (:query = ''
            OR title LIKE '%' || :query || '%'
            OR content LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun observeNotes(query: String, folderId: Long, showStarred: Boolean): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query("UPDATE notes SET isPinned = 1, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun starByIds(ids: List<Long>, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM notes WHERE folderId = :folderId")
    suspend fun deleteByFolderId(folderId: Long): Int

    @Query("SELECT * FROM notes ORDER BY id ASC")
    suspend fun getAll(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes")
    suspend fun clearAll()
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, createdAt ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: FolderEntity): Long

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FolderEntity?

    @Query("SELECT * FROM folders ORDER BY id ASC")
    suspend fun getAll(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<FolderEntity>)

    @Query("DELETE FROM folders")
    suspend fun clearAll()

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [NoteEntity::class, FolderEntity::class], version = 4, exportSchema = false)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isMarkdown INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS folders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, sortOrder INTEGER NOT NULL)")
                db.execSQL("INSERT OR IGNORE INTO folders (id, name, createdAt, sortOrder) VALUES (1, '默认收藏夹', 0, 0)")
                db.execSQL("ALTER TABLE notes ADD COLUMN folderId INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE notes_clean (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, color INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isPinned INTEGER NOT NULL, isMarkdown INTEGER NOT NULL, folderId INTEGER NOT NULL)")
                db.execSQL("INSERT INTO notes_clean (id, title, content, color, createdAt, updatedAt, isPinned, isMarkdown, folderId) SELECT id, title, content, color, createdAt, updatedAt, isPinned, isMarkdown, folderId FROM notes")
                db.execSQL("DROP TABLE notes")
                db.execSQL("ALTER TABLE notes_clean RENAME TO notes")
            }
        }
    }
}

class NotesRepository(
    private val database: NotesDatabase,
    private val noteDao: NoteDao,
    private val folderDao: FolderDao
) {
    fun observeNotes(query: String, folderId: Long): Flow<List<NoteEntity>> =
        noteDao.observeNotes(
            query = query.trim(),
            folderId = folderId,
            showStarred = folderId == STARRED_FOLDER_ID
        )
    fun observeFolders(): Flow<List<FolderEntity>> = folderDao.observeFolders()

    suspend fun ensureDefaultFolder() {
        folderDao.insert(FolderEntity(id = DEFAULT_FOLDER_ID, name = "默认收藏夹", createdAt = 0, sortOrder = 0))
    }

    suspend fun createFolder(name: String): Long {
        val cleanedName = name.trim()
        require(cleanedName.isNotBlank()) { "请输入收藏夹名称" }
        val id = folderDao.insert(FolderEntity(name = cleanedName))
        check(id > 0) { "收藏夹保存失败，请重试" }
        return id
    }

    suspend fun exportSnapshot(): BackupSnapshot = BackupSnapshot(
        folders = folderDao.getAll(),
        notes = noteDao.getAll()
    )

    suspend fun restoreSnapshot(snapshot: BackupSnapshot) {
        database.withTransaction {
            val folders = snapshot.folders.ifEmpty {
                listOf(FolderEntity(id = DEFAULT_FOLDER_ID, name = "默认收藏夹", createdAt = 0, sortOrder = 0))
            }.let { imported ->
                if (imported.any { it.id == DEFAULT_FOLDER_ID }) imported
                else listOf(FolderEntity(id = DEFAULT_FOLDER_ID, name = "默认收藏夹", createdAt = 0, sortOrder = 0)) + imported
            }
            val validFolderIds = folders.map { it.id }.toSet()
            val notes = snapshot.notes.map { note ->
                if (note.folderId in validFolderIds) note else note.copy(folderId = DEFAULT_FOLDER_ID)
            }
            noteDao.clearAll()
            folderDao.clearAll()
            folderDao.insertAll(folders)
            noteDao.insertAll(notes)
        }
    }

    suspend fun save(note: NoteEntity): Long = noteDao.upsert(note)

    /**
     * 删除自定义收藏夹时，级联删除其中全部笔记。
     * 默认收藏夹与虚拟星标入口均不可删除。
     */
    suspend fun deleteFolder(folderId: Long) {
        require(folderId != DEFAULT_FOLDER_ID && folderId != STARRED_FOLDER_ID) { "该收藏夹不可删除" }
        database.withTransaction {
            noteDao.deleteByFolderId(folderId)
            folderDao.deleteById(folderId)
        }
    }

    suspend fun delete(id: Long) = noteDao.deleteById(id)

    private suspend fun applyToDistinctNoteIds(
        ids: Collection<Long>,
        action: suspend (List<Long>) -> Int
    ): Int {
        val distinctIds = ids.distinct()
        return if (distinctIds.isEmpty()) 0 else action(distinctIds)
    }

    suspend fun deleteMany(ids: Collection<Long>): Int = applyToDistinctNoteIds(ids, noteDao::deleteByIds)

    suspend fun starMany(ids: Collection<Long>): Int = applyToDistinctNoteIds(ids, noteDao::starByIds)
}
