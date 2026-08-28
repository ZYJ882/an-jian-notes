package com.example.anjiannotes.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportSupportTest {
    @Test
    fun looksLikeMarkdown_recognizesBlockAndInlineSyntax() {
        assertTrue(looksLikeMarkdown("# 今日计划\n- **完成** 导入功能"))
        assertTrue(looksLikeMarkdown("这是 `代码` 与 *强调* 内容"))
        assertTrue(looksLikeMarkdown("> 引用内容\n\n```kotlin\nprintln(\"hello\")\n```"))
        assertTrue(looksLikeMarkdown("- [x] 已完成\n- [ ] 待处理"))
    }

    @Test
    fun looksLikeMarkdown_recognizesTableLinksAndImages() {
        assertTrue(looksLikeMarkdown("名称 | 说明\n--- | ---\n安笺 | 离线笔记"))
        assertTrue(looksLikeMarkdown("参见 [官网](https://example.com)。"))
        assertTrue(looksLikeMarkdown("![应用图标](https://example.com/icon.png)"))
        assertTrue(looksLikeMarkdown("这是 **重点内容**。"))
    }

    @Test
    fun looksLikeMarkdown_keepsOrdinaryTextAsPlain() {
        assertFalse(looksLikeMarkdown("今天记录了一段普通文字，没有格式符号。"))
        assertFalse(looksLikeMarkdown("2026.08.22 的随手记"))
        assertFalse(looksLikeMarkdown("价格是 100 元，编号为 A-203。"))
        assertFalse(looksLikeMarkdown("计算式为 2 * 3 = 6。"))
        assertFalse(looksLikeMarkdown("路径：/storage/emulated_0/Notes。"))
        assertFalse(looksLikeMarkdown("请访问 https://example.com 查看详情。"))
        assertFalse(looksLikeMarkdown("1. 这一条只是普通编号。"))
    }

    @Test
    fun formatForFileName_usesMarkdownExtensionOrAutomaticDetection() {
        assertEquals(NoteFormatMode.MARKDOWN, formatForFileName("阅读清单.md"))
        assertEquals(NoteFormatMode.MARKDOWN, formatForFileName("README.MARKDOWN"))
        assertEquals(NoteFormatMode.AUTO, formatForFileName("随手记.txt"))
        assertEquals(NoteFormatMode.AUTO, formatForFileName("无扩展名笔记"))
    }

    @Test
    fun txtContentWithMarkdownSyntaxUsesMarkdownPreviewInAutoMode() {
        val txtContent = """
            # 项目记录

            - 第一项
            - 第二项

            | 名称 | 状态 |
            | --- | --- |
            | 安笺 | 完成 |

            ```kotlin
            println("ok")
            ```
        """.trimIndent()
        assertEquals(NoteFormatMode.AUTO, formatForFileName("项目记录.txt"))
        assertTrue(NoteFormatMode.AUTO.resolvesToMarkdown(txtContent))
    }

    @Test
    fun manualModeStillOverridesAutomaticDetection() {
        assertFalse(NoteFormatMode.PLAIN.resolvesToMarkdown("# 标题\n- 项目"))
        assertTrue(NoteFormatMode.MARKDOWN.resolvesToMarkdown("普通文字"))
    }
}
