package com.example.anjiannotes.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.atomic.AtomicReference

/**
 * 轻量 Markdown 文档模型。解析层只负责将原始文本分为文章块；Compose 层只负责排版与交互，
 * 避免“一行对应一个 UI 组件”造成的间距混乱和普通重组中的重复解析。
 */
internal data class MarkdownDocument(internal val blocks: List<MarkdownBlock>)

internal sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: SourceText) : MarkdownBlock
    data class Paragraph(val text: SourceText) : MarkdownBlock
    data class Quote(val lines: List<SourceText>) : MarkdownBlock
    data class ListBlock(val items: List<MarkdownListItem>) : MarkdownBlock
    data class Code(val language: String, val content: SourceText, val startOffset: Int) : MarkdownBlock
    data class Table(
        val header: List<SourceText>,
        val rows: List<List<SourceText>>,
        val alignments: List<TableAlignment>,
        val startOffset: Int
    ) : MarkdownBlock
    data object Divider : MarkdownBlock
}

internal data class MarkdownListItem(
    val marker: String,
    val depth: Int,
    val text: SourceText
)

internal enum class TableAlignment { START, CENTER, END }

/** 渲染文本与原始 Markdown 字符位置的映射，用于预览双击回到正确光标位置。 */
internal data class SourceText(
    val text: String,
    private val sourceOffsets: IntArray
) {
    init {
        require(text.length == sourceOffsets.size)
    }

    fun sourceOffsetAt(displayOffset: Int): Int {
        if (sourceOffsets.isEmpty()) return 0
        return sourceOffsets[displayOffset.coerceIn(0, sourceOffsets.lastIndex)]
    }

    fun containsSourceOffset(sourceOffset: Int): Boolean =
        sourceOffsets.isNotEmpty() && sourceOffset in sourceOffsets.first()..sourceOffsets.last()
}

private fun MarkdownBlock.containsSourceOffset(sourceOffset: Int): Boolean = when (this) {
    is MarkdownBlock.Heading -> text.containsSourceOffset(sourceOffset)
    is MarkdownBlock.Paragraph -> text.containsSourceOffset(sourceOffset)
    is MarkdownBlock.Quote -> lines.any { it.containsSourceOffset(sourceOffset) }
    is MarkdownBlock.ListBlock -> items.any { it.text.containsSourceOffset(sourceOffset) }
    is MarkdownBlock.Code -> content.containsSourceOffset(sourceOffset)
    is MarkdownBlock.Table -> header.any { it.containsSourceOffset(sourceOffset) } ||
        rows.any { row -> row.any { it.containsSourceOffset(sourceOffset) } }
    MarkdownBlock.Divider -> false
}


private data class SourceLine(
    val text: String,
    val startOffset: Int,
    val contentEndOffset: Int
) {
    fun asSourceText(startIndex: Int = 0): SourceText {
        val safeStart = startIndex.coerceIn(0, text.length)
        val value = text.substring(safeStart)
        return SourceText(value, IntArray(value.length) { startOffset + safeStart + it })
    }
}

private data class InlineContent(
    val text: AnnotatedString,
    val sourceOffsets: IntArray,
    val links: List<InlineLink>
) {
    fun sourceOffsetAt(displayOffset: Int): Int {
        if (sourceOffsets.isEmpty()) return 0
        return sourceOffsets[displayOffset.coerceIn(0, sourceOffsets.lastIndex)]
    }

    fun linkAt(displayOffset: Int): String? = links.firstOrNull {
        displayOffset in it.start until it.endExclusive
    }?.url
}

private data class InlineLink(val start: Int, val endExclusive: Int, val url: String)

