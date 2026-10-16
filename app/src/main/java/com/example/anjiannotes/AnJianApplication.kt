package com.example.anjiannotes

import android.app.Application
import androidx.room.Room
import com.example.anjiannotes.data.AppSortingPreferences
import com.example.anjiannotes.data.FolderSelectionPreferences
import com.example.anjiannotes.data.NotePositionStore
import com.example.anjiannotes.data.NotesDatabase
import com.example.anjiannotes.data.WebDavBackupClient
import com.example.anjiannotes.data.WebDavConfigStore
import com.example.anjiannotes.ui.theme.AppearancePreferences
import com.example.anjiannotes.ui.theme.FontPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AnJianApplication : Application() {
    val appearancePreferences: AppearancePreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AppearancePreferences(this) }
    val fontPreferences: FontPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { FontPreferences(this) }
    val folderSelectionPreferences: FolderSelectionPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { FolderSelectionPreferences(this) }
    val appSortingPreferences: AppSortingPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AppSortingPreferences(this) }
    val notePositionStore: NotePositionStore by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { NotePositionStore(this) }
    val webDavConfigStore: WebDavConfigStore by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { WebDavConfigStore(this) }
    val webDavBackupClient: WebDavBackupClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { WebDavBackupClient(this) }

    val database: NotesDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Room.databaseBuilder(this, NotesDatabase::class.java, "an_jian_notes.db")
            .addMigrations(
                NotesDatabase.MIGRATION_1_2,
                NotesDatabase.MIGRATION_2_3,
                NotesDatabase.MIGRATION_3_4,
                NotesDatabase.MIGRATION_4_5,
                NotesDatabase.MIGRATION_5_6,
                NotesDatabase.MIGRATION_6_7,
                NotesDatabase.MIGRATION_7_8
            )
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        // 冷启动预热：SharedPreferences 各文件的首次读取是磁盘 I/O，统一放到后台线程完成。
        // 之后主线程首帧读取这些偏好时直接命中内存缓存，不在关键路径上阻塞。
        // Room 数据库仍保持懒构建：构建不打开连接，首次查询时才真正打开。
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                appearancePreferences.mode.value
                fontPreferences.font.value
                fontPreferences.customPath.value
                folderSelectionPreferences.load()
                appSortingPreferences.loadNoteListByRecentOpen()
                webDavConfigStore.load()
                notePositionStore.warmUp()
            }
        }
    }
}
