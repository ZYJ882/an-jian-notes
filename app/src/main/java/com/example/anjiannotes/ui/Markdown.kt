package com.example.anjiannotes.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.unit.em
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
    val links: List<InlineLink>,
    /** 行内代码 span 在展示文本中的位置区间，用于绘制圆角代码片背景。 */
    val codeRanges: List<IntRange> = emptyList()
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

/** 预览页面「舒展阅读」式排版的尺寸常量。
 *
 * 设计目标：像现代阅读器一样，靠字号、字重与留白建立层级，让界面退到背景里——
 * - 正文 17sp / 行高 30sp，贴近手机文档阅读器的舒展节奏；
 * - 标题 H1/H2 用 Bold 与充足上间距，章节在滚动中自然“浮起”；
 * - 段间 14dp，与标题上间距一起形成清晰的呼吸感；
 * - 引用 / 代码块 / 表格使用克制的浅灰容器，行内代码为圆角代码片，
 *   与正文形成明确的材质区分。
 */
private val BlockSpacing = 14.dp
private val BodyFontSize = 17.sp
private val BodyLineHeight = 30.sp
private val CardCorner = 8.dp
private val ChipCorner = 4.dp
private val ChipPaddingHorizontal = 3.dp
private val ChipPaddingVertical = 1.dp

/** 正文 / 列表 / 引用统一使用此样式，保证阅读节奏一致。 */
val LocalMarkdownFontFamily = staticCompositionLocalOf { com.example.anjiannotes.ui.theme.NotoSansSC }

@Composable
private fun bodyStyle() = androidx.compose.ui.text.TextStyle(
    fontFamily = LocalMarkdownFontFamily.current,
    fontWeight = FontWeight.Normal,
    fontSize = BodyFontSize,
    lineHeight = BodyLineHeight
)

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
            style = bodyStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }

    val content: @Composable () -> Unit = {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BlockSpacing)) {
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
            style = bodyStyle(),
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
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

/**
 * 完整标题层级。
 *
 * 视觉降级链路（sp / weight / top padding）：
 *  H1 24   / Bold     / 20dp  ← 文档主标题，接近页面标题体量
 *  H2 21   / Bold     / 18dp  ← 章节级，Bold + 上间距与正文拉开
 *  H3 18   / SemiBold / 14dp  ← 小节标题
 *  H4 16.5 / SemiBold / 12dp  ← 子小节，比正文略大
 *  H5 15.5 / SemiBold / 10dp  ← 极弱强调
 *  H6 14.5 / Medium   /  8dp  ← 接近 label
 *
 * 设计要点：只有 H1/H2 用 Bold 建立章节感，H3 以下用 SemiBold/Medium 收敛；
 * 上 padding 随层级递减，连续阅读中标题自然“浮起”而不会突兀占空间。
 */
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
    val (fontSize, lineHeight, weight, topPadding) = when (heading.level) {
        1 -> Quad(28.sp, 37.sp, FontWeight.Bold, 20.dp)
        2 -> Quad(23.sp, 32.sp, FontWeight.Bold, 18.dp)
        3 -> Quad(19.sp, 29.sp, FontWeight.SemiBold, 15.dp)
        4 -> Quad(17.5.sp, 27.sp, FontWeight.SemiBold, 12.dp)
        5 -> Quad(16.5.sp, 25.sp, FontWeight.SemiBold, 10.dp)
        else -> Quad(15.5.sp, 24.sp, FontWeight.Medium, 8.dp)
    }
    val style = bodyStyle().copy(
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = weight
    )
    MarkdownInteractiveText(
        source = heading.text,
        style = style,
        modifier = Modifier.fillMaxWidth().padding(top = topPadding, bottom = 3.dp),
        enableTextSelection = enableTextSelection,
        onLinkLongPress = onLinkLongPress,
        onLinkClick = onLinkClick,
        onDoubleClickAt = onDoubleClickAt,
        onLongPress = onLongPress,
        onClick = onClick
    )
}

