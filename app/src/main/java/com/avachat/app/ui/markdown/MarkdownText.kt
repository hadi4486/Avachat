package com.avachat.app.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create as ContentCopyIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avachat.app.ui.markdown.parser.MarkdownBlock
import com.avachat.app.ui.markdown.parser.parseMarkdown

/**
 * Lightweight markdown renderer built on the custom parser in
 * [com.avachat.app.ui.markdown.parser]. Renders headings, lists, quotes,
 * inline code, links and code blocks with copy support and horizontal
 * scrolling so long lines never break the layout.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit = {}
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> Text(
                    text = renderInline(block.text),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.SemiBold
                )
                is MarkdownBlock.Paragraph -> Text(
                    text = renderInline(block.text),
                    style = MaterialTheme.typography.bodyLarge
                )
                is MarkdownBlock.UnorderedListItem -> Row {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge)
                    Text(renderInline(block.text), style = MaterialTheme.typography.bodyLarge)
                }
                is MarkdownBlock.OrderedListItem -> Row {
                    Text("${block.number}.  ", style = MaterialTheme.typography.bodyLarge)
                    Text(renderInline(block.text), style = MaterialTheme.typography.bodyLarge)
                }
                is MarkdownBlock.Quote -> Row {
                    Box(
                        Modifier
                            .padding(end = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                RoundedCornerShape(2.dp)
                            )
                            .padding(vertical = 2.dp, horizontal = 1.dp)
                    )
                    Text(
                        renderInline(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is MarkdownBlock.CodeBlock -> CodeBlockCard(code = block.code)
            }
        }
    }
}

@Composable
private fun CodeBlockCard(code: String) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Code",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Icon(
                    Icons.Filled.ContentCopyIcon,
                    contentDescription = androidx.compose.ui.res.stringResource(
                        com.avachat.app.R.string.copy
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Horizontal scroll keeps long lines from breaking the layout.
        SelectionContainer {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            )
        }
    }
}

/**
 * Inline rendering: **bold**, *italic*, `code`, [links](labels).
 */
@Composable
private fun renderInline(text: String): AnnotatedString = remember(text) {
    InlineMarkdown.toAnnotatedString(text)
}
