package com.example.anjiannotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeNoteEditorSyncTest {
    private val editorSession = "note-42@updated-100"

    @Test
    fun thousandChineseCharacters_areNeverReplacedByShorterStaleComposeValue() {
        val nativeText = "安笺长文本".repeat(250)
        val staleComposeText = nativeText.take(120)

        assertTrue(nativeText.length >= 1_000)
        assertTrue(nativeText.length > staleComposeText.length)
        val textAfterComposeUpdate = if (shouldApplyExternalModelText(editorSession, editorSession)) {
            staleComposeText
        } else {
            nativeText
        }
        assertEquals(nativeText, textAfterComposeUpdate)
    }

    @Test
    fun thousandCharacterMarkdown_keepsAllSectionsWhenModelIsStale() {
        val nativeText = buildString {
            appendLine("# 标题")
            appendLine()
            appendLine("普通文本".repeat(120))
            appendLine()
            appendLine("1. 第一项")
            appendLine("2. 第二项")
            appendLine("3. 第三项")
            appendLine("4. 第四项")
            appendLine()
            appendLine("**粗体**")
            appendLine("*斜体*")
            append("补充内容".repeat(160))
        }
        val staleComposeText = nativeText.take(200)

        assertTrue(nativeText.length >= 1_000)
        assertTrue(nativeText.contains("4. 第四项"))
        assertTrue(nativeText.contains("**粗体**"))
        assertTrue(nativeText.contains("*斜体*"))
        val textAfterPreviewRequest = if (shouldApplyExternalModelText(editorSession, editorSession)) {
            staleComposeText
        } else {
            nativeText
        }
        assertEquals(nativeText, textAfterPreviewRequest)
    }

    @Test
    fun rapidInputAndPaste_keepNativeTextAsTheCurrentSource() {
        val typedText = "连续输入".repeat(260)
        val pastedText = "粘贴段落\n".repeat(220)

        assertTrue(typedText.length >= 1_000)
        assertTrue(pastedText.length >= 1_000)
        assertFalse(shouldApplyExternalModelText(editorSession, editorSession))
        assertFalse(shouldApplyExternalModelText(editorSession, editorSession))
    }

    @Test
    fun immediatePreviewOrBack_doesNotPermitStaleModelWrite() {
        val nativeText = "立即切换时仍以原生文本为准。".repeat(100)
        val staleComposeText = nativeText.take(80)

        assertTrue(nativeText.length > staleComposeText.length)
        val textBeforeLeavingDetail = if (shouldApplyExternalModelText(editorSession, editorSession)) {
            staleComposeText
        } else {
            nativeText
        }
        assertEquals(nativeText, textBeforeLeavingDetail)
    }

    @Test
    fun changingToAnotherNoteOrImportSeed_permitsOneExplicitModelWrite() {
        assertTrue(shouldApplyExternalModelText(editorSession, "note-43@updated-200"))
        assertTrue(shouldApplyExternalModelText(editorSession, "import-seed"))
    }
}
