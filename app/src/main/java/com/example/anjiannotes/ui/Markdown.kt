package com.example.anjiannotes.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import java.util.concurrent.atomic.AtomicReference

private sealed interface MarkdownBlock {
    data class Line(val content: String, val startOffset: Int) : MarkdownBlock
    data class Code(val content: String, val language: String, val startOffset: Int) : MarkdownBlock
    data class Table(val header: List<String>, val rows: List<List<String>>, val startOffset: Int) : MarkdownBlock
}

@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier,
    enableTextSelection: Boolean = false,
    onLinkLongPress: (String) -> Unit = {},
    onLinkClick: (String) -> Unit = {},
    onDoubleClickAt: (Int) -> Unit = {},
    onLongPress: () -> Unit = {},
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            key(
                when (block) {
                    is MarkdownBlock.Line -> block.startOffset
                    is MarkdownBlock.Code -> block.startOffset
                    is MarkdownBlock.Table -> block.startOffset
                }
            ) {
                when (block) {
                    is MarkdownBlock.Line -> MarkdownLine(block.content, block.startOffset, enableTextSelection, onLinkLongPress, onLinkClick, onDoubleClickAt, onLongPress, onClick)
                    is MarkdownBlock.Code -> MarkdownCodeBlock(block, enableTextSelection, onLinkLongPress, onLinkClick, onDoubleClickAt, onLongPress, onClick)
                    is MarkdownBlock.Table -> MarkdownTable(block, enableTextSelection, onLinkLongPress, onLinkClick, onDoubleClickAt, onLongPress, onClick)
                }
            }
        }
    }
}

