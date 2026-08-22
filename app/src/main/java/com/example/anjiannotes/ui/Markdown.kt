package com.example.anjiannotes.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

private sealed interface MarkdownBlock {
    data class Line(val content: String, val startOffset: Int) : MarkdownBlock
    data class Table(val header: List<String>, val rows: List<List<String>>, val startOffset: Int) : MarkdownBlock
}

@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier,
    onLinkLongPress: (String) -> Unit = {},
    onLinkClick: (String) -> Unit = {},
    onDoubleClickAt: (Int) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    if (blocks.isEmpty() || markdown.isBlank()) {
        Text(
            text = "开始输入 Markdown…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Line -> MarkdownLine(block.content, block.startOffset, onLinkLongPress, onLinkClick, onDoubleClickAt, onClick)
                is MarkdownBlock.Table -> MarkdownTable(block, onLinkLongPress, onLinkClick, onDoubleClickAt, onClick)
            }
        }
    }
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.lineSequence().toList()
    val result = mutableListOf<MarkdownBlock>()
    var index = 0
    var offset = 0
    while (index < lines.size) {
        val current = lines[index]
        val blockStart = offset
        if (index + 1 < lines.size && isTableRow(current) && isTableSeparator(lines[index + 1])) {
            val header = parseTableRow(current)
            val rows = mutableListOf<List<String>>()
            offset += current.length + 1
            offset += lines[index + 1].length + 1
            index += 2
            while (index < lines.size && isTableRow(lines[index])) {
                rows += parseTableRow(lines[index])
                offset += lines[index].length + 1
                index++
            }
            result += MarkdownBlock.Table(header, rows, blockStart)
        } else {
            result += MarkdownBlock.Line(current, blockStart)
            index++
            offset += current.length + 1
        }
    }
    return result
}

private fun isTableRow(line: String): Boolean = line.count { it == '|' } >= 2

private fun isTableSeparator(line: String): Boolean {
    val cells = parseTableRow(line)
    return cells.isNotEmpty() && cells.all { cell -> cell.matches(Regex("^:?-{3,}:?$")) }
}

private fun parseTableRow(line: String): List<String> = line
    .trim()
    .removePrefix("|")
    .removeSuffix("|")
    .split('|')
    .map { it.trim() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarkdownTable(
    table: MarkdownBlock.Table,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onClick: () -> Unit
) {
    val blockText = buildString {
        append(table.header.joinToString(" | "))
        table.rows.forEach { append('\n').append(it.joinToString(" | ")) }
    }
    val link = remember(blockText) { extractFirstLink(blockText) }
    val modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .combinedClickable(
            onClick = { link?.let(onLinkClick) ?: onClick() },
            onDoubleClick = { onDoubleClickAt(table.startOffset) },
            onLongClick = { link?.let(onLinkLongPress) }
        )
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            MarkdownTableRow(table.header, header = true)
            table.rows.forEach { row ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                MarkdownTableRow(row, header = false)
            }
        }
    }
}

