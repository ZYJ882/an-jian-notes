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
import kotlinx.coroutines.flow.map

const val DEFAULT_FOLDER_ID = 1L
/** 仅用于界面筛选的内置收藏夹，不会写入 folders 表或改变笔记原有 folderId。 */
const val STARRED_FOLDER_ID = -1L
/** 仅用于全局检索的界面筛选值，不写入 notes 表。 */
const val ALL_FOLDERS_ID = -2L

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
    val sortOrder: Long = System.currentTimeMillis(),
    /** 最近一次在抽屉中点开该收藏夹的时间；0 表示从未点开。保留字段：收藏夹列表当前按手动基准顺序（sortOrder）排序，不读取该值。 */
    val lastOpenedAt: Long = 0
)

val DEFAULT_FOLDER = FolderEntity(
    id = DEFAULT_FOLDER_ID,
    name = "默认收藏夹",
    createdAt = 0,
    sortOrder = 0
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
    val isTopPinned: Boolean = false,
    val isMarkdown: Boolean = false,
    val folderId: Long = DEFAULT_FOLDER_ID,
    /** 最近一次从列表打开该笔记的时间；0 表示从未打开，供“最近打开置顶”排序使用。 */
    val lastOpenedAt: Long = 0
)

/** 将空格、逗号和常见中文分隔符视为多个独立关键词。 */
internal fun parseSearchTerms(query: String): List<String> =
    query.trim().split(Regex("[\\s,，;；、]+"))
        .filter(String::isNotBlank)

/** 每个关键词可出现在标题或正文中；所有关键词都命中才保留该笔记。 */
internal fun NoteEntity.matchesSearchTerms(terms: List<String>): Boolean =
    terms.all { term ->
        title.contains(term, ignoreCase = true) || content.contains(term, ignoreCase = true)
    }

/** 搜索结果的正文命中位置与阅读摘要；标题命中但正文未命中时正文位置为空。 */
internal data class NoteSearchMatch(
    val contentOffset: Int?,
    val snippet: String
)

internal fun NoteEntity.findSearchMatch(query: String): NoteSearchMatch? {
    val terms = parseSearchTerms(query)
    if (terms.isEmpty() || !matchesSearchTerms(terms)) return null
    val contentOffset = terms.asSequence()
        .map { term -> content.indexOf(term, ignoreCase = true) }
        .firstOrNull { it >= 0 }
        ?.takeIf { it >= 0 }
    val normalizedContent = content.replace(Regex("\\s+"), " ").trim()
    val snippet = if (contentOffset != null) searchSnippet(content, contentOffset) else normalizedContent.take(92)
    return NoteSearchMatch(contentOffset, snippet)
}

internal fun searchSnippet(content: String, matchOffset: Int, maxLength: Int = 92): String {
    if (content.isBlank()) return "标题匹配"
    val start = (matchOffset - maxLength / 3).coerceAtLeast(0)
    val end = (start + maxLength).coerceAtMost(content.length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < content.length) "…" else ""
    return prefix + content.substring(start, end).replace(Regex("\\s+"), " ").trim() + suffix
}

@Dao
interface NoteDao {
    @Query(
        """
        SELECT * FROM notes
        WHERE (
            (:showStarred = 1 AND isPinned = 1)
            OR (:showAll = 1)
            OR (:showStarred = 0 AND :showAll = 0 AND folderId = :folderId)
        )
          AND (:query = ''
            OR title LIKE '%' || :query || '%'
            OR content LIKE '%' || :query || '%')
        ORDER BY isTopPinned DESC,
            CASE WHEN :sortByOpen = 1 AND lastOpenedAt > 0 THEN 0 ELSE 1 END,
            CASE WHEN :sortByOpen = 1 AND lastOpenedAt > 0 THEN -lastOpenedAt ELSE -updatedAt END
        """
    )
    fun observeNotes(
        query: String,
        folderId: Long,
        showStarred: Boolean,
        showAll: Boolean,
        sortByOpen: Boolean
    ): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query("UPDATE notes SET isPinned = 1 WHERE id IN (:ids)")
    suspend fun starByIds(ids: List<Long>): Int

