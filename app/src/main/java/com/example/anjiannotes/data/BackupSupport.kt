package com.example.anjiannotes.data

import org.json.JSONArray
import org.json.JSONObject

private const val BACKUP_SCHEMA_VERSION = 1

data class BackupSnapshot(
    val folders: List<FolderEntity>,
    val notes: List<NoteEntity>
)

object BackupCodec {
    fun encode(snapshot: BackupSnapshot): String {
        val root = JSONObject().apply {
            put("schemaVersion", BACKUP_SCHEMA_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("folders", JSONArray().apply {
                snapshot.folders.forEach { folder ->
                    put(JSONObject().apply {
                        put("id", folder.id)
                        put("name", folder.name)
                        put("createdAt", folder.createdAt)
                        put("sortOrder", folder.sortOrder)
                    })
                }
            })
            put("notes", JSONArray().apply {
                snapshot.notes.forEach { note ->
                    put(JSONObject().apply {
                        put("id", note.id)
                        put("title", note.title)
                        put("content", note.content)
                        put("tags", note.tags)
                        put("color", note.color)
                        put("createdAt", note.createdAt)
                        put("updatedAt", note.updatedAt)
                        put("isPinned", note.isPinned)
                        put("isMarkdown", note.isMarkdown)
                        put("folderId", note.folderId)
                    })
                }
            })
        }
        return root.toString(2)
    }

    fun decode(raw: String): BackupSnapshot {
        val root = JSONObject(raw)
        require(root.optInt("schemaVersion") == BACKUP_SCHEMA_VERSION) { "不支持的备份版本" }
        val foldersJson = root.optJSONArray("folders") ?: throw IllegalArgumentException("备份中缺少收藏夹数据")
        val notesJson = root.optJSONArray("notes") ?: throw IllegalArgumentException("备份中缺少笔记数据")
        val folders = buildList {
            for (index in 0 until foldersJson.length()) {
                val item = foldersJson.getJSONObject(index)
                add(
                    FolderEntity(
                        id = item.getLong("id"),
                        name = item.getString("name"),
                        createdAt = item.optLong("createdAt", 0L),
                        sortOrder = item.optLong("sortOrder", index.toLong())
                    )
                )
            }
        }
        val validFolderIds = folders.mapTo(mutableSetOf()) { it.id }.apply { add(DEFAULT_FOLDER_ID) }
        val notes = buildList {
            for (index in 0 until notesJson.length()) {
                val item = notesJson.getJSONObject(index)
                val targetFolder = item.optLong("folderId", DEFAULT_FOLDER_ID).let {
                    if (it in validFolderIds) it else DEFAULT_FOLDER_ID
                }
                add(
                    NoteEntity(
                        id = item.getLong("id"),
                        title = item.optString("title"),
                        content = item.optString("content"),
                        tags = item.optString("tags"),
                        color = item.optLong("color", 0xFFF5F0E8),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                        isPinned = item.optBoolean("isPinned", false),
                        isMarkdown = item.optBoolean("isMarkdown", false),
                        folderId = targetFolder
                    )
                )
            }
        }
        return BackupSnapshot(folders = folders, notes = notes)
    }
}
