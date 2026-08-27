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

data class ImportedNoteText(
    val title: String,
    val content: String,
    val formatMode: NoteFormatMode
)

fun formatForFileName(fileName: String): NoteFormatMode = if (
    fileName.endsWith(".md", ignoreCase = true) || fileName.endsWith(".markdown", ignoreCase = true)
) {
    NoteFormatMode.MARKDOWN
} else {
    // `.txt` 与无扩展名文本也可能实际采用 Markdown；交给内容识别，
    // 普通文本仍会自然解析为纯文本，用户也始终可以手动切换格式。
    NoteFormatMode.AUTO
}

sealed interface ImportReadResult {
    data class Success(val note: ImportedNoteText) : ImportReadResult
    data class Failure(val message: String) : ImportReadResult
}

private object MarkdownDetectionPatterns {
    val atxHeading = Regex("^\\s{0,3}#{1,6}\\s+\\S+.*$")
    val fencedCode = Regex("^\\s*(```+|~~~+).*$")
    val taskItem = Regex("^\\s{0,3}[-+*]\\s+\\[[ xX]\\]\\s+\\S+.*$")
    val quote = Regex("^\\s{0,3}>\\s?\\S+.*$")
    val horizontalRule = Regex("^([-*_])(?:\\s*\\1){2,}\\s*$")
    val setextUnderline = Regex("^(=+|-{3,})$")
    val tableSeparator = Regex("^:?-{3,}:?$")
    val listItem = Regex("^\\s{0,3}([-+*]|\\d+[.)])\\s+\\S+.*$")
    val link = Regex("\\[[^]\\n]+]\\(https?://[^\\s)]+(?:\\s+[^)]*)?\\)", RegexOption.IGNORE_CASE)
    val image = Regex("!\\[[^]\\n]*]\\([^\\s)]+(?:\\s+[^)]*)?\\)")
    val strongInline = Regex("\\*\\*[^*\\n]+\\*\\*|__[^_\\n]+__|~~[^~\\n]+~~|`[^`\\n]+`")
    val italic = Regex("(?<!\\*)\\*[^*\\n]+\\*(?!\\*)|(?<!_)_[^_\\n]+_(?!_)")
}

fun looksLikeMarkdown(content: String): Boolean {
    if (content.isBlank()) return false

    // 自动模式只根据明确的 Markdown 特征判断。扫描上限既覆盖一篇普通笔记，
    // 也避免超长导入文本在每次编辑时重复进行不必要的全文正则匹配。
    val sample = content.take(24_000)
    val lines = sample.lineSequence().toList()
    val patterns = MarkdownDetectionPatterns

    var score = 0

    // 块级语法清晰明确：任意一项即可超过判定阈值。
    if (lines.any { it.matches(patterns.atxHeading) }) score += 3
    if (lines.any { it.matches(patterns.fencedCode) }) score += 3
    if (lines.any { it.matches(patterns.taskItem) }) score += 3
    if (lines.any { it.matches(patterns.quote) }) score += 3
    if (lines.any { it.trim().matches(patterns.horizontalRule) }) score += 3
    if (lines.zipWithNext().any { (_, next) -> next.trim().matches(patterns.setextUnderline) }) score += 3
    if (lines.zipWithNext().any { (header, separator) ->
            header.contains('|') &&
                separator.trim().removePrefix("|").removeSuffix("|").split('|').all {
                    it.trim().matches(patterns.tableSeparator)
                }
        }
    ) score += 3

    // 单个“1. 文本”在普通笔记中很常见，因此仅连续列表才作为强信号。
    if (lines.count { it.matches(patterns.listItem) } >= 2) score += 3

    // 图片与标准 Markdown 链接具有清晰结构；粗体、删除线和行内代码也使用成对标记。
    score += patterns.link.findAll(sample).count() * 3
    score += patterns.image.findAll(sample).count() * 3
    score += patterns.strongInline.findAll(sample).count() * 2

    // 单个星号或下划线容易是普通标点、公式或路径的一部分，至少需要两个斜体信号。
    score += patterns.italic.findAll(sample).count()
    return score >= 2
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
        val name = displayName(context, uri)
        ImportReadResult.Success(
            ImportedNoteText(
                title = name.substringBeforeLast('.', name),
                content = content,
                formatMode = formatForFileName(name)
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
