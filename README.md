# OpenCode Mobile

[![CI](https://github.com/a2heng/opencode-mobile/actions/workflows/ci.yml/badge.svg)](https://github.com/a2heng/opencode-mobile/actions/workflows/ci.yml)
[![Release](https://github.com/a2heng/opencode-mobile/actions/workflows/release.yml/badge.svg)](https://github.com/a2heng/opencode-mobile/actions/workflows/release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

局域网 [opencode](https://opencode.ai) 的原生 Android 客户端：Kotlin + Jetpack Compose，
界面完全自绘，**直连** `http://<你的电脑>:42000`，不需要中转服务。

## 安装

1. 从 [Releases](https://github.com/a2heng/opencode-mobile/releases) 下载 `opencode-mobile-x.y.z.apk`（CI 签名）。
2. Android 8.0+，手机与电脑在同一局域网。
3. 服务端开放局域网并设置密码：

   ```bash
   OPENCODE_PASSWORD=你的密码 opencode serve --hostname 0.0.0.0 --port 42000
   ```

   在 App 里填 `http://<电脑IP>:42000` 与密码即可（Basic 认证，用户名固定 `opencode`）。

## 功能

| 模块 | 说明 |
| --- | --- |
| 会话列表 | 「运行中」独立分区；没有运行中的会话时顶部显示最近 3 条；按文件夹分组 / 按时间切换 |
| 新建会话 | 目录浏览器 + 最近使用 + 新建文件夹，可指定任意目录 |
| 聊天 | SSE 流式回答、轻量 Markdown、思考过程、工具卡片（按工具图标；read 内容；edit 统一 diff；write 内容预览；shell 命令/输出分色块；图片直接显示） |
| 输入区 | 左下角圆形「+」旋转变叉并滑出 图片 / 文件 / 命令；文件按 `@相对路径` 插入引用；Android 分享面板可直接收图文 |
| 命令 | 从 `/api/command` 拉取预置命令，选中后把 `/name ` 预置到输入框（保留已输入文字），发送时走 `/api/session/{id}/command`；也支持手输 |
| 上下文占用 | 顶栏显示 `ctx xx%`（最近一次请求 tokens / 模型 context window，≥50% 变黄、≥75% 变红） |
| 显示精细度 | **精简**：工具压成一行，点一下用气泡看完整内容；**完整**：全部展开，超长内容限高滚动 |
| 运行中发送 | **steer（默认）**：在下一步前插入、引导当前回答；**queue**：本轮结束后作为下一轮输入（消息以 pending 气泡显示，可撤回回填）；「中断后发送」是本 App 的客户端行为。对话右上角可切换 |
| 回退 | 长按已发出的用户消息 → opencode revert，可选择是否连文件一起还原 |
| 缓存命中率 | App 内聚合最近 20 个会话：总命中率、缓存读取/未命中/写入、花费、最近 40 步柱状图与逐步明细 |
| 模型 / Agent | 会话内切换 |
| 权限 / 表单 | 权限弹窗（一次 / 总是 / 拒绝）；结构化表单弹窗 |
| 文件 / 终端 | 目录浏览、文本预览；PTY 列表/新建/结束，WebSocket 实时输入输出 |
| 主题 | 默认黑白灰；可切 Material You 动态取色或蓝/紫/橙/青预设 |
| 系统集成 | 分享面板直接收文本/图片、Android 12+ 启动画面、预测式返回、主题图标（monochrome）、长按触感反馈 |

## 构建

需要 JDK 17、Android SDK 34。

```bash
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Release（需要签名凭据，见下节）：

```bash
OM_KEYSTORE=/abs/path/release.jks \
OM_KEY_ALIAS=opencode-mobile \
OM_KEY_PASS=****** \
OM_REQUIRE_SIGNING=1 \
./gradlew assembleRelease -PomVersionName=0.1.3 -PomVersionCode=103
```

## 版本与发版

- **只用语义化版本**，不用日期序号：tag `vX.Y.Z` → `versionName=X.Y.Z`，
  `versionCode = major*10000 + minor*100 + patch`（`0.1.3` → `103`）。
- 推 tag `v*` 触发 [Release](.github/workflows/release.yml)：CI 用 Secrets 签名、校验签名、生成 APK + sha256 并创建 GitHub Release。
- 仓库 Secrets：`OM_KEYSTORE_BASE64`（keystore 的 base64）、`OM_KEY_ALIAS`、`OM_KEY_PASS`。
- 本地没有凭据时 `assembleRelease` 会出**未签名**包（装不上，仅供检查产物）。

## 反馈

- 用 [Issues](https://github.com/a2heng/opencode-mobile/issues) 提，已提供 Bug / 功能建议模板。
- **优先范围**：opencode 原生（Web / TUI / Desktop）已经有、但 Android 端还没实现或不好用的能力；
  纯手机端的新点子也欢迎，只是排序会靠后。
- 参考实现放在子模块 `reference/opencode`（只用于对齐 API 语义，界面按手机习惯重做，不复刻）。

## 目录

```
app/src/main/java/com/a2heng/opencode/mobile/
├── MainActivity.kt          # 启动画面 / 分享入口
├── data/
│   ├── OcClient.kt          # HTTP / SSE / PTY WebSocket
│   ├── Models.kt            # 宽松 JSON 解析、缓存统计
│   └── AppModel.kt          # 全局状态、事件流、队列/权限/终端
└── ui/
    ├── AppRoot.kt           # 主框架 / 设置
    ├── SessionsScreen.kt  ChatScreen.kt  CacheScreen.kt
    ├── FilesScreen.kt     TerminalScreen.kt  NewSessionDialog.kt
    ├── Markdown.kt        ToolViews.kt       Components.kt  Theme.kt
```

接口细节与踩坑记录见 [docs/API-NOTES.md](docs/API-NOTES.md)。

## 已知限制

- Markdown 暂不支持表格与嵌套列表；附件只支持图片。
- 消息没有游标分页，超长会话首屏会慢；缓存页只取最近 20 个会话。
- 退到后台 SSE 会断开，回前台自动重连；没有推送。
- 平板 / 横屏还没有专门布局。

## License

[MIT](LICENSE) © 2026 a2heng