private data class Quad(val fontSize: androidx.compose.ui.unit.TextUnit, val lineHeight: androidx.compose.ui.unit.TextUnit, val weight: FontWeight, val topPadding: androidx.compose.ui.unit.Dp)

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
    // 引用：浅灰圆角卡片。给「版本信息 / 补充说明」一类内容一个安静的底色容器，
    // 让引用在长文中更醒目、更好扫读。
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                shape = RoundedCornerShape(CardCorner)
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        quote.lines.forEach { line ->
            MarkdownInteractiveText(
                source = line,
                style = bodyStyle(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
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
private fun MarkdownList(
    list: MarkdownBlock.ListBlock,
    enableTextSelection: Boolean,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        list.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = (item.depth.coerceAtMost(5) * 20).dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.marker,
                    style = bodyStyle(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(if (item.marker.lastOrNull() == '.') 30.dp else 20.dp)
                )
                MarkdownInteractiveText(
                    source = item.text,
                    style = bodyStyle(),
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
    // 代码块：浅灰圆角卡片 + 语言标签；横向滚动查看长行。
    val clipboard = LocalClipboardManager.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(CardCorner)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (block.language.isNotBlank()) {
                    Text(
                        text = block.language.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(block.content.text)) },
                    modifier = Modifier.width(30.dp)
                ) {
                    Text(
                        text = "⧉",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            MarkdownInteractiveText(
                source = block.content,
        style = bodyStyle().copy(fontSize = 15.sp, lineHeight = 25.sp),
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
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
    val bodyStyle = bodyStyle().copy(fontSize = 16.sp, lineHeight = 27.sp)
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
            with(density) { (maxOf(headerWidth, bodyWidth).toDp() + 28.dp).coerceIn(88.dp, 320.dp) }
        }
    }
    val clipboard = LocalClipboardManager.current
    val tableCopyText = remember(header, rows) {
        buildString {
            appendLine(header.joinToString("\t") { it.text })
            rows.forEach { row -> appendLine(row.joinToString("\t") { it.text }) }
        }.trimEnd()
    }
    // 表格：右上角提供小型复制按钮；内容保持横向滚动，复制结果为便于二次编辑的 TSV。
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "⧉",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f),
                modifier = Modifier
                    .clickable { clipboard.setText(AnnotatedString(tableCopyText)) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier
                    .wrapContentWidth(unbounded = true)
                    .clip(RoundedCornerShape(CardCorner))
            ) {
            MarkdownTableRow(
                cells = header,
                columnWidths = columnWidths,
                alignments = table.alignments,
                header = true,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                enableTextSelection = enableTextSelection,
                onLinkLongPress = onLinkLongPress,
                onLinkClick = onLinkClick,
                onDoubleClickAt = onDoubleClickAt,
                onLongPress = onLongPress,
                onClick = onClick
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f))
            rows.forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                }
                MarkdownTableRow(
                    cells = row,
                    columnWidths = columnWidths,
                    alignments = table.alignments,
                    header = false,
                    containerColor = Color.Unspecified,
                    enableTextSelection = enableTextSelection,
                    onLinkLongPress = onLinkLongPress,
                    onLinkClick = onLinkClick,
                    onDoubleClickAt = onDoubleClickAt,
                    onLongPress = onLongPress,
                    onClick = onClick
                )
            }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
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
    containerColor: Color,
    enableTextSelection: Boolean,
    onLinkLongPress: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onDoubleClickAt: (Int) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = if (containerColor == Color.Unspecified) {
            Modifier
        } else {
            Modifier.background(containerColor)
        }
    ) {
        cells.forEachIndexed { index, cell ->
            MarkdownInteractiveText(
                source = cell,
                    style = bodyStyle().copy(fontSize = 16.sp, lineHeight = 27.sp),
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                color = if (header) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                textAlign = when (alignments.getOrElse(index) { TableAlignment.START }) {
                    TableAlignment.START -> TextAlign.Start
                    TableAlignment.CENTER -> TextAlign.Center
                    TableAlignment.END -> TextAlign.End
                },
                modifier = Modifier.width(columnWidths[index]).padding(horizontal = 14.dp, vertical = 11.dp),
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
    // 行内代码片：SpanStyle 的 background 只能是直角矩形，因此这里在布局完成后
    // 用 drawBehind 依据每个字符的包围盒绘制圆角胶囊背景。
    val chipColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current
    val chipRadiusPx = remember(density) { with(density) { ChipCorner.toPx() } }
    val chipPaddingXPx = remember(density) { with(density) { ChipPaddingHorizontal.toPx() } }
    val chipPaddingYPx = remember(density) { with(density) { ChipPaddingVertical.toPx() } }
    var chipRects by remember(inline) { mutableStateOf<List<RoundRect>>(emptyList()) }
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
        onTextLayout = { layout ->
            textLayout.set(layout)
            chipRects = if (inline.codeRanges.isEmpty()) {
                emptyList()
            } else {
                computeCodeChipRects(layout, inline.codeRanges, chipPaddingXPx, chipPaddingYPx)
            }
        },
        modifier = modifier
            .drawBehind {
                chipRects.forEach { rect ->
                    drawRoundRect(
                        color = chipColor,
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width, rect.height),
                        cornerRadius = CornerRadius(chipRadiusPx, chipRadiusPx)
                    )
                }
            }
            .then(interactionModifier)
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
    links = UrlPattern.findAll(source.text).map { match -> InlineLink(match.range.first, match.range.last + 1, match.value) }.toList(),
    codeRanges = emptyList()
)

/** 小型递归 tokenizer，支持常用嵌套强调、链接和转义，并输出显示字符到原文位置的映射。 */
private fun parseMarkdownInline(source: SourceText, linkColor: Color): InlineContent {
    val value = source.text
    val builder = AnnotatedString.Builder()
    val sourceOffsets = mutableListOf<Int>()
    val links = mutableListOf<InlineLink>()
    val codeRanges = mutableListOf<IntRange>()

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
                // 阅读器式排版原则：行内强调不应接近标题视觉重量。
                // - `**粗体**` 用 SemiBold（内置 Noto Sans SC SemiBold 字重）保留强调但不压过标题；
                // - `***粗斜体***` 用 Bold + Italic；
                // - `*斜体*` 用 Italic（不加粗）；
                // - `~~删除~~` 用 LineThrough；
                // - `` `代码` `` 用等宽字体 + 略缩小字号；圆角灰色代码片
                //   由 MarkdownInteractiveText 在布局完成后用 drawBehind 绘制。
                val style = when (marker) {
                    "***", "___" -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                    "**", "__" -> SpanStyle(fontWeight = FontWeight.SemiBold)
                    "*", "_" -> SpanStyle(fontStyle = FontStyle.Italic)
                    "~~" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    else -> SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 0.88f.em)
                }
                builder.pushStyle(style)
                val codeDisplayStart = if (marker == "`") builder.length else -1
                appendRange(cursor + marker.length, closing)
                builder.pop()
                if (codeDisplayStart >= 0) codeRanges += codeDisplayStart until builder.length
                cursor = closing + marker.length
            } else {
                appendRaw(cursor)
                cursor++
            }
        }
    }

    appendRange(0, value.length)
    return InlineContent(builder.toAnnotatedString(), sourceOffsets.toIntArray(), links, codeRanges)
}

