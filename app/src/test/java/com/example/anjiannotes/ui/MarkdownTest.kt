package com.example.anjiannotes.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun extractFirstLink_recognizesOnlyTheTargetForLongPressConfirmation() {
        assertEquals("https://example.com/a", extractFirstLink("请访问 https://example.com/a 查看"))
        assertEquals("https://example.com/b", extractFirstLink("[帮助页面](https://example.com/b)"))
        assertNull(extractFirstLink("没有链接的笔记"))
    }
}
