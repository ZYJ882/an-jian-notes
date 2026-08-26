package com.example.anjiannotes.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportSupportTest {
    @Test
    fun looksLikeMarkdown_recognizesBlockAndInlineSyntax() {
        assertTrue(looksLikeMarkdown("# 今日计划\n- **完成** 导入功能"))
        assertTrue(looksLikeMarkdown("这是 `代码` 与 *强调* 内容"))
    }

    @Test
    fun looksLikeMarkdown_keepsOrdinaryTextAsPlain() {
        assertFalse(looksLikeMarkdown("今天记录了一段普通文字，没有格式符号。"))
        assertFalse(looksLikeMarkdown("2026.08.22 的随手记"))
    }

    @Test
    fun formatForFileName_usesMarkdownExtensionFromDisplayName() {
        assertTrue(formatForFileName("阅读清单.md") == NoteFormatMode.MARKDOWN)
        assertTrue(formatForFileName("README.MARKDOWN") == NoteFormatMode.MARKDOWN)
        assertTrue(formatForFileName("随手记.txt") == NoteFormatMode.PLAIN)
    }
}
