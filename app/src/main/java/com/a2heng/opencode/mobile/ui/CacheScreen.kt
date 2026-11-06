package com.a2heng.opencode.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.a2heng.opencode.mobile.data.AppModel
import com.a2heng.opencode.mobile.data.CacheStep
import com.a2heng.opencode.mobile.data.SessionCache
import com.a2heng.opencode.mobile.data.Tokens
import com.a2heng.opencode.mobile.data.formatCost
import com.a2heng.opencode.mobile.data.formatTokens
import com.a2heng.opencode.mobile.data.rateColorHint
import com.a2heng.opencode.mobile.data.rateLabel

@Composable
fun CacheScreen(model: AppModel) {
    var detail by remember { mutableStateOf<SessionCache?>(null) }

    LaunchedEffect(Unit) {
        if (model.caches.isEmpty()) model.refreshCache()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("最近 20 个会话", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (model.cacheFetchedAt == 0L) "点击刷新开始统计"
                    else "更新于 ${formatTime(model.cacheFetchedAt)} · App 内直连计算",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { model.refreshCache() }, enabled = !model.cacheLoading) {
                if (model.cacheLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                }
            }
        }

        model.cacheError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }

        if (model.caches.isNotEmpty()) {
            val totals = model.caches.fold(Tokens()) { acc, c -> acc.plus(c.totals) }
            val cost = model.caches.sumOf { it.cost }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(
                    "总体命中",
                    rateLabel(totals.hitRate),
                    Modifier.weight(1f),
                    valueColor = rateColor(totals.hitRate, MaterialTheme.colorScheme.background.luminanceIsDark()),
                    hint = rateColorHint(totals.hitRate),
                )
                StatCard(
                    "缓存读取",
                    formatTokens(totals.cacheRead),
                    Modifier.weight(1f),
                    hint = "未命中 ${formatTokens(totals.input)}",
                )
                StatCard("累计花费", formatCost(cost), Modifier.weight(1f), hint = "${model.caches.size} 个会话")
            }
        }

        when {
            model.caches.isEmpty() && model.cacheLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("正在拉取会话统计…", style = MaterialTheme.typography.labelSmall)
                }
            }
            model.caches.isEmpty() -> EmptyState("还没有统计", "点右上角刷新，App 会直接向 opencode 拉取")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(model.caches, key = { it.session.id }) { cache ->
                    CacheRow(cache) { detail = cache }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    detail?.let { cache ->
        CacheDetailDialog(cache, onDismiss = { detail = null })
    }
}

@Composable
private fun CacheRow(cache: SessionCache, onClick: () -> Unit) {
    val rate = cache.rate
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    Column(
        Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                cache.session.shortTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                rateLabel(rate),
                style = MaterialTheme.typography.labelMedium,
                color = rateColor(rate, dark),
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (rate / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = rateColor(rate, dark),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "${cache.steps.size} 步 · 读 ${formatTokens(cache.totals.cacheRead)} / 未命中 ${formatTokens(cache.totals.input)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatCost(cache.cost) + " · " + formatRelative(cache.session.updated),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CacheDetailDialog(cache: SessionCache, onDismiss: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val t = cache.totals
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(cache.session.shortTitle, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatCard("命中", rateLabel(cache.rate), Modifier.weight(1f), rateColor(cache.rate, dark))
                    StatCard("最近", rateLabel(cache.lastRate), Modifier.weight(1f), rateColor(cache.lastRate, dark))
                    StatCard("花费", formatCost(cache.cost), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatCard("缓存读取", formatTokens(t.cacheRead), Modifier.weight(1f))
                    StatCard("未命中输入", formatTokens(t.input), Modifier.weight(1f))
                    StatCard("缓存写入", formatTokens(t.cacheWrite), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatCard("输出", formatTokens(t.outputAll), Modifier.weight(1f))
                    StatCard("步骤", cache.steps.size.toString(), Modifier.weight(1f))
                    StatCard("模型", cache.session.model?.label ?: "—", Modifier.weight(1f))
                }

                Text("最近 ${minOf(40, cache.steps.size)} 步命中率", style = MaterialTheme.typography.labelLarge)
                RateChart(cache.steps.takeLast(40))

                HorizontalDivider()
                Text("逐步明细", style = MaterialTheme.typography.labelLarge)
                cache.steps.asReversed().forEach { step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(rateColor(step.rate, dark), RoundedCornerShape(4.dp)),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            formatTime(step.time),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(58.dp),
                        )
                        Text(
                            rateLabel(step.rate),
                            style = MaterialTheme.typography.labelSmall,
                            color = rateColor(step.rate, dark),
                            modifier = Modifier.width(52.dp),
                        )
                        Text(
                            "in ${formatTokens(step.tokens.input)} · cache ${formatTokens(step.tokens.cacheRead)} · out ${formatTokens(step.tokens.outputAll)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun RateChart(steps: List<CacheStep>) {
    if (steps.isEmpty()) {
        Text("没有数据", style = MaterialTheme.typography.labelSmall)
        return
    }
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val track = MaterialTheme.colorScheme.surfaceVariant
    Surface(color = track.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(6.dp),
        ) {
            val gap = 2.dp.toPx()
            val barWidth = ((size.width - gap * (steps.size - 1)) / steps.size).coerceAtLeast(1f)
            val h = size.height
            steps.forEachIndexed { index, step ->
                val barH = (h * (step.rate / 100.0)).toFloat().coerceIn(2f, h)
                val x = index * (barWidth + gap)
                drawRect(
                    color = rateColor(step.rate, dark),
                    topLeft = Offset(x, h - barH),
                    size = Size(barWidth, barH),
                )
            }
        }
    }
}
