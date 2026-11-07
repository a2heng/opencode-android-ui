package com.a2heng.opencode.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.opencode.mobile.data.AppModel
import com.a2heng.opencode.mobile.data.FsEntry

@Composable
fun FilesScreen(model: AppModel, wide: Boolean = false) {
    LaunchedEffect(Unit) {
        if (model.fsDir.isBlank()) model.loadDir(null)
    }

    BackHandler(enabled = model.openFile != null || model.fsDir.trimEnd('/') != model.fsRoot.trimEnd('/')) {
        if (model.openFile != null) {
            model.closeFile()
        } else {
            val root = model.fsRoot.trimEnd('/')
            val current = model.fsDir.trimEnd('/')
            val parent = current.substringBeforeLast('/')
            model.loadDir(if (parent.length < root.length) root else parent)
        }
    }

    if (wide) {
        // 横屏：左边目录/文件列表，右边预览，选文件不再把列表顶掉
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(320.dp).fillMaxHeight()) { FsListPane(model) }
            androidx.compose.material3.VerticalDivider(
                Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            )
            Box(Modifier.weight(1f).fillMaxHeight()) {
                val open = model.openFile
                if (open != null) {
                    FileViewer(model, open)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "选一个文件在这里查看",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        return
    }

    val open = model.openFile
    if (open != null) {
        FileViewer(model, open)
        return
    }

    FsListPane(model)
}

@Composable
private fun FsListPane(model: AppModel) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    val root = model.fsRoot.trimEnd('/')
                    val current = model.fsDir.trimEnd('/')
                    if (current.isNotEmpty() && current != root) {
                        val parent = current.substringBeforeLast('/')
                        model.loadDir(if (parent.length < root.length) root else parent)
                    }
                },
                enabled = model.fsDir.trimEnd('/') != model.fsRoot.trimEnd('/'),
            ) {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = "上级目录")
            }
            Text(
                model.fsDir.replace(model.fsRoot, "").ifBlank { "/" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (model.fsLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }

        model.fsError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }

        if (model.fsEntries.isEmpty() && !model.fsLoading) {
            EmptyState("目录是空的")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(model.fsEntries, key = { it.path }) { entry ->
                    FsRow(entry) {
                        val full = fullPath(model.fsRoot, entry.path)
                        if (entry.isDir) model.loadDir(full) else model.openFile(full)
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

private fun fullPath(root: String, relative: String): String =
    root.trimEnd('/') + "/" + relative.trimStart('/')

@Composable
private fun FsRow(entry: FsEntry, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (entry.isDir) Icons.Rounded.Folder else Icons.Rounded.Description,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (entry.isDir) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            entry.path.trimEnd('/').substringAfterLast('/'),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FileViewer(model: AppModel, path: String) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { model.closeFile() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    path.substringAfterLast('/'),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    path.substringBeforeLast('/', ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (model.openFileLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(end = 4.dp), strokeWidth = 2.dp)
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                model.openFileText.take(120_000),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