/**
 * 把行内代码 span 映射为圆角矩形列表：跨行的代码 span 会按行拆分成多个矩形，
 * 避免绘制出一整块跨行色斑。坐标为像素值，在绘制时直接使用。
 */
private fun computeCodeChipRects(
    layout: TextLayoutResult,
    ranges: List<IntRange>,
    paddingXPx: Float,
    paddingYPx: Float
): List<RoundRect> {
    if (ranges.isEmpty()) return emptyList()
    val textLength = layout.layoutInput.text.length
    if (textLength == 0) return emptyList()
    val rects = mutableListOf<RoundRect>()
    ranges.forEach { range ->
        if (range.isEmpty()) return@forEach
        val start = range.first.coerceIn(0, textLength - 1)
        val endExclusive = (range.last + 1).coerceIn(start + 1, textLength)
        var segmentStart = start
        var segmentLine = layout.getLineForOffset(start)
        for (offset in start + 1 until endExclusive) {
            val line = layout.getLineForOffset(offset)
            if (line != segmentLine) {
                addCodeChipRect(layout, segmentStart, offset, segmentLine, rects, paddingXPx, paddingYPx)
                segmentStart = offset
                segmentLine = line
            }
        }
        addCodeChipRect(layout, segmentStart, endExclusive, segmentLine, rects, paddingXPx, paddingYPx)
    }
    return rects
}

private fun addCodeChipRect(
    layout: TextLayoutResult,
    start: Int,
    endExclusive: Int,
    line: Int,
    out: MutableList<RoundRect>,
    paddingXPx: Float,
    paddingYPx: Float
) {
    if (endExclusive <= start) return
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    var hasVisibleBox = false
    for (offset in start until endExclusive) {
        val box = layout.getBoundingBox(offset)
        if (box.width <= 0f || box.height <= 0f) continue
        hasVisibleBox = true
        if (box.left < left) left = box.left
        if (box.top < top) top = box.top
        if (box.right > right) right = box.right
        if (box.bottom > bottom) bottom = box.bottom
    }
    if (!hasVisibleBox) {
        // 区间内没有可见字形（如换行占位）时，退化为该行行首的一个最小胶囊。
        left = layout.getLineLeft(line)
        top = layout.getLineTop(line)
        right = left + paddingXPx * 2f
        bottom = layout.getLineBottom(line)
    }
    out += RoundRect(
        left = left - paddingXPx,
        top = top - paddingYPx,
        right = right + paddingXPx,
        bottom = bottom + paddingYPx
    )
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
    val linkColor = MaterialTheme.colorScheme.primary
    // 直接渲染 Markdown 而不是显示语法字面字符。`**粗体**` 显示成 Medium 强调、
    // `*斜体*` 显示成真正的斜体、`` `代码` `` 显示成等宽代码字，让用户直观看到
    // Markdown 的效果，而不是看到一堆星号和反引号。
    val annotated = remember(linkColor) {
        val raw = "Markdown：**粗体** · *斜体* · ~~删除~~ · `代码` · [链接](https://example.com)"
        val source = SourceText(raw, IntArray(raw.length) { it })
        parseMarkdownInline(source, linkColor).text
    }
    Text(
        text = annotated,
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
    )
}
