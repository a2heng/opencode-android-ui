package com.a2heng.opencode.mobile.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.opencode.mobile.data.AppModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 后台 shell 的悬浮球：默认只显示一个小圆点，点开是卡片，可拖动避免挡住内容。
 * 数据来自 /api/shell（后台命令列表）与 /api/shell/{id}/output。
 */
@Composable
fun FloatingShells(model: AppModel, modifier: Modifier = Modifier) {
    val running = model.shells.filter { it.status.isEmpty() || it.status == "running" }
    if (running.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<String?>(null) }
    var output by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(modifier.offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }) {
        if (!expanded) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(46.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            offset += drag
                        }
                    }
                    .clickableNoRipple { expanded = true },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Terminal,
                        contentDescription = "后台 shell",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                    if (running.size > 1) {
                        Text(
                            running.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 26.dp, bottom = 26.dp),
                        )
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
                            Icons.Rounded.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "后台 shell · ${running.size}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .weight(1f),
                        )
                        IconButton(onClick = { expanded = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "收起", modifier = Modifier.size(15.dp))
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    running.take(4).forEach { shell ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickableNoRipple {
                                    selected = shell.id
                                    scope.launch { output = model.loadShellOutput(shell.id) }
                                }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
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
                                if (selected == shell.id) "查看中" else "输出",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (output.isNotBlank()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(top = 4.dp),
                        ) {
                            Text(
                                output.takeLast(4000),
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
