package com.example.anjiannotes

import android.app.Application
import androidx.room.Room
import com.example.anjiannotes.data.NotesDatabase

class AnJianApplication : Application() {
    val database: NotesDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Room.databaseBuilder(this, NotesDatabase::class.java, "an_jian_notes.db")
            .addMigrations(NotesDatabase.MIGRATION_1_2, NotesDatabase.MIGRATION_2_3, NotesDatabase.MIGRATION_3_4)
            .build()
    }
}
