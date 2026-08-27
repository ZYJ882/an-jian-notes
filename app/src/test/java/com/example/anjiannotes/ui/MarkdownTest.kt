package com.example.anjiannotes.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun markdownPreview_keepsEveryLineOfLongOrderedList() {
        val markdown = """
            本次做性能优化
            1. 改善冷启动：把主线程阻塞的初始化逻辑延迟到首帧绘制完成之后放到后台执行；不要在启动阶段执行大量 IO、解析工作。
            2. 提升页面运行流畅度：Compose 层面优化，减少不必要重组，耗时计算使用 remember 缓存，懒列表补充稳定 key，避免滑动卡顿。
            3. 优化动画：解决动画卡顿、丢帧问题。
        """.trimIndent()

        assertEquals(4, markdownPreviewBlockCount(markdown))
        assertEquals(
            "本次做性能优化 改善冷启动：把主线程阻塞的初始化逻辑延迟到首帧绘制完成之后放到后台执行；不要在启动阶段执行大量 IO、解析工作。 提升页面运行流畅度：Compose 层面优化，减少不必要重组，耗时计算使用 remember 缓存，懒列表补充稳定 key，避免滑动卡顿。 优化动画：解决动画卡顿、丢帧问题。",
            markdownToPlainText(markdown)
        )
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
}
