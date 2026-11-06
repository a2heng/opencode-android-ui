package com.a2heng.opencode.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.opencode.mobile.data.AppModel
import com.a2heng.opencode.mobile.data.PtyInfo
import com.a2heng.opencode.mobile.data.stripAnsi

@Composable
fun TerminalScreen(model: AppModel) {
    LaunchedEffect(Unit) {
        if (model.ptys.isEmpty()) model.loadPtys()
    }

    val active = model.ptyActive
    if (active != null) {
        BackHandler { model.detachPty() }
        TerminalView(model, active)
    } else {
        PtyList(model)
    }
}

@Composable
private fun PtyList(model: AppModel) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("终端会话", style = MaterialTheme.typography.titleSmall)
                Text(
                    "电脑上开一个 PTY，手机直接敲命令",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { model.loadPtys() }) { Text("刷新") }
            TextButton(onClick = { model.createPty() }) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("新建")
            }
        }

        model.ptyError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }

        if (model.ptys.isEmpty()) {
            EmptyState("还没有终端", "点「新建」会在服务端当前目录启动 bash")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(model.ptys, key = { it.id }) { pty ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickableNoRipple { model.attachPty(pty) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (pty.status == "running") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pty.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(
                                "${pty.command} · pid ${pty.pid} · ${pty.cwd}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            pty.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pty.status == "running") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { model.removePty(pty.id) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "结束",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

private val controlKeys = listOf(
    "Ctrl+C" to "\u0003",
    "Tab" to "\t",
    "Esc" to "\u001b",
    "↑" to "\u001b[A",
    "↓" to "\u001b[B",
    "←" to "\u001b[D",
    "→" to "\u001b[C",
)

@Composable
private fun TerminalView(model: AppModel, pty: PtyInfo) {
    var command by rememberSaveable(pty.id) { mutableStateOf("") }
    val scroll = rememberScrollState()
    val output = stripAnsi(model.ptyOutput)

    LaunchedEffect(output.length) {
        if (scroll.maxValue > 0) scroll.scrollTo(scroll.maxValue)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { model.detachPty() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "离开")
            }
            Column(Modifier.weight(1f)) {
                Text(pty.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(
                    if (model.ptyConnecting) "连接中…"
                    else if (model.ptyConnected) "已连接 · ${pty.cwd}"
                    else "已断开",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (model.ptyConnected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (model.ptyConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            IconButton(onClick = { model.removePty(pty.id) }) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "结束终端",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        model.ptyError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(8.dp),
            ) {
                if (output.isBlank()) {
                    Text(
                        "（屏幕一片空白，敲一条命令试试）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                } else {
                    Text(
                        output,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            controlKeys.forEach { (label, value) ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.clickableNoRipple { model.sendPty(value) },
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                placeholder = { Text("输入命令…") },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(14.dp),
                enabled = model.ptyConnected,
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    model.sendPty(command + "\r")
                    command = ""
                },
                enabled = model.ptyConnected,
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
            ) {
                Icon(
                    Icons.Rounded.Send,
                    contentDescription = "发送",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
