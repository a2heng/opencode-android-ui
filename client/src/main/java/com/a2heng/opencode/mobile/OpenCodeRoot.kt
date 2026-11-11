package com.a2heng.opencode.mobile

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.a2heng.opencode.mobile.data.AppModel
import com.a2heng.opencode.mobile.data.SharedPayload
import com.a2heng.opencode.mobile.ui.AppRoot
import com.a2heng.opencode.mobile.ui.OcTheme

/**
 * 整个 Compose 客户端的根组合。
 *
 * - 独立 APK：`serverUrl = null`，走保存的服务器 / 登录页。
 * - Adia（单 APK）：传入本机 opencode 的 URL 和随机密码，同进程直接连。
 */
@Composable
fun OpenCodeMobileRoot(
    serverUrl: String? = null,
    password: String? = null,
    shared: SharedPayload? = null,
    onSharedConsumed: () -> Unit = {},
) {
    val model: AppModel = viewModel()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refreshOnResume() }
    OcTheme(themeMode = model.themeMode, appearance = model.appearance) {
        LaunchedEffect(shared) {
            shared?.let {
                model.applyShare(it.text, it.image)
                onSharedConsumed()
            }
        }
        LaunchedEffect(serverUrl, password) {
            if (!serverUrl.isNullOrBlank()) {
                // 同进程服务：不写入 SharedPreferences，避免把独立客户端的配置覆盖掉。
                model.connect(serverUrl, password.orEmpty(), remember = false)
            } else {
                model.autoConnect()
            }
        }
        AppRoot(model)
    }
}

/**
 * 给宿主（Adia 的 Java Activity）用的入口：
 * 启动本地 opencode 之后直接调用它，整个界面就是这套 Compose 客户端。
 */
object OpenCodeMobileHost {
    @JvmStatic
    fun show(activity: ComponentActivity, url: String, password: String) {
        activity.setContent {
            OpenCodeMobileRoot(serverUrl = url, password = password)
        }
    }
}
