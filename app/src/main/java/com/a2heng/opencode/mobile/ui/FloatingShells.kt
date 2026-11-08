package com.a2heng.opencode.mobile.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.opencode.mobile.data.AppModel
import kotlin.math.roundToInt

/**
 * 后台 shell 的悬浮球：默认只显示一个小圆点，点开是卡片，可拖动避免挡住内容。
 * 数据来自 /api/shell（后台命令列表）与 /api/shell/{id}/output。
 */
@Composable
fun FloatingShells(model: AppModel, sessionID: String, modifier: Modifier = Modifier) {
    // 只显示当前会话自己的后台 shell；别的会话在跑命令不在这里弹球
    val shells = model.shells.filter { it.sessionID == sessionID }
    if (shells.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<String?>(null) }
    var output by remember { mutableStateOf("") }
    var confirmStopId by remember { mutableStateOf<String?>(null) }
    // 每秒刷新一次已运行时长
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    // 展开的那一条持续刷新输出（后台命令的输出还在增长）
    LaunchedEffect(selected) {
        val id = selected ?: return@LaunchedEffect
        while (true) {
            output = model.loadShellOutput(id)
            kotlinx.coroutines.delay(1500)
        }
    }

    confirmStopId?.let { stopId ->
        val cmd = shells.firstOrNull { it.id == stopId }?.command.orEmpty().trim()
        val short = when {
            cmd.isBlank() -> ""
            cmd.length <= 28 -> cmd
            else -> cmd.take(28).trimEnd() + "…"
        }
        AlertDialog(
            onDismissRequest = { confirmStopId = null },
            text = {
                Text(
                    if (short.isNotBlank()) "关闭「$short」这个 shell？"
                    else "关闭这个 shell？",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selected == stopId) {
                        selected = null
                        output = ""
                    }
                    model.stopShell(stopId)
                    confirmStopId = null
                }) { Text("关闭", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmStopId = null }) { Text("取消") }
            },
        )
    }

    Box(modifier.offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }) {
        if (!expanded) {
            Box(
                Modifier
                    .size(52.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            offset += drag
                        }
                    }
                    .clickableNoRipple { expanded = true },
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(46.dp)
                        .align(Alignment.BottomStart),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.PlaylistPlay,
                            contentDescription = "后台 shell",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                if (shells.size > 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(19.dp)
                            .align(Alignment.TopEnd),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                shells.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.width(300.dp),
            ) {
                Column(Modifier.padding(10.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    offset += drag
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.PlaylistPlay,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "后台 shell · ${shells.size}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .weight(1f),
                        )
                        Text(
                            "收起",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { expanded = false }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "收起窗口（不会停止命令）",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    shells.take(6).forEach { shell ->
                        // 每一行：最左边是已运行时长，点一下就在这一行下面展开输出；
                        // 同一时间只允许展开一条（selected 只有一个）。
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickableNoRipple {
                                    if (selected == shell.id) {
                                        selected = null
                                        output = ""
                                    } else {
                                        selected = shell.id
                                        output = ""
                                    }
                                }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                elapsedLabel(shell.started, nowMs),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(44.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                shell.command,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (selected == shell.id) "收起" else "查看",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            IconButton(
                                onClick = { confirmStopId = shell.id },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Stop,
                                    contentDescription = "停止这个命令",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        if (selected == shell.id) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(start = 50.dp, end = 4.dp, bottom = 6.dp),
                            ) {
                                Text(
                                    output.ifBlank { "（还没有输出）" }.takeLast(4000),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 已运行时长：<1 分钟显示 12s，<1 小时显示 3m05s，再长显示 1h02m。 */
private fun elapsedLabel(started: Long, now: Long): String {
    if (started <= 0L) return ""
    val s = ((now - started) / 1000L).coerceAtLeast(0L)
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "%dm%02ds".format(s / 60, s % 60)
        else -> "%dh%02dm".format(s / 3600, (s % 3600) / 60)
    }
}