private val MarkdownHeadingPattern = Regex("^(#{1,6})\\s+(.*)$")
private val MarkdownListPattern = Regex("^(\\s*)([-+*]|\\d+\\.)\\s+(.*)$")
private val MarkdownDividerPattern = Regex("^\\s{0,3}(?:---+|\\*\\*\\*+|___+)\\s*$")
private val MarkdownLinkPattern = Regex("\\[([^\\]]+)]\\((https?://[^\\s)]+)\\)", RegexOption.IGNORE_CASE)
private val UrlPattern = Regex("https?://[^\\s)]+", RegexOption.IGNORE_CASE)
private val MarkdownTableSeparatorPattern = Regex("^:?-{3,}:?$")
private val ListPreviewSourceLimit = 640
private val ListPreviewOutputLimit = 360
private val ListPreviewLinePrefixPattern = Regex("^\\s{0,3}(?:#{1,6}|>|[-+*]|\\d+\\.)\\s+")
private val ListPreviewStrongPattern = Regex("(?:\\*\\*|__)(.*?)\\1")
private val ListPreviewStrikePattern = Regex("~~(.*?)~~")
private val ListPreviewCodePattern = Regex("`(.*?)`")
private val ListPreviewEmphasisPattern = Regex("(?:\\*|_)(.*?)\\1")
private val ListPreviewWhitespacePattern = Regex("\\s+")
private val PreviewLinkBlueLight = Color(0xFF765F82)
private val PreviewLinkBlueDark = Color(0xFFC7B1CF)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier,
    enableTextSelection: Boolean = true,
    onLinkLongPress: (String) -> Unit = {},
    onLinkClick: (String) -> Unit = {},
    onDoubleClickAt: (Int) -> Unit = {},
    initialSourceOffset: Int? = null,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val document = remember(markdown) { MarkdownParser.parse(markdown) }
    val initialBlockIndex = remember(document, initialSourceOffset) {
        initialSourceOffset?.let { offset -> document.blocks.indexOfFirst { it.containsSourceOffset(offset) } }
            ?.takeIf { it >= 0 }
    }
    if (document.blocks.isEmpty()) {
        Text(
            text = "开始输入 Markdown…",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 29.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }

    val content: @Composable () -> Unit = {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            document.blocks.forEachIndexed { index, block ->
                key(index, block) {
                    val blockRequester = remember { BringIntoViewRequester() }
                    if (index == initialBlockIndex) {
                        LaunchedEffect(blockRequester, initialBlockIndex) {
                            withFrameNanos { }
                            blockRequester.bringIntoView()
                        }
                    }
                    Box(modifier = Modifier.bringIntoViewRequester(blockRequester)) {
                        MarkdownBlockRenderer(
                            block = block,
                            enableTextSelection = enableTextSelection,
                            onLinkLongPress = onLinkLongPress,
                            onLinkClick = onLinkClick,
                            onDoubleClickAt = onDoubleClickAt,
                            onLongPress = onLongPress,
                            onClick = onClick
                        )
                    }
                }
            }
        }
    }
    if (enableTextSelection) SelectionContainer(content = content) else content()
}

@Composable
private fun MarkdownBlockRenderer(
    block: MarkdownBlock,
    enableTextSelection: Boolean,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    when (block) {
        is MarkdownBlock.Heading -> MarkdownHeading(
            heading = block,
            enableTextSelection = enableTextSelection,
            onLinkLongPress = onLinkLongPress,
            onLinkClick = onLinkClick,
            onDoubleClickAt = onDoubleClickAt,
            onLongPress = onLongPress,
            onClick = onClick
        )
        is MarkdownBlock.Paragraph -> MarkdownInteractiveText(
            source = block.text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 29.sp),
            enableTextSelection = enableTextSelection,
            onLinkLongPress = onLinkLongPress,
            onLinkClick = onLinkClick,
            onDoubleClickAt = onDoubleClickAt,
            onLongPress = onLongPress,
            onClick = onClick
        )
        is MarkdownBlock.Quote -> MarkdownQuote(
            quote = block,
            enableTextSelection = enableTextSelection,
            onLinkLongPress = onLinkLongPress,
            onLinkClick = onLinkClick,
            onDoubleClickAt = onDoubleClickAt,
            onLongPress = onLongPress,
            onClick = onClick
        )
        is MarkdownBlock.ListBlock -> MarkdownList(
            list = block,
            enableTextSelection = enableTextSelection,
            onLinkLongPress = onLinkLongPress,
            onLinkClick = onLinkClick,
            onDoubleClickAt = onDoubleClickAt,
            onLongPress = onLongPress,
            onClick = onClick
        )
        is MarkdownBlock.Code -> MarkdownCodeBlock(
            block = block,
            enableTextSelection = enableTextSelection,
            onLinkLongPress = onLinkLongPress,
            onLinkClick = onLinkClick,
            onDoubleClickAt = onDoubleClickAt,
            onLongPress = onLongPress,
            onClick = onClick
        )
        is MarkdownBlock.Table -> MarkdownTable(
            table = block,
            enableTextSelection = enableTextSelection,
            onLinkLongPress = onLinkLongPress,
            onLinkClick = onLinkClick,
            onDoubleClickAt = onDoubleClickAt,
            onLongPress = onLongPress,
            onClick = onClick
        )
        MarkdownBlock.Divider -> HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun MarkdownHeading(
    heading: MarkdownBlock.Heading,
    enableTextSelection: Boolean,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val style = when (heading.level) {
        1 -> MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp, lineHeight = 39.sp)
        2 -> MaterialTheme.typography.titleLarge.copy(fontSize = 25.sp, lineHeight = 34.sp)
        3 -> MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, lineHeight = 30.sp)
        4 -> MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, lineHeight = 28.sp)
        5 -> MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 27.sp)
        else -> MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp)
    }
    val topPadding = when (heading.level) {
        1 -> 20.dp
        2 -> 16.dp
        3 -> 12.dp
        else -> 9.dp
    }
    MarkdownInteractiveText(
        source = heading.text,
        style = style,
        fontWeight = if (heading.level <= 2) FontWeight.Bold else FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(top = topPadding, bottom = 3.dp),
        enableTextSelection = enableTextSelection,
        onLinkLongPress = onLinkLongPress,
        onLinkClick = onLinkClick,
        onDoubleClickAt = onDoubleClickAt,
        onLongPress = onLongPress,
        onClick = onClick
    )
}

