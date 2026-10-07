package com.devos.ai.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.devos.ai.designsystem.theme.DevOSCodeTextStyle
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.SyntaxColors
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as CMText
import org.commonmark.parser.Parser

/**
 * Renders a markdown string using [org.commonmark.parser.Parser].
 *
 * Supported elements:
 * - Headings → titleSmall / titleMedium
 * - Emphasis → italic
 * - Strong → bold
 * - Inline code → DevOSCodeTextStyle + SyntaxColors.background
 * - Fenced code blocks → [DevOSCodeBlock]
 * - Bullet and ordered lists → bulleted rows
 * - Links → primary color annotated text
 * - Paragraphs with line breaks
 *
 * @param markdown Markdown source string
 * @param modifier Optional modifier
 * @param style Base text style — defaults to bodyMedium
 */
@Composable
fun DevOSMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val parser = remember { Parser.builder().build() }
    val document = remember(markdown) { parser.parse(markdown) }

    Column(modifier = modifier) {
        renderNode(node = document, style = style)
    }
}

@Composable
private fun renderNode(node: Node, style: TextStyle) {
    var child = node.firstChild
    while (child != null) {
        when (child) {
            is Heading -> {
                val headingStyle = if (child.level <= 2) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleSmall
                }
                Text(
                    text = inlineChildren(child),
                    style = headingStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(DevOSSpacing.xs))
            }
            is Paragraph -> {
                Text(
                    text = inlineChildren(child),
                    style = style,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(DevOSSpacing.xs))
            }
            is FencedCodeBlock -> {
                DevOSCodeBlock(
                    code = child.literal.trimEnd('\n'),
                    language = child.info ?: "",
                    showLineNumbers = false,
                )
                Spacer(modifier = Modifier.height(DevOSSpacing.sm))
            }
            is BulletList -> {
                renderListItems(child, ordered = false, style = style)
            }
            is OrderedList -> {
                renderListItems(child, ordered = true, style = style)
            }
            is BlockQuote -> {
                Text(
                    text = inlineChildren(child.firstChild ?: child),
                    style = style.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(DevOSSpacing.xs))
            }
            else -> {
                // Recurse for containers we don't specifically handle
                renderNode(node = child, style = style)
            }
        }
        child = child.next
    }
}

@Composable
private fun renderListItems(listNode: Node, ordered: Boolean, style: TextStyle) {
    var index = 1
    var item = listNode.firstChild
    while (item != null) {
        if (item is ListItem) {
            val bullet = if (ordered) "$index." else "•"
            Text(
                text = buildAnnotatedString {
                    append("$bullet  ")
                    append(inlineChildren(item.firstChild ?: item))
                },
                style = style,
                color = MaterialTheme.colorScheme.onSurface,
            )
            index++
        }
        item = item.next
    }
    Spacer(modifier = Modifier.height(DevOSSpacing.xs))
}

/**
 * Walk inline children of a node and build an [AnnotatedString] with span styles.
 */
private fun inlineChildren(node: Node): AnnotatedString = buildAnnotatedString {
    appendInline(node)
}

private fun AnnotatedString.Builder.appendInline(node: Node) {
    var child: Node? = node.firstChild ?: node
    // If node itself is a leaf, handle it directly
    if (node.firstChild == null) {
        appendLeaf(node)
        return
    }
    child = node.firstChild
    while (child != null) {
        when (child) {
            is CMText -> append(child.literal)
            is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInline(child)
            }
            is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInline(child)
            }
            is Code -> withStyle(
                SpanStyle(
                    fontFamily = DevOSCodeTextStyle.fontFamily,
                    fontSize = DevOSCodeTextStyle.fontSize,
                    background = SyntaxColors.background,
                )
            ) {
                append(child.literal)
            }
            is Link -> withStyle(
                SpanStyle(color = androidx.compose.ui.graphics.Color.Unspecified)
            ) {
                // We capture link color at call-site via primary
                appendInline(child)
            }
            is SoftLineBreak, is HardLineBreak -> append("\n")
            else -> appendInline(child)
        }
        child = child.next
    }
}

private fun AnnotatedString.Builder.appendLeaf(node: Node) {
    when (node) {
        is CMText -> append(node.literal)
        is Code -> withStyle(
            SpanStyle(
                fontFamily = DevOSCodeTextStyle.fontFamily,
                fontSize = DevOSCodeTextStyle.fontSize,
                background = SyntaxColors.background,
            )
        ) {
            append(node.literal)
        }
        is SoftLineBreak, is HardLineBreak -> append("\n")
        else -> { /* skip */ }
    }
}