    @Query("UPDATE notes SET isTopPinned = :isTopPinned WHERE id IN (:ids)")
    suspend fun setTopPinnedByIds(ids: List<Long>, isTopPinned: Boolean): Int

    @Query("DELETE FROM notes WHERE folderId = :folderId")
    suspend fun deleteByFolderId(folderId: Long): Int

    @Query("UPDATE notes SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun touchLastOpened(id: Long, timestamp: Long)

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

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun renameById(id: Long, name: String)

    @Query("UPDATE folders SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Long)
}

@Database(entities = [NoteEntity::class, FolderEntity::class], version = 6, exportSchema = false)
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isTopPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 新增“最近打开/使用”时间戳，旧行默认 0（从未打开），旧数据无需回填。
                db.execSQL("ALTER TABLE notes ADD COLUMN lastOpenedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN lastOpenedAt INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

class NotesRepository(
    private val database: NotesDatabase,
    private val noteDao: NoteDao,
    private val folderDao: FolderDao
) {
    fun observeNotes(query: String, folderId: Long, sortByOpen: Boolean): Flow<List<NoteEntity>> {
        val terms = parseSearchTerms(query)
        return noteDao.observeNotes(
            query = terms.firstOrNull().orEmpty(),
            folderId = folderId,
            showStarred = folderId == STARRED_FOLDER_ID,
            showAll = folderId == ALL_FOLDERS_ID,
            sortByOpen = sortByOpen
        ).map { notes ->
            if (terms.size < 2) notes else notes.filter { it.matchesSearchTerms(terms) }
        }
    }
    fun observeFolders(): Flow<List<FolderEntity>> = folderDao.observeFolders()

    /** 从列表打开笔记时刷新“最近打开”时间戳；失败不影响打开流程。 */
    suspend fun touchNoteOpened(noteId: Long) {
        if (noteId > 0) noteDao.touchLastOpened(noteId, System.currentTimeMillis())
    }

    /** 重命名收藏夹；星标为虚拟入口不可重命名。 */
    suspend fun renameFolder(folderId: Long, rawName: String) {
        val cleanedName = rawName.trim()
        require(cleanedName.isNotBlank()) { "请输入收藏夹名称" }
        require(folderId != STARRED_FOLDER_ID) { "该收藏夹不可重命名" }
        folderDao.renameById(folderId, cleanedName)
    }

    /** 置顶收藏夹：将其 sortOrder 提到当前最小值之前；已是最前时保持不变。 */
    suspend fun moveFolderToTop(folderId: Long) {
        require(folderId != STARRED_FOLDER_ID) { "该收藏夹不可排序" }
        val folders = folderDao.getAll().sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
        val target = folders.firstOrNull { it.id == folderId } ?: return
        val minSortOrder = folders.minOf { it.sortOrder }
        if (target.sortOrder <= minSortOrder) return
        folderDao.updateSortOrder(folderId, minSortOrder - 1)
    }

    /** 上移（offset = -1）或下移（offset = +1）收藏夹，与相邻项交换 sortOrder。 */
    suspend fun moveFolderByOffset(folderId: Long, offset: Int) {
        require(folderId != STARRED_FOLDER_ID) { "该收藏夹不可排序" }
        val folders = folderDao.getAll().sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
        val index = folders.indexOfFirst { it.id == folderId }
        val neighborIndex = index + offset
        if (index < 0 || neighborIndex !in folders.indices) return
        database.withTransaction {
            folderDao.updateSortOrder(folders[index].id, folders[neighborIndex].sortOrder)
            folderDao.updateSortOrder(folders[neighborIndex].id, folders[index].sortOrder)
        }
    }

    suspend fun ensureDefaultFolder() {
        folderDao.insert(DEFAULT_FOLDER)
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
            val folders = snapshot.folders.ifEmpty { listOf(DEFAULT_FOLDER) }.let { imported ->
                if (imported.any { it.id == DEFAULT_FOLDER_ID }) imported else listOf(DEFAULT_FOLDER) + imported
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

    suspend fun setTopPinnedMany(ids: Collection<Long>, isTopPinned: Boolean): Int =
        applyToDistinctNoteIds(ids) { noteDao.setTopPinnedByIds(it, isTopPinned) }
}
