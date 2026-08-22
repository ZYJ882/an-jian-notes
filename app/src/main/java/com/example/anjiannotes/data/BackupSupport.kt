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

object PlainTextBackupCodec {
    private const val HEADER = "[[ANJIAN_TEXT_BACKUP_V1]]"

    fun encode(snapshot: BackupSnapshot): String = buildString {
        appendLine(HEADER)
        appendLine("# 安笺明文备份；请勿删除结构标记，文件可直接导入恢复。")
        appendLine()
        snapshot.folders.forEach { folder ->
            appendLine("[[FOLDER]]")
            appendLine("id=${folder.id}")
            appendLine("name=${folder.name}")
            appendLine("createdAt=${folder.createdAt}")
            appendLine("sortOrder=${folder.sortOrder}")
            appendLine("[[/FOLDER]]")
            appendLine()
        }
        snapshot.notes.forEach { note ->
            appendLine("[[NOTE]]")
            appendLine("id=${note.id}")
            appendLine("folderId=${note.folderId}")
            appendLine("title=${note.title}")
            appendLine("color=${note.color}")
            appendLine("createdAt=${note.createdAt}")
            appendLine("updatedAt=${note.updatedAt}")
            appendLine("isPinned=${note.isPinned}")
            appendLine("isMarkdown=${note.isMarkdown}")
            appendLine("[[CONTENT]]")
            append(note.content)
            if (!note.content.endsWith('\n')) appendLine()
            appendLine("[[/CONTENT]]")
            appendLine("[[/NOTE]]")
            appendLine()
        }
    }

    fun decode(raw: String): BackupSnapshot {
        val lines = raw.replace("\r\n", "\n").lines()
        require(lines.firstOrNull()?.trim() == HEADER) { "不是安笺 TXT 明文备份文件" }
        val folders = mutableListOf<FolderEntity>()
        val notes = mutableListOf<NoteEntity>()
        var index = 1
        while (index < lines.size) {
            when (lines[index].trim()) {
                "[[FOLDER]]" -> {
                    val values = mutableMapOf<String, String>()
                    index++
                    while (index < lines.size && lines[index].trim() != "[[/FOLDER]]") {
                        putValue(values, lines[index])
                        index++
                    }
                    require(index < lines.size) { "收藏夹备份区块不完整" }
                    folders += FolderEntity(
                        id = values.requiredLong("id"),
                        name = values.required("name"),
                        createdAt = values.longOr("createdAt", 0L),
                        sortOrder = values.longOr("sortOrder", 0L)
                    )
                }
                "[[NOTE]]" -> {
                    val values = mutableMapOf<String, String>()
                    index++
                    while (index < lines.size && lines[index].trim() != "[[CONTENT]]") {
                        putValue(values, lines[index])
                        index++
                    }
                    require(index < lines.size) { "笔记备份缺少正文起始标记" }
                    index++
                    val contentLines = mutableListOf<String>()
                    while (index < lines.size && lines[index].trim() != "[[/CONTENT]]") {
                        contentLines += lines[index]
                        index++
                    }
                    require(index < lines.size) { "笔记备份正文不完整" }
                    index++
                    require(index < lines.size && lines[index].trim() == "[[/NOTE]]") { "笔记备份结束标记不完整" }
                    notes += NoteEntity(
                        id = values.requiredLong("id"),
                        title = values["title"].orEmpty(),
                        content = contentLines.joinToString("\n").removeSuffix("\n"),
                        color = values.longOr("color", 0xFFF5F0E8),
                        createdAt = values.longOr("createdAt", System.currentTimeMillis()),
                        updatedAt = values.longOr("updatedAt", System.currentTimeMillis()),
                        isPinned = values["isPinned"].toBoolean(),
                        isMarkdown = values["isMarkdown"].toBoolean(),
                        folderId = values.longOr("folderId", DEFAULT_FOLDER_ID)
                    )
                }
            }
            index++
        }
        return BackupSnapshot(folders = folders, notes = notes)
    }

    private fun putValue(values: MutableMap<String, String>, line: String) {
        val separator = line.indexOf('=')
        if (separator > 0) values[line.substring(0, separator)] = line.substring(separator + 1)
    }

    private fun Map<String, String>.required(key: String): String = this[key] ?: throw IllegalArgumentException("备份缺少 $key")
    private fun Map<String, String>.requiredLong(key: String): Long = required(key).toLongOrNull()
        ?: throw IllegalArgumentException("备份中的 $key 格式错误")
    private fun Map<String, String>.longOr(key: String, fallback: Long): Long = this[key]?.toLongOrNull() ?: fallback
}
