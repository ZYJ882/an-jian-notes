package com.example.anjiannotes.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSupportTest {
    private val meetingNote = NoteEntity(
        id = 1,
        title = "项目会议记录",
        content = "确认发布计划和负责人"
    )

    @Test
    fun parseSearchTerms_supportsWhitespaceAndCommonCommaSeparators() {
        assertEquals(listOf("项目", "发布", "计划"), parseSearchTerms(" 项目，发布 计划 "))
        assertEquals(listOf("项目", "发布", "计划"), parseSearchTerms("项目,发布、计划"))
    }

    @Test
    fun matchesSearchTerms_requiresEveryTerm() {
        assertTrue(meetingNote.matchesSearchTerms(parseSearchTerms("项目 发布")))
        assertTrue(meetingNote.matchesSearchTerms(parseSearchTerms("会议，负责人")))
        assertFalse(meetingNote.matchesSearchTerms(parseSearchTerms("项目 缺失")))
    }

    @Test
    fun matchesSearchTerms_allowsTermsAcrossTitleAndContent() {
        assertTrue(meetingNote.matchesSearchTerms(listOf("会议", "计划")))
    }

    @Test
    fun blankSearchProducesNoTermsAndMatchesWithoutFiltering() {
        val terms = parseSearchTerms(" ， ,  ")
        assertTrue(terms.isEmpty())
        assertTrue(meetingNote.matchesSearchTerms(terms))
    }

    @Test
    fun findSearchMatch_returnsBodyOffsetAndReadableContext() {
        val note = NoteEntity(
            title = "观测日志",
            content = "第一段背景。\n\n今天看到太阳是一颗恒星，适合记录。\n\n第三段。"
        )

        val match = requireNotNull(note.findSearchMatch("太阳"))
        assertEquals(note.content.indexOf("太阳"), match.contentOffset)
        assertTrue(match.snippet.contains("太阳是一颗恒星"))
    }

    @Test
    fun findSearchMatch_keepsTitleOnlyResultWithoutFalseBodyPosition() {
        val note = NoteEntity(title = "太阳观测", content = "只有正文背景信息")

        val match = requireNotNull(note.findSearchMatch("太阳"))
        assertEquals(null, match.contentOffset)
        assertEquals("只有正文背景信息", match.snippet)
    }

    @Test
    fun fiveThousandNotes_keepMultiTermFilteringDeterministic() {
        val notes = List(5_000) { index ->
            NoteEntity(
                id = index.toLong() + 1L,
                title = if (index % 250 == 0) "项目发布 #$index" else "普通笔记 #$index",
                content = if (index % 250 == 0) "包含计划与负责人" else "日常记录"
            )
        }

        val results = notes.filter { it.matchesSearchTerms(parseSearchTerms("项目 发布 计划")) }
        assertEquals(20, results.size)
        assertTrue(results.all { it.title.contains("项目") && it.content.contains("计划") })
    }
}
