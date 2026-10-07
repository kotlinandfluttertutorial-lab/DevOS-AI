package com.devos.ai.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.devos.ai.designsystem.theme.CodeBlockShape
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.DevOSCodeTextStyle
import com.devos.ai.designsystem.theme.SyntaxColors

// Simple keyword sets by language for token highlighting
private val KOTLIN_KEYWORDS = setOf(
    "fun", "val", "var", "class", "object", "interface", "sealed", "data",
    "return", "if", "else", "when", "for", "while", "do", "in", "is", "as",
    "import", "package", "suspend", "override", "private", "public", "internal",
    "protected", "companion", "abstract", "open", "final", "by", "null", "true",
    "false", "this", "super", "constructor", "init", "typealias", "enum", "annotation",
)

private val JAVA_KEYWORDS = setOf(
    "class", "interface", "extends", "implements", "import", "package",
    "public", "private", "protected", "static", "final", "abstract", "void",
    "return", "if", "else", "for", "while", "do", "new", "null", "true", "false",
    "this", "super", "try", "catch", "finally", "throw", "throws", "instanceof",
    "int", "long", "double", "float", "boolean", "char", "byte", "short",
)

private fun languageKeywords(language: String): Set<String> = when (language.lowercase()) {
    "kotlin", "kt" -> KOTLIN_KEYWORDS
    "java" -> JAVA_KEYWORDS
    else -> emptySet()
}

/**
 * Code block with syntax highlighting and optional line numbers.
 *
 * Background is always [SyntaxColors.background] (#1E1E2E) regardless of the current theme.
 * Font is always [DevOSCodeTextStyle] (JetBrains Mono 13sp).
 *
 * @param code Source code string to render
 * @param language Language hint for keyword highlighting (e.g. "kotlin", "java")
 * @param modifier Optional modifier
 * @param showLineNumbers Whether to show line numbers on the left
 * @param onCopy Called when the copy icon is tapped, passing the raw [code] string
 */
@Composable
fun DevOSCodeBlock(
    code: String,
    language: String = "",
    modifier: Modifier = Modifier,
    showLineNumbers: Boolean = false,
    onCopy: ((String) -> Unit)? = null,
) {
    val keywords = languageKeywords(language)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = SyntaxColors.background, shape = CodeBlockShape),
    ) {
        Column(
            modifier = Modifier
                .padding(DevOSSpacing.base)
                .horizontalScroll(rememberScrollState()),
        ) {
            val lines = code.lines()
            lines.forEachIndexed { index, line ->
                Row(verticalAlignment = Alignment.Top) {
                    if (showLineNumbers) {
                        Text(
                            text = "${index + 1}",
                            style = DevOSCodeTextStyle,
                            color = SyntaxColors.lineNumber,
                            modifier = Modifier.width(DevOSSpacing.xxl),
                        )
                        Spacer(modifier = Modifier.width(DevOSSpacing.sm))
                    }
                    Text(
                        text = highlightLine(line, keywords),
                        style = DevOSCodeTextStyle,
                    )
                }
            }
        }

        if (onCopy != null) {
            IconButton(
                onClick = { onCopy(code) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(DevOSSpacing.xs)
                    .size(DevOSSpacing.touchTarget),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy code",
                    tint = SyntaxColors.lineNumber,
                    modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                )
            }
        }
    }
}

/**
 * Tokenize a single line and return an [androidx.compose.ui.text.AnnotatedString]
 * with syntax colors applied.
 */
private fun highlightLine(
    line: String,
    keywords: Set<String>,
) = buildAnnotatedString {
    // Handle single-line comments
    val commentIndex = line.indexOf("//")
    val activeCode = if (commentIndex >= 0) line.substring(0, commentIndex) else line
    val commentPart = if (commentIndex >= 0) line.substring(commentIndex) else null

    tokenizeLine(activeCode, keywords)

    if (commentPart != null) {
        withStyle(SpanStyle(color = SyntaxColors.comment)) {
            append(commentPart)
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.tokenizeLine(
    text: String,
    keywords: Set<String>,
) {
    var i = 0
    while (i < text.length) {
        when {
            // String literal — double quote
            text[i] == '"' -> {
                val end = text.indexOf('"', i + 1).takeIf { it >= 0 } ?: text.length
                withStyle(SpanStyle(color = SyntaxColors.string)) {
                    append(text.substring(i, end + 1))
                }
                i = end + 1
            }
            // String literal — single quote
            text[i] == '\'' -> {
                val end = text.indexOf('\'', i + 1).takeIf { it >= 0 } ?: text.length
                withStyle(SpanStyle(color = SyntaxColors.string)) {
                    append(text.substring(i, end + 1))
                }
                i = end + 1
            }
            // Annotation @
            text[i] == '@' -> {
                val end = text.drop(i + 1).indexOfFirst { !it.isLetterOrDigit() && it != '_' }
                    .takeIf { it >= 0 }?.let { i + 1 + it } ?: text.length
                withStyle(SpanStyle(color = SyntaxColors.annotation)) {
                    append(text.substring(i, end))
                }
                i = end
            }
            // Identifier or keyword
            text[i].isLetter() || text[i] == '_' -> {
                val end = text.drop(i).indexOfFirst { !it.isLetterOrDigit() && it != '_' }
                    .takeIf { it >= 0 }?.let { i + it } ?: text.length
                val word = text.substring(i, end)
                val color: Color = when {
                    word in keywords -> SyntaxColors.keyword
                    word[0].isUpperCase() -> SyntaxColors.type
                    else -> SyntaxColors.variable
                }
                withStyle(SpanStyle(color = color)) {
                    append(word)
                }
                i = end
            }
            // Number
            text[i].isDigit() -> {
                val end = text.drop(i).indexOfFirst { !it.isDigit() && it != '.' && it != '_' }
                    .takeIf { it >= 0 }?.let { i + it } ?: text.length
                withStyle(SpanStyle(color = SyntaxColors.number)) {
                    append(text.substring(i, end))
                }
                i = end
            }
            // Operator characters
            text[i] in "=<>!&|+-*/%^~" -> {
                withStyle(SpanStyle(color = SyntaxColors.operator)) {
                    append(text[i].toString())
                }
                i++
            }
            else -> {
                withStyle(SpanStyle(color = SyntaxColors.variable)) {
                    append(text[i].toString())
                }
                i++
            }
        }
    }
}
