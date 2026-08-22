package com.example.anjiannotes.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

enum class NoteFormatMode(val label: String) {
    AUTO("自动识别"),
    MARKDOWN("Markdown"),
    PLAIN("纯文本");

    fun next(): NoteFormatMode = when (this) {
        AUTO -> MARKDOWN
        MARKDOWN -> PLAIN
        PLAIN -> AUTO
    }

    fun resolvesToMarkdown(content: String): Boolean = when (this) {
        AUTO -> looksLikeMarkdown(content)
        MARKDOWN -> true
        PLAIN -> false
    }
}

data class EditorSeed(
    val title: String = "",
    val content: String = "",
    val formatMode: NoteFormatMode = NoteFormatMode.AUTO
)

data class ImportedNoteText(val title: String, val content: String)

sealed interface ImportReadResult {
    data class Success(val note: ImportedNoteText) : ImportReadResult
    data class Failure(val message: String) : ImportReadResult
}

fun looksLikeMarkdown(content: String): Boolean {
    if (content.isBlank()) return false
    val blockSyntax = Regex(
        "(?m)^\\s{0,3}(#{1,6}\\s+\\S+|[-+*]\\s+\\S+|\\d+\\.\\s+\\S+|>\\s+\\S+|(```|---|\\*\\*\\*))"
    )
    val inlineSyntax = Regex("\\*\\*[^*\\n]+\\*\\*|~~[^~\\n]+~~|`[^`\\n]+`|(?<!\\*)\\*[^*\\n]+\\*(?!\\*)")
    return blockSyntax.containsMatchIn(content) || inlineSyntax.containsMatchIn(content)
}

fun readTextImport(context: Context, uri: Uri): ImportReadResult {
    return try {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
            if (reader == null) return ImportReadResult.Failure("无法读取该文件")
            val buffer = CharArray(8_192)
            val builder = StringBuilder()
            while (true) {
                val count = reader.read(buffer)
                if (count <= 0) break
                if (builder.length + count > 1_000_000) {
                    return ImportReadResult.Failure("文件过大，请导入小于 100 万字符的文本")
                }
                builder.append(buffer, 0, count)
            }
            builder.toString().removePrefix("\uFEFF")
        }
        if (content.isNullOrBlank()) return ImportReadResult.Failure("文件内容为空")
        ImportReadResult.Success(
            ImportedNoteText(
                title = displayName(context, uri).substringBeforeLast('.', displayName(context, uri)),
                content = content
            )
        )
    } catch (_: Exception) {
        ImportReadResult.Failure("导入失败，请确认文件为 UTF-8 文本")
    }
}

private fun displayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index).orEmpty()
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "导入笔记"
}
