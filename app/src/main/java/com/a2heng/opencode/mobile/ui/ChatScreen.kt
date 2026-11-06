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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.opencode.mobile.data.AppModel
import com.a2heng.opencode.mobile.data.FormField
import com.a2heng.opencode.mobile.data.FormInfo
import com.a2heng.opencode.mobile.data.Msg
import com.a2heng.opencode.mobile.data.Part
import com.a2heng.opencode.mobile.data.PermissionReq
import com.a2heng.opencode.mobile.data.SessionInfo
import com.a2heng.opencode.mobile.data.formatCost
import com.a2heng.opencode.mobile.data.formatTokens
import com.a2heng.opencode.mobile.data.rateLabel
import com.a2heng.opencode.mobile.data.str
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(model: AppModel, session: SessionInfo) {
    var input by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var pickModel by remember { mutableStateOf(false) }
    var pickAgent by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val pendingPermissions = model.permissions.filter { it.sessionID == session.id }
    val pendingForms = model.forms.filter { it.sessionID == session.id }
    // 通过 API 新建的会话在服务端没有 model 字段，从最后一条 assistant 消息里补
    val effectiveModel = session.model ?: model.messages.lastOrNull { it.model != null }?.model

    BackHandler {
        model.closeSession()
        model.refreshSessions()
    }

    LaunchedEffect(model.messages.size, model.liveText.length, model.liveReasoning.length) {
        val total = model.messages.size + 1
        if (total > 0) {
            runCatching { listState.scrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(0)) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        model.closeSession()
                        model.refreshSessions()
                    }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Column {
                        Text(session.shortTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(
                                effectiveModel?.label?.takeIf { it.isNotBlank() } ?: "默认模型",
                                session.agent.takeIf { it.isNotBlank() },
                                if (model.sending) model.statusLine.ifBlank { "运行中…" } else null,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (model.sending) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                actions = {
                    if (model.sending) {
                        IconButton(onClick = { model.interrupt() }) {
                            Icon(Icons.Rounded.Stop, contentDescription = "中断", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("切换模型") },
                                onClick = {
                                    menu = false
                                    model.loadMeta()
                                    pickModel = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("切换 Agent") },
                                onClick = {
                                    menu = false
                                    model.loadMeta()
                                    pickAgent = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("刷新消息") },
                                onClick = {
                                    menu = false
                                    model.loadMessages()
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            ChatComposer(
                model = model,
                busy = model.sending,
                value = input,
                onChange = { input = it },
                onShowQueue = { showQueue = true },
                onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        input = ""
                        model.sendPrompt(text)
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ErrorBanner(model)

            if (pendingPermissions.isNotEmpty()) {
                // 权限请求用弹窗，避免被长对话挤到屏幕外
                val req = pendingPermissions.first()
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("需要你的确认") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(req.action, fontWeight = FontWeight.SemiBold)
                            if (req.resources.isNotEmpty()) {
                                Text(
                                    req.resources.joinToString("\n").take(1200),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (req.message.isNotBlank()) {
                                Text(req.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { model.replyPermission(req, "once") }) { Text("允许一次") }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = { model.replyPermission(req, "reject") }) {
                                Text("拒绝", color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(onClick = { model.replyPermission(req, "always") }) { Text("总是允许") }
                        }
                    },
                )
            }

            if (model.messagesLoading && model.messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(model.messages, key = { it.id }) { msg ->
                        MessageItem(msg)
                    }
                    if (model.liveReasoning.isNotBlank() || model.liveText.isNotBlank() || model.sending) {
                        item(key = "live") {
                            LiveBubble(model)
                        }
                    }
                }
            }
        }
    }

    if (pickModel) {
        PickerSheet(
            title = "选择模型",
            rows = model.models.mapNotNull { m ->
                val id = m.str("id") ?: return@mapNotNull null
                val provider = m.str("providerID") ?: ""
                val name = m.str("name") ?: id
                PickerRow(name, "$provider · $id") {
                    model.setModel(com.a2heng.opencode.mobile.data.ModelRef(id, provider))
                    pickModel = false
                }
            },
            onDismiss = { pickModel = false },
        )
    }

    if (pickAgent) {
        PickerSheet(
            title = "选择 Agent",
            rows = model.agents.mapNotNull { a ->
                val id = a.str("id") ?: return@mapNotNull null
                PickerRow(a.str("name") ?: id, a.str("description") ?: id) {
                    model.setAgent(id)
                    pickAgent = false
                }
            },
            onDismiss = { pickAgent = false },
        )
    }

    pendingForms.firstOrNull()?.let { form ->
        FormDialog(
            form = form,
            onDismiss = { model.dismissForm(form.id) },
            onSubmit = { answer -> model.replyForm(form, answer) },
        )
    }

    if (showQueue) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            title = { Text("待发送的消息") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (model.queue.isEmpty()) {
                        Text("队列是空的", style = MaterialTheme.typography.bodySmall)
                    }
                    model.queue.forEach { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                TextButton(onClick = { model.cancelQueued(item.id) }) {
                                    Text("取消", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQueue = false }) { Text("关闭") } },
        )
    }
}

@Composable
private fun ChatComposer(
    model: AppModel,
    busy: Boolean,
    value: String,
    onChange: (String) -> Unit,
    onShowQueue: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            if (model.queue.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickableNoRipple(onShowQueue)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "待发送 ${model.queue.size} 条 · 空闲后自动发送",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    placeholder = {
                        Text(if (busy) "运行中 · 发送会加入队列" else "发消息给 opencode…")
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 5,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                )
                if (busy) {
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { model.interrupt() },
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    ) {
                        Icon(
                            Icons.Rounded.Stop,
                            contentDescription = "中断",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = onSend,
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                ) {
                    Icon(
                        Icons.Rounded.ArrowUpward,
                        contentDescription = "发送",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageItem(msg: Msg) {
    if (msg.isUser) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                modifier = Modifier.widthIn(max = 330.dp),
            ) {
                Text(
                    msg.text,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth()) {
        msg.parts.forEach { part ->
            when (part) {
                is Part.Reasoning -> ReasoningBlock(part.text)
                is Part.Tool -> ToolBlock(part)
                is Part.Text -> AssistantText(part.text)
            }
        }
        if (msg.parts.isEmpty() && msg.text.isNotBlank()) AssistantText(msg.text)
        msg.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Footer(msg)
    }
}

@Composable
private fun AssistantText(text: String) {
    if (text.isBlank()) return
    MarkdownText(text, Modifier.fillMaxWidth().padding(vertical = 2.dp))
}

@Composable
private fun ReasoningBlock(text: String) {
    var expanded by remember { mutableStateOf(false) }
    if (text.isBlank()) return
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.clickableNoRipple { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                if (expanded) "思考过程" else "思考过程（${text.length} 字）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, top = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun ToolBlock(part: Part.Tool) {
    var expanded by remember { mutableStateOf(false) }
    val statusColor = when (part.status) {
        "completed", "success" -> Accent
        "error", "failed" -> MaterialTheme.colorScheme.error
        "running" -> Sky
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Column(
            Modifier
                .clickableNoRipple { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Build,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = statusColor,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    part.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    part.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
                Spacer(Modifier.weight(1f))
                if (part.input.isNotBlank() || part.output.isNotBlank() || part.error.isNotBlank()) {
                    Icon(
                        if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (part.input.isNotBlank()) {
                Text(
                    part.input.lineSequence().firstOrNull()?.take(160) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) 6 else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (part.output.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (expanded) part.output.takeLast(8000) else part.output.take(600),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 8,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (expanded && part.error.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    part.error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun Footer(msg: Msg) {
    val tokens = msg.tokens ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) {
        Text(
            "命中 ${rateLabel(tokens.hitRate)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "in ${formatTokens(tokens.input)} · cache ${formatTokens(tokens.cacheRead)} · out ${formatTokens(tokens.outputAll)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (msg.cost > 0) {
            Text(
                formatCost(msg.cost),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveBubble(model: AppModel) {
    Column(Modifier.fillMaxWidth()) {
        if (model.liveReasoning.isNotBlank()) {
            ReasoningBlock(model.liveReasoning)
        }
        if (model.liveText.isNotBlank()) {
            AssistantText(model.liveText)
        } else if (model.sending) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    model.statusLine.ifBlank { "正在生成…" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

data class PickerRow(val title: String, val subtitle: String, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(title: String, rows: List<PickerRow>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
            LazyColumn(Modifier.fillMaxWidth().height(420.dp)) {
                items(rows.size) { index ->
                    val row = rows[index]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickableNoRipple(row.onClick)
                            .padding(vertical = 11.dp),
                    ) {
                        Column {
                            Text(row.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                row.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// 表单（opencode 的 question / 结构化输入）
// ---------------------------------------------------------------------------

@Composable
private fun FormDialog(form: FormInfo, onDismiss: () -> Unit, onSubmit: (JsonObject) -> Unit) {
    val values = remember(form.id) { mutableStateMapOf<String, String>() }
    val bools = remember(form.id) { mutableStateMapOf<String, Boolean>() }
    val multis = remember(form.id) { mutableStateMapOf<String, MutableList<String>>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(form.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                form.fields.forEach { field ->
                    FieldEditor(field, values, bools, multis)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val answer = buildJsonObject {
                    form.fields.forEach { f ->
                        when (f.type) {
                            "boolean" -> put(f.key, JsonPrimitive(bools[f.key] ?: f.defaultBool ?: false))
                            "multiselect" -> put(
                                f.key,
                                buildJsonArray {
                                    (multis[f.key] ?: mutableListOf()).forEach { add(JsonPrimitive(it)) }
                                },
                            )
                            "integer" -> values[f.key]?.toLongOrNull()?.let { put(f.key, JsonPrimitive(it)) }
                            "number" -> values[f.key]?.toDoubleOrNull()?.let { put(f.key, JsonPrimitive(it)) }
                            else -> values[f.key]?.let { put(f.key, JsonPrimitive(it)) }
                        }
                    }
                }
                onSubmit(answer)
            }) { Text("提交") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun FieldEditor(
    field: FormField,
    values: MutableMap<String, String>,
    bools: MutableMap<String, Boolean>,
    multis: MutableMap<String, MutableList<String>>,
) {
    Column {
        Text(
            field.title + if (field.required) " *" else "",
            style = MaterialTheme.typography.labelLarge,
        )
        if (field.description.isNotBlank()) {
            Text(
                field.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (field.type) {
            "boolean" -> Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    bools[field.key] = !(bools[field.key] ?: field.defaultBool ?: false)
                }) {
                    Text(if (bools[field.key] ?: field.defaultBool ?: false) "是" else "否")
                }
            }
            "string" -> {
                OutlinedTextField(
                    value = values[field.key] ?: field.defaultText ?: "",
                    onValueChange = { values[field.key] = it },
                    placeholder = { if (field.placeholder.isNotBlank()) Text(field.placeholder) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                )
                if (field.options.isNotEmpty()) {
                    OptionChips(field, multis, values)
                }
            }
            "multiselect" -> OptionChips(field, multis, values)
            else -> OutlinedTextField(
                value = values[field.key] ?: field.defaultText ?: "",
                onValueChange = { values[field.key] = it },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun OptionChips(field: FormField, multis: MutableMap<String, MutableList<String>>, values: MutableMap<String, String>) {
    val selected = multis.getOrPut(field.key) { mutableListOf() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
        field.options.forEach { option ->
            val isSelected = option.value in selected
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableNoRipple {
                        if (isSelected) selected.remove(option.value)
                        else if (field.type == "multiselect") selected.add(option.value)
                        else {
                            selected.clear()
                            selected.add(option.value)
                            values[field.key] = option.value
                        }
                    },
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Text(option.label, style = MaterialTheme.typography.bodySmall)
                    if (option.description.isNotBlank()) {
                        Text(
                            option.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
