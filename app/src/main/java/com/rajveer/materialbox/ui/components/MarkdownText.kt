package com.rajveer.materialbox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val lines = markdown.lines()
    var index = 0

    SelectionContainer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            while (index < lines.size) {
                val line = lines[index]

                when {
                    line.isBlank() -> Unit
                    line.trimStart().startsWith("```") -> {
                        val codeLines = mutableListOf<String>()
                        index++
                        while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                            codeLines.add(lines[index])
                            index++
                        }
                        CodeBlock(codeLines.joinToString("\n"))
                    }
                    line.startsWith("### ") -> MarkdownLine(line.removePrefix("### "), MarkdownLineStyle.HeadingSmall)
                    line.startsWith("## ") -> MarkdownLine(line.removePrefix("## "), MarkdownLineStyle.HeadingMedium)
                    line.startsWith("# ") -> MarkdownLine(line.removePrefix("# "), MarkdownLineStyle.HeadingLarge)
                    line.trimStart().matches(Regex("[-*]\\s+.*")) -> {
                        MarkdownLine("• ${line.trimStart().drop(2)}", MarkdownLineStyle.Body)
                    }
                    line.trimStart().matches(Regex("\\d+\\.\\s+.*")) -> {
                        MarkdownLine(line.trimStart(), MarkdownLineStyle.Body)
                    }
                    else -> MarkdownLine(line, MarkdownLineStyle.Body)
                }

                index++
            }
        }
    }
}

private enum class MarkdownLineStyle {
    HeadingLarge,
    HeadingMedium,
    HeadingSmall,
    Body
}

@Composable
private fun MarkdownLine(
    text: String,
    style: MarkdownLineStyle
) {
    val textStyle = when (style) {
        MarkdownLineStyle.HeadingLarge -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        MarkdownLineStyle.HeadingMedium -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        MarkdownLineStyle.HeadingSmall -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        MarkdownLineStyle.Body -> MaterialTheme.typography.bodyLarge
    }

    Text(
        text = markdownInlineText(text),
        style = textStyle,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun CodeBlock(code: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun markdownInlineText(text: String) = buildAnnotatedString {
    var index = 0

    while (index < text.length) {
        when {
            text.startsWith("**", index) -> {
                val end = text.indexOf("**", startIndex = index + 2)
                if (end > index) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(index + 2, end))
                    }
                    index = end + 2
                } else {
                    append(text[index])
                    index++
                }
            }
            text[index] == '`' -> {
                val end = text.indexOf('`', startIndex = index + 1)
                if (end > index) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = MaterialTheme.colorScheme.surfaceVariant,
                            color = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        append(text.substring(index + 1, end))
                    }
                    index = end + 1
                } else {
                    append(text[index])
                    index++
                }
            }
            text[index] == '*' -> {
                val end = text.indexOf('*', startIndex = index + 1)
                if (end > index) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(index + 1, end))
                    }
                    index = end + 1
                } else {
                    append(text[index])
                    index++
                }
            }
            else -> {
                append(text[index])
                index++
            }
        }
    }
}
