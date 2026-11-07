package com.a2heng.opencode.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import androidx.compose.ui.unit.sp
import com.a2heng.opencode.mobile.data.AppModel
import com.a2heng.opencode.mobile.data.PtyInfo
import org.connectbot.terminal.Terminal

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
    val emulator = model.ptyEmulator
    val focus = remember { FocusRequester() }
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val terminalTypeface = remember { terminalTypeface(appContext) }
    var ctrl by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(false) }
    var alt by remember { mutableStateOf(false) }

    val cfg = LocalConfiguration.current
    val wide = cfg.screenWidthDp >= 600
    // 固定键盘占位（不随键盘高度变化）：竖屏底部 42% 高，横屏右侧 34% 宽
    val reserved = if (wide) (cfg.screenWidthDp * 0.37f).dp else (cfg.screenHeightDp * 0.46f).dp
    val controls: @Composable (Boolean) -> Unit = { compact ->
        TerminalControls(model, ctrl, shift, alt, compact) { c, s, a -> ctrl = c; shift = s; alt = a }
    }

    Box(Modifier.fillMaxSize()) {
        TerminalBox(
            model = model,
            emulator = emulator,
            focus = focus,
            typeface = terminalTypeface,
            modifier = Modifier
                .fillMaxSize()
                .then(if (wide) Modifier.padding(end = reserved) else Modifier.padding(bottom = reserved)),
        )
        if (wide) {
            // 横屏：按钮在右侧留白区的顶部
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 10.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(reserved),
            ) {
                Column(
                    Modifier.padding(4.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    )
                    Box(Modifier.padding(top = 6.dp)) { controls(true) }
                }
            }
        } else {
            // 竖屏：按钮在底部留白区的上沿（分隔线下面）
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(reserved),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                )
                Box(Modifier.padding(top = 4.dp), contentAlignment = Alignment.Center) { controls(false) }
            }
        }
    }
}

@Composable
private fun TerminalBox(
    model: AppModel,
    emulator: org.connectbot.terminal.TerminalEmulator?,
    focus: FocusRequester,
    typeface: Typeface,
    modifier: Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        if (emulator == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "正在连接终端…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            key(model.ptyFontSize) {
                Terminal(
                    terminalEmulator = emulator,
                    modifier = Modifier.fillMaxSize(),
                    initialFontSize = model.ptyFontSize.sp,
                    minFontSize = 5.sp,
                    maxFontSize = 28.sp,
                    typeface = typeface,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    foregroundColor = MaterialTheme.colorScheme.onSurface,
                    keyboardEnabled = true,
                    showSoftKeyboard = true,
                    focusRequester = focus,
                )
            }
        }
    }
}

private fun arrowSeq(dir: Char, ctrl: Boolean, shift: Boolean, alt: Boolean): String {
    val mod = 1 + (if (shift) 1 else 0) + (if (alt) 2 else 0) + (if (ctrl) 4 else 0)
    return if (mod == 1) "\u001b[$dir" else "\u001b[1;${mod}$dir"
}

/**
 * 按键区（两行）：
 *   左侧功能键 5 列：A− Esc ^C Ctrl Alt / A+ Tab 回车 Shift Del
 *   右侧方向区 3 列：Home ↑ End / ← ↓ →
 * compact = 横屏（右侧窄条里上下排）。
 */
@Composable
private fun TerminalControls(
    model: AppModel,
    ctrl: Boolean,
    shift: Boolean,
    alt: Boolean,
    compact: Boolean,
    onMods: (Boolean, Boolean, Boolean) -> Unit,
) {
    val keys: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                KeyChip("A−") { model.updatePtyFontSize(model.ptyFontSize - 1f) }
                KeyChip("Esc") { model.sendPty("\u001b") }
                KeyChip("^C") { model.sendPty("\u0003") }
                ToggleChip("Ctrl", ctrl) { onMods(!ctrl, shift, alt) }
                ToggleChip("Alt", alt) { onMods(ctrl, shift, !alt) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                KeyChip("A+") { model.updatePtyFontSize(model.ptyFontSize + 1f) }
                KeyChip("Tab") { model.sendPty(if (shift) "\u001b[Z" else "\t") }
                EnterChip { cr -> model.sendPty(if (cr) "\r" else "\n") }
                ToggleChip("Shift", shift) { onMods(ctrl, !shift, alt) }
                KeyChip("Del") { model.sendPty("\u007f") }
            }
        }
    }
    val pad: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                KeyChip("Home") { model.sendPty("\u001b[H") }
                KeyChip("↑") { model.sendPty(arrowSeq('A', ctrl, shift, alt)) }
                KeyChip("End") { model.sendPty("\u001b[F") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                KeyChip("←") { model.sendPty(arrowSeq('D', ctrl, shift, alt)) }
                KeyChip("↓") { model.sendPty(arrowSeq('B', ctrl, shift, alt)) }
                KeyChip("→") { model.sendPty(arrowSeq('C', ctrl, shift, alt)) }
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys()
        pad()
    }
}

@Composable
private fun KeyChip(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(7.dp),
        modifier = Modifier
            .size(width = 36.dp, height = 26.dp)
            .clickableNoRipple(onClick),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 9.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun ToggleChip(label: String, on: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (on) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (on) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.30f),
        ),
        shape = RoundedCornerShape(7.dp),
        modifier = Modifier
            .size(width = 36.dp, height = 26.dp)
            .clickableNoRipple(onClick),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun EnterChip(onSend: (Boolean) -> Unit) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(7.dp),
        modifier = Modifier
            .size(width = 36.dp, height = 26.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSend(true) },
                    onLongPress = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onSend(false)
                    },
                )
            },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("回车", fontSize = 9.sp, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

/**
 * 终端字体：优先用打包的 Sarasa Fixed SC（中英文等宽、拉丁 0.5em），
 * 取不到再回退系统窄等宽字体。
 */
private fun terminalTypeface(context: Context): Typeface {
    runCatching {
        return Typeface.createFromAsset(context.assets, "fonts/SarasaFixedSC-Regular.ttf")
    }
    val candidates = listOf(
        "/system/fonts/DroidSansMono.ttf",
        "/system/fonts/RobotoMono-Regular.ttf",
        "/system/fonts/CutiveMono.ttf",
    )
    for (path in candidates) {
        if (java.io.File(path).exists()) runCatching { return Typeface.createFromFile(path) }
    }
    return Typeface.MONOSPACE
}
