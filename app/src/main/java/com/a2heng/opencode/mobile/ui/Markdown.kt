package com.a2heng.opencode.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
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

private sealed interface Line {
    data class Heading(val level: Int, val text: String) : Line
    data class Quote(val text: String) : Line
    data class ListItem(val indent: Int, val marker: String, val text: String) : Line
    data class Para(val text: String) : Line
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val aligns: List<Int>,
    ) : Line
    data object Rule : Line
    data object Gap : Line
}

private val headingRe = Regex("^(#{1,6})\\s+(.*)$")
private val quoteRe = Regex("^\\s*>\\s?(.*)$")
private val listRe = Regex("^(\\s*)([-*+]|\\d+[.)])\\s+(.*)$")
private val ruleRe = Regex("^\\s*(---+|\\*\\*\\*+|___+)\\s*$")

private fun splitRow(line: String): List<String> =
    line.trim().trim('|').split('|').map { it.trim() }

private fun isTableSeparator(line: String): Boolean {
    val t = line.trim()
    if (!t.contains('-')) return false
    val cells = t.trim('|').split('|')
    return cells.isNotEmpty() && cells.all { Regex("^:?-{2,}:?$").matches(it.trim()) }
}

private fun tableAligns(line: String): List<Int> =
    splitRow(line).map { cell ->
        when {
            cell.startsWith(":") && cell.endsWith(":") -> 0
            cell.endsWith(":") -> 1
            else -> -1
        }
    }

private fun parseLines(text: String): List<Line> {
    val lines = text.split('\n')
    val out = mutableListOf<Line>()
    val para = mutableListOf<String>()
    fun flush() {
        if (para.isNotEmpty()) {
            out += Line.Para(para.joinToString("\n"))
            para.clear()
        }
    }
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trimEnd()
        // 竖线表格：| a | b | + | --- | --- |
        if (line.trimStart().startsWith("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
            flush()
            val headers = splitRow(line)
            val aligns = tableAligns(lines[i + 1])
            i += 2
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trimStart().startsWith("|")) {
                rows += splitRow(lines[i])
                i++
            }
            out += Line.Table(headers, rows, aligns)
            continue
        }
        val h = headingRe.find(line)
        if (h != null) {
            flush()
            out += Line.Heading(h.groupValues[1].length, h.groupValues[2].trim())
            i++
            continue
        }
        if (line.isBlank()) {
            flush()
            out += Line.Gap
            i++
            continue
        }
        if (ruleRe.matches(line)) {
            flush()
            out += Line.Rule
            i++
            continue
        }
        val q = quoteRe.find(line)
        if (q != null) {
            flush()
            out += Line.Quote(q.groupValues[1])
            i++
            continue
        }
        val l = listRe.find(line)
        if (l != null) {
            flush()
            val indent = l.groupValues[1].length / 2
            out += Line.ListItem(indent, l.groupValues[2], l.groupValues[3])
            i++
            continue
        }
        para += line
        i++
    }
    flush()
    return out
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
private fun MarkdownTable(
    line: Line.Table,
    codeColor: Color,
    linkColor: Color,
    compact: Boolean,
) {
    val colCount = line.headers.size.coerceAtLeast(1)
    val widths = (0 until colCount).map { c ->
        val maxLen = (listOf(line.headers.getOrElse(c) { "" }) + line.rows.map { it.getOrElse(c) { "" } })
            .maxOfOrNull { it.length } ?: 0
        (maxLen * 7).coerceIn(64, 220).dp
    }
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
    ) {
        Column {
            Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))) {
                line.headers.forEachIndexed { c, cell ->
                    Text(
                        inline(cell, codeColor, linkColor),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = if (compact) 10.sp else 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .width(widths[c])
                            .border(0.5.dp, grid)
                            .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = if (compact) 3.dp else 5.dp),
                    )
                }
            }
            line.rows.forEachIndexed { r, row ->
                Row(
                    Modifier.background(
                        if (r % 2 == 0) Color.Transparent
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                    ),
                ) {
                    (0 until colCount).forEach { c ->
                        val cell = row.getOrElse(c) { "" }
                        Text(
                            inline(cell, codeColor, linkColor),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = if (compact) 10.sp else 11.sp,
                            textAlign = when (line.aligns.getOrElse(c) { -1 }) {
                                0 -> TextAlign.Center
                                1 -> TextAlign.End
                                else -> TextAlign.Start
                            },
                            modifier = Modifier
                                .width(widths[c])
                                .border(0.5.dp, grid)
                                .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = if (compact) 3.dp else 5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownLines(text: String, codeColor: Color, linkColor: Color, compact: Boolean) {
    val lines = remember(text) { parseLines(text) }
    val base = if (compact) {
        MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp)
    } else {
        MaterialTheme.typography.bodyMedium
    }
    Column(Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            when (line) {
                is Line.Heading -> Text(
                    inline(line.text, codeColor, linkColor),
                    style = if (compact) {
                        when (line.level) {
                            1 -> MaterialTheme.typography.titleSmall
                            2 -> MaterialTheme.typography.labelLarge
                            else -> MaterialTheme.typography.labelMedium
                        }
                    } else {
                        when (line.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        }
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = if (compact) 4.dp else 6.dp, bottom = 2.dp),
                )
                is Line.Quote -> Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 2.dp),
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(2.dp)),
                    )
                    Text(
                        inline(line.text, codeColor, linkColor),
                        style = base,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                is Line.ListItem -> Row(
                    Modifier.padding(start = (line.indent * 12).dp, top = 1.dp, bottom = 1.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        if (line.marker in listOf("-", "*", "+")) "•" else line.marker,
                        style = base,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(if (compact) 16.dp else 18.dp),
                    )
                    Text(
                        inline(line.text, codeColor, linkColor),
                        style = base,
                        modifier = Modifier.weight(1f),
                    )
                }
                is Line.Table -> MarkdownTable(line, codeColor, linkColor, compact)
                Line.Rule -> HorizontalDivider(Modifier.padding(vertical = if (compact) 4.dp else 6.dp))
                Line.Gap -> Spacer(Modifier.height(if (compact) 4.dp else 6.dp))
                is Line.Para -> SelectionContainer {
                    Text(
                        inline(line.text, codeColor, linkColor),
                        style = base,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, compact: Boolean = false) {
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
                            fontSize = if (compact) 10.5.sp else 11.sp,
                            color = mono,
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
                is Block.Text -> MarkdownLines(block.text, codeColor, linkColor, compact)
            }
        }
    }
}
