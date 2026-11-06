# OpenCode Mobile

局域网 opencode 服务的原生 Android 客户端（Kotlin + Jetpack Compose）。

> **当前状态：闭源第一版（v0.1.0），仓库私有。**
> 前端界面完全自绘，不复用 opencode 的 Web UI；App 直连 `http://<你的电脑>:42000`，
> 不需要额外的中转服务。

## 功能

| 模块 | 说明 |
| --- | --- |
| 会话列表 | 「运行中」独立分区 + 按文件夹分区，可切换「按文件夹 / 按时间」；支持搜索、重命名、删除 |
| 新建会话 | 选择或新建文件夹（目录浏览器 + 最近使用），在指定目录创建会话 |
| 聊天 | 历史消息、SSE 流式回答、推理过程折叠、工具卡片（含 read 内容预览）、Markdown / 代码块 |
| 运行中发送 | 会话正在跑也可以继续输入；默认以 `delivery=queue` 加入队列，显示待发送列表并支持取消 |
| 缓存命中率 | App 内直连聚合最近 20 个会话：总命中率、缓存读取/未命中/写入、花费、最近 40 步柱状图、逐步明细 |
| 权限确认 | `permission.asked` 弹窗，支持「允许一次 / 总是允许 / 拒绝」 |
| 表单 | `question` 等结构化输入的表单弹窗（string / number / boolean / multiselect） |
| 模型 / Agent | 会话内切换模型与 Agent |
| 文件 | 目录浏览与文本预览（走 `/api/fs/list`、`/api/fs/read`） |
| 终端 | 列出 / 新建 / 结束 PTY，WebSocket 实时输入输出，附 Ctrl+C、Tab、Esc、方向键 |

## 环境要求

- 手机 Android 8.0+（`minSdk 26`），与电脑在同一局域网。
- 服务端 opencode **v2**（HTTP API：`/api/*`、`/api/event`），已用 `v2.2.5` 实测。
- 服务端监听局域网：

  ```bash
  opencode serve --hostname 0.0.0.0 --port 42000
  ```

  默认密码来自 `OPENCODE_PASSWORD` 环境变量；App 里填这个密码即可（Basic 认证，用户名固定 `opencode`）。

## 构建

需要 JDK 17、Android SDK 34。

```bash
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

安装：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

如果手机用无线调试：`adb pair <ip>:<pair-port> <code>` 然后 `adb connect <ip>:<connect-port>`。

## 使用

1. 打开 App，填服务器地址（如 `http://192.168.0.105:42000`）与密码，点连接。
2. 「会话」里点一个会话进入聊天；右下角「新建会话」可以选目录。
3. 会话运行中继续发消息会进入队列，输入框上方会显示「待发送 N 条」。
4. 「缓存」页直接向服务端拉取最近 20 个会话的消息并聚合，不依赖 42003 看板。
5. 「文件」「终端」都作用于服务端当前 location（`/api/location` 的目录）。

## 技术栈

- Kotlin 2.0.21 / Compose BOM 2024.10.01 / Material 3
- OkHttp 4.12（REST、SSE、WebSocket）
- kotlinx.serialization（宽松解析 `JsonElement`，兼容服务端字段变化）
- AGP 8.7.3 / Gradle 8.9 / compileSdk 34

## 目录

```
app/src/main/java/com/a2heng/opencode/mobile/
├── MainActivity.kt
├── data/
│   ├── OcClient.kt      # HTTP / SSE / PTY WebSocket 封装
│   ├── Models.kt        # 宽松 JSON 解析、消息/缓存统计
│   └── AppModel.kt      # 全局状态 + 事件流 + 队列/权限/终端
└── ui/
    ├── AppRoot.kt       # 连接页 / 主框架 / 设置
    ├── SessionsScreen.kt
    ├── ChatScreen.kt
    ├── CacheScreen.kt
    ├── FilesScreen.kt
    ├── TerminalScreen.kt
    ├── NewSessionDialog.kt
    ├── Markdown.kt
    ├── Components.kt
    └── Theme.kt
```

## 已知限制 / 后续

- 助手消息的 Markdown 为轻量实现（代码块、行内代码、粗体/斜体、链接），暂不支持表格与嵌套列表。
- 图片/文件附件只显示占位，不能从手机上传附件。
- 会话列表分页仅取服务端默认返回；消息很多时首屏加载会变慢（后续做游标分页）。
- 暂无推送 / 后台常驻；App 退到后台后 SSE 会断开，回前台自动重连。
- 平板/横屏还没有专门布局。

## 版权

闭源项目，保留所有权利（All rights reserved）。未经授权请勿分发。
