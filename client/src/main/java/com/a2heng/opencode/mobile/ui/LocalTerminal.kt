package com.a2heng.opencode.mobile.ui

import android.graphics.Typeface
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.opencode.mobile.LocalShellHost
import com.a2heng.opencode.mobile.data.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.connectbot.terminal.Terminal
import org.connectbot.terminal.TerminalEmulator
import org.connectbot.terminal.TerminalEmulatorFactory

/**
 * 本机 Termux 终端（Adia 内置）：宿主提供 proot + `script` 的 bash 进程，
 * 直接在这台手机上开一个真 PTY，不依赖 opencode 的 PTY（Android 上不可用）。
 */
@Composable
fun LocalTerminalView(model: AppModel, onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val typeface = remember { localTerminalTypeface(context) }
    val focus = remember { FocusRequester() }
    var emulator by remember { mutableStateOf<TerminalEmulator?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val process = remember { runCatching { LocalShellHost.spawner?.spawn() }.getOrNull() }

    DisposableEffect(process) {
        if (process == null) {
            error = "无法启动本机 shell（toolbox 可能还在安装）"
        } else {
            emulator = TerminalEmulatorFactory.create(
                looper = Looper.getMainLooper(),
                initialRows = 40,
                initialCols = 100,
                defaultForeground = Color(0xFFE6EDF3),
                defaultBackground = Color(0xFF0E1116),
                onKeyboardInput = { bytes ->
                    runCatching {
                        process.outputStream.write(bytes)
                        process.outputStream.flush()
                    }
                },
                onResize = { },
            )
        }
        onDispose {
            runCatching { process?.destroy() }
        }
    }

    LaunchedEffect(emulator) {
        val emu = emulator ?: return@LaunchedEffect
        val p = process ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val buf = ByteArray(8192)
            try {
                while (true) {
                    val n = p.inputStream.read(buf)
                    if (n <= 0) break
                    val chunk = buf.copyOf(n)
                    withContext(Dispatchers.Main) { emu.writeInput(chunk) }
                }
            } catch (_: Exception) {
            }
        }
    }

    Surface(color = Color(0xFF0E1116), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            val emu = emulator
            if (emu != null) {
                Terminal(
                    terminalEmulator = emu,
                    modifier = Modifier.fillMaxSize(),
                    initialFontSize = model.ptyFontSize.sp,
                    minFontSize = 5f.sp,
                    maxFontSize = 28f.sp,
                    typeface = typeface,
                    backgroundColor = Color(0xFF0E1116),
                    foregroundColor = Color(0xFFE6EDF3),
                    keyboardEnabled = true,
                    showSoftKeyboard = true,
                    focusRequester = focus,
                )
            } else {
                Text(
                    error ?: "正在打开 Termux…",
                    color = Color(0xFFC7CDD6),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 8.dp)
                    .size(38.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFF1B2029),
                    contentColor = Color(0xFFE6EDF3),
                ),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

private fun localTerminalTypeface(context: android.content.Context): Typeface {
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