internal fun markdownPreviewBlockCount(markdown: String): Int = parseMarkdownBlocks(markdown).size

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.lineSequence().toList()
    val result = mutableListOf<MarkdownBlock>()
    var index = 0
    var offset = 0
    while (index < lines.size) {
        val current = lines[index]
        val blockStart = offset
        if (current.startsWith("```") || current.startsWith("~~~")) {
            val fence = current.take(3)
            val language = current.drop(3).trim()
            val codeLines = mutableListOf<String>()
            offset += current.length + 1
            index++
            while (index < lines.size && !lines[index].startsWith(fence)) {
                codeLines += lines[index]
                offset += lines[index].length + 1
                index++
            }
            if (index < lines.size) {
                offset += lines[index].length + 1
                index++
            }
            result += MarkdownBlock.Code(codeLines.joinToString("\n"), language, blockStart)
        } else if (index + 1 < lines.size && isTableRow(current) && isTableSeparator(lines[index + 1])) {
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
    return cells.isNotEmpty() && cells.all(MarkdownTableSeparatorPattern::matches)
}

private fun parseTableRow(line: String): List<String> = line
    .trim()
    .removePrefix("|")
    .removeSuffix("|")
    .split('|')
    .map { it.trim() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarkdownCodeBlock(
    block: MarkdownBlock.Code,
    enableTextSelection: Boolean,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val interactionModifier = if (enableTextSelection) {
        Modifier.nonConsumingTapGestures(
            onTap = { onClick() },
            onDoubleTap = { onDoubleClickAt(block.startOffset) }
        )
    } else {
        Modifier.combinedClickable(
            onClick = onClick,
            onDoubleClick = { onDoubleClickAt(block.startOffset) },
            onLongClick = onLongPress
        )
    }
    Surface(
        modifier = interactionModifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (block.language.isNotBlank()) {
                Text(
                    text = block.language.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 7.dp)
                )
            }
            Text(
                text = block.content.ifEmpty { " " },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                softWrap = false
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarkdownTable(
    table: MarkdownBlock.Table,
    enableTextSelection: Boolean,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val columnCount = table.header.size.coerceAtLeast(1)
    // Markdown 的数据行可能缺少或多出分隔符。先固定为与表头一致的列数，
    // 防止单独一行改变整张表的宽度和列位置。
    val rows = remember(table.rows, columnCount) {
        table.rows.map { row ->
            List(columnCount) { columnIndex -> row.getOrNull(columnIndex).orEmpty() }
        }
    }
    val header = remember(table.header, columnCount) {
        List(columnCount) { columnIndex -> table.header.getOrNull(columnIndex).orEmpty() }
    }
    val blockText = buildString {
        append(header.joinToString(" | "))
        rows.forEach { append('\n').append(it.joinToString(" | ")) }
    }
    val link = remember(blockText) { extractFirstLink(blockText) }
    val textMeasurer = rememberTextMeasurer()
    val bodyStyle = MaterialTheme.typography.bodySmall
    val headerStyle = bodyStyle.copy(fontWeight = FontWeight.SemiBold)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val columnWidths = remember(header, rows, bodyStyle, headerStyle, density) {
        List(columnCount) { columnIndex ->
            val headerWidth = textMeasurer.measure(
                text = AnnotatedString(header[columnIndex]),
                style = headerStyle,
                maxLines = 1,
                softWrap = false,
                constraints = Constraints()
            ).size.width
            val bodyWidth = rows.maxOfOrNull { row ->
                textMeasurer.measure(
                    text = AnnotatedString(row[columnIndex]),
                    style = bodyStyle,
                    maxLines = 1,
                    softWrap = false,
                    constraints = Constraints()
                ).size.width
            } ?: 0
            with(density) {
                (maxOf(headerWidth, bodyWidth).toDp() + 20.dp).coerceIn(116.dp, 280.dp)
            }
        }
    }
    val surfaceModifier = if (enableTextSelection) {
        Modifier.nonConsumingTapGestures(
            onTap = { link?.let(onLinkClick) ?: onClick() },
            onDoubleTap = { onDoubleClickAt(table.startOffset) }
        )
    } else {
        Modifier.combinedClickable(
            onClick = { link?.let(onLinkClick) ?: onClick() },
            onDoubleClick = { onDoubleClickAt(table.startOffset) },
            onLongClick = { link?.let(onLinkLongPress) ?: onLongPress() }
        )
    }
    // 外层是固定的屏幕视口；内部各行共享同一组列宽，因此可对齐并可横向查看。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = surfaceModifier.wrapContentWidth(unbounded = true),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column {
                MarkdownTableRow(header, columnWidths, header = true)
                rows.forEach { row ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    MarkdownTableRow(row, columnWidths, header = false)
                }
            }
        }
    }
}

@Composable
private fun MarkdownTableRow(cells: List<String>, columnWidths: List<androidx.compose.ui.unit.Dp>, header: Boolean) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    Row(verticalAlignment = Alignment.Top) {
        cells.forEachIndexed { index, cell ->
            Text(
                text = markdownInline(cell, linkColor, codeBackground),
                modifier = Modifier.width(columnWidths[index]).padding(horizontal = 10.dp, vertical = 9.dp),
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
    enableTextSelection: Boolean,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val link = remember(line) { extractFirstLink(line) }
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val textLayout = remember(line) { AtomicReference<TextLayoutResult?>(null) }
    val textLayoutCallback: (TextLayoutResult) -> Unit = { layout -> textLayout.set(layout) }
    val lineModifier = if (enableTextSelection) {
        // 不消费长按与拖动，让父级 SelectionContainer 接管系统选择菜单和手柄。
        Modifier.nonConsumingTapGestures(
            onTap = { if (link != null) onLinkClick(link) else onClick() },
            onDoubleTap = { position ->
                val localOffset = textLayout.get()?.getOffsetForPosition(position) ?: 0
                onDoubleClickAt((startOffset + localOffset).coerceIn(startOffset, startOffset + line.length))
            }
        )
    } else {
        Modifier.pointerInput(line) {
            detectTapGestures(
                onTap = { if (link != null) onLinkClick(link) else onClick() },
                onDoubleTap = { position ->
                    val localOffset = textLayout.get()?.getOffsetForPosition(position) ?: 0
                    onDoubleClickAt((startOffset + localOffset).coerceIn(startOffset, startOffset + line.length))
                },
                onLongPress = { link?.let(onLinkLongPress) ?: onLongPress() }
            )
        }
    }
    when {
        line.startsWith("### ") -> MarkdownLineText(markdownInline(line.removePrefix("### "), linkColor, codeBackground), MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 27.sp), lineModifier.padding(top = 4.dp), FontWeight.SemiBold, onTextLayout = textLayoutCallback)
        line.startsWith("## ") -> MarkdownLineText(markdownInline(line.removePrefix("## "), linkColor, codeBackground), MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 31.sp), lineModifier.padding(top = 7.dp), FontWeight.SemiBold, onTextLayout = textLayoutCallback)
        line.startsWith("# ") -> MarkdownLineText(markdownInline(line.removePrefix("# "), linkColor, codeBackground), MaterialTheme.typography.headlineSmall.copy(fontSize = 27.sp, lineHeight = 36.sp), lineModifier.padding(top = 10.dp), FontWeight.SemiBold, onTextLayout = textLayoutCallback)
        line.trim() == "---" || line.trim() == "***" -> Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        line.startsWith("> ") -> Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), shape = RoundedCornerShape(3.dp)) {
            MarkdownLineText(markdownInline(line.removePrefix("> "), linkColor, codeBackground), MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 29.sp), lineModifier.padding(horizontal = 13.dp, vertical = 9.dp), onTextLayout = textLayoutCallback)
        }
        line.matches(MarkdownUnorderedListPattern) -> MarkdownLineText(
            markdownListItem(
                marker = "•",
                content = line.replaceFirst(MarkdownUnorderedListPrefixPattern, ""),
                markerStyle = SpanStyle(color = MaterialTheme.colorScheme.primary),
                linkColor = linkColor,
                codeBackground = codeBackground
            ),
            MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 29.sp),
            lineModifier.fillMaxWidth(),
            onTextLayout = textLayoutCallback
        )
        line.matches(MarkdownOrderedListPattern) -> {
            val prefixMatch = MarkdownOrderedListPrefixPattern.find(line) ?: return
            MarkdownLineText(
                markdownListItem(
                    marker = prefixMatch.groupValues[1],
                    content = line.removeRange(prefixMatch.range),
                    markerStyle = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold),
                    linkColor = linkColor,
                    codeBackground = codeBackground
                ),
                MaterialTheme.typography.bodyMedium,
                lineModifier.fillMaxWidth(),
                onTextLayout = textLayoutCallback
            )
        }
        else -> MarkdownLineText(markdownInline(line, linkColor, codeBackground), MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 29.sp), lineModifier, onTextLayout = textLayoutCallback)
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