@Composable
private fun MarkdownTableRow(cells: List<String>, header: Boolean) {
    val linkColor = previewLinkColor(darkTheme = true)
    Row(verticalAlignment = Alignment.Top) {
        cells.forEach { cell ->
            Text(
                text = markdownInline(cell, linkColor),
                modifier = Modifier.widthIn(min = 104.dp, max = 240.dp).padding(horizontal = 10.dp, vertical = 9.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                color = if (header) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarkdownLine(
    line: String,
    startOffset: Int,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onClick: () -> Unit
) {
    val link = remember(line) { extractFirstLink(line) }
    val linkColor = previewLinkColor(darkTheme = true)
    var textLayout by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    val lineModifier = Modifier.pointerInput(line) {
        detectTapGestures(
            onTap = { if (link != null) onLinkClick(link) else onClick() },
            onDoubleTap = { position ->
                val localOffset = textLayout?.getOffsetForPosition(position) ?: 0
                onDoubleClickAt((startOffset + localOffset).coerceIn(startOffset, startOffset + line.length))
            },
            onLongPress = { link?.let(onLinkLongPress) }
        )
    }
    when {
        line.startsWith("### ") -> MarkdownLineText(markdownInline(line.removePrefix("### "), linkColor), MaterialTheme.typography.titleMedium, lineModifier, FontWeight.Bold, onTextLayout = { textLayout = it })
        line.startsWith("## ") -> MarkdownLineText(markdownInline(line.removePrefix("## "), linkColor), MaterialTheme.typography.titleLarge, lineModifier, FontWeight.Bold, onTextLayout = { textLayout = it })
        line.startsWith("# ") -> MarkdownLineText(markdownInline(line.removePrefix("# "), linkColor), MaterialTheme.typography.headlineSmall, lineModifier, FontWeight.Bold, onTextLayout = { textLayout = it })
        line.trim() == "---" || line.trim() == "***" -> Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        line.startsWith("> ") -> Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            MarkdownLineText(markdownInline(line.removePrefix("> "), linkColor), MaterialTheme.typography.bodyMedium, lineModifier.padding(horizontal = 12.dp, vertical = 9.dp), onTextLayout = { textLayout = it })
        }
        line.matches(Regex("^[-+*]\\s+.*")) -> Row(verticalAlignment = Alignment.Top) {
            Text("•", modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
            MarkdownLineText(markdownInline(line.replaceFirst(Regex("^[-+*]\\s+"), ""), linkColor), MaterialTheme.typography.bodyMedium, lineModifier, onTextLayout = { textLayout = it })
        }
        line.matches(Regex("^\\d+\\.\\s+.*")) -> {
            val prefix = line.substringBefore(' ')
            Row(verticalAlignment = Alignment.Top) {
                Text(prefix, modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                MarkdownLineText(markdownInline(line.removePrefix("$prefix "), linkColor), MaterialTheme.typography.bodyMedium, lineModifier, onTextLayout = { textLayout = it })
            }
        }
        else -> MarkdownLineText(markdownInline(line, linkColor), MaterialTheme.typography.bodyMedium, lineModifier, onTextLayout = { textLayout = it })
    }
}

@Composable
private fun MarkdownLineText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier,
    weight: FontWeight? = null,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(text = text, style = style, fontWeight = weight, onTextLayout = onTextLayout, modifier = modifier)
}

private val MarkdownLinkPattern = Regex("\\[([^\\]]+)\\]\\((https?://[^\\s)]+)\\)", RegexOption.IGNORE_CASE)
private val UrlPattern = Regex("https?://[^\\s)]+", RegexOption.IGNORE_CASE)
private val PreviewLinkBlueLight = Color(0xFF0B57D0)
private val PreviewLinkBlueDark = Color(0xFF8AB4F8)

fun previewLinkColor(darkTheme: Boolean): Color = if (darkTheme) PreviewLinkBlueDark else PreviewLinkBlueLight

fun linkifyPlainText(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    while (cursor < text.length) {
        val url = UrlPattern.find(text, cursor)
        if (url != null && url.range.first == cursor) {
            pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
            append(url.value)
            pop()
            cursor = url.range.last + 1
        } else {
            append(text[cursor])
            cursor++
        }
    }
}

private fun markdownInline(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    while (cursor < text.length) {
        val markdownLink = MarkdownLinkPattern.find(text, cursor)
        if (markdownLink != null && markdownLink.range.first == cursor) {
            pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
            append(markdownLink.groupValues[1])
            pop()
            cursor = markdownLink.range.last + 1
            continue
        }
        val url = UrlPattern.find(text, cursor)
        if (url != null && url.range.first == cursor) {
            pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
            append(url.value)
            pop()
            cursor = url.range.last + 1
            continue
        }
        val marker = listOf("**", "~~", "`", "*").firstOrNull { text.startsWith(it, cursor) }
        if (marker == null) {
            append(text[cursor])
            cursor++
            continue
        }
        val closing = text.indexOf(marker, cursor + marker.length)
        if (closing <= cursor + marker.length) {
            append(marker)
            cursor += marker.length
            continue
        }
        val content = text.substring(cursor + marker.length, closing)
        val style = when (marker) {
            "**" -> SpanStyle(fontWeight = FontWeight.Bold)
            "*" -> SpanStyle(fontStyle = FontStyle.Italic)
            "~~" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            else -> SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x18000000))
        }
        pushStyle(style)
        append(content)
        pop()
        cursor = closing + marker.length
    }
}

fun extractFirstLink(text: String): String? {
    val markdownLink = MarkdownLinkPattern.find(text)?.groupValues?.getOrNull(2)
    if (!markdownLink.isNullOrBlank()) return markdownLink
    return UrlPattern.find(text)?.value
}

fun extractLinkAt(text: String, offset: Int): String? {
    MarkdownLinkPattern.findAll(text).forEach { match ->
        if (offset in match.range) return match.groupValues.getOrNull(2)
    }
    UrlPattern.findAll(text).forEach { match ->
        if (offset in match.range) return match.value
    }
    return null
}

fun markdownToPlainText(markdown: String): String = markdown
    .lineSequence()
    .joinToString(" ") { line ->
        line.replace(Regex("^\\s{0,3}(#{1,6}|>|[-+*]|\\d+\\.)\\s+"), "")
            .replace(Regex("\\[([^\\]]+)]\\((https?://[^\\s)]+)\\)"), "${'$'}1")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "${'$'}1")
            .replace(Regex("~~(.*?)~~"), "${'$'}1")
            .replace(Regex("`(.*?)`"), "${'$'}1")
            .replace(Regex("\\*(.*?)\\*"), "${'$'}1")
    }
    .replace(Regex("\\s+"), " ")
    .trim()

@Composable
fun MarkdownSyntaxHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = "# 标题   **粗体**   *斜体*   - 列表   > 引用   | 表格 |",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
