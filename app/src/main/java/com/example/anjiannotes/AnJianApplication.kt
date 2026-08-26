package com.example.anjiannotes

import android.app.Application
import androidx.room.Room
import com.example.anjiannotes.data.FolderSelectionPreferences
import com.example.anjiannotes.data.NotesDatabase
import com.example.anjiannotes.data.WebDavBackupClient
import com.example.anjiannotes.data.WebDavConfigStore
import com.example.anjiannotes.ui.theme.AppearancePreferences

class AnJianApplication : Application() {
    val appearancePreferences: AppearancePreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AppearancePreferences(this) }
    val folderSelectionPreferences: FolderSelectionPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { FolderSelectionPreferences(this) }
    val webDavConfigStore: WebDavConfigStore by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { WebDavConfigStore(this) }
    val webDavBackupClient: WebDavBackupClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { WebDavBackupClient(this) }

    val database: NotesDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Room.databaseBuilder(this, NotesDatabase::class.java, "an_jian_notes.db")
            .addMigrations(NotesDatabase.MIGRATION_1_2, NotesDatabase.MIGRATION_2_3, NotesDatabase.MIGRATION_3_4)
            .build()
    }
}
