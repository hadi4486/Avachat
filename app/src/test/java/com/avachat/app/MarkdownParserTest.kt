package com.avachat.app

import com.avachat.app.ui.markdown.parser.MarkdownBlock
import com.avachat.app.ui.markdown.parser.parseMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun `parses heading and paragraph`() {
        val blocks = parseMarkdown("# Title\n\nHello world")
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Heading)
        assertEquals("Title", (blocks[0] as MarkdownBlock.Heading).text)
        assertEquals("Hello world", (blocks[1] as MarkdownBlock.Paragraph).text)
    }

    @Test
    fun `parses fenced code block across lines`() {
        val source = "Before\n```kotlin\nval a = 1\nval b = 2\n```\nAfter"
        val blocks = parseMarkdown(source)
        assertEquals(3, blocks.size)
        val code = blocks[1] as MarkdownBlock.CodeBlock
        assertEquals("val a = 1\nval b = 2", code.code)
    }

    @Test
    fun `handles unterminated code fence without crashing`() {
        val blocks = parseMarkdown("```python\nprint('x')")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.CodeBlock)
    }

    @Test
    fun `parses ordered and unordered lists`() {
        val blocks = parseMarkdown("- one\n- two\n1. first\n2. second")
        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.UnorderedListItem)
        assertTrue(blocks[2] is MarkdownBlock.OrderedListItem)
        assertEquals(2, (blocks[3] as MarkdownBlock.OrderedListItem).number)
    }

    @Test
    fun `parses blockquote`() {
        val blocks = parseMarkdown("> quoted text")
        assertEquals("quoted text", (blocks[0] as MarkdownBlock.Quote).text)
    }

    @Test
    fun `empty input yields no blocks`() {
        assertTrue(parseMarkdown("").isEmpty())
        assertTrue(parseMarkdown("   \n  ").isEmpty())
    }
}
