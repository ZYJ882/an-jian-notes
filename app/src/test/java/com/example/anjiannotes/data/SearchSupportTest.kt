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
}