private val MarkdownTableSeparatorPattern = Regex("^:?-{3,}:?$")
private val MarkdownUnorderedListPattern = Regex("^[-+*]\\s+.*")
private val MarkdownUnorderedListPrefixPattern = Regex("^[-+*]\\s+")
private val MarkdownOrderedListPattern = Regex("^\\d+\\.\\s+.*")
private val MarkdownOrderedListPrefixPattern = Regex("^(\\d+\\.)\\s+")
private val MarkdownLinkPattern = Regex("\\[([^\\]]+)\\]\\((https?://[^\\s)]+)\\)", RegexOption.IGNORE_CASE)
private val UrlPattern = Regex("https?://[^\\s)]+", RegexOption.IGNORE_CASE)
private val PreviewLinkBlueLight = Color(0xFF765F82)
private val PreviewLinkBlueDark = Color(0xFFC7B1CF)

/** 兼容旧备份与单元测试；实际预览界面直接使用 MaterialTheme.colorScheme.primary。 */
fun previewLinkColor(darkTheme: Boolean): Color = if (darkTheme) PreviewLinkBlueDark else PreviewLinkBlueLight
fun linkifyPlainText(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    UrlPattern.findAll(text).forEach { url ->
        append(text, cursor, url.range.first)
        pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
        append(url.value)
        pop()
        cursor = url.range.last + 1
    }
    append(text, cursor, text.length)
}

private fun markdownListItem(
    marker: String,
    content: String,
    markerStyle: SpanStyle,
    linkColor: Color,
    codeBackground: Color
): AnnotatedString = buildAnnotatedString {
    pushStyle(markerStyle)
    append(marker)
    pop()
    append(" ")
    append(markdownInline(content, linkColor, codeBackground))
}

private fun markdownInline(text: String, linkColor: Color, codeBackground: Color): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    while (cursor < text.length) {
        val markdownLink = if (text[cursor] == '[') MarkdownLinkPattern.matchAt(text, cursor) else null
        if (markdownLink != null) {
            pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
            append(markdownLink.groupValues[1])
            pop()
            cursor = markdownLink.range.last + 1
            continue
        }
        val url = if (text[cursor].lowercaseChar() == 'h') UrlPattern.matchAt(text, cursor) else null
        if (url != null) {
            pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
            append(url.value)
            pop()
            cursor = url.range.last + 1
            continue
        }
        val marker = when {
            text.startsWith("**", cursor) -> "**"
            text.startsWith("~~", cursor) -> "~~"
            text.startsWith("`", cursor) -> "`"
            text.startsWith("*", cursor) -> "*"
            else -> null
        }
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
            else -> SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
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

/*
 * 主页列表只展示两行摘要：限制输入并复用预编译规则，避免新卡片进入可视区时
 * 对整篇 Markdown 做全文解析。详情页的 Markdown 编辑与预览路径不使用此函数。
 */
private const val ListPreviewSourceLimit = 640
private const val ListPreviewOutputLimit = 360
private val ListPreviewLinePrefixPattern = Regex("^\\s{0,3}(?:#{1,6}|>|[-+*]|\\d+\\.)\\s+")
private val ListPreviewStrongPattern = Regex("\\*\\*(.*?)\\*\\*")
private val ListPreviewStrikePattern = Regex("~~(.*?)~~")
private val ListPreviewCodePattern = Regex("`(.*?)`")
private val ListPreviewEmphasisPattern = Regex("\\*(.*?)\\*")
private val ListPreviewWhitespacePattern = Regex("\\s+")

fun markdownToListPreview(markdown: String): String = buildListPreview(markdown) { line ->
    line.replace(ListPreviewLinePrefixPattern, "")
        .replace(MarkdownLinkPattern, "${'$'}1")
        .replace(ListPreviewStrongPattern, "${'$'}1")
        .replace(ListPreviewStrikePattern, "${'$'}1")
        .replace(ListPreviewCodePattern, "${'$'}1")
        .replace(ListPreviewEmphasisPattern, "${'$'}1")
}

fun plainTextToListPreview(text: String): String = buildListPreview(text) { it }

private fun buildListPreview(text: String, lineTransform: (String) -> String): String {
    if (text.isEmpty()) return ""
    return text
        .take(ListPreviewSourceLimit)
        .lineSequence()
        .joinToString(" ") { lineTransform(it) }
        .replace(ListPreviewWhitespacePattern, " ")
        .trim()
        .take(ListPreviewOutputLimit)
}

@Composable
fun MarkdownSyntaxHint(modifier: Modifier = Modifier) {
    Text(
        text = "Markdown：# 标题 · **粗体** · *斜体* · - 列表",
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
    )
}
