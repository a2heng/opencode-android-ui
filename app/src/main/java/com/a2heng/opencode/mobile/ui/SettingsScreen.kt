package com.a2heng.opencode.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a2heng.opencode.mobile.data.AppModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(model: AppModel) {
    var showModelPicker by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { model.loadMeta() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("连接", style = MaterialTheme.typography.titleSmall)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LabelValue("服务器", model.serverUrl)
                    LabelValue("服务端版本", if (model.serverVersion.isBlank()) "未知" else "v${model.serverVersion}")
                    LabelValue("认证", if (model.password.isBlank()) "无密码" else "Basic · opencode")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { model.disconnect() }) {
                    Icon(
                        Icons.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("断开连接", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("默认模型", style = MaterialTheme.typography.titleSmall)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickableNoRipple {
                        model.loadMeta()
                        showModelPicker = true
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("选择模型", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        model.defaultModel?.let {
                            modelDisplay(it, model.modelGroupById) + " · " + variantLabel(it.variant)
                        } ?: model.serverDefaultModel?.let {
                            "服务器默认 · " + modelDisplay(it, model.modelGroupById)
                        } ?: "服务器默认",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("外观", style = MaterialTheme.typography.titleSmall)
        OptionRow(
            title = "跟随系统",
            subtitle = "深浅色由系统决定（默认）",
            selected = model.appearance == "system",
        ) { model.updateAppearance("system") }
        OptionRow(
            title = "浅色",
            subtitle = "始终使用浅色",
            selected = model.appearance == "light",
        ) { model.updateAppearance("light") }
        OptionRow(
            title = "深色",
            subtitle = "始终使用深色",
            selected = model.appearance == "dark",
        ) { model.updateAppearance("dark") }

        Spacer(Modifier.height(8.dp))
        Text("主题色", style = MaterialTheme.typography.titleSmall)
        themePresetKeys().forEach { key ->
            OptionRow(
                title = themePresetLabel(key),
                subtitle = if (key == "dynamic") "Android 12+ Material You" else "预设",
                selected = model.themeMode == key,
            ) { model.updateThemeMode(key) }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("运行中发送消息时", style = MaterialTheme.typography.titleSmall)
        OptionRow(
            title = "steer（默认）",
            subtitle = "在下一个步骤前插入，引导当前这轮回答；opencode 默认行为",
            selected = model.sendMode == "steer",
        ) { model.updateSendMode("steer") }
        OptionRow(
            title = "queue",
            subtitle = "不打断当前回答；这轮结束后作为下一轮输入",
            selected = model.sendMode == "queue",
        ) { model.updateSendMode("queue") }
        OptionRow(
            title = "中断后发送",
            subtitle = "先中断当前回答再发送；本 App 的客户端行为，不是 opencode delivery",
            selected = model.sendMode == "interrupt",
        ) { model.updateSendMode("interrupt") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("聊天显示", style = MaterialTheme.typography.titleSmall)
        OptionRow(
            title = "精简",
            subtitle = "工具压成一行，点一下用气泡看完整内容",
            selected = model.detailLevel == "compact",
        ) { model.updateDetailLevel("compact") }
        OptionRow(
            title = "完整",
            subtitle = "思考、diff、工具输出全部展开",
            selected = model.detailLevel == "full",
        ) { model.updateDetailLevel("full") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("更新", style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier
                .fillMaxWidth()
                .clickableNoRipple { model.checkUpdate() }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("检查更新", style = MaterialTheme.typography.bodyMedium)
                Text(
                    model.updateMessage ?: "当前 v${model.appVersion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (model.checkingUpdate) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        val info = model.updateInfo
        if (info != null && com.a2heng.opencode.mobile.data.UpdateClient.isNewer(model.appVersion, info.tag)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickableNoRipple { model.downloadUpdate() }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("下载并安装 ${info.tag}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (model.downloadingUpdate) "下载中 ${model.updateProgress}%"
                        else "APK 来自 GitHub Release（${info.tag}）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (model.downloadingUpdate) {
                    androidx.compose.material3.CircularProgressIndicator(
                        progress = { model.updateProgress / 100f },
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("关于", style = MaterialTheme.typography.titleSmall)
        Text(
            "OpenCode Mobile v${model.appVersion}（MIT）\n" +
                "数据全部来自局域网 opencode 服务，App 只在内存里做聚合。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
    }

    if (showModelPicker) {
        val noneLabel = model.serverDefaultModel?.let {
            "服务器默认（${modelDisplay(it, model.modelGroupById)}）"
        } ?: "服务器默认"
        ModelPickerSheet(
            model = model,
            title = "默认模型",
            selected = model.defaultModel,
            allowNone = true,
            noneLabel = noneLabel,
            onPick = { ref -> model.updateDefaultModel(ref) },
            onDismiss = { showModelPicker = false },
        )
    }
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
