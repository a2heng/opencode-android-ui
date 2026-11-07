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
import androidx.compose.runtime.remember
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
        return
    }
    // 会话页盖在列表上：预测式返回时整页滑走，露出下面的列表（真正的上一页）
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        MainScreen(model)
        val current = model.current
        if (current != null) {
            ChatScreen(model, current)
        }
    }
}

private data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("会话", Icons.Rounded.ChatBubbleOutline),
    Tab("文件", Icons.Rounded.FolderOpen),
    Tab("终端", Icons.Rounded.Terminal),
    Tab("设置", Icons.Rounded.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(model: AppModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showNewSession by rememberSaveable { mutableStateOf(false) }

    val cache = model.cacheTarget
    if (cache != null) {
        SessionCacheScreen(
            model = model,
            session = cache,
            onBack = { model.closeCache() },
            onOpenChat = { session ->
                model.closeCache()
                model.openSession(session)
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            when (tab) {
                                0 -> "会话"
                                1 -> "文件"
                                2 -> "终端"
                                else -> "设置"
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
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
            ErrorBanner(model)
            when (tab) {
                0 -> SessionsScreen(
                    model = model,
                    onNewSession = { showNewSession = true },
                    onOpenCache = { model.openCache(it) },
                )
                1 -> FilesScreen(model)
                2 -> TerminalScreen(model)
                else -> SettingsScreen(model)
            }
        }
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
