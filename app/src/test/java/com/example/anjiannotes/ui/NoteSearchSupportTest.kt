package com.example.anjiannotes.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSearchSupportTest {

    @Test
    fun findSearchMatchRanges_isCaseInsensitiveAndNonOverlapping() {
        assertEquals(listOf(0..2, 3..5), findSearchMatchRanges("abcABC", "abc"))
        assertEquals(listOf(0..1), findSearchMatchRanges("aAa", "aa"))
    }

    @Test
    fun findSearchMatchRanges_returnsEmptyForBlankQueryOrText() {
        assertTrue(findSearchMatchRanges("", "关键词").isEmpty())
        assertTrue(findSearchMatchRanges("正文内容", "  ").isEmpty())
        assertTrue(findSearchMatchRanges("正文内容", "不存在").isEmpty())
    }

    @Test
    fun findSearchMatchRanges_locatesChinesePhrases() {
        assertEquals(listOf(3..4), findSearchMatchRanges("今天的记录今天", "记录"))
    }

    @Test
    fun searchIndex_countsMatchesOnDisplayTextNotMarkdownSyntax() {
        val blocks = listOf(
            MarkdownBlock.Heading(1, sourceText("会议记录")),
            MarkdownBlock.Paragraph(sourceText("hello **World** hello"))
        )
        // 展示文本为 "hello World hello"，Markdown 标记不参与匹配
        val index = computeMarkdownSearchIndex(blocks, "hello", -1)
        assertEquals(2, index.totalMatches)
        assertEquals(2, index.blockSegments[1][0].ranges.size)
    }

    @Test
    fun searchIndex_marksActiveMatchAcrossSegments() {
        val blocks = listOf(
            MarkdownBlock.Heading(1, sourceText("会议记录")),
            MarkdownBlock.Paragraph(sourceText("hello World hello"))
        )
        val index = computeMarkdownSearchIndex(blocks, "hello", 1)
        val active = index.blockSegments[1][0].activeRange
        assertEquals(12, active?.first)
        assertEquals(16, active?.last)
    }

    @Test
    fun searchIndex_coversCodeBlocksAndPaddedTableCells() {
        val table = MarkdownBlock.Table(
            header = listOf(sourceText("列A"), sourceText("列B")),
            rows = listOf(listOf(sourceText("内容"))),
            alignments = listOf(TableAlignment.START, TableAlignment.START),
            startOffset = 0
        )
        val blocks = listOf(
            MarkdownBlock.Code("kotlin", sourceText("val answer = 42"), 0),
            table
        )
        val codeMatch = computeMarkdownSearchIndex(blocks, "42", -1)
        assertEquals(1, codeMatch.totalMatches)

        val headerMatch = computeMarkdownSearchIndex(blocks, "列B", -1)
        assertEquals(1, headerMatch.totalMatches)
        assertEquals(1, headerMatch.blockSegments[1][1].ranges.size)
    }

    @Test
    fun searchIndex_returnsEmptyForBlankQuery() {
        val blocks = listOf(MarkdownBlock.Paragraph(sourceText("任意内容")))
        val index = computeMarkdownSearchIndex(blocks, "  ", 0)
        assertEquals(0, index.totalMatches)
        assertTrue(index.blockSegments.isEmpty())
    }

    private fun sourceText(text: String) = SourceText(text, IntArray(text.length) { it })
}
