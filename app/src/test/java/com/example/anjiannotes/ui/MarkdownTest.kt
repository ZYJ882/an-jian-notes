package com.example.anjiannotes.ui

import org.junit.Assert.assertEquals
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
}
