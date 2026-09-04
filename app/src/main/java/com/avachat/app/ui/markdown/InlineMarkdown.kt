package com.avachat.app.ui.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Inline markdown (**bold**, *italic*, `code`, link labels) to AnnotatedString.
 * Fully defensive: any parse hiccup degrades to plain text.
 */
object InlineMarkdown {

    fun toAnnotatedString(text: String): androidx.compose.ui.text.AnnotatedString = try {
        buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                when {
                    text.startsWith("**", i) -> {
                        val end = text.indexOf("**", i + 2)
                        if (end > i + 1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(text.substring(i + 2, end))
                            }
                            i = end + 2
                        } else {
                            append(text[i]); i++
                        }
                    }
                    text[i] == '`' -> {
                        val end = text.indexOf('`', i + 1)
                        if (end > i) {
                            withStyle(
                                SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            ) {
                                append(text.substring(i + 1, end))
                            }
                            i = end + 1
                        } else {
                            append(text[i]); i++
                        }
                    }
                    text[i] == '*' && i + 1 < text.length && text[i + 1] != '*' -> {
                        val end = text.indexOf('*', i + 1)
                        if (end > i + 1) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(text.substring(i + 1, end))
                            }
                            i = end + 1
                        } else {
                            append(text[i]); i++
                        }
                    }
                    text.startsWith("[", i) -> {
                        val close = text.indexOf(']', i)
                        val openParen = close + 1
                        if (close != -1 && openParen < text.length && text[openParen] == '(') {
                            val parenClose = text.indexOf(')', openParen)
                            if (parenClose != -1) {
                                append(text.substring(i + 1, close))
                                i = parenClose + 1
                            } else {
                                append(text[i]); i++
                            }
                        } else {
                            append(text[i]); i++
                        }
                    }
                    else -> {
                        append(text[i]); i++
                    }
                }
            }
        }
    } catch (t: Throwable) {
        androidx.compose.ui.text.AnnotatedString(text)
    }
}
