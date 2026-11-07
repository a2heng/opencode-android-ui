package com.a2heng.opencode.mobile.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.UTurnLeft
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.a2heng.opencode.mobile.data.Attachment
import com.a2heng.opencode.mobile.data.CommandInfo
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(model: AppModel, session: SessionInfo) {
    var input by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var pickModel by remember { mutableStateOf(false) }
    var pickAgent by remember { mutableStateOf(false) }
    var revertTarget by remember { mutableStateOf<Msg?>(null) }
    var detailTool by remember { mutableStateOf<Part.Tool?>(null) }
    var showCommands by remember { mutableStateOf(false) }
    var showFilePicker by remember { mutableStateOf(false) }
    var openMenu by remember { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val pendingTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pending-clock")
    val pendingPhase by pendingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 2400,
                easing = androidx.compose.animation.core.LinearEasing,
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "pending-phase",
    )
    val context = LocalContext.current
    val attachments = remember { mutableStateListOf<Attachment>() }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val att = withContext(Dispatchers.IO) { loadImageAttachment(context, uri) }
                if (att != null) attachments.add(att)
            }
        }
    }

    // 其它 App 分享进来的文本 / 图片
    LaunchedEffect(model.pendingDraft, model.pendingAttachment) {
        if (model.pendingDraft != null || model.pendingAttachment != null) {
            val (text, image) = model.consumeDraft()
            if (!text.isNullOrBlank()) input = text
            if (image != null) attachments.add(image)
        }
    }

    LaunchedEffect(session.id) { model.loadMeta(session.directory) }

    LaunchedEffect(session.id) {
        while (true) {
            model.refreshShells()
            kotlinx.coroutines.delay(3000)
        }
    }

    // 发出 pending / 新消息后回到最底部（reverseLayout 的 0 就是底部）
    LaunchedEffect(model.queue.size) {
        if (model.queue.isNotEmpty()) listState.animateScrollToItem(0)
    }

    val pendingPermissions = model.permissions.filter { it.sessionID == session.id }
    val pendingForms = model.forms.filter { it.sessionID == session.id }
    // 通过 API 新建的会话在服务端没有 model 字段，从最后一条 assistant 消息里补
    val effectiveModel = session.model ?: model.messages.lastOrNull { it.model != null }?.model
    val lastTokens = model.messages.lastOrNull { it.isAssistant && it.tokens != null }?.tokens
    val contextLimit = effectiveModel?.let { model.contextLimits[it.id] } ?: 0L
    val contextUsed = (lastTokens?.input ?: 0L) + (lastTokens?.cacheRead ?: 0L) + (lastTokens?.cacheWrite ?: 0L)
    val contextPct = if (contextLimit > 0 && contextUsed > 0) {
        (contextUsed * 100.0 / contextLimit).coerceIn(0.0, 100.0)
    } else {
        null
    }
    val contextColor = when {
        contextPct == null -> MaterialTheme.colorScheme.onSurfaceVariant
        contextPct >= 75 -> MaterialTheme.colorScheme.error
        contextPct >= 50 -> Warn
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val backOwner = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current
    if (backOwner != null) {
        PredictiveBackHandler { events ->
            try {
                events.collect { event -> backProgress = event.progress }
                model.closeSession()
                model.refreshSessions()
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                backProgress = 0f
                throw cancel
            }
        }
    } else {
        BackHandler {
            model.closeSession()
            model.refreshSessions()
        }
    }

    Scaffold(
        modifier = Modifier.graphicsLayer {
            val scale = 1f - 0.07f * backProgress
            scaleX = scale
            scaleY = scale
            // 往右滑走，左下露出下面的会话列表
            translationX = size.width * 0.32f * backProgress
            alpha = 1f - 0.08f * backProgress
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (contextPct != null) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "ctx ${contextPct.roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contextColor,
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (model.sending) {
                        IconButton(onClick = { model.interrupt() }) {
                            Icon(Icons.Rounded.Stop, contentDescription = "中断", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Box {
                        IconButton(onClick = { openMenu = true }) {
                            Icon(
                                Icons.Rounded.UnfoldMore,
                                contentDescription = "打开的会话",
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        DropdownMenu(expanded = openMenu, onDismissRequest = { openMenu = false }) {
                            if (model.openSessions.isEmpty()) {
                                DropdownMenuItem(text = { Text("还没有打开的会话") }, onClick = { openMenu = false })
                            }
                            model.openSessions.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            item.shortTitle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    trailingIcon = {
                                        if (item.id == session.id) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = "当前",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        } else if (model.isActive(item.id)) {
                                            Box(
                                                Modifier
                                                    .size(7.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                            )
                                        }
                                    },
                                    onClick = {
                                        openMenu = false
                                        if (item.id != session.id) model.openSession(item)
                                    },
                                )
                            }
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
                attachments = attachments,
                onPickImage = { imagePicker.launch("image/*") },
                onPickFile = { showFilePicker = true },
                onPickCommand = { showCommands = true; model.loadMeta(session.directory) },
                onRemoveAttachment = { attachments.removeAt(it) },
                onClose = {
                    model.closeSession()
                    model.refreshSessions()
                },
                onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty() || attachments.isNotEmpty()) {
                        input = ""
                        val cmd = model.commands.firstOrNull {
                            text == "/" + it.name || text.startsWith("/" + it.name + " ")
                        }
                        if (cmd != null) {
                            when (cmd.name) {
                                "compact" -> model.compactSession()
                                "interrupt" -> model.interrupt()
                                "model" -> pickModel = true
                                "agent" -> pickAgent = true
                                "reload" -> model.reloadConfig()
                                else -> model.runCommand(
                                    cmd.name,
                                    text.removePrefix("/" + cmd.name).trim(),
                                    attachments.toList(),
                                )
                            }
                        } else {
                            model.sendPrompt(text, attachments.toList())
                        }
                        attachments.clear()
                        scope.launch { listState.animateScrollToItem(0) }
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
                AppDialog(
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
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            if (model.detailLevel == "compact") 8.dp else 12.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            if (model.detailLevel == "compact") 4.dp else 10.dp,
                        ),
                    ) {
                        items(model.queue.asReversed(), key = { "pending:${it.id}" }) { item ->
                            PendingBubble(item.text, pendingPhase) {
                                model.withdrawQueued(item.id) { restored -> input = restored }
                            }
                        }
                        if (model.liveReasoning.isNotBlank() || model.liveText.isNotBlank() || model.sending) {
                            item(key = "live") {
                                LiveBubble(model, model.detailLevel)
                            }
                        }
                        items(model.messages.asReversed(), key = { it.id }) { msg ->
                            MessageItem(
                                msg = msg,
                                detail = model.detailLevel,
                                onLongPressUser = { revertTarget = it },
                                onOpenTool = { detailTool = it },
                            )
                        }
                    }
                    if (listState.firstVisibleItemIndex > 2) {
                        SmallScrollToBottom(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                        ) {
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                    }
                    if (model.shells.isNotEmpty()) {
                        FloatingShells(
                            model = model,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 70.dp),
                        )
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

    revertTarget?.let { msg ->
        AppDialog(
            onDismissRequest = { revertTarget = null },
            title = { Text("回退到这条消息？") },
            text = {
                Text(
                    "会用 opencode 的 revert 撤销这条消息及其之后的内容。" +
                        "「回退并还原文件」会把这一段的文件改动一起还原。",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    model.revertTo(msg.id, false)
                    revertTarget = null
                }) { Text("回退对话") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { revertTarget = null }) { Text("取消") }
                    TextButton(onClick = {
                        model.revertTo(msg.id, true)
                        revertTarget = null
                    }) {
                        Text("回退并还原文件", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
    }

    detailTool?.let { tool ->
        ToolDetailDialog(tool) { detailTool = null }
    }

    if (showFilePicker) {
        FileMentionDialog(
            model = model,
            startDir = session.directory.ifBlank { model.homeDir },
            onDismiss = { showFilePicker = false },
        ) { absolute ->
            val base = session.directory.ifBlank { model.homeDir }
            val rel = if (base.isNotBlank() && absolute.startsWith("$base/")) {
                absolute.removePrefix("$base/")
            } else {
                absolute
            }
            input = (input.trimEnd() + " @" + rel + " ").trimStart()
        }
    }

    if (showCommands) {
        ModalBottomSheet(onDismissRequest = { showCommands = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("命令", style = MaterialTheme.typography.titleMedium)
                val commandList = allCommands(model)
                if (commandList.isEmpty()) {
                    Text(
                        "没有可用命令",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(commandList, key = { it.name }) { cmd ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickableNoRipple {
                                    showCommands = false
                                    input = "/" + cmd.name + " " + input.trim()
                                }
                                .padding(vertical = 10.dp),
                        ) {
                            Column {
                                Text("/${cmd.name}", style = MaterialTheme.typography.bodyLarge)
                                if (cmd.description.isNotBlank()) {
                                    Text(
                                        cmd.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

}

@Composable
private fun ChatComposer(
    model: AppModel,
    busy: Boolean,
    value: String,
    onChange: (String) -> Unit,
    attachments: List<Attachment>,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onPickCommand: () -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
) {
    var actionsOpen by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (actionsOpen) 45f else 0f, label = "plus-rotate")
    val slashQuery = if (value.startsWith("/") && !value.contains(' ')) value.substring(1) else null
    val suggestions = if (slashQuery == null) {
        emptyList()
    } else {
        allCommands(model)
            .filter { it.name.contains(slashQuery, ignoreCase = true) }
            .take(6)
    }
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        ) {
            if (suggestions.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Column {
                        suggestions.forEach { cmd ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickableNoRipple { onChange("/" + cmd.name + " ") }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "/" + cmd.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    cmd.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            if (attachments.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    attachments.forEachIndexed { index, att ->
                        Box {
                            val bmp = remember(att.base64) {
                                decodeDataUri("data:${att.mime};base64,${att.base64}", 240)
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            }
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                    .clickableNoRipple { onRemoveAttachment(index) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "移除",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickableNoRipple { actionsOpen = !actionsOpen },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = if (actionsOpen) "收起" else "添加",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp).rotate(rotation),
                    )
                }
                AnimatedVisibility(
                    visible = actionsOpen,
                    enter = androidx.compose.animation.expandHorizontally(
                        animationSpec = tween(
                            durationMillis = 220,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        ),
                        expandFrom = Alignment.Start,
                    ) + androidx.compose.animation.fadeIn(
                        animationSpec = tween(durationMillis = 160),
                    ),
                    exit = androidx.compose.animation.shrinkHorizontally(
                        animationSpec = tween(
                            durationMillis = 160,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        ),
                        shrinkTowards = Alignment.Start,
                    ) + androidx.compose.animation.fadeOut(
                        animationSpec = tween(durationMillis = 120),
                    ),
                ) {
                    Row(
                        Modifier.padding(start = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ComposerAction(Icons.Rounded.AddPhotoAlternate, "图片", 26, MaterialTheme.colorScheme.primary, onPickImage)
                        ComposerAction(Icons.Rounded.NoteAdd, "文件", 24, MaterialTheme.colorScheme.primary, onPickFile)
                        ComposerAction(Icons.Rounded.Terminal, "命令", 23, MaterialTheme.colorScheme.primary, onPickCommand)
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    placeholder = {
                        if (!actionsOpen) {
                            Text(
                                when {
                                    !busy -> "发消息给 opencode…"
                                    model.sendMode == "steer" -> "运行中 · 引导当前回答"
                                    model.sendMode == "interrupt" -> "运行中 · 中断后发送"
                                    else -> "运行中 · 结束后再发"
                                },
                                maxLines = 1,
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    maxLines = 1,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    trailingIcon = {
                        if (value.isNotEmpty()) {
                            IconButton(onClick = { onChange("") }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "清空",
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )
                Spacer(Modifier.width(8.dp))
                val hasContent = value.isNotBlank() || attachments.isNotEmpty()
                FloatingActionButton(
                    onClick = { if (hasContent) onSend() else onClose() },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (hasContent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (hasContent) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    shape = CircleShape,
                ) {
                    Icon(
                        if (hasContent) Icons.Rounded.ArrowUpward else Icons.Rounded.Close,
                        contentDescription = if (hasContent) "发送" else "关闭",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerAction(
    icon: ImageVector,
    label: String,
    size: Int,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageItem(
    msg: Msg,
    detail: String,
    onLongPressUser: (Msg) -> Unit = {},
    onOpenTool: (Part.Tool) -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    if (msg.isUser) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                modifier = Modifier
                    .widthIn(max = 330.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPressUser(msg)
                        },
                    ),
            ) {
                Column(Modifier.padding(horizontal = if (detail == "compact") 10.dp else 13.dp, vertical = if (detail == "compact") 6.dp else 9.dp)) {
                    msg.images.forEach { DataUriImage(it) }
                    Text(
                        msg.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth()) {
        msg.parts.forEach { part ->
            when (part) {
                is Part.Reasoning -> ReasoningBlock(part.text, detail)
                is Part.Tool -> ToolBlock(part, detail, onOpenTool, msg.tokens?.hitRate)
                is Part.Text -> AssistantText(part.text)
            }
        }
        if (msg.parts.isEmpty() && msg.text.isNotBlank()) AssistantText(msg.text)
        msg.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (detail != "compact") Footer(msg)
    }
}

@Composable
private fun AssistantText(text: String) {
    if (text.isBlank()) return
    MarkdownText(text, Modifier.fillMaxWidth().padding(vertical = 2.dp))
}

@Composable
private fun ReasoningBlock(text: String, detail: String) {
    if (text.isBlank() || detail == "compact") return
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "思考过程 · ${text.length} 字",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, top = 4.dp)
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(8.dp),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
            )
        }
    }
}

@Composable
private fun ShellBlock(text: String, command: Boolean, maxLines: Int) {
    val accent = if (command) Sky else MaterialTheme.colorScheme.outline
    val bg = if (command) Sky.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .background(bg, RoundedCornerShape(6.dp)),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun ToolBlock(
    part: Part.Tool,
    detail: String,
    onOpen: (Part.Tool) -> Unit,
    rate: Double? = null,
) {
    val hasDiff = part.diff.isNotBlank()
    val hasOutput = part.output.isNotBlank()
    val hasImages = part.images.isNotEmpty()
    val hasError = part.error.isNotBlank()
    val statusColor = when (part.status) {
        "completed", "success" -> Accent
        "error", "failed" -> MaterialTheme.colorScheme.error
        "running" -> Sky
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    if (detail == "compact") {
        val summary = part.input.lineSequence().firstOrNull()?.take(140).orEmpty()
        Row(
            Modifier
                .fillMaxWidth()
                .clickableNoRipple { onOpen(part) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(toolIcon(part.name), contentDescription = null, modifier = Modifier.size(12.dp), tint = statusColor)
            Spacer(Modifier.width(5.dp))
            Text(
                part.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            val extra = when {
                hasImages -> "图片"
                hasDiff -> "diff"
                part.payload.isNotBlank() -> "内容"
                hasOutput -> "输出"
                hasError -> "错误"
                else -> ""
            }
            if (rate != null) {
                Spacer(Modifier.width(6.dp))
                val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
                Text(
                    rateLabel(rate),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = rateColor(rate, dark),
                )
            } else if (extra.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(extra, style = MaterialTheme.typography.labelSmall, color = statusColor.copy(alpha = 0.9f))
            }
        }
        return
    }

    val isShellTool = part.name.lowercase() in setOf("shell", "bash", "sh")
    val isEditTool = part.name in setOf("edit", "patch", "multiedit")
    val isWriteTool = part.name == "write"
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(toolIcon(part.name), contentDescription = null, modifier = Modifier.size(15.dp), tint = statusColor)
                Spacer(Modifier.width(6.dp))
                Text(part.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                Text(
                    when {
                        part.exitCode != null && part.exitCode != 0L -> "exit ${part.exitCode}"
                        part.status == "completed" -> "完成"
                        part.status == "running" -> "运行中"
                        part.status == "error" -> "失败"
                        else -> part.status
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
                Spacer(Modifier.weight(1f))
                if (part.truncated) {
                    Text("已截断", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (isShellTool) {
                if (part.input.isNotBlank()) {
                    ShellBlock(part.input.take(4000), command = true, maxLines = 6)
                }
                if (hasOutput) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ShellBlock(part.output.takeLast(12000), command = false, maxLines = Int.MAX_VALUE)
                    }
                }
            } else {
                if (part.input.isNotBlank()) {
                    Text(
                        part.input.lineSequence().firstOrNull()?.take(220) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                if (hasImages) {
                    part.images.forEach { DataUriImage(it) }
                }
                if (hasDiff) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        DiffView(part.diff)
                    }
                }
                if (isWriteTool && part.payload.isNotBlank()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .padding(top = 5.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                RoundedCornerShape(6.dp),
                            )
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                    ) {
                        Text(
                            part.payload.take(12000),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (hasOutput && !isEditTool && !isWriteTool) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            part.output.takeLast(12000),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }
            if (hasError) {
                Text(
                    part.error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun ToolDetailDialog(part: Part.Tool, onDismiss: () -> Unit) {
    val isShellTool = part.name.lowercase() in setOf("shell", "bash", "sh")
    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(toolIcon(part.name), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(part.name)
                Spacer(Modifier.width(8.dp))
                Text(part.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (part.input.isNotBlank()) {
                    if (isShellTool) {
                        ShellBlock(part.input, command = true, maxLines = Int.MAX_VALUE)
                    } else {
                        Text(
                            part.input,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (part.images.isNotEmpty()) {
                    part.images.forEach { DataUriImage(it) }
                }
                if (part.diff.isNotBlank()) {
                    DiffView(part.diff)
                }
                if (part.output.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        part.output,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (part.error.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(part.error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun Footer(msg: Msg) {
    val tokens = msg.tokens ?: return
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val color = rateColor(tokens.hitRate, dark)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(color, RoundedCornerShape(4.dp)),
        )
        Text(
            "命中 ${rateLabel(tokens.hitRate)}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
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
private fun LiveBubble(model: AppModel, detail: String) {
    Column(Modifier.fillMaxWidth()) {
        if (model.liveReasoning.isNotBlank()) {
            ReasoningBlock(model.liveReasoning, detail)
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

@Composable
private fun PendingBubble(text: String, phase: Float, onWithdraw: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 28.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onWithdraw()
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                androidx.compose.ui.res.painterResource(com.a2heng.opencode.mobile.R.drawable.ic_return_horizontal),
                contentDescription = "撤回编辑",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            ),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    )
                    AnimatedEllipsis(phase)
                }
            }
        }
    }
}

@Composable
private fun AnimatedEllipsis(phase: Float) {
    val step = phase.toInt().coerceIn(0, 3)
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            Text(
                ".",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (index < step) 0.9f else 0.08f,
                ),
            )
        }
    }
}

@Composable
private fun SmallScrollToBottom(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
    ) {
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "回到底部", modifier = Modifier.size(22.dp))
    }
}

/** 选一张图 -> 最长边压到 1600px -> JPEG 85 -> inline base64（opencode 的 files[].uri）。 */
fun loadImageAttachment(context: Context, uri: Uri): Attachment? = runCatching {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: return@runCatching null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sample = 1
    while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
    val bitmap = BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    ) ?: return@runCatching null
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    Attachment(
        name = "image_${System.currentTimeMillis()}.jpg",
        mime = "image/jpeg",
        base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP),
    )
}.getOrNull()

data class PickerRow(val title: String, val subtitle: String, val onClick: () -> Unit)

/** opencode 原生命令 + 本 App 的客户端命令（去重）。 */
private fun builtinClientCommands(): List<CommandInfo> = listOf(
    CommandInfo("compact", "压缩当前会话的上下文"),
    CommandInfo("interrupt", "中断当前回答"),
    CommandInfo("model", "切换模型"),
    CommandInfo("agent", "切换 Agent"),
    CommandInfo("reload", "重新加载服务端配置"),
)

private fun allCommands(model: AppModel): List<CommandInfo> =
    model.commands + builtinClientCommands().filter { extra ->
        model.commands.none { it.name == extra.name }
    }

@Composable
private fun SendModeItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "当前",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(title: String, rows: List<PickerRow>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
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

    AppDialog(
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
