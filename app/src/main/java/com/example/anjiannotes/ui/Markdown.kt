package com.example.anjiannotes.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier,
    onLinkLongPress: (String) -> Unit = {}
) {
    val lines = remember(markdown) { markdown.lineSequence().toList() }
    if (lines.isEmpty() || markdown.isBlank()) {
        Text(
            text = "开始输入 Markdown…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        lines.forEach { line -> MarkdownLine(line, onLinkLongPress) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarkdownLine(line: String, onLinkLongPress: (String) -> Unit) {
    val link = remember(line) { extractFirstLink(line) }
    val lineModifier = if (link == null) Modifier else Modifier.combinedClickable(
        onClick = {},
        onLongClick = { onLinkLongPress(link) }
    )
    when {
        line.startsWith("### ") -> MarkdownLineText(markdownInline(line.removePrefix("### ")), MaterialTheme.typography.titleMedium, lineModifier, FontWeight.Bold)
        line.startsWith("## ") -> MarkdownLineText(markdownInline(line.removePrefix("## ")), MaterialTheme.typography.titleLarge, lineModifier, FontWeight.Bold)
        line.startsWith("# ") -> MarkdownLineText(markdownInline(line.removePrefix("# ")), MaterialTheme.typography.headlineSmall, lineModifier, FontWeight.Bold)
        line.trim() == "---" || line.trim() == "***" -> Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        line.startsWith("> ") -> Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            MarkdownLineText(markdownInline(line.removePrefix("> ")), MaterialTheme.typography.bodyMedium, lineModifier.padding(horizontal = 12.dp, vertical = 9.dp))
        }
        line.matches(Regex("^[-+*]\\s+.*")) -> Row(verticalAlignment = Alignment.Top) {
            Text("•", modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
            MarkdownLineText(markdownInline(line.replaceFirst(Regex("^[-+*]\\s+"), "")), MaterialTheme.typography.bodyMedium, lineModifier)
        }
        line.matches(Regex("^\\d+\\.\\s+.*")) -> {
            val prefix = line.substringBefore(' ')
            Row(verticalAlignment = Alignment.Top) {
                Text(prefix, modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                MarkdownLineText(markdownInline(line.removePrefix("$prefix ")), MaterialTheme.typography.bodyMedium, lineModifier)
            }
        }
        else -> MarkdownLineText(markdownInline(line), MaterialTheme.typography.bodyMedium, lineModifier)
    }
}

@Composable
private fun MarkdownLineText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier,
    weight: FontWeight? = null
) {
    Text(text = text, style = style, fontWeight = weight, modifier = modifier)
}

private fun markdownInline(text: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    while (cursor < text.length) {
        val marker = listOf("**", "~~", "`", "*").firstOrNull { text.startsWith(it, cursor) }
        if (marker == null) {
            append(text[cursor])
            cursor++
            continue
        }
        val closing = text.indexOf(marker, cursor + marker.length)
        if (closing <= cursor + marker.length) {
            append(marker)
            cursor += marker.length
            continue
        }
        val content = text.substring(cursor + marker.length, closing)
        val style = when (marker) {
            "**" -> SpanStyle(fontWeight = FontWeight.Bold)
            "*" -> SpanStyle(fontStyle = FontStyle.Italic)
            "~~" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            else -> SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x18000000))
        }
        pushStyle(style)
        append(content)
        pop()
        cursor = closing + marker.length
    }
}

fun extractFirstLink(text: String): String? {
    val markdownLink = Regex("\\[[^\\]]+\\]\\((https?://[^\\s)]+)\\)").find(text)?.groupValues?.getOrNull(1)
    if (!markdownLink.isNullOrBlank()) return markdownLink
    return Regex("https?://[^\\s)]+", RegexOption.IGNORE_CASE).find(text)?.value
}

fun markdownToPlainText(markdown: String): String = markdown
    .lineSequence()
    .joinToString(" ") { line ->
        line.replace(Regex("^\\s{0,3}(#{1,6}|>|[-+*]|\\d+\\.)\\s+"), "")
            .replace(Regex("\\[([^\\]]+)]\\((https?://[^\\s)]+)\\)"), "${'$'}1")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "${'$'}1")
            .replace(Regex("~~(.*?)~~"), "${'$'}1")
            .replace(Regex("`(.*?)`"), "${'$'}1")
            .replace(Regex("\\*(.*?)\\*"), "${'$'}1")
    }
    .replace(Regex("\\s+"), " ")
    .trim()

@Composable
fun MarkdownSyntaxHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = "# 标题   **粗体**   *斜体*   - 列表   > 引用   `代码`",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
