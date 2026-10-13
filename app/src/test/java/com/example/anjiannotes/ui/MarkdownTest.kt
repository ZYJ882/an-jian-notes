package com.example.anjiannotes.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {
    @Test
    fun markdownToPlainText_removesSupportedSyntaxAndPreservesWords() {
        val markdown = """
            # 今天的记录
            - **完成** *Markdown* 编辑
            > `保持` 流畅
            ~~旧内容~~
        """.trimIndent()

        assertEquals("今天的记录 完成 Markdown 编辑 保持 流畅 旧内容", markdownToPlainText(markdown))
    }

    @Test
    fun markdownToPlainText_keepsOrdinaryTextReadable() {
        assertEquals("简洁的普通笔记", markdownToPlainText("简洁的普通笔记"))
    }

    @Test
    fun parser_mergesContinuousLinesIntoParagraphs() {
        val document = MarkdownParser.parse("第一行\n第二行\n\n第三段")
        val paragraphs = document.blocks.filterIsInstance<MarkdownBlock.Paragraph>()

        assertEquals(2, paragraphs.size)
        assertEquals("第一行\n第二行", paragraphs[0].text.text)
        assertEquals("第三段", paragraphs[1].text.text)
    }

    @Test
    fun parser_supportsEveryHeadingLevel() {
        val markdown = (1..6).joinToString("\n") { level -> "${"#".repeat(level)} 标题$level" }
        val headings = MarkdownParser.parse(markdown).blocks.filterIsInstance<MarkdownBlock.Heading>()

        assertEquals(listOf(1, 2, 3, 4, 5, 6), headings.map { it.level })
        assertEquals("标题6", headings.last().text.text)
    }

    @Test
    fun parser_keepsNestedListDepthAndListItemsAfterCodeBlock() {
        val markdown = """
            ```kotlin
            val message = "安笺"
            ```
            - 第一项
              - 子项目
            1. 第二项
               1. 子项目
        """.trimIndent()
        val blocks = MarkdownParser.parse(markdown).blocks
        val code = blocks.filterIsInstance<MarkdownBlock.Code>().single()
        val list = blocks.filterIsInstance<MarkdownBlock.ListBlock>().single()

        assertEquals("kotlin", code.language)
        assertEquals(4, list.items.size)
        assertEquals(listOf(0, 1, 0, 1), list.items.map { it.depth })
        assertEquals(listOf("•", "•", "1.", "1."), list.items.map { it.marker })
    }

    @Test
    fun inlineParser_supportsNestedFormattingAndUnderscoreMarkers() {
        assertEquals("这是 粗体斜体", markdownInlineDisplayText("**这是 *粗体斜体***"))
        assertEquals("粗体 斜体 删除 代码", markdownInlineDisplayText("__粗体__ _斜体_ ~~删除~~ `代码`"))
        assertEquals("重要链接", markdownInlineDisplayText("[**重要链接**](https://example.com)"))
        assertEquals("*不是斜体*", markdownInlineDisplayText("\\*不是斜体\\*"))
    }

    @Test
    fun linkHitTesting_onlyOpensWhenTheTappedDisplayCharacterIsALink() {
        val markdown = "请访问 [帮助页面](https://example.com/help) 后继续"
        assertEquals("https://example.com/help", markdownInlineLinkAt(markdown, 4))
        assertNull(markdownInlineLinkAt(markdown, 1))
    }

    @Test
    fun parser_mapsCrLfParagraphDisplayOffsetsBackToOriginalText() {
        val markdown = "第一行\r\n第二行"
        assertEquals(5, markdownFirstParagraphSourceOffset(markdown, 4))
    }

    @Test
    fun parser_preservesEscapedTablePipesAndAlignment() {
        val markdown = """
            | 左 | 中 | 右 |
            |:---|:---:|---:|
            | A | CPU \| GPU | 42 |
        """.trimIndent()
        val table = MarkdownParser.parse(markdown).blocks.filterIsInstance<MarkdownBlock.Table>().single()

        assertEquals(listOf(TableAlignment.START, TableAlignment.CENTER, TableAlignment.END), table.alignments)
        assertEquals("CPU | GPU", table.rows.single()[1].text)
    }

    @Test
    fun extractFirstLink_recognizesOnlyTheTargetForLongPressConfirmation() {
        assertEquals("https://example.com/a", extractFirstLink("请访问 https://example.com/a 查看"))
        assertEquals("https://example.com/b", extractFirstLink("[帮助页面](https://example.com/b)"))
        assertNull(extractFirstLink("没有链接的笔记"))
    }

    @Test
    fun extractLinkAt_onlyReturnsLinkWhenTheTappedOffsetIsInsideIt() {
        val text = "先读 https://example.com/a 再继续"
        assertEquals("https://example.com/a", extractLinkAt(text, text.indexOf("example")))
        assertNull(extractLinkAt(text, 1))
    }

    @Test
    fun linkifyPlainText_marksUrlsWithAnExplicitStyle() {
        val result = linkifyPlainText("访问 https://example.com", previewLinkColor(darkTheme = false))
        assertEquals("访问 https://example.com", result.text)
        assertNotNull(result.spanStyles.firstOrNull())
    }

    @Test
    fun markdownPreview_documentContainsNoUnexpectedLineLoss() {
        val markdown = """
            本次做性能优化
            1. 改善冷启动
            2. 提升页面运行流畅度
            3. 优化动画
        """.trimIndent()
        val document = MarkdownParser.parse(markdown)

        assertEquals(2, document.blocks.size)
        assertTrue(markdownToPlainText(markdown).contains("优化动画"))
    }

    @Test
    fun markdownToListPreview_rendersTableRowsAsPlainWords() {
        val markdown = """
            | 游戏 | 平台 | 官方链接 | 实测状态 |
            | --- | --- | --- | --- |
            | 视眈眈（Out of Sight） | PC | [商店页](https://store.epicgames.com/zh-CN/p/xxx) | 已通关 |
        """.trimIndent()

        val preview = markdownToListPreview(markdown)

        assertTrue(preview, !preview.contains("|"))
        assertTrue(preview, !preview.contains("---"))
        assertTrue(preview, preview.contains("游戏 平台 官方链接 实测状态"))
        assertTrue(preview, preview.contains("视眈眈（Out of Sight） PC"))
        assertTrue(preview, preview.contains("商店页"))
    }

    @Test
    fun markdownToListPreview_removesTableSeparatorOnlyLines() {
        val markdown = """
            标题行
            |---|---|
            | :--- | ---: |
        """.trimIndent()

        val preview = markdownToListPreview(markdown)

        assertTrue(preview, !preview.contains("|"))
        assertTrue(preview, preview.contains("标题行"))
    }

    @Test
    fun markdownToListPreview_keepsEscapedPipeInsideTableCell() {
        val markdown = """
            | 选项 | 说明 |
            | --- | --- |
            | A \| B | 第二列 |
        """.trimIndent()

        val preview = markdownToListPreview(markdown)

        assertTrue(preview, preview.contains("A | B 第二列"))
    }

    @Test
    fun markdownToListPreview_stripsEdgePipesOfSingleColumnRow() {
        assertEquals("单独一列", markdownToListPreview("| 单独一列 |"))
    }

    @Test
    fun markdownToListPreview_keepsOrdinaryTextLinesUntouched() {
        assertEquals("简洁的普通笔记", markdownToListPreview("简洁的普通笔记"))
        assertEquals("价格是 5", markdownToListPreview("价格是 5"))
    }

    @Test
    fun markdownToListPreview_convertsFullWidthPipeTableRows() {
        val markdown = "｜游戏｜平台｜实测状态｜\n｜---｜---｜---｜\n｜视眈眈｜PC｜已通关｜"

        val preview = markdownToListPreview(markdown)

        assertTrue(preview, !preview.contains("｜"))
        assertTrue(preview, !preview.contains("|"))
        assertTrue(preview, preview.contains("游戏 平台 实测状态"))
        assertTrue(preview, preview.contains("视眈眈 PC 已通关"))
    }

    @Test
    fun searchIndexFromCachedDisplayTexts_matchesBlockBasedComputation() {
        // 展示文本缓存路径必须与按 block 直接计算完全一致：命中总数、每块区间与当前命中都不漂移。
        val markdown = """
            # 会议记录
            **今天**讨论了 *发布* 计划
            - 第一项 `代码`
            - 第二项
            | 列甲 | 列乙 |
            | --- | --- |
            | 今天 | 明天 |
        """.trimIndent()
        val blocks = MarkdownParser.parse(markdown).blocks

        val fromBlocks = computeMarkdownSearchIndex(blocks, query = "今天", activeOrdinal = 1)
        val displayTexts = blocks.map { block ->
            blockSearchSegments(block).map { segment -> blockSearchDisplayText(block, segment) }
        }
        val fromTexts = computeMarkdownSearchIndexFromTexts(displayTexts, query = "今天", activeOrdinal = 1)

        assertEquals(fromBlocks.totalMatches, fromTexts.totalMatches)
        assertEquals(fromBlocks.blockSegments.size, fromTexts.blockSegments.size)
        fromBlocks.blockSegments.forEachIndexed { index, segments ->
            assertEquals(segments.size, fromTexts.blockSegments[index].size)
            segments.forEachIndexed { segmentIndex, segment ->
                assertEquals(segment.ranges, fromTexts.blockSegments[index][segmentIndex].ranges)
                assertEquals(segment.activeRange, fromTexts.blockSegments[index][segmentIndex].activeRange)
            }
        }
        assertTrue(fromTexts.totalMatches > 0)
    }

    @Test
    fun searchIndexFromCachedDisplayTexts_blankQueryYieldsNoMatches() {
        val blocks = MarkdownParser.parse("普通正文\n\n第二段").blocks

        val index = computeMarkdownSearchIndex(blocks, query = "   ", activeOrdinal = 0)

        assertEquals(0, index.totalMatches)
        assertTrue(index.blockSegments.all { segments -> segments.all { it.ranges.isEmpty() } })
    }
}
