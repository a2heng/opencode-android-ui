package com.a2heng.opencode.mobile.data

import android.content.Context
import android.net.Uri
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 读取本机 Adia（android-dev-in-android）暴露的 opencode 服务信息。
 *
 * Adia 的 opencode 只监听 127.0.0.1，并用一次性随机密码做 Basic 认证；
 * 密码存在它自己的私有 SharedPreferences 里，别的应用读不到。
 * 所以 Adia 开了一个只读 ContentProvider
 * `content://<applicationId>.local-server/info`，返回：
 *
 * ```json
 * {"url":"http://127.0.0.1:4098","port":4098,"username":"opencode",
 *  "password":"...","running":true,"pid":1234,"app":"adia"}
 * ```
 *
 * 调用方必须在清单里声明权限
 * `com.a2heng.adia.permission.LOCAL_SERVER_INFO`（normal，会自动授予），
 * 并在 `<queries>` 里声明 Adia 的包名（Android 11+ 的包可见性）。
 */
object LocalServerProbe {
    /** 正式包 + debug 包（applicationIdSuffix .debug）。 */
    private val AUTHORITIES = listOf(
        "com.a2heng.adia.local-server",
        "com.a2heng.adia.debug.local-server",
    )

    data class ServerInfo(
        val url: String,
        val password: String,
        val running: Boolean,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** 找到正在运行的本机 Adia 服务器；没有就返回 null。 */
    fun findRunning(app: Context): ServerInfo? =
        AUTHORITIES.firstNotNullOfOrNull { authority -> query(app, authority) }
            ?.takeIf { it.running }

    private fun query(app: Context, authority: String): ServerInfo? = runCatching {
        app.contentResolver
            .query(Uri.parse("content://$authority/info"), null, null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val body = cursor.getString(0) ?: return@use null
                val o = json.parseToJsonElement(body).jsonObject
                val url = o["url"]?.jsonPrimitive?.contentOrNull ?: return@use null
                ServerInfo(
                    url = url,
                    password = o["password"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    running = o["running"]?.jsonPrimitive?.contentOrNull == "true",
                )
            }
    }.getOrNull()
}