@Composable
private fun MarkdownQuote(
    quote: MarkdownBlock.Quote,
    enableTextSelection: Boolean,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Spacer(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.84f))
        )
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            quote.lines.forEach { line ->
                MarkdownInteractiveText(
                    source = line,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 29.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    enableTextSelection = enableTextSelection,
                    onLinkLongPress = onLinkLongPress,
                    onLinkClick = onLinkClick,
                    onDoubleClickAt = onDoubleClickAt,
                    onLongPress = onLongPress,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
private fun MarkdownList(
    list: MarkdownBlock.ListBlock,
    enableTextSelection: Boolean,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        list.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = (item.depth.coerceAtMost(5) * 18).dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.marker,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 29.sp),
                    fontWeight = if (item.marker.lastOrNull() == '.') FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(if (item.marker.lastOrNull() == '.') 28.dp else 20.dp)
                )
                MarkdownInteractiveText(
                    source = item.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 29.sp),
                    modifier = Modifier.weight(1f),
                    enableTextSelection = enableTextSelection,
                    onLinkLongPress = onLinkLongPress,
                    onLinkClick = onLinkClick,
                    onDoubleClickAt = onDoubleClickAt,
                    onLongPress = onLongPress,
                    onClick = onClick
                )
            }
        }
    }
}

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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            if (block.language.isNotBlank()) {
                Text(
                    text = block.language.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            MarkdownInteractiveText(
                source = block.content,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 22.sp),
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                parseFormatting = false,
                enableTextSelection = enableTextSelection,
                onLinkLongPress = onLinkLongPress,
                onLinkClick = onLinkClick,
                onDoubleClickAt = onDoubleClickAt,
                onLongPress = onLongPress,
                onClick = onClick
            )
        }
    }
}

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
    val rows = remember(table.rows, columnCount) {
        table.rows.map { row -> List(columnCount) { columnIndex -> row.getOrElse(columnIndex) { SourceText("", IntArray(0)) } } }
    }
    val header = remember(table.header, columnCount) {
        List(columnCount) { columnIndex -> table.header.getOrElse(columnIndex) { SourceText("", IntArray(0)) } }
    }
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val bodyStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp)
    val headerStyle = bodyStyle.copy(fontWeight = FontWeight.SemiBold)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val columnWidths = remember(header, rows, bodyStyle, headerStyle, density) {
        List(columnCount) { columnIndex ->
            val headerWidth = textMeasurer.measure(
                text = AnnotatedString(header[columnIndex].text),
                style = headerStyle,
                maxLines = 1,
                softWrap = false,
                constraints = Constraints()
            ).size.width
            val bodyWidth = rows.maxOfOrNull { row ->
                textMeasurer.measure(
                    text = AnnotatedString(row[columnIndex].text),
                    style = bodyStyle,
                    maxLines = 1,
                    softWrap = false,
                    constraints = Constraints()
                ).size.width
            } ?: 0
            with(density) { (maxOf(headerWidth, bodyWidth).toDp() + 20.dp).coerceIn(92.dp, 260.dp) }
        }
    }
    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Surface(
            modifier = Modifier.wrapContentWidth(unbounded = true),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column {
                MarkdownTableRow(
                    cells = header,
                    columnWidths = columnWidths,
                    alignments = table.alignments,
                    header = true,
                    enableTextSelection = enableTextSelection,
                    onLinkLongPress = onLinkLongPress,
                    onLinkClick = onLinkClick,
                    onDoubleClickAt = onDoubleClickAt,
                    onLongPress = onLongPress,
                    onClick = onClick
                )
                rows.forEach { row ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    MarkdownTableRow(
                        cells = row,
                        columnWidths = columnWidths,
                        alignments = table.alignments,
                        header = false,
                        enableTextSelection = enableTextSelection,
                        onLinkLongPress = onLinkLongPress,
                        onLinkClick = onLinkClick,
                        onDoubleClickAt = onDoubleClickAt,
                        onLongPress = onLongPress,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownTableRow(
    cells: List<SourceText>,
    columnWidths: List<androidx.compose.ui.unit.Dp>,
    alignments: List<TableAlignment>,
    header: Boolean,
    enableTextSelection: Boolean,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        cells.forEachIndexed { index, cell ->
            MarkdownInteractiveText(
                source = cell,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                color = if (header) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = when (alignments.getOrElse(index) { TableAlignment.START }) {
                    TableAlignment.START -> TextAlign.Start
                    TableAlignment.CENTER -> TextAlign.Center
                    TableAlignment.END -> TextAlign.End
                },
                modifier = Modifier.width(columnWidths[index]).padding(horizontal = 10.dp, vertical = 8.dp),
                enableTextSelection = enableTextSelection,
                onLinkLongPress = onLinkLongPress,
                onLinkClick = onLinkClick,
                onDoubleClickAt = onDoubleClickAt,
                onLongPress = onLongPress,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun MarkdownInteractiveText(
    source: SourceText,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign? = null,
    parseFormatting: Boolean = true,
    enableTextSelection: Boolean,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val inline = remember(source, linkColor, parseFormatting) {
        if (parseFormatting) parseMarkdownInline(source, linkColor) else rawInlineContent(source)
    }
    val textLayout = remember(source, inline.text) { AtomicReference<TextLayoutResult?>(null) }
    fun displayOffsetAt(position: androidx.compose.ui.geometry.Offset): Int =
        textLayout.get()?.getOffsetForPosition(position) ?: 0
    val interactionModifier = if (enableTextSelection) {
        Modifier.nonConsumingTapGestures(
            onTap = { position ->
                inline.linkAt(displayOffsetAt(position))?.let(onLinkClick) ?: onClick()
            },
            onDoubleTap = { position -> onDoubleClickAt(inline.sourceOffsetAt(displayOffsetAt(position))) }
        )
    } else {
        Modifier.pointerInput(inline) {
            detectTapGestures(
                onTap = { position ->
                    inline.linkAt(displayOffsetAt(position))?.let(onLinkClick) ?: onClick()
                },
                onDoubleTap = { position -> onDoubleClickAt(inline.sourceOffsetAt(displayOffsetAt(position))) },
                onLongPress = { position -> inline.linkAt(displayOffsetAt(position))?.let(onLinkLongPress) ?: onLongPress() }
            )
        }
    }
    Text(
        text = inline.text,
        style = style,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        color = color,
        textAlign = textAlign,
        onTextLayout = { textLayout.set(it) },
        modifier = modifier.then(interactionModifier)
    )
}

/** 解析层与 UI 渲染层分离的唯一入口，便于单元测试覆盖源位置映射。 */
internal object MarkdownParser {
    fun parse(markdown: String): MarkdownDocument {
        val lines = splitSourceLines(markdown)
        val blocks = mutableListOf<MarkdownBlock>()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            if (line.text.isBlank()) {
                index++
                continue
            }
            if (isFence(line.text)) {
                val fence = line.text.trimStart().take(3)
                val language = line.text.trimStart().drop(3).trim()
                val codeLines = mutableListOf<SourceLine>()
                val blockStart = line.startOffset
                index++
                while (index < lines.size && !lines[index].text.trimStart().startsWith(fence)) {
                    codeLines += lines[index]
                    index++
                }
                if (index < lines.size) index++
                blocks += MarkdownBlock.Code(language, sourceTextOf(codeLines), blockStart)
                continue
            }
            val heading = MarkdownHeadingPattern.matchEntire(line.text)
            if (heading != null) {
                val contentRange = heading.groups[2]?.range
                val contentStart = contentRange?.first ?: line.text.length
                blocks += MarkdownBlock.Heading(heading.groupValues[1].length, line.asSourceText(contentStart))
                index++
                continue
            }
            if (MarkdownDividerPattern.matches(line.text)) {
                blocks += MarkdownBlock.Divider
                index++
                continue
            }
            if (isTableStart(lines, index)) {
                val header = splitTableRow(lines[index]).map { it.source }
                val alignments = parseTableAlignments(lines[index + 1])
                index += 2
                val rows = mutableListOf<List<SourceText>>()
                while (index < lines.size && isTableRow(lines[index])) {
                    rows += splitTableRow(lines[index]).map { it.source }
                    index++
                }
                blocks += MarkdownBlock.Table(header, rows, alignments, line.startOffset)
                continue
            }
            if (line.text.trimStart().startsWith(">")) {
                val quoteLines = mutableListOf<SourceText>()
                while (index < lines.size) {
                    val quoteMatch = QuotePrefixPattern.find(lines[index].text) ?: break
                    quoteLines += lines[index].asSourceText(quoteMatch.range.last + 1)
                    index++
                }
                blocks += MarkdownBlock.Quote(quoteLines)
                continue
            }
            if (MarkdownListPattern.matches(line.text)) {
                val items = mutableListOf<MarkdownListItem>()
                while (index < lines.size) {
                    val listMatch = MarkdownListPattern.matchEntire(lines[index].text) ?: break
                    val contentRange = listMatch.groups[3]?.range
                    val contentStart = contentRange?.first ?: lines[index].text.length
                    val rawMarker = listMatch.groupValues[2]
                    items += MarkdownListItem(
                        marker = if (rawMarker.endsWith('.')) rawMarker else "•",
                        depth = (listMatch.groupValues[1].length / 2).coerceAtMost(5),
                        text = lines[index].asSourceText(contentStart)
                    )
                    index++
                }
                blocks += MarkdownBlock.ListBlock(items)
                continue
            }

            val paragraphLines = mutableListOf<SourceLine>()
            while (index < lines.size && !lines[index].text.isBlank() && !startsSpecialBlock(lines, index)) {
                paragraphLines += lines[index]
                index++
            }
            if (paragraphLines.isNotEmpty()) {
                blocks += MarkdownBlock.Paragraph(sourceTextOf(paragraphLines))
            } else {
                // 防御性推进，避免输入不完整的 Markdown 导致解析循环停滞。
                blocks += MarkdownBlock.Paragraph(line.asSourceText())
                index++
            }
        }
        return MarkdownDocument(blocks)
    }
}

private data class TableCell(val source: SourceText, val raw: String)
private val QuotePrefixPattern = Regex("^\\s{0,3}>\\s?")

private fun splitSourceLines(source: String): List<SourceLine> {
    if (source.isEmpty()) return emptyList()
    val lines = mutableListOf<SourceLine>()
    var start = 0
    while (start < source.length) {
        val newLine = source.indexOf('\n', start)
        val rawEnd = if (newLine >= 0) newLine else source.length
        val contentEnd = if (rawEnd > start && source[rawEnd - 1] == '\r') rawEnd - 1 else rawEnd
        lines += SourceLine(source.substring(start, contentEnd), start, contentEnd)
        if (newLine < 0) break
        start = newLine + 1
    }
    return lines
}

private fun sourceTextOf(lines: List<SourceLine>): SourceText {
    if (lines.isEmpty()) return SourceText("", IntArray(0))
    val builder = StringBuilder()
    val offsets = ArrayList<Int>()
    lines.forEachIndexed { index, line ->
        line.text.forEachIndexed { characterIndex, character ->
            builder.append(character)
            offsets += line.startOffset + characterIndex
        }
        if (index != lines.lastIndex) {
            builder.append('\n')
            // CRLF 的展示换行只占一个字符，但下一行首字符仍映射到原文正确的 \n 后位置。
            offsets += line.contentEndOffset
        }
    }
    return SourceText(builder.toString(), offsets.toIntArray())
}

private fun isFence(text: String): Boolean {
    val trimmed = text.trimStart()
    return trimmed.startsWith("```") || trimmed.startsWith("~~~")
}

private fun isTableStart(lines: List<SourceLine>, index: Int): Boolean =
    index + 1 < lines.size && isTableRow(lines[index]) && parseTableAlignments(lines[index + 1]).isNotEmpty()

private fun isTableRow(line: SourceLine): Boolean = splitTableRow(line).size >= 2

private fun startsSpecialBlock(lines: List<SourceLine>, index: Int): Boolean {
    val text = lines[index].text
    return isFence(text) ||
        MarkdownHeadingPattern.matches(text) ||
        MarkdownDividerPattern.matches(text) ||
        isTableStart(lines, index) ||
        QuotePrefixPattern.containsMatchIn(text) ||
        MarkdownListPattern.matches(text)
}

private fun splitTableRow(line: SourceLine): List<TableCell> {
    val text = line.text.trim()
    if (text.isEmpty()) return emptyList()
    val leadingTrim = line.text.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
    val withoutLeading = text.removePrefix("|")
    val content = withoutLeading.removeSuffix("|")
    var sourceIndex = leadingTrim + if (text.startsWith("|")) 1 else 0
    val cells = mutableListOf<TableCell>()
    val value = StringBuilder()
    val offsets = mutableListOf<Int>()
    var escaped = false

    fun addCell() {
        var from = 0
        var to = value.length
        while (from < to && value[from].isWhitespace()) from++
        while (to > from && value[to - 1].isWhitespace()) to--
        val cellText = value.substring(from, to)
        val cellOffsets = IntArray(cellText.length) { offsets[from + it] }
        cells += TableCell(SourceText(cellText, cellOffsets), cellText)
        value.clear()
        offsets.clear()
    }

    content.forEach { character ->
        when {
            escaped -> {
                value.append(character)
                offsets += sourceIndex
                escaped = false
            }
            character == '\\' -> escaped = true
            character == '|' -> addCell()
            else -> {
                value.append(character)
                offsets += sourceIndex
            }
        }
        sourceIndex++
    }
    if (escaped) {
        value.append('\\')
        offsets += sourceIndex - 1
    }
    addCell()
    return cells
}

private fun parseTableAlignments(line: SourceLine): List<TableAlignment> {
    val cells = splitTableRow(line)
    if (cells.isEmpty() || cells.any { !MarkdownTableSeparatorPattern.matches(it.raw) }) return emptyList()
    return cells.map { cell ->
        when {
            cell.raw.startsWith(":") && cell.raw.endsWith(":") -> TableAlignment.CENTER
            cell.raw.endsWith(":") -> TableAlignment.END
            else -> TableAlignment.START
        }
    }
}

private fun rawInlineContent(source: SourceText): InlineContent = InlineContent(
    text = AnnotatedString(source.text),
    sourceOffsets = IntArray(source.text.length) { source.sourceOffsetAt(it) },
    links = UrlPattern.findAll(source.text).map { match -> InlineLink(match.range.first, match.range.last + 1, match.value) }.toList()
)

/** 小型递归 tokenizer，支持常用嵌套强调、链接和转义，并输出显示字符到原文位置的映射。 */
private fun parseMarkdownInline(source: SourceText, linkColor: Color): InlineContent {
    val value = source.text
    val builder = AnnotatedString.Builder()
    val sourceOffsets = mutableListOf<Int>()
    val links = mutableListOf<InlineLink>()

    fun appendRaw(index: Int) {
        builder.append(value[index])
        sourceOffsets += source.sourceOffsetAt(index)
    }

    fun appendRange(start: Int, endExclusive: Int) {
        var cursor = start
        while (cursor < endExclusive) {
            if (value[cursor] == '\\' && cursor + 1 < endExclusive && value[cursor + 1] in "\\`*_[]()|~") {
                appendRaw(cursor + 1)
                cursor += 2
                continue
            }
            val markdownLink = if (value[cursor] == '[') MarkdownLinkPattern.matchAt(value, cursor) else null
            if (markdownLink != null && markdownLink.range.last < endExclusive) {
                val labelRange = markdownLink.groups[1]?.range
                val labelStart = labelRange?.first ?: cursor + 1
                val labelEnd = (labelRange?.last ?: labelStart - 1) + 1
                val url = markdownLink.groupValues[2]
                val displayStart = builder.length
                builder.pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                appendRange(labelStart, labelEnd)
                builder.pop()
                links += InlineLink(displayStart, builder.length, url)
                cursor = markdownLink.range.last + 1
                continue
            }
            val url = if (value[cursor].lowercaseChar() == 'h') UrlPattern.matchAt(value, cursor) else null
            if (url != null && url.range.last < endExclusive) {
                val displayStart = builder.length
                builder.pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                for (index in url.range) appendRaw(index)
                builder.pop()
                links += InlineLink(displayStart, builder.length, url.value)
                cursor = url.range.last + 1
                continue
            }
            val marker = inlineMarkerAt(value, cursor, endExclusive)
            val closing = marker?.let { findClosingMarker(value, it, cursor + it.length, endExclusive) }
            if (marker != null && closing != null) {
                val style = when (marker) {
                    "***", "___" -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                    "**", "__" -> SpanStyle(fontWeight = FontWeight.Bold)
                    "*", "_" -> SpanStyle(fontStyle = FontStyle.Italic)
                    "~~" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    else -> SpanStyle(fontFamily = FontFamily.Monospace, background = linkColor.copy(alpha = 0.12f))
                }
                builder.pushStyle(style)
                appendRange(cursor + marker.length, closing)
                builder.pop()
                cursor = closing + marker.length
            } else {
                appendRaw(cursor)
                cursor++
            }
        }
    }

    appendRange(0, value.length)
    return InlineContent(builder.toAnnotatedString(), sourceOffsets.toIntArray(), links)
}

private fun inlineMarkerAt(value: String, cursor: Int, endExclusive: Int): String? {
    val candidates = listOf("***", "___", "**", "__", "~~", "`", "*", "_")
    return candidates.firstOrNull { marker ->
        value.startsWith(marker, cursor) && cursor + marker.length < endExclusive &&
            !(marker.startsWith("_") && cursor > 0 && value[cursor - 1].isLetterOrDigit())
    }
}

private fun findClosingMarker(value: String, marker: String, start: Int, endExclusive: Int): Int? {
    var searchFrom = start
    while (searchFrom < endExclusive) {
        val closing = value.indexOf(marker, searchFrom)
        if (closing < 0 || closing >= endExclusive) return null
        if (closing > start && !(marker.startsWith("_") && closing + marker.length < value.length && value[closing + marker.length].isLetterOrDigit())) {
            // **粗体 *斜体*** 的外层闭合符位于结尾三个星号中的后两个，
            // 这样递归解析内容时可先识别内部 *斜体*，且不会遗留一个星号。
            if (marker == "**" && closing + 2 < endExclusive && value[closing + 2] == '*') return closing + 1
            if (marker == "__" && closing + 2 < endExclusive && value[closing + 2] == '_') return closing + 1
            return closing
        }
        searchFrom = closing + marker.length
    }
    return null
}

internal fun markdownPreviewBlockCount(markdown: String): Int = MarkdownParser.parse(markdown).blocks.size

internal fun markdownInlineDisplayText(markdown: String): String {
    val source = SourceText(markdown, IntArray(markdown.length) { it })
    return parseMarkdownInline(source, Color.Black).text.text
}

internal fun markdownInlineLinkAt(markdown: String, displayOffset: Int): String? {
    val source = SourceText(markdown, IntArray(markdown.length) { it })
    return parseMarkdownInline(source, Color.Black).linkAt(displayOffset)
}

internal fun markdownFirstParagraphSourceOffset(markdown: String, displayOffset: Int): Int? {
    val paragraph = MarkdownParser.parse(markdown).blocks.filterIsInstance<MarkdownBlock.Paragraph>().firstOrNull() ?: return null
    return paragraph.text.sourceOffsetAt(displayOffset)
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

/** 兼容旧备份与单元测试；实际预览界面直接使用主题主色。 */
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

fun markdownToPlainText(markdown: String): String = markdown
    .lineSequence()
    .joinToString(" ") { line ->
        line.replace(Regex("^\\s{0,3}(#{1,6}|>|[-+*]|\\d+\\.)\\s+"), "")
            .replace(Regex("\\[([^\\]]+)]\\((https?://[^\\s)]+)\\)"), "${'$'}1")
            .replace(Regex("(?:\\*\\*|__)(.*?)(?:\\*\\*|__)"), "${'$'}1")
            .replace(Regex("~~(.*?)~~"), "${'$'}1")
            .replace(Regex("`(.*?)`"), "${'$'}1")
            .replace(Regex("(?:\\*|_)(.*?)(?:\\*|_)"), "${'$'}1")
    }
    .replace(Regex("\\s+"), " ")
    .trim()

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
