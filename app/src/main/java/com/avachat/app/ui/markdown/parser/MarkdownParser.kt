package com.avachat.app.ui.markdown.parser

/**
 * Minimal, allocation-conscious markdown block parser tailored to chat
 * responses. Handles fenced code blocks, headings, unordered/ordered lists,
 * block quotes and paragraphs. Never throws on malformed input.
 */
sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class UnorderedListItem(val text: String) : MarkdownBlock()
    data class OrderedListItem(val number: Int, val text: String) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
    data class CodeBlock(val code: String) : MarkdownBlock()
}

fun parseMarkdown(source: String): List<MarkdownBlock> {
    if (source.isBlank()) return emptyList()
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = source.lines()
    var index = 0
    val paragraph = StringBuilder()

    fun flushParagraph() {
        val text = paragraph.toString().trim()
        if (text.isNotEmpty()) blocks += MarkdownBlock.Paragraph(text)
        paragraph.setLength(0)
    }

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()

        when {
            trimmed.startsWith("```") -> {
                flushParagraph()
                val code = StringBuilder()
                index++
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    code.appendLine(lines[index])
                    index++
                }
                index++ // skip closing fence (or EOL)
                blocks += MarkdownBlock.CodeBlock(code.toString().trimEnd('\n'))
            }
            trimmed.isEmpty() -> flushParagraph()
            trimmed.startsWith("###") || trimmed.startsWith("##") || trimmed.startsWith("#") -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length
                val text = trimmed.dropWhile { it == '#' }.trim()
                if (text.isNotEmpty()) blocks += MarkdownBlock.Heading(level.coerceIn(1, 3), text)
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                flushParagraph()
                blocks += MarkdownBlock.UnorderedListItem(trimmed.substring(2).trim())
            }
            trimmed.startsWith("> ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Quote(trimmed.substring(2).trim())
            }
            else -> {
                val ordered = Regex("^(\\d{1,3})[.)]\\s+(.*)$").find(trimmed)
                if (ordered != null) {
                    flushParagraph()
                    blocks += MarkdownBlock.OrderedListItem(
                        number = ordered.groupValues[1].toIntOrNull() ?: 1,
                        text = ordered.groupValues[2]
                    )
                } else {
                    paragraph.append(if (paragraph.isNotEmpty()) "\n" else "").append(trimmed)
                }
            }
        }
        index++
    }
    flushParagraph()
    return blocks
}
