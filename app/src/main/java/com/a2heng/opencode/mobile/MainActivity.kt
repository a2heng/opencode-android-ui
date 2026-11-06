package com.a2heng.opencode.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.a2heng.opencode.mobile.data.AppModel
import com.a2heng.opencode.mobile.ui.AppRoot
import com.a2heng.opencode.mobile.ui.OcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OcTheme {
                val model: AppModel = viewModel()
                LaunchedEffect(Unit) { model.autoConnect() }
                AppRoot(model)
            }
        }
    }
}
