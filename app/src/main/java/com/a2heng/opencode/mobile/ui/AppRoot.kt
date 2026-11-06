package com.a2heng.opencode.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.a2heng.opencode.mobile.data.AppModel

@Composable
fun AppRoot(model: AppModel) {
    if (!model.connected) {
        ConnectScreen(model)
    } else {
        MainScreen(model)
    }
}

private data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("会话", Icons.Rounded.ChatBubbleOutline),
    Tab("缓存", Icons.Rounded.Speed),
    Tab("文件", Icons.Rounded.FolderOpen),
    Tab("终端", Icons.Rounded.Terminal),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(model: AppModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showNewSession by rememberSaveable { mutableStateOf(false) }
    val chatOpen = tab == 0 && model.current != null

    Scaffold(
        topBar = {
            if (!chatOpen) {
                TopAppBar(
                title = {
                    Column {
                        Text(
                            when (tab) {
                                0 -> "会话"
                                1 -> "缓存命中率"
                                2 -> "文件"
                                else -> "终端"
                            },
                        )
                        Text(
                            if (model.serverVersion.isBlank()) model.serverUrl
                            else "${model.serverUrl}  ·  v${model.serverVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = {
                            if (index == 0 && tab == 0 && model.current != null) {
                                model.closeSession()
                                model.refreshSessions()
                            } else {
                                tab = index
                            }
                        },
                        icon = {
                            if (index == 0 && model.activeIds.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary)
                                    },
                                ) {
                                    Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(22.dp))
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(22.dp))
                            }
                        },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (chatOpen) {
                ChatScreen(model, model.current!!)
            } else {
                ErrorBanner(model)
                when (tab) {
                    0 -> SessionsScreen(model, onNewSession = { showNewSession = true })
                    1 -> CacheScreen(model)
                    2 -> FilesScreen(model)
                    else -> TerminalScreen(model)
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(model, onDismiss = { showSettings = false })
    }
    if (showNewSession) {
        NewSessionDialog(
            model = model,
            onDismiss = { showNewSession = false },
            onCreate = { dir ->
                showNewSession = false
                model.createSession(dir)
            },
        )
    }
}

@Composable
fun ErrorBanner(model: AppModel) {
    val error = model.lastError ?: model.connectionError
    if (error != null) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { model.clearError() }) { Text("关闭") }
        }
    }
}

@Composable
private fun SettingsDialog(model: AppModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LabelValue("服务器", model.serverUrl)
                LabelValue("版本", if (model.serverVersion.isBlank()) "未知" else "v${model.serverVersion}")
                LabelValue("认证", if (model.password.isBlank()) "无密码" else "Basic · opencode")

                Spacer(Modifier.height(8.dp))
                Text("会话运行中发送消息时", style = MaterialTheme.typography.labelLarge)
                OptionRow(
                    title = "排队等待（pending）",
                    subtitle = "等当前回答结束后再发，默认",
                    selected = model.sendMode == "pending",
                ) { model.updateSendMode("pending") }
                OptionRow(
                    title = "立即插入（steer）",
                    subtitle = "马上插进当前回答，模型会看到",
                    selected = model.sendMode == "steer",
                ) { model.updateSendMode("steer") }
                OptionRow(
                    title = "中断后发送",
                    subtitle = "先停掉当前回答，再发这条",
                    selected = model.sendMode == "interrupt",
                ) { model.updateSendMode("interrupt") }

                Spacer(Modifier.height(8.dp))
                Text("聊天显示精细度", style = MaterialTheme.typography.labelLarge)
                OptionRow(
                    title = "精简",
                    subtitle = "只留回答，隐藏思考、工具与页脚",
                    selected = model.detailLevel == "compact",
                ) { model.updateDetailLevel("compact") }
                OptionRow(
                    title = "标准",
                    subtitle = "思考和工具折叠，点开可看",
                    selected = model.detailLevel == "normal",
                ) { model.updateDetailLevel("normal") }
                OptionRow(
                    title = "详细",
                    subtitle = "思考、diff、工具输出默认展开",
                    selected = model.detailLevel == "verbose",
                ) { model.updateDetailLevel("verbose") }

                Spacer(Modifier.height(8.dp))
                Text(
                    "OpenCode Mobile v0.1.0（闭源第一版）\n数据全部来自局域网 opencode 服务，App 只在内存里做聚合。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        dismissButton = {
            TextButton(onClick = {
                model.disconnect()
                onDismiss()
            }) { Text("断开连接", color = MaterialTheme.colorScheme.error) }
        },
    )
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
