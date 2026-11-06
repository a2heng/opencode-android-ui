package com.a2heng.opencode.mobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.a2heng.opencode.mobile.data.AppModel
import com.a2heng.opencode.mobile.data.SharedPayload
import com.a2heng.opencode.mobile.ui.AppRoot
import com.a2heng.opencode.mobile.ui.OcTheme
import com.a2heng.opencode.mobile.ui.loadImageAttachment

class MainActivity : ComponentActivity() {

    private var shared by mutableStateOf<SharedPayload?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        shared = readShare(intent)
        setContent {
            val model: AppModel = viewModel()
            OcTheme(themeMode = model.themeMode) {
                LaunchedEffect(shared) {
                    shared?.let {
                        model.applyShare(it.text, it.image)
                        shared = null
                    }
                }
                LaunchedEffect(Unit) { model.autoConnect() }
                AppRoot(model)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shared = readShare(intent)
    }

    /** 接收其它 App 通过分享面板发来的文本 / 图片。 */
    private fun readShare(intent: Intent?): SharedPayload? {
        intent ?: return null
        if (intent.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        val image = uri?.let { runCatching { loadImageAttachment(this, it) }.getOrNull() }
        if (text.isNullOrBlank() && image == null) return null
        return SharedPayload(text, image)
    }
}
