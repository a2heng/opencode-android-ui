package com.a2heng.opencode.mobile.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class AppModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("opencode-mobile", Application.MODE_PRIVATE)

    // ---- 连接 ---------------------------------------------------------------
    var serverUrl by mutableStateOf(prefs.getString("url", "") ?: "")
        private set
    var password by mutableStateOf(prefs.getString("password", "") ?: "")
        private set
    var connected by mutableStateOf(false)
        private set
    var connecting by mutableStateOf(false)
        private set
    var connectionError by mutableStateOf<String?>(null)
        private set
    var serverVersion by mutableStateOf("")
        private set
    var homeDir by mutableStateOf("")
        private set

    /** 运行中发送方式：pending（排队，默认）/ steer（立即插入）/ interrupt（中断后发送） */
    var sendMode by mutableStateOf(prefs.getString("send_mode", "pending") ?: "pending")
        private set

    /** 聊天显示：compact（精简，工具一行 + 点击气泡看详情）/ full（完整，全部展开） */
    var detailLevel by mutableStateOf(
        (prefs.getString("detail_level", "full") ?: "full").let {
            if (it == "normal" || it == "verbose") "full" else it
        },
    )
        private set

    /** 主题色：mono（默认）/ dynamic（Material You）/ blue / purple / orange / cyan */
    var themeMode by mutableStateOf(prefs.getString("theme_mode", "mono") ?: "mono")
        private set

    /** 从其它应用分享进来的内容，等聊天页消费 */
    var pendingDraft by mutableStateOf<String?>(null)
        private set
    var pendingAttachment by mutableStateOf<Attachment?>(null)
        private set

    val appVersion: String = com.a2heng.opencode.mobile.BuildConfig.VERSION_NAME
    var checkingUpdate by mutableStateOf(false)
        private set
    var updateMessage by mutableStateOf<String?>(null)
        private set
    var updateInfo by mutableStateOf<UpdateInfo?>(null)
        private set
    var downloadingUpdate by mutableStateOf(false)
        private set
    var updateProgress by mutableStateOf(0)
        private set

    var client: OcClient? = null
        private set

    // ---- 会话 ---------------------------------------------------------------
    var sessions by mutableStateOf<List<SessionInfo>>(emptyList())
        private set
    var activeIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var sessionsLoading by mutableStateOf(false)
        private set
    var current by mutableStateOf<SessionInfo?>(null)
        private set
    var messages by mutableStateOf<List<Msg>>(emptyList())
        private set
    var messagesLoading by mutableStateOf(false)
        private set
    var sending by mutableStateOf(false)
        private set
    var liveText by mutableStateOf("")
        private set
    var liveReasoning by mutableStateOf("")
        private set
    var statusLine by mutableStateOf("")
        private set
    var queue by mutableStateOf<List<Queued>>(emptyList())
        private set
    val queuedCount: Int get() = queue.size
    var lastError by mutableStateOf<String?>(null)
        private set

    // ---- 权限 / 表单 --------------------------------------------------------
    var permissions by mutableStateOf<List<PermissionReq>>(emptyList())
        private set
    var forms by mutableStateOf<List<FormInfo>>(emptyList())
        private set

    // ---- 模型 / Agent -------------------------------------------------------
    var agents by mutableStateOf<List<JsonObject>>(emptyList())
        private set
    var models by mutableStateOf<List<JsonObject>>(emptyList())
        private set
    var commands by mutableStateOf<List<CommandInfo>>(emptyList())
        private set
    /** modelID -> context window */
    var contextLimits by mutableStateOf<Map<String, Long>>(emptyMap())
        private set

    // ---- 缓存看板 -----------------------------------------------------------
    var caches by mutableStateOf<List<SessionCache>>(emptyList())
        private set
    var cacheLoading by mutableStateOf(false)
        private set
    var cacheError by mutableStateOf<String?>(null)
        private set
    var cacheFetchedAt by mutableStateOf(0L)
        private set

    // ---- 文件 ---------------------------------------------------------------
    var fsDir by mutableStateOf("")
        private set
    var fsRoot by mutableStateOf("")
        private set
    var fsEntries by mutableStateOf<List<FsEntry>>(emptyList())
        private set
    var fsLoading by mutableStateOf(false)
        private set
    var fsError by mutableStateOf<String?>(null)
        private set
    var openFile by mutableStateOf<String?>(null)
        private set
    var openFileText by mutableStateOf("")
        private set
    var openFileLoading by mutableStateOf(false)
        private set

    // ---- 终端 ---------------------------------------------------------------
    var ptys by mutableStateOf<List<PtyInfo>>(emptyList())
        private set
    var ptyActive by mutableStateOf<PtyInfo?>(null)
        private set
    var ptyOutput by mutableStateOf("")
        private set
    var ptyConnected by mutableStateOf(false)
        private set
    var ptyConnecting by mutableStateOf(false)
        private set
    var ptyError by mutableStateOf<String?>(null)
        private set
    private var ptySocket: WebSocket? = null
    private var ptyDecoding = false

    private var eventJob: Job? = null
    private var reloadJob: Job? = null
    private var sessionsJob: Job? = null

    // =========================================================================
    // 连接
    // =========================================================================

    fun connect(url: String, pwd: String, remember: Boolean = true) {
        val normalized = OcClient.normalize(url)
        connecting = true
        connectionError = null
        viewModelScope.launch {
            try {
                val c = OcClient(normalized, pwd)
                val info = withContext(Dispatchers.IO) { c.ping() } as? JsonObject
                serverUrl = normalized
                password = pwd
                client = c
                serverVersion = info?.str("version") ?: ""
                runCatching { withContext(Dispatchers.IO) { c.locationRoot() } }
                    .onSuccess { homeDir = it }
                connected = true
                connecting = false
                if (remember) {
                    prefs.edit().putString("url", normalized).putString("password", pwd).apply()
                }
                refreshSessions()
                loadPermissions()
                loadMeta()
                startEvents()
            } catch (e: Exception) {
                connecting = false
                connected = false
                connectionError = e.message ?: e.javaClass.simpleName
            }
        }
    }

    fun disconnect() {
        eventJob?.cancel()
        reloadJob?.cancel()
        sessionsJob?.cancel()
        detachPty()
        client = null
        connected = false
        current = null
        messages = emptyList()
        sessions = emptyList()
        caches = emptyList()
        permissions = emptyList()
        forms = emptyList()
        serverVersion = ""
    }

    fun autoConnect() {
        if (serverUrl.isNotBlank() && !connected) connect(serverUrl, password)
    }

    fun clearError() {
        lastError = null
    }

    fun updateSendMode(mode: String) {
        sendMode = mode
        prefs.edit().putString("send_mode", mode).apply()
    }

    fun updateDetailLevel(level: String) {
        detailLevel = level
        prefs.edit().putString("detail_level", level).apply()
    }

    fun updateThemeMode(mode: String) {
        themeMode = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun applyShare(text: String?, image: Attachment?) {
        if (!text.isNullOrBlank()) pendingDraft = text
        if (image != null) pendingAttachment = image
    }
    fun consumeDraft(): Pair<String?, Attachment?> {
        val result = pendingDraft to pendingAttachment
        pendingDraft = null
        pendingAttachment = null
        return result
    }

    fun checkUpdate() {
        if (checkingUpdate) return
        checkingUpdate = true
        updateMessage = "检查中…"
        viewModelScope.launch {
            try {
                val info = withContext(Dispatchers.IO) { UpdateClient.latest() }
                if (info == null) {
                    updateMessage = "没有找到 Release"
                } else {
                    updateInfo = info
                    updateMessage = if (UpdateClient.isNewer(appVersion, info.tag)) {
                        "发现新版本 ${info.tag}"
                    } else {
                        "已是最新版本 v$appVersion"
                    }
                }
            } catch (e: Exception) {
                updateMessage = "检查失败：" + (e.message ?: "")
            } finally {
                checkingUpdate = false
            }
        }
    }

    fun downloadUpdate() {
        val info = updateInfo ?: return
        val url = info.apkUrl
        if (url == null) {
            updateMessage = "这个版本没有 APK 附件"
            return
        }
        if (downloadingUpdate) return
        downloadingUpdate = true
        updateProgress = 0
        viewModelScope.launch {
            try {
                val dir = java.io.File(getApplication<Application>().cacheDir, "update").apply { mkdirs() }
                val dest = java.io.File(dir, "opencode-mobile-${info.tag.removePrefix("v")}.apk")
                withContext(Dispatchers.IO) {
                    UpdateClient.download(url, dest) { p -> updateProgress = p }
                }
                installApk(dest)
                updateMessage = "已下载，交给系统安装"
            } catch (e: Exception) {
                updateMessage = "下载失败：" + (e.message ?: "")
            } finally {
                downloadingUpdate = false
            }
        }
    }

    private fun installApk(file: java.io.File) {
        val ctx = getApplication<Application>()
        val uri = androidx.core.content.FileProvider.getUriForFile(
            ctx,
            ctx.packageName + ".fileprovider",
            file,
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }

    // =========================================================================
    // 会话
    // =========================================================================

    fun refreshSessions() {
        val c = client ?: return
        if (sessionsJob?.isActive == true) return
        sessionsJob = viewModelScope.launch {
            sessionsLoading = true
            try {
                val (list, active) = withContext(Dispatchers.IO) {
                    c.sessionList() to c.activeSessionIds()
                }
                sessions = list.sortedByDescending { it.updated }
                activeIds = active
                current?.let { cur -> sessions.firstOrNull { it.id == cur.id }?.let { current = it } }
            } catch (e: Exception) {
                connectionError = e.message
            } finally {
                sessionsLoading = false
            }
        }
    }

    fun openSession(session: SessionInfo) {
        current = session
        messages = emptyList()
        liveText = ""
        liveReasoning = ""
        sending = isActive(session.id)
        queue = emptyList()
        lastError = null
        loadMessages()
        loadForms(session.id)
        loadInbox()
    }

    fun closeSession() {
        current = null
        messages = emptyList()
        liveText = ""
        liveReasoning = ""
        forms = emptyList()
    }

    fun loadMessages(silent: Boolean = false) {
        val c = client ?: return
        val s = current ?: return
        viewModelScope.launch {
            if (!silent) messagesLoading = true
            try {
                val list = withContext(Dispatchers.IO) { c.messages(s.id) }
                messages = collapseMessages(list)
                liveText = ""
                liveReasoning = ""
            } catch (e: Exception) {
                if (!silent) lastError = e.message
            } finally {
                messagesLoading = false
            }
        }
    }

    private fun collapseMessages(list: List<Msg>): List<Msg> {
        // opencode 的 /api/session/<id>/message 是「新的在前」，这里统一排成旧 → 新，
        // 否则聊天界面整个是倒序的。工具/推理消息可能很碎；顺手去掉完全空白的系统消息。
        return list.filter {
            when {
                it.isUser -> true
                it.isAssistant -> it.parts.isNotEmpty() || it.text.isNotBlank()
                else -> false
            }
        }.sortedBy { it.created }
    }

    fun sendPrompt(text: String, attachments: List<Attachment> = emptyList()) {
        val c = client ?: return
        val s = current ?: return
        if (text.isBlank() && attachments.isEmpty()) return
        val busy = isActive(s.id)
        val mode = if (!busy) "direct" else sendMode
        lastError = null
        if (mode != "queue" && mode != "steer") {
            sending = true
            liveText = ""
            liveReasoning = ""
        }
        val bodyText = text.ifBlank { if (attachments.isNotEmpty()) "请看这张图片" else "" }
        viewModelScope.launch {
            try {
                when (mode) {
                    "queue" -> {
                        withContext(Dispatchers.IO) { c.prompt(s.id, bodyText, "queue", attachments) }
                        statusLine = "已加入队列"
                        delay(300)
                        loadInbox()
                    }
                    "steer" -> {
                        withContext(Dispatchers.IO) { c.prompt(s.id, bodyText, "steer", attachments) }
                        statusLine = "已插入当前回答"
                    }
                    "interrupt" -> {
                        withContext(Dispatchers.IO) {
                            c.interrupt(s.id)
                            delay(300)
                            c.prompt(s.id, bodyText, null, attachments)
                        }
                    }
                    else -> {
                        withContext(Dispatchers.IO) { c.prompt(s.id, bodyText, null, attachments) }
                        delay(350)
                        loadMessages(silent = true)
                    }
                }
                refreshSessions()
            } catch (e: Exception) {
                lastError = e.message
                if (mode != "queue" && mode != "steer") sending = false
            }
        }
    }

    fun runCommand(name: String, text: String, attachments: List<Attachment> = emptyList()) {
        val c = client ?: return
        val s = current ?: return
        val busy = isActive(s.id)
        val mode = if (!busy) "direct" else sendMode
        lastError = null
        if (mode != "queue" && mode != "steer") {
            sending = true
            liveText = ""
            liveReasoning = ""
        }
        viewModelScope.launch {
            try {
                when (mode) {
                    "queue" -> {
                        withContext(Dispatchers.IO) { c.runCommand(s.id, name, text, "queue", attachments) }
                        statusLine = "命令已加入队列"
                        delay(300)
                        loadInbox()
                    }
                    "steer" -> {
                        withContext(Dispatchers.IO) { c.runCommand(s.id, name, text, "steer", attachments) }
                        statusLine = "命令已插入"
                    }
                    "interrupt" -> {
                        withContext(Dispatchers.IO) {
                            c.interrupt(s.id)
                            delay(300)
                            c.runCommand(s.id, name, text, null, attachments)
                        }
                    }
                    else -> {
                        withContext(Dispatchers.IO) { c.runCommand(s.id, name, text, null, attachments) }
                        delay(350)
                        loadMessages(silent = true)
                    }
                }
                refreshSessions()
            } catch (e: Exception) {
                lastError = e.message
                if (mode != "queue" && mode != "steer") sending = false
            }
        }
    }

    fun loadInbox() {
        val c = client ?: return
        val s = current ?: return
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { c.inbox(s.id) }
                queue = list.filter { it.str("type") == "user" }.map {
                    Queued(
                        id = it.str("id") ?: "",
                        text = it.obj("payload")?.str("text") ?: "",
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    fun cancelQueued(id: String) {
        val c = client ?: return
        val s = current ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { c.cancelInbox(s.id, id) }
                queue = queue.filterNot { it.id == id }
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    /** 撤回排队消息：取消服务端队列项，成功后再把原文还给输入框。 */
    fun withdrawQueued(id: String, onDone: (String) -> Unit) {
        val c = client ?: return
        val s = current ?: return
        val item = queue.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { c.cancelInbox(s.id, id) }
                queue = queue.filterNot { it.id == id }
                onDone(item.text)
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    /**
     * opencode 的 revert：长按已发出的用户消息时使用。
     * files=true 连文件改动一起还原；false 只回退对话。
     */
    fun revertTo(messageID: String, files: Boolean) {
        val c = client ?: return
        val s = current ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    c.stageRevert(s.id, messageID, files)
                    c.commitRevert(s.id)
                }
                queue = emptyList()
                loadMessages(silent = true)
                refreshSessions()
            } catch (e: Exception) {
                lastError = "回退失败：" + (e.message ?: "")
            }
        }
    }

    fun interrupt() {
        val c = client ?: return
        val s = current ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { c.interrupt(s.id) }
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    fun createSession(directory: String? = null) {
        val c = client ?: return
        viewModelScope.launch {
            try {
                val dir = directory?.takeIf { it.isNotBlank() }
                    ?: current?.directory?.takeIf { it.isNotBlank() }
                    ?: sessions.firstOrNull()?.directory
                val s = withContext(Dispatchers.IO) { c.createSession(dir) }
                refreshSessions()
                if (s != null) openSession(s)
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    /** 新建会话对话框里浏览目录用，不影响「文件」页签的状态。 */
    suspend fun browseDirs(path: String?): Pair<String, List<FsEntry>> {
        val c = client ?: return "" to emptyList()
        return withContext(Dispatchers.IO) { c.listDir(path) }
    }

    /** 在指定目录下新建文件夹（走服务端的 shell，随后让对话框自己刷新）。 */
    fun createFolder(parent: String, name: String, onDone: () -> Unit) {
        val c = client ?: return
        val target = parent.trimEnd('/') + "/" + name.trim().trim('/')
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    c.runShell("mkdir -p -- " + shellQuote(target), parent)
                }
                delay(400)
                onDone()
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    private fun shellQuote(s: String) = "'" + s.replace("'", "'\\''") + "'"

    fun renameSession(id: String, title: String) {
        val c = client ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { c.renameSession(id, title) }
                refreshSessions()
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    fun deleteSession(id: String) {
        val c = client ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { c.deleteSession(id) }
                if (current?.id == id) closeSession()
                refreshSessions()
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    fun setModel(model: ModelRef) {
        val c = client ?: return
        val s = current ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { c.setModel(s.id, model) }
                current = s.copy(model = model)
                refreshSessions()
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    fun setAgent(agent: String) {
        val c = client ?: return
        val s = current ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { c.setAgent(s.id, agent) }
                current = s.copy(agent = agent)
                refreshSessions()
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    // =========================================================================
    // 事件流
    // =========================================================================

    private fun startEvents() {
        eventJob?.cancel()
        eventJob = viewModelScope.launch {
            var backoff = 1000L
            while (connected) {
                val c = client ?: break
                try {
                    c.events().collect { event -> handleEvent(event) }
                } catch (_: Exception) {
                }
                if (!connected) break
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(10_000)
                refreshSessions()
            }
        }
    }

    private fun handleEvent(event: JsonObject) {
        val type = event.str("type") ?: return
        if (type == "__error") return
        val data = event.obj("data") ?: JsonObject(emptyMap())
        var sessionID = data.str("sessionID")
        if (sessionID == null) {
            sessionID = data.obj("info")?.str("sessionID") ?: data.str("id")
        }

        if (type == "server.connected") {
            refreshSessions()
            loadPermissions()
            return
        }

        if (type.startsWith("permission.")) {
            loadPermissions()
            return
        }

        if (type.startsWith("session.inbox.")) {
            loadInbox()
            return
        }

        if (type.startsWith("form.")) {
            sessionID?.let { loadForms(it) }
            if (current != null) loadForms(current!!.id)
            return
        }

        if (type == "session.updated" || type == "session.created" || type == "session.deleted") {
            refreshSessions()
            return
        }

        val sid = sessionID ?: return

        if (current?.id != sid) return

        when (type) {
            "session.text.started" -> {
                if (liveText.isNotEmpty() && !liveText.endsWith("\n")) liveText += "\n"
            }
            "session.text.delta" -> {
                liveText += data.str("delta") ?: ""
            }
            "session.text.ended" -> {
                data.str("text")?.let { full ->
                    if (liveText.length < full.length) liveText = full
                }
            }
            "session.reasoning.started" -> {
                if (liveReasoning.isNotEmpty()) liveReasoning += "\n\n"
            }
            "session.reasoning.delta" -> {
                liveReasoning += data.str("delta") ?: ""
            }
            "session.reasoning.ended" -> {
                data.str("text")?.let { full ->
                    if (liveReasoning.length < full.length) liveReasoning = full
                }
            }
            "session.execution.started" -> {
                sending = true
                activeIds = activeIds + sid
                statusLine = "正在思考…"
            }
            "session.step.started" -> {
                val n = data.str("ordinal") ?: data.str("step") ?: data.str("index")
                statusLine = if (n.isNullOrBlank()) "思考中…" else "第 $n 步"
            }
            "session.step.ended", "session.step.streamed" -> {
                sending = true
                scheduleReload()
            }
            "session.tool.input.started" -> statusLine = "准备工具 ${data.str("name") ?: ""}"
            "session.tool.input.ended", "session.tool.called", "session.tool.progress" -> {
                statusLine = "执行工具 ${data.str("name") ?: ""}"
                scheduleReload()
            }
            "session.tool.success", "session.tool.failed" -> {
                scheduleReload()
            }
            "session.execution.succeeded", "session.execution.failed",
            "session.execution.interrupted", "session.idle" -> {
                sending = false
                activeIds = activeIds - sid
                statusLine = ""
                scheduleReload(immediate = true)
                refreshSessions()
            }
            "session.error" -> {
                sending = false
                lastError = data.str("message")
                scheduleReload(immediate = true)
            }
            "session.message.content.updated", "session.message" -> scheduleReload()
            "session.compaction.started" -> statusLine = "正在压缩上下文…"
            "session.compaction.ended" -> statusLine = ""
            "session.usage.recorded", "session.usage.updated" -> scheduleReload()
            else -> {
                if (type.startsWith("session.tool.")) scheduleReload()
            }
        }
    }

    private fun scheduleReload(immediate: Boolean = false) {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            delay(if (immediate) 80 else 450)
            loadMessages(silent = true)
        }
    }

    // =========================================================================
    // 权限 / 表单
    // =========================================================================

    fun loadPermissions() {
        val c = client ?: return
        viewModelScope.launch {
            try {
                permissions = withContext(Dispatchers.IO) { c.permissions() }
            } catch (_: Exception) {
            }
        }
    }

    fun replyPermission(req: PermissionReq, decision: String) {
        val c = client ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { c.replyPermission(req.sessionID, req.id, decision) }
                permissions = permissions.filterNot { it.id == req.id }
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    fun loadForms(sessionID: String) {
        val c = client ?: return
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { c.forms(sessionID) }
                forms = forms.filterNot { it.sessionID == sessionID } + list
            } catch (_: Exception) {
            }
        }
    }

    fun replyForm(form: FormInfo, answer: kotlinx.serialization.json.JsonElement) {
        val c = client ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { c.replyForm(form.sessionID, form.id, answer) }
                forms = forms.filterNot { it.id == form.id }
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    fun dismissForm(formID: String) {
        forms = forms.filterNot { it.id == formID }
    }

    // =========================================================================
    // 模型 / Agent
    // =========================================================================

    fun loadMeta() {
        val c = client ?: return
        viewModelScope.launch {
            try {
                val (a, m, cmds) = withContext(Dispatchers.IO) {
                    Triple(c.agents(), c.models(), c.commands())
                }
                agents = a.filter { it.str("mode") != "subagent" }
                contextLimits = m.mapNotNull { item ->
                    val id = item.str("id") ?: return@mapNotNull null
                    val limit = item.obj("limit")?.long("context") ?: 0L
                    if (limit > 0) id to limit else null
                }.toMap()
                models = m.filter {
                    val enabled = (it["enabled"] as? JsonPrimitive)?.booleanOrNull
                    val status = it.str("status")
                    (enabled != false) && (status == null || status == "active")
                }
                commands = cmds
            } catch (_: Exception) {
            }
        }
    }

    // =========================================================================
    // 缓存看板
    // =========================================================================

    fun refreshCache() {
        val c = client ?: return
        if (cacheLoading) return
        cacheLoading = true
        cacheError = null
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { c.sessionList() }
                    .sortedByDescending { it.updated }
                    .take(20)
                val out = mutableListOf<SessionCache>()
                // 并发 4 个，避免把服务端和手机网络打死
                for (chunk in list.chunked(4)) {
                    val rows = chunk.map { s ->
                        async(Dispatchers.IO) {
                            try {
                                SessionCache(s, stepsOf(c.messages(s.id)))
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                    out += rows
                }
                caches = out.sortedByDescending { it.session.updated }
                cacheFetchedAt = System.currentTimeMillis()
            } catch (e: Exception) {
                cacheError = e.message
            } finally {
                cacheLoading = false
            }
        }
    }

    fun cacheFor(sessionID: String): SessionCache? = caches.firstOrNull { it.session.id == sessionID }

    fun isActive(sessionID: String): Boolean = sessionID in activeIds

    // =========================================================================
    // 文件
    // =========================================================================

    fun loadDir(path: String?) {
        val c = client ?: return
        if (fsLoading) return
        fsLoading = true
        fsError = null
        viewModelScope.launch {
            try {
                val (dir, entries) = withContext(Dispatchers.IO) { c.listDir(path) }
                fsRoot = dir
                fsDir = if (path.isNullOrBlank()) dir else path.trimEnd('/')
                fsEntries = entries.sortedWith(compareByDescending<FsEntry> { it.isDir }.thenBy { it.name.lowercase() })
            } catch (e: Exception) {
                fsError = e.message
            } finally {
                fsLoading = false
            }
        }
    }

    fun openFile(path: String) {
        val c = client ?: return
        openFile = path
        openFileText = "加载中…"
        openFileLoading = true
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) { c.readFile(path) }
                openFileText = if (text.length > 300_000) text.take(300_000) + "\n\n…（文件过大，已截断）" else text
            } catch (e: Exception) {
                openFileText = "读取失败：" + (e.message ?: "")
            } finally {
                openFileLoading = false
            }
        }
    }

    fun closeFile() {
        openFile = null
        openFileText = ""
    }

    // =========================================================================
    // 终端 PTY
    // =========================================================================

    fun loadPtys() {
        val c = client ?: return
        viewModelScope.launch {
            try {
                ptys = withContext(Dispatchers.IO) { c.ptyList() }
            } catch (e: Exception) {
                ptyError = e.message
            }
        }
    }

    fun createPty() {
        val c = client ?: return
        viewModelScope.launch {
            try {
                val cwd = current?.directory?.takeIf { it.isNotBlank() }
                val p = withContext(Dispatchers.IO) { c.ptyCreate("bash", cwd) }
                if (p != null) {
                    loadPtys()
                    attachPty(p)
                }
            } catch (e: Exception) {
                ptyError = e.message
            }
        }
    }

    fun attachPty(pty: PtyInfo) {
        val c = client ?: return
        detachPty()
        ptyActive = pty
        ptyOutput = ""
        ptyError = null
        ptyConnecting = true
        viewModelScope.launch {
            try {
                val ticket = withContext(Dispatchers.IO) { c.ptyTicket(pty.id) }
                if (ticket == null) {
                    ptyError = "服务端没有返回终端票据"
                    ptyConnecting = false
                    return@launch
                }
                val socket = c.ptySocket(pty.id, ticket, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        ptyConnected = true
                        ptyConnecting = false
                        // 让远端 bash 的换行/宽度正常
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                c.ptyResize(pty.id, 40, 100)
                            } catch (_: Exception) {
                            }
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        ptyOutput = (ptyOutput + text).takeLast(160_000)
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        // 控制帧：0x00 + JSON {cursor}
                        if (bytes.size > 1 && bytes[0] == 0.toByte()) return
                        ptyOutput = (ptyOutput + bytes.utf8()).takeLast(160_000)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        ptyConnected = false
                        ptyConnecting = false
                        ptyError = t.message ?: "终端连接失败"
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        ptyConnected = false
                        ptyConnecting = false
                    }
                })
                ptySocket = socket
            } catch (e: Exception) {
                ptyConnecting = false
                ptyError = e.message
            }
        }
    }

    fun sendPty(text: String) {
        ptySocket?.send(text)
    }

    fun detachPty() {
        try {
            ptySocket?.close(1000, "leave")
        } catch (_: Exception) {
        }
        ptySocket = null
        ptyActive = null
        ptyConnected = false
        ptyConnecting = false
        ptyOutput = ""
    }

    fun removePty(id: String) {
        val c = client ?: return
        if (ptyActive?.id == id) detachPty()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { c.ptyRemove(id) }
                loadPtys()
            } catch (e: Exception) {
                ptyError = e.message
            }
        }
    }
}
