package com.a2heng.opencode.mobile.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 带预测式返回动画的对话框：返回手势拖动时整体轻微缩小/淡出，
 * 松手取消会回弹，完成才真正 dismiss（Android 14+ 才有完整手势，低版本仍是普通返回）。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    contentPadding: androidx.compose.ui.unit.Dp = 24.dp,
) {
    var backProgress by remember { mutableFloatStateOf(0f) }
    val backOwner = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        if (backOwner != null) {
            PredictiveBackHandler { back ->
                try {
                    back.collect { event -> backProgress = event.progress }
                    onDismissRequest()
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                    backProgress = 0f
                    throw cancel
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .imePadding()
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .graphicsLayer {
                    val scale = 1f - 0.07f * backProgress
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - 0.35f * backProgress
                },
        ) {
            Column(Modifier.padding(contentPadding)) {
                title()
                Column(
                    Modifier
                        .padding(top = 16.dp)
                        .heightIn(max = 500.dp),
                ) { text() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}
