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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.a2heng.opencode.mobile.data.AppModel
import com.a2heng.opencode.mobile.data.Attachment
import com.a2heng.opencode.mobile.data.CommandInfo
import com.a2heng.opencode.mobile.data.FormField
import com.a2heng.opencode.mobile.data.FormInfo
import com.a2heng.opencode.mobile.data.Msg
import com.a2heng.opencode.mobile.data.Part
import com.a2heng.opencode.mobile.data.PermissionReq
import com.a2heng.opencode.mobile.data.Queued
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
    var confirmStop by remember { mutableStateOf(false) }
    var morph by remember { mutableStateOf<PendingMorph?>(null) }
    var prevQueue by remember { mutableStateOf<List<Queued>>(emptyList()) }
    var queueSession by remember { mutableStateOf(session.id) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enterProgress by animateFloatAsState(
        targetValue = if (entered) 0f else 1f,
        animationSpec = tween(durationMillis = 260, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "chat-enter",
    )
    val motion = maxOf(backProgress, enterProgress)
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

    // 滚动到最上面（reverseLayout 的末尾）时，继续往更早翻一页
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 4) model.loadOlderMessages()
        }
    }

    // 队列项消失有两种：被服务端取走（变成真正的消息）或用户撤回。前者做 morph 动画。
    LaunchedEffect(model.queue, session.id) {
        // 切会话时不要把上一个会话的队列当成「刚消失」
        if (queueSession != session.id) {
            queueSession = session.id
            prevQueue = model.queue
            morph = null
            return@LaunchedEffect
        }
        val now = model.queue
        val removed = prevQueue.filterNot { old -> now.any { it.id == old.id } }
        prevQueue = now
        removed.lastOrNull()?.let { item ->
            if (!model.consumeWithdrawn(item.id)) morph = PendingMorph(item.text)
        }
    }
    val morphMsg = morph?.let { m ->
        model.messages.lastOrNull { it.isUser && it.text == m.text }
    }
    var morphForced by remember(morph) { mutableStateOf(false) }
    LaunchedEffect(morph) {
        if (morph != null) {
            kotlinx.coroutines.delay(1200)
            morphForced = true
        }
    }
    val visibleMessages = remember(model.messages, morphMsg?.id) {
        if (morphMsg == null) model.messages else model.messages.filterNot { it.id == morphMsg.id }
    }

    val pendingPermissions = model.permissions.filter { it.sessionID == session.id }
    val pendingForms = model.forms.filter { it.sessionID == session.id }
    // 通过 API 新建的会话在服务端没有 model 字段，从最后一条 assistant 消息里补
    val effectiveModel = session.model ?: model.messages.lastOrNull { it.model != null }?.model
    val lastTokens = model.messages.lastOrNull { it.isAssistant && it.tokens != null }?.tokens
    // 命中率用整个会话的累计口径（服务端 session.tokens 就是全会话汇总），
    // 上下文占用仍然只看最近一步（context 是单次请求的窗口占用）。
    val aggregateTokens = session.tokens
        ?: model.messages.mapNotNull { it.tokens }.reduceOrNull { a, b -> a.plus(b) }
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
        modifier = Modifier
            .clip(RoundedCornerShape((34f * motion).dp))
            .graphicsLayer {
                val scale = 1f - 0.10f * motion
                scaleX = scale
                scaleY = scale
                // 往右滑走，左下露出下面的上一页；不做淡化
                translationX = size.width * 0.85f * motion
            },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(52.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            model.closeSession()
                            model.refreshSessions()
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // 左侧：标题一行 + 模型一行
                    Column(Modifier.weight(1f)) {
                        Text(
                            session.shortTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            effectiveModel?.let { modelDisplay(it, model.modelGroupById) }
                                ?.takeIf { it.isNotBlank() } ?: "选择模型",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickableNoRipple {
                                model.loadMeta(session.directory)
                                pickModel = true
                            },
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    // 右侧三个方块：模式 / 命中 / 上下文
                    InfoSquare("模式", session.agent.ifBlank { "默认" }) {
                        model.loadMeta(session.directory)
                        pickAgent = true
                    }
                    Spacer(Modifier.width(4.dp))
                    InfoSquare(
                        "命中",
                        aggregateTokens?.let { rateLabel(it.hitRate) } ?: "—",
                        valueColor = aggregateTokens?.let {
                            rateColor(it.hitRate, MaterialTheme.colorScheme.background.luminanceIsDark())
                        } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    ) { model.openCache(session) }
                    Spacer(Modifier.width(4.dp))
                    InfoSquare(
                        "上下文",
                        contextPct?.let { "${it.roundToInt()}%" } ?: "—",
                        valueColor = contextColor,
                    ) { model.openCache(session) }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalIconButton(
                        onClick = { openMenu = true },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Rounded.UnfoldMore,
                            contentDescription = "打开的会话",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
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
                onStop = { confirmStop = true },
                onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty() || attachments.isNotEmpty()) {
                        input = ""
                        // 用「服务端命令 + 客户端内建命令」合并后的列表判断：
                        // compact 这类命令不在 /api/command 里，只看 model.commands 会被当成普通文本发出去。
                        val cmd = allCommands(model).firstOrNull {
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
        Box(Modifier.padding(padding).fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
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
                        // 排队项被服务端取走时，先在原位 morph 成用户消息，再让真正的消息接管
                        morph?.let { m ->
                            item(key = "morph") {
                                MorphingUserBubble(
                                    text = m.text,
                                    started = morphMsg != null || morphForced,
                                    detail = model.detailLevel,
                                    phase = pendingPhase,
                                ) { morph = null }
                            }
                        }
                        items(visibleMessages.asReversed(), key = { it.id }) { msg ->
                            MessageItem(
                                msg = msg,
                                detail = model.detailLevel,
                                onLongPressUser = { revertTarget = it },
                                onOpenTool = { detailTool = it },
                            )
                        }
                        if (model.loadingOlderMessages) {
                            item(key = "older-loading") {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
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
                    if (model.shells.any { it.sessionID == session.id }) {
                        FloatingShells(
                            model = model,
                            sessionID = session.id,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 70.dp),
                        )
                    }
                }
            }
        }

        // 打开的会话：卷帘卡片，占满可视宽度，向下展开；点外面收起
        if (openMenu) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickableNoRipple { openMenu = false },
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = openMenu,
                modifier = Modifier.align(Alignment.TopStart),
                enter = androidx.compose.animation.expandVertically(
                    animationSpec = tween(220),
                    expandFrom = Alignment.Top,
                ) + androidx.compose.animation.fadeIn(tween(140)),
                exit = androidx.compose.animation.shrinkVertically(
                    animationSpec = tween(180),
                    shrinkTowards = Alignment.Top,
                ) + androidx.compose.animation.fadeOut(tween(120)),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(0.dp, 0.dp, 18.dp, 18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "打开的会话",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${model.openSessions.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            IconButton(
                                onClick = {
                                    openMenu = false
                                    model.closeAllOpenSessions()
                                },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "退出多会话",
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(bottom = 4.dp))
                        if (model.openSessions.isEmpty()) {
                            Text(
                                "还没有打开的会话",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(model.openSessions, key = { it.id }) { item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickableNoRipple {
                                            openMenu = false
                                            if (item.id != session.id) model.openSession(item)
                                        }
                                        .padding(start = 8.dp, top = 7.dp, end = 2.dp, bottom = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier
                                            .size(7.dp)
                                            .background(
                                                if (item.id == session.id) MaterialTheme.colorScheme.primary
                                                else if (model.isActive(item.id)) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                CircleShape,
                                            ),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            item.shortTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (item.id == session.id) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            item.directory.substringAfterLast('/').ifBlank { item.directory },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (item.id == session.id) {
                                        Text(
                                            "当前",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    IconButton(
                                        onClick = { model.closeOpenSession(item.id) },
                                        modifier = Modifier.size(30.dp),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "关闭这个会话",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (pickModel) {
        ModelPickerSheet(
            model = model,
            title = "选择模型",
            selected = effectiveModel,
            onPick = { ref -> if (ref != null) model.setModel(ref) },
            onDismiss = { pickModel = false },
        )
    }

    if (pickAgent) {
        PickerSheet(
            title = "选择 Agent",
            rows = model.agents.mapNotNull { a ->
                val id = a.str("id") ?: return@mapNotNull null
                PickerRow(a.str("name") ?: id, a.str("description") ?: id, onClick = {
                    model.setAgent(id)
                    pickAgent = false
                })
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

    if (confirmStop) {
        AppDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("停止当前任务？") },
            text = {
                Text(
                    "会中断这个会话正在进行的回答（等同于把运行中的任务停下来）。",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    model.interrupt()
                }) { Text("停止", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) { Text("继续") }
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
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f)
                    .padding(horizontal = 16.dp),
            ) {
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
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
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
    onStop: () -> Unit,
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
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickableNoRipple { actionsOpen = !actionsOpen },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = if (actionsOpen) "收起" else "添加",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp).rotate(rotation),
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
                Spacer(Modifier.width(6.dp))
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
                    singleLine = false,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
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
                Spacer(Modifier.width(6.dp))
                val hasContent = value.isNotBlank() || attachments.isNotEmpty()
                val stopMode = !hasContent && busy
                if (hasContent || stopMode) {
                    FloatingActionButton(
                        onClick = { if (hasContent) onSend() else onStop() },
                        modifier = Modifier.size(42.dp),
                        containerColor = if (hasContent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        contentColor = if (hasContent) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onError
                        },
                        shape = CircleShape,
                    ) {
                        Icon(
                            if (hasContent) Icons.Rounded.ArrowUpward else Icons.Rounded.Stop,
                            contentDescription = if (hasContent) "发送" else "停止",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSquare(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(38.dp)
            .clickableNoRipple(onClick),
    ) {
        Column(
            Modifier.fillMaxSize().padding(top = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                label,
                fontSize = 8.5.sp,
                lineHeight = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                value,
                fontSize = 10.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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

    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize(tween(durationMillis = 180)),
    ) {
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

/**
 * 内嵌滚动区：保持正常的滚动链（不在边界处抢手势），
 * 滑到底时在底部弹出一条柔和的弧线阴影，提示「已经到底了」。
 */
@Composable
private fun GuardedScroll(
    state: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Box(Modifier.fillMaxWidth().verticalScroll(state)) {
            content()
        }
        val atEnd = state.maxValue > 0 && state.value >= state.maxValue - 1
        var edgeHint by remember { mutableStateOf(false) }
        LaunchedEffect(atEnd) {
            if (atEnd) {
                edgeHint = true
                kotlinx.coroutines.delay(500)
                edgeHint = false
            }
        }
        val hint by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (edgeHint) 1f else 0f,
            animationSpec = tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            label = "scroll-end-hint",
        )
        if (hint > 0.01f) {
            // 类似旧版 Android 的到头提示：边缘一条固定的半透明渐隐，出现一会儿自动消失
            val shadow = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f)
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(10.dp)
                    .alpha(hint)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, shadow),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun ReasoningBlock(text: String, detail: String, live: Boolean = false) {
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
        val state = rememberScrollState()
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, top = 4.dp)
                .then(
                    // 流式思考时高度封顶，只随内容平滑增长，避免整块突然变高把上面顶飞
                    if (live) Modifier
                        .heightIn(max = 150.dp)
                        .animateContentSize(tween(durationMillis = 180))
                    else Modifier.heightIn(max = 220.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(8.dp),
        ) {
            GuardedScroll(state, Modifier.fillMaxWidth()) {
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
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
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
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(48.dp),
                )
            } else if (extra.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    extra,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor.copy(alpha = 0.9f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(48.dp),
                )
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
                    toolStatusLabel(part),
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
                    GuardedScroll(
                        rememberScrollState(),
                        Modifier.fillMaxWidth().heightIn(max = 300.dp),
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
                    GuardedScroll(
                        rememberScrollState(),
                        Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    ) {
                        DiffView(part.diff)
                    }
                }
                if (isWriteTool && part.payload.isNotBlank()) {
                    GuardedScroll(
                        rememberScrollState(),
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .padding(top = 5.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                RoundedCornerShape(6.dp),
                            ),
                    ) {
                        Text(
                            part.payload.take(12000),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                if (hasOutput && !isEditTool && !isWriteTool) {
                    GuardedScroll(
                        rememberScrollState(),
                        Modifier.fillMaxWidth().heightIn(max = 300.dp),
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
    val isWriteTool = part.name == "write"
    val isEditTool = part.name in setOf("edit", "patch", "multiedit")
    val statusColor = when (part.status) {
        "completed", "success" -> Accent
        "error", "failed" -> MaterialTheme.colorScheme.error
        "running" -> Sky
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    var shown by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    LaunchedEffect(closing) {
        if (closing) {
            kotlinx.coroutines.delay(160)
            onDismiss()
        }
    }
    Dialog(
        onDismissRequest = { closing = true },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = shown && !closing,
            enter = androidx.compose.animation.fadeIn(tween(150)) +
                androidx.compose.animation.scaleIn(
                    initialScale = 0.90f,
                    animationSpec = tween(190, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                ),
            exit = androidx.compose.animation.fadeOut(tween(130)) +
                androidx.compose.animation.scaleOut(
                    targetScale = 0.94f,
                    animationSpec = tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                ),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 28.dp)
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            toolIcon(part.name),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = statusColor,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            part.name,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            toolStatusLabel(part),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Column(
                        Modifier
                            .heightIn(max = 500.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (isWriteTool) {
                            // write：上面是文件路径，下面直接显示写入的内容（不是状态/输出）
                            if (part.input.isNotBlank()) {
                                Text(
                                    part.input,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            if (part.payload.isNotBlank()) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .padding(10.dp),
                                ) {
                                    Text(
                                        part.payload.take(20000),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            } else {
                                Text(
                                    "（没有写入内容）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
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
                            if (part.output.isNotBlank() && (isShellTool || !isEditTool)) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    part.output,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (part.error.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                part.error,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { closing = true }) { Text("关闭") }
                    }
                }
            }
        }
    }
}

private fun toolStatusLabel(part: Part.Tool): String = when {
    part.exitCode != null && part.exitCode != 0L -> "exit ${part.exitCode}"
    part.status == "completed" || part.status == "success" -> "完成"
    part.status == "running" -> "运行中"
    part.status == "error" || part.status == "failed" -> "失败"
    else -> part.status.ifBlank { "—" }
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
    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize(tween(durationMillis = 180)),
    ) {
        if (model.liveReasoning.isNotBlank()) {
            ReasoningBlock(model.liveReasoning, detail, live = true)
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

private data class PendingMorph(val text: String)

/**
 * 排队消息被取走时的过渡：保持同一位置，只把底部 pending 行高度收掉，
 * 同时颜色从 pending 灰底渐变到用户气泡色，最后交给真正的消息。
 */
@Composable
private fun MorphingUserBubble(
    text: String,
    started: Boolean,
    detail: String,
    phase: Float,
    onDone: () -> Unit,
) {
    var begun by remember { mutableStateOf(false) }
    LaunchedEffect(started) {
        if (started) {
            // 先让上一条助手消息的收尾动画/重排落定，再做 pending → 消息的 morph
            kotlinx.coroutines.delay(220)
            begun = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (begun) 1f else 0f,
        animationSpec = tween(
            durationMillis = 380,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "pending-morph",
    )
    LaunchedEffect(begun) {
        if (begun) {
            kotlinx.coroutines.delay(430)
            onDone()
        }
    }

    val pendingBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val userBg = MaterialTheme.colorScheme.primaryContainer
    val bg = androidx.compose.ui.graphics.lerp(pendingBg, userBg, progress)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f * (1f - progress))
    val textColor = androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        MaterialTheme.colorScheme.onPrimaryContainer,
        progress,
    )
    val hPad = androidx.compose.ui.unit.lerp(12.dp, if (detail == "compact") 10.dp else 13.dp, progress)
    val vPad = androidx.compose.ui.unit.lerp(7.dp, if (detail == "compact") 6.dp else 9.dp, progress)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 28.dp * (1f - progress)),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 撤回按钮也一起收起，避免横向位置跳变
        Box(
            Modifier
                .size(40.dp * (1f - progress))
                .alpha(1f - progress),
            contentAlignment = Alignment.Center,
        ) {
            if (progress < 0.99f) {
                Icon(
                    androidx.compose.ui.res.painterResource(com.a2heng.opencode.mobile.R.drawable.ic_return_horizontal),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Surface(
            color = bg,
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            modifier = Modifier.widthIn(
                max = androidx.compose.ui.unit.lerp(300.dp, 330.dp, progress),
            ),
        ) {
            Column(Modifier.padding(horizontal = hPad, vertical = vPad)) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
                // 只收掉这一行：高度 + 透明度一起走
                Box(Modifier.fillMaxWidth().height(18.dp * (1f - progress))) {
                    if (progress < 0.99f) {
                        Row(
                            Modifier.alpha(1f - progress),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
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

data class PickerRow(
    val title: String,
    val subtitle: String,
    val isHeader: Boolean = false,
    val onClick: () -> Unit = {},
)

private fun buildModelRows(
    models: List<JsonObject>,
    onPick: (com.a2heng.opencode.mobile.data.ModelRef) -> Unit,
): List<PickerRow> {
    val rows = mutableListOf<PickerRow>()
    var lastGroup = -1
    modelEntries(models).forEach { entry ->
        if (entry.group != lastGroup) {
            rows += PickerRow(modelGroupTitle(entry.group), "", isHeader = true)
            lastGroup = entry.group
        }
        rows += PickerRow(entry.name, "${entry.providerID} · ${entry.ref.id}", onClick = { onPick(entry.ref) })
    }
    return rows
}

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
                    if (row.isHeader) {
                        Text(
                            row.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    } else {
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
