package com.example.anjiannotes.data

import org.json.JSONArray
import org.json.JSONObject

private const val BACKUP_SCHEMA_VERSION = 1

data class BackupSnapshot(
    val folders: List<FolderEntity>,
    val notes: List<NoteEntity>
)

private fun FolderEntity.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("createdAt", createdAt)
    put("sortOrder", sortOrder)
}

private fun decodeBackupFolders(values: JSONArray): List<FolderEntity> = buildList {
    for (index in 0 until values.length()) {
        val item = values.getJSONObject(index)
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

object BackupCodec {
    fun encode(snapshot: BackupSnapshot): String {
        val root = JSONObject().apply {
            put("schemaVersion", BACKUP_SCHEMA_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("folders", JSONArray().apply {
                snapshot.folders.forEach { folder -> put(folder.toBackupJson()) }
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
        val folders = decodeBackupFolders(foldersJson)
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


/**
 * 面向跨平台使用的 Markdown ZIP 备份格式。
 *
 * ZIP 内包含：metadata.json、folders.json 以及 notes/{id}.md。每个 Markdown
 * 文件均带 YAML Front Matter，因此无需安装安笺也能直接阅读与迁移文本内容。
 */
object MarkdownZipBackupCodec {
    private const val FORMAT = "an-jian-markdown-zip"
    private const val SCHEMA_VERSION = 1
    private const val METADATA_FILE = "metadata.json"
    private const val FOLDERS_FILE = "folders.json"
    private const val NOTES_PREFIX = "notes/"

    fun encode(snapshot: BackupSnapshot, appVersion: String): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(output).use { zip ->
            writeEntry(zip, METADATA_FILE, metadataJson(snapshot, appVersion).toByteArray(Charsets.UTF_8))
            writeEntry(zip, FOLDERS_FILE, foldersJson(snapshot.folders).toByteArray(Charsets.UTF_8))
            snapshot.notes.forEach { note ->
                writeEntry(zip, noteFileName(note), encodeNote(note).toByteArray(Charsets.UTF_8))
            }
        }
        return output.toByteArray()
    }

    fun decode(payload: ByteArray): BackupSnapshot {
        var metadata: JSONObject? = null
        var foldersRaw: String? = null
        val noteFiles = mutableListOf<String>()
        java.util.zip.ZipInputStream(payload.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                require(!entry.isDirectory && !name.contains("..") && !name.startsWith("/")) { "备份 ZIP 包含无效路径" }
                val content = zip.readBytes().toString(Charsets.UTF_8)
                when {
                    name == METADATA_FILE -> metadata = JSONObject(content)
                    name == FOLDERS_FILE -> foldersRaw = content
                    name.startsWith(NOTES_PREFIX) && name.endsWith(".md") -> noteFiles += content
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val backupMetadata = metadata ?: throw IllegalArgumentException("备份中缺少 metadata.json")
        require(backupMetadata.optString("format") == FORMAT) { "不是安笺 Markdown ZIP 备份" }
        require(backupMetadata.optInt("schemaVersion") == SCHEMA_VERSION) { "不支持的 Markdown ZIP 备份版本" }
        val folders = decodeFolders(foldersRaw ?: throw IllegalArgumentException("备份中缺少 folders.json"))
        val validFolderIds = folders.mapTo(mutableSetOf()) { it.id }.apply { add(DEFAULT_FOLDER_ID) }
        val notes = noteFiles.map { decodeNote(it) }.map { note ->
            if (note.folderId in validFolderIds) note else note.copy(folderId = DEFAULT_FOLDER_ID)
        }
        return BackupSnapshot(folders = folders, notes = notes)
    }

    fun metadataJson(snapshot: BackupSnapshot, appVersion: String): String = JSONObject().apply {
        put("format", FORMAT)
        put("schemaVersion", SCHEMA_VERSION)
        put("appVersion", appVersion)
        put("exportedAt", System.currentTimeMillis())
        put("noteCount", snapshot.notes.size)
        put("folderCount", snapshot.folders.size)
    }.toString(2)

    fun foldersJson(folders: List<FolderEntity>): String = JSONObject().apply {
        put("schemaVersion", SCHEMA_VERSION)
        put("folders", JSONArray().apply {
            folders.forEach { folder -> put(folder.toBackupJson()) }
        })
    }.toString(2)

    fun noteFileName(note: NoteEntity): String = "$NOTES_PREFIX${note.id}.md"

    fun encodeNote(note: NoteEntity): String = buildString {
        appendLine("---")
        appendLine("id: ${note.id}")
        appendLine("title: ${yamlQuote(note.title)}")
        appendLine("createdAt: ${note.createdAt}")
        appendLine("updatedAt: ${note.updatedAt}")
        appendLine("pinned: ${note.isPinned}")
        appendLine("markdown: ${note.isMarkdown}")
        appendLine("folderId: ${note.folderId}")
        appendLine("color: ${note.color}")
        appendLine("---")
        append(note.content)
    }

    private fun decodeFolders(raw: String): List<FolderEntity> = decodeBackupFolders(
        JSONObject(raw).optJSONArray("folders")
            ?: throw IllegalArgumentException("folders.json 缺少收藏夹列表")
    )

    private fun decodeNote(raw: String): NoteEntity {
        val normalized = raw.replace("\r\n", "\n")
        require(normalized.startsWith("---\n")) { "笔记缺少 YAML Front Matter" }
        val end = normalized.indexOf("\n---\n", startIndex = 4)
        require(end >= 0) { "笔记 YAML Front Matter 不完整" }
        val values = normalized.substring(4, end)
            .lineSequence()
            .mapNotNull { line ->
                val delimiter = line.indexOf(": ")
                if (delimiter > 0) line.substring(0, delimiter) to line.substring(delimiter + 2) else null
            }
            .toMap()
        val content = normalized.substring(end + 5)
        return NoteEntity(
            id = values.requiredLong("id"),
            title = yamlUnquote(values["title"].orEmpty()),
            content = content,
            color = values.longOr("color", 0xFFF5F0E8),
            createdAt = values.longOr("createdAt", System.currentTimeMillis()),
            updatedAt = values.longOr("updatedAt", System.currentTimeMillis()),
            isPinned = values["pinned"].toBoolean(),
            isMarkdown = values["markdown"].toBoolean(),
            folderId = values.longOr("folderId", DEFAULT_FOLDER_ID)
        )
    }

    private fun writeEntry(zip: java.util.zip.ZipOutputStream, name: String, content: ByteArray) {
        zip.putNextEntry(java.util.zip.ZipEntry(name))
        zip.write(content)
        zip.closeEntry()
    }

    private fun yamlQuote(value: String): String = "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n") + "\""

    private fun yamlUnquote(value: String): String {
        val unwrapped = value.removePrefix("\"").removeSuffix("\"")
        return unwrapped.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
    }

    private fun Map<String, String>.requiredLong(key: String): Long = this[key]?.toLongOrNull()
        ?: throw IllegalArgumentException("笔记元数据缺少或包含无效 $key")

    private fun Map<String, String>.longOr(key: String, fallback: Long): Long = this[key]?.toLongOrNull() ?: fallback
}
