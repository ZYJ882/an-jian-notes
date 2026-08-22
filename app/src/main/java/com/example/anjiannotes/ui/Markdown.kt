package com.example.anjiannotes.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownPreview(markdown: String, modifier: Modifier = Modifier) {
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
        lines.forEach { line -> MarkdownLine(line) }
    }
}

@Composable
private fun MarkdownLine(line: String) {
    when {
        line.startsWith("### ") -> Text(markdownInline(line.removePrefix("### ")), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        line.startsWith("## ") -> Text(markdownInline(line.removePrefix("## ")), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        line.startsWith("# ") -> Text(markdownInline(line.removePrefix("# ")), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        line.trim() == "---" || line.trim() == "***" -> Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        line.startsWith("> ") -> Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            Text(markdownInline(line.removePrefix("> ")), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
        }
        line.matches(Regex("^[-+*]\\s+.*")) -> Row(verticalAlignment = Alignment.Top) {
            Text("•", modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
            Text(markdownInline(line.replaceFirst(Regex("^[-+*]\\s+"), "")), style = MaterialTheme.typography.bodyMedium)
        }
        line.matches(Regex("^\\d+\\.\\s+.*")) -> {
            val prefix = line.substringBefore(' ') 
            Row(verticalAlignment = Alignment.Top) {
                Text(prefix, modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(markdownInline(line.removePrefix("$prefix ")), style = MaterialTheme.typography.bodyMedium)
            }
        }
        else -> Text(markdownInline(line), style = MaterialTheme.typography.bodyMedium)
    }
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
            "*" -> SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            "~~" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            else -> SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x18000000))
        }
        pushStyle(style)
        append(content)
        pop()
        cursor = closing + marker.length
    }
}

fun markdownToPlainText(markdown: String): String = markdown
    .lineSequence()
    .joinToString(" ") { line ->
        line.replace(Regex("^\\s{0,3}(#{1,6}|>|[-+*]|\\d+\\.)\\s+"), "")
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
