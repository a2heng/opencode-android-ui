package com.a2heng.opencode.mobile.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.a2heng.opencode.mobile.data.AppModel
import com.a2heng.opencode.mobile.data.FsEntry

/**
 * 新建会话时选择（或新建）一个文件夹。
 *
 * 目录来自 /api/fs/list：返回的 entry.path 是相对 location 根目录的，
 * 所以拼绝对路径时要用 root + entry.path。
 */
@Composable
fun NewSessionDialog(
    model: AppModel,
    onDismiss: () -> Unit,
    onCreate: (String?) -> Unit,
) {
    val initial = model.current?.directory?.takeIf { it.isNotBlank() }
        ?: model.sessions.firstOrNull()?.directory
        ?: ""
    var path by remember { mutableStateOf(initial) }
    var root by remember { mutableStateOf("") }
    var dirs by remember { mutableStateOf<List<FsEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    var creatingFolder by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }

    LaunchedEffect(path, reload) {
        loading = true
        error = null
        try {
            val (r, list) = model.browseDirs(path.ifBlank { null })
            root = r
            if (path.isBlank()) path = r
            dirs = list.filter { it.isDir && !it.name.startsWith(".") }
                .sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建会话") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val r = root.trimEnd('/')
                            val cur = path.trimEnd('/')
                            val parent = cur.substringBeforeLast('/')
                            if (cur.isNotEmpty() && cur != r && parent.length >= r.length) {
                                path = parent.ifBlank { r }
                            }
                        },
                        enabled = path.trimEnd('/').isNotEmpty() && path.trimEnd('/') != root.trimEnd('/'),
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "上一级", modifier = Modifier.size(20.dp))
                    }
                    Text(
                        path,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider()
                val recents = remember(model.sessions) {
                    model.sessions.map { it.directory }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(6)
                }
                if (recents.isNotEmpty()) {
                    Text(
                        "最近使用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        recents.forEach { dir ->
                            Surface(
                                color = if (path.trimEnd('/') == dir.trimEnd('/')) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickableNoRipple { path = dir },
                            ) {
                                Text(
                                    shortDir(dir, model.homeDir),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
                Box(Modifier.fillMaxWidth().height(260.dp)) {
                    when {
                        loading -> Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        }
                        error != null -> Text(
                            error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp),
                        )
                        dirs.isEmpty() -> EmptyState("这个目录下没有子文件夹")
                        else -> LazyColumn {
                            items(dirs, key = { it.path }) { entry ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickableNoRipple {
                                            path = fullPath(root, entry.path)
                                        }
                                        .padding(vertical = 9.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        entry.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { creatingFolder = true }) {
                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("新建文件夹")
                    }
                    Text(
                        "会话将在此目录运行",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(path.ifBlank { null }) }) {
                Text("在这里创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )

    if (creatingFolder) {
        AlertDialog(
            onDismissRequest = { creatingFolder = false },
            title = { Text("新建文件夹") },
            text = {
                Column {
                    Text(
                        "在 $path 下创建",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        placeholder = { Text("文件夹名，例如 PureVox") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = folderName.isNotBlank(),
                    onClick = {
                        val name = folderName.trim()
                        creatingFolder = false
                        folderName = ""
                        if (name.isNotEmpty()) {
                            model.createFolder(path, name) {
                                path = path.trimEnd('/') + "/" + name
                                reload++
                            }
                        }
                    },
                ) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = {
                    creatingFolder = false
                    folderName = ""
                }) { Text("取消") }
            },
        )
    }
}

private fun fullPath(root: String, relative: String): String =
    root.trimEnd('/') + "/" + relative.trimStart('/')

private fun shortDir(dir: String, home: String): String {
    val h = home.trimEnd('/')
    return when {
        h.isNotEmpty() && dir.trimEnd('/') == h -> "~"
        h.isNotEmpty() && dir.startsWith("$h/") -> dir.removePrefix("$h/").trimEnd('/')
        else -> dir.trimEnd('/')
    }
}
