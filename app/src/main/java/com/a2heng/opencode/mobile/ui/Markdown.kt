package com.a2heng.opencode.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// 一个够用的 Markdown 渲染器（不引第三方库）：
//   · 围栏代码块 ```lang ... ``` 单独成块，等宽 + 可横向滚动
//   · 行内 `code` / **粗体** / *斜体* / [链接](url) / # 标题
//   · 其余按纯文本渲染，保留换行
// ---------------------------------------------------------------------------

private sealed interface Block {
    data class Text(val text: String) : Block
    data class Code(val lang: String, val code: String) : Block
}

private fun splitBlocks(source: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val buffer = StringBuilder()
    var code: StringBuilder? = null
    var lang = ""

    fun flushText() {
        val t = buffer.toString().trim('\n')
        if (t.isNotBlank()) blocks += Block.Text(t)
        buffer.clear()
    }

    for (line in source.split('\n')) {
        val fence = line.trimStart()
        if (fence.startsWith("```")) {
            if (code == null) {
                flushText()
                code = StringBuilder()
                lang = fence.removePrefix("```").trim()
            } else {
                blocks += Block.Code(lang, code.toString().trimEnd('\n'))
                code = null
                lang = ""
            }
            continue
        }
        if (code != null) {
            code.append(line).append('\n')
        } else {
            buffer.append(line).append('\n')
        }
    }
    if (code != null) {
        // 没闭合的代码块也按代码显示，总比吞掉好
        blocks += Block.Code(lang, code.toString().trimEnd('\n'))
    } else {
        flushText()
    }
    return blocks
}

private val inlinePattern = Regex(
    "(`[^`\\n]+`)|(\\*\\*[^*\\n]+\\*\\*)|(\\*[^*\\n]+\\*)|(\\[[^\\]\\n]+\\]\\([^)\\n]+\\))",
)

private fun inline(text: String, code: Color, link: Color): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in inlinePattern.findAll(text)) {
        if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
        val token = match.value
        when {
            token.startsWith("`") -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = code,
                        background = code.copy(alpha = 0.12f),
                    ),
                ) { append(token.substring(1, token.length - 1)) }
            }
            token.startsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(token.substring(2, token.length - 2))
                }
            }
            token.startsWith("*") -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(token.substring(1, token.length - 1))
                }
            }
            token.startsWith("[") -> {
                val label = token.substringAfter('[').substringBefore(']')
                withStyle(SpanStyle(color = link, textDecoration = TextDecoration.Underline)) {
                    append(label)
                }
            }
            else -> append(token)
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { splitBlocks(text) }
    val codeColor = MaterialTheme.colorScheme.primary
    val linkColor = MaterialTheme.colorScheme.secondary
    val mono = MaterialTheme.colorScheme.onSurface

    Column(modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is Block.Code -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp),
                            ),
                    ) {
                        if (block.lang.isNotBlank()) {
                            Text(
                                block.lang,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 10.dp, top = 5.dp),
                            )
                        }
                        Text(
                            block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = mono,
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
                is Block.Text -> {
                    SelectionContainer {
                        Text(
                            inline(block.text, codeColor, linkColor),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}
