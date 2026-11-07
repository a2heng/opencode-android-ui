package com.a2heng.opencode.mobile.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.a2heng.opencode.mobile.data.AppModel
import com.a2heng.opencode.mobile.data.SessionInfo
import com.a2heng.opencode.mobile.data.formatTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionsScreen(
    model: AppModel,
    onNewSession: () -> Unit,
    onOpenCache: (SessionInfo) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var grouped by rememberSaveable { mutableStateOf(true) }
    val list = remember(model.sessions, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) model.sessions
        else model.sessions.filter {
            it.title.lowercase().contains(q) || it.directory.lowercase().contains(q) ||
                it.agent.lowercase().contains(q)
        }
    }

    val running = remember(list, model.activeIds) { list.filter { model.isActive(it.id) } }
    val rest = remember(list, model.activeIds) { list.filterNot { model.isActive(it.id) } }
    // 没有运行中的会话时，顶部用「最近」补位，避免一大片空白
    val recent = remember(rest, running) { if (running.isEmpty()) rest.take(3) else emptyList() }
    val groupedRest = remember(rest, recent) {
        if (recent.isEmpty()) rest else rest.drop(recent.size)
    }
    val groups = remember(groupedRest) {
        groupedRest.groupBy { it.directory.ifBlank { "（默认目录）" } }
            .entries
            .sortedByDescending { entry -> entry.value.maxOf { it.updated } }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索会话") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { model.refreshSessions() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "共 ${list.size} 个会话",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { grouped = true }) {
                    Text(
                        "按文件夹",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (grouped) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { grouped = false }) {
                    Text(
                        "按时间",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!grouped) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
                model.sessionsLoading && model.sessions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }
                list.isEmpty() -> EmptyState("还没有会话", "点右下角新建一个，或在电脑上先跑起来")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    if (running.isNotEmpty()) {
                        stickyHeader(key = "running") {
                            RunningHeader(running.size)
                        }
                        items(running, key = { "run:${it.id}" }) { session ->
                            SessionRow(model, session, onOpenCache)
                        }
                    } else if (recent.isNotEmpty()) {
                        stickyHeader(key = "recent") {
                            RecentHeader()
                        }
                        items(recent, key = { "recent:${it.id}" }) { session ->
                            SessionRow(model, session, onOpenCache)
                        }
                    }
                    if (grouped) {
                        groups.forEach { (dir, items) ->
                            stickyHeader(key = "folder:$dir") {
                                FolderHeader(dir, items.size, model.homeDir)
                            }
                            items(items, key = { it.id }) { session ->
                                SessionRow(model, session, onOpenCache)
                            }
                        }
                    } else {
                        items(rest, key = { it.id }) { session ->
                            SessionRow(model, session, onOpenCache)
                        }
                    }
                    item { Spacer(Modifier.height(90.dp)) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNewSession,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp),
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("新建会话") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun RunningHeader(count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.size(7.dp))
        Text(
            "运行中",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun RecentHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.History,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            "最近",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            "没有运行中的会话",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun FolderHeader(dir: String, count: Int, home: String) {
    val homePrefix = home.trimEnd('/')
    val label = when {
        homePrefix.isNotEmpty() && dir.trimEnd('/') == homePrefix -> "~"
        homePrefix.isNotEmpty() && dir.startsWith("$homePrefix/") -> dir.removePrefix("$homePrefix/")
        else -> dir
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Folder,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(Modifier.size(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (label != dir) {
                Text(
                    dir,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionRow(
    model: AppModel,
    session: SessionInfo,
    onOpenCache: (SessionInfo) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    val active = model.isActive(session.id)
    val shape = RoundedCornerShape(12.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(
                if (active) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), shape)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), shape)
                } else {
                    Modifier
                },
            )
            .clickableNoRipple { model.openSession(session) }
            .padding(start = 8.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val rate = session.tokens?.hitRate ?: 0.0
        val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
        val rateTint = if (rate <= 0.0) MaterialTheme.colorScheme.onSurfaceVariant else rateColor(rate, dark)
        // 左：命中率按钮（整块可点，进入缓存页）
        Surface(
            color = rateTint.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.clickableNoRipple { onOpenCache(session) },
        ) {
            Text(
                rateLabelSafe(rate),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = rateTint,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .width(46.dp)
                    .padding(vertical = 7.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                session.shortTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                buildList {
                    add(session.directory.substringAfterLast('/').ifBlank { session.directory })
                    add(session.model?.let { modelDisplay(it, model.modelGroupById) }?.takeIf { it.isNotBlank() }
                        ?: session.agent.ifBlank { "—" })
                    session.tokens?.let { add(formatTokens(it.input + it.cacheRead) + " tok") }
                    add(formatRelative(session.updated))
                }.joinToString("  ·  "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // 右：三个点（尽量紧凑，避免抢走命中率按钮的可点区域）
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "更多", modifier = Modifier.size(17.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        menu = false
                        rename = true
                    },
                )
                DropdownMenuItem(
                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    onClick = {
                        menu = false
                        confirmDelete = true
                    },
                )
            }
        }
    }

    if (confirmDelete) {
        AppDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除会话？") },
            text = { Text(session.shortTitle) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    model.deleteSession(session.id)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }

    if (rename) {
        var title by remember { mutableStateOf(session.shortTitle) }
        AppDialog(
            onDismissRequest = { rename = false },
            title = { Text("重命名会话") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    rename = false
                    if (title.isNotBlank()) model.renameSession(session.id, title)
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { rename = false }) { Text("取消") } },
        )
    }
}

private fun rateLabelSafe(rate: Double): String =
    if (rate <= 0.0) "—" else String.format("%.0f%%", rate)
