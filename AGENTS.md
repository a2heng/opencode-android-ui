# OpenCode Mobile 项目指南

> 这份文件只放**本项目**的约定与经验。系统级的东西（磁盘、服务、代理、ADB 等）在 `~/AGENTS.md`，这里不再重复。

## 1. 项目定位

- 仓库：`a2heng/opencode-mobile`（公开 · MIT），本机 `~/opencode-mobile`。
- 原生 Kotlin + Jetpack Compose（`compileSdk 34` / `minSdk 26` / AGP 8.7.3 / Gradle 8.9 / Kotlin 2.0.21），不是 WebView 壳。
- 直连局域网 opencode 服务 `http://<电脑>:42000`；Basic 用户名固定 `opencode`，密码 = 服务端 `OPENCODE_PASSWORD`。
- 正式包 `com.a2heng.opencode.mobile`，开发包 `com.a2heng.opencode.mobile.debug`（名称 `OpenCode Dev`，图标带 DEV 角标，可与正式版共存）。

## 2. 构建与安装（只做 debug）

```bash
cd ~/opencode-mobile
./gradlew assembleDebug
adb -s 192.168.0.101:34049 install -r app/build/outputs/apk/debug/app-debug.apk
```

- **用户明确：不要打 tag、不要发 Release。** 在线更新已经验证过，不需要同时维护两个 App；后续只构建/安装 debug。
- 无线调试失效时：`adb pair <ip>:<pair-port> <码>`；连接端口用 `avahi-browse -rt _adb-tls-connect._tcp` 查（MagicOS 上 `adb mdns services` 不可靠）。
- 手机默认微信输入法：`adb shell input text` 会进拼音候选，自动化测试需要空格/选词提交。
- 安装完直接告诉用户「已更新」，不要在对话里截屏或乱点手机。

## 3. 开发方式（重要）

1. **正向开发，不要逆向试探。** 优先看官方源码、公开 API 和文档；不要靠抓包、猜参数、反复试错或逆向二进制来凑功能。
2. 上游源码已浅克隆在 **`~/opencode`**（`anomalyco/opencode`，`dev` 分支），前端在 `packages/app`，客户端在 `packages/client`；要参考就 `grep` / `read` 本地源码。
3. 改文件用 `edit` / `write` 工具，不要用 Python 批量改源码。
4. **间歇性打包**：连续做几件事的中间主动 `assembleDebug` + `adb install -r`，让用户能及时看到进展；不必每改一行都打包。
5. 服务端 API 的权威描述在 `http://127.0.0.1:42000/openapi.json`；分页、参数、字段以它为准。

## 4. 用户的风格 / 产品偏好（从真实反馈总结）

### 4.1 信息层级与标题

- 对话页顶栏（自定义，不用 M3 `TopAppBar`）：
  - 左侧：40dp 圆形返回按钮 + 标题（`titleSmall`，单行省略）+ 模型（`labelSmall`，单行省略，点击换模型）。
  - 右侧：三个 **38dp 方形块**（两行：小标签 + 值），从左到右 **模式（Agent）/ 命中 / 上下文**；再右边是 40dp 圆形「打开会话」按钮。
  - 顶栏内容高度 52dp，背景与底部输入区一致（`surface` + `tonalElevation = 3.dp`），不要用 background 色融进内容。
  - 方块里的 `Text` 必须显式写 `lineHeight`（约 9sp / 11sp），否则默认行高会把第二行裁掉。
  - 圆形按钮尺寸保持不变（40dp），和输入区左右按钮一致。
- 标题里**不要再显示运行状态文字**（「正在思考」「正在使用工具」「已加入队列」「已插入当前回答」等）。运行状态只放在消息区的 LiveBubble 里。
- **命中率用整个会话的累计口径**（`session.tokens` 或所有消息 token 求和），不是最近一步；**上下文 ctx 仍然是最新一步的窗口占用**（input + cache.read + cache.write）。

### 4.2 会话列表

- 每行：**左边命中率按钮（整块可点 → 缓存页）**，右边 **3 个点**；3 个点要紧凑（30dp 按钮 / 17dp 图标），不要占太多右侧空间，避免误触命中率。
- 时间、目录、模型、token 放进副标题一行，避免右侧再堆一列。
- 命中率 0 / 没有数据时用中性色，不要显示成红色告警。

### 4.3 对话页 / 缓存页导航

- 对话页和缓存页都**覆盖在会话列表之上**（`AppRoot` 里叠放），预测式返回时滑走并露出真正的上一页；不要用替换内容的方式（否则返回背景是黑的）。
- 进入要**有动画**（从右侧滑入 + 圆角），离开用预测式返回、**不要淡化**，滑动幅度要大（当前 `translationX = 0.85 * width`，scale 0.10）。
- 顶栏返回 / 展开按钮要有**按钮背景**（`FilledTonalIconButton`，40dp，与输入区左右按钮同尺寸）；图标 20dp。

### 4.4 打开会话（标签页 / 卷帘）

- 「打开的会话」= 整个 App 里**处于打开状态**的会话，不是「正在运行」。`openSessions` 最多 20 个。
- 展开是**卷帘动画**（从上往下 `expandVertically`），卡片占满可视宽度，左右边距和底部输入区对齐（6dp），点外面收起。
- 每一项右侧有 **×** 关闭该标签；关闭当前会话时自动切到下一个，没了就回列表。
- 卡片头部有总数量和一个 **×**，表示退出多会话切换状态（关掉全部打开的会话回到列表）。
- 返回按钮只离开对话、不关闭标签；标签要通过 × 关。

### 4.5 输入区与按钮

- 左右按钮不要太大（当前 40dp 圆形「+」、42dp 发送/停止），否则输入框太窄、不够紧凑；左右边距 6dp。
- 输入框**多行**（`singleLine = false`，`maxLines = 5`）：换行时能显示多行、光标可在行间移动，不要只显示第一行。
- 右下角按钮：
  - 有文字/图片 → 发送（主题色，向上箭头）。
  - 空输入 + 正在运行 → **停止**（红色方块），点击先弹**确认框**「停止当前任务？」再中断。
  - 空输入 + 空闲 → **不显示任何按钮**（左上角已经有返回，不要再放一个叉）。
- 排队 / steer 发送后**不要改 statusLine**，保留服务端真实状态；用户知道自己已经插进队列，多余的提示只会带来 bug。

### 4.6 消息与工具卡片

- Pending 不再是瀑布流里的气泡，而是**输入框上方的独立薄行区域**（在 LazyColumn 之外）：
  - 每行：左边「撤销」（取消队列并把原文回填输入框）、中间一行文字（超长省略）、右边「×」（直接移除）。
  - 最多 150dp 高，多了内部滚动；进出用 100ms 的 `animateContentSize`，不抢瀑布流的手势。
  - 顶上的瀑布流按极快速度输出完成 → 极短间隔后 Pending 行消失、消息进入瀑布流；Pending 之前顶上的内容不会被它挤动，也不会出现工具闪到 Pending 下面再跳上去的问题。
  - 不要再用原来的 pending 气泡 / morph 动画；那套已经删除。
- 工具详情（点工具行打开）：
  - 布局要紧凑高效，宽度占满（max 560dp），内容可滚动。
  - 状态只显示一次，用中文（完成 / 运行中 / 失败 / exit N），不要显示两次或英文 `completed`。
  - **`write` 顶部是文件名，下面必须是写入的文件内容**（`Part.Tool.payload`），不是状态或 output。
  - 卡片出现/消失用淡入淡出（不要瞬间出现，也不要夸张的缩放）。
- 精简模式工具行右侧的命中率要**右对齐、固定宽度**（当前 48dp，`TextAlign.End`），不同行要对齐。
- 流式思考框：卡片里面带头部（图标 +「思考」+ 进行中/完成 + 字数），内容最多 16 行 + 右下角「展开」弹窗；不要固定高度/内嵌滚动，也不要把标题放在卡片外面。
- 工具多行输出（shell / diff / write / read）：统一 `TOOL_PREVIEW_LINES = 20` 行阈值，超出在右下角嵌一个「展开」按钮（按钮右下圆角=卡片圆角、左上切角，不要用渐变阴影），点开工具详情弹窗；正文里不要塞滚动条，滚动效果交给系统原生 overscroll。
- 工具卡片信息去重：`read` 路径只显示一次（副标题 + 行号范围），正文去掉 `Read file …` 头；`edit` 显示路径 + `+N −M`；`write` 显示路径 + 行数。
- 工具/思考卡片整张都可以点开详情弹窗（「展开」按钮只是额外提示）；弹窗直接 80ms 淡入、70ms 淡出，不要 scale、不要从右下角弹出，干脆利落；弹窗内边距要小（10dp 左右）。
- 显示模式三档：**精简（compact）** / **适中（full）** / **完整（expanded）**。
  - 精简：工具一行、思考一行，点开看详情。
  - 适中：工具/思考预览 `TOOL_PREVIEW_LINES = 20` 行（思考 16 行），超出右下角「展开」。
  - 完整：全部平铺，不做任何 clamp/展开，卡片自然变高，内容完整。
- 卡片内边距尽量小（工具/思考卡片约 8dp 横向、4dp 纵向，外面 2dp 间距）；详情弹窗左/右/下 8dp、上 14dp，内部 8~10dp，可滚动内容再留 5dp，内容高度上限 680dp。
- 长按用户消息的弹窗里除了回退，还要有「复制」（复制到系统剪贴板并提示「已复制」）。
- 展开详情卡的内容高度统一用 `rememberDialogContentHeight()`：上限 = 屏幕高 − 2×设备 R 角 − 120dp，下限 = 上限 × 0.62；工具/思考共用，避免有的太矮、有的顶出 R 角。
- 内部内容要有视觉突出：思考卡左侧加 primary 竖条（`LeftAccent`），shell 输出左侧加 Sky 竖条；详情弹窗里的 shell 输出用 `ShellBlock`（自带左侧亮条），read/write 保持代码块样式。
- 详情弹窗**不要底部操作条**（不要「关闭 / 完成」横条），内容占满；左上角标题前放 32dp 的返回按钮，配合系统返回手势。
- diff 每行 `fontSize = 10.sp` 时必须显式 `lineHeight = 12.sp`（默认 body 行高会虚高），行间 padding 0.5dp。详情弹窗的圆角只加在**外层卡片**（工具详情 16dp、AppDialog 18dp，不用设备 R 角），内部滚动内容不要再单独裁 R 角。
- Markdown：自己实现的渲染器要支持标题 `#`、列表（`-`/`*`/`+`/数字，显示为 • / 数字）、引用 `>`、分隔线 `---`、竖线表格（带网格线），以及行内 code/粗体/斜体/链接；最终回答和思考详情都要用 `MarkdownText`。展开后的字号要和卡片预览一致（工具 10.5sp；思考详情用 compact Markdown 11sp），别放大导致要滚很多屏。
- 精简模式也要有一行「思考」（图标 + 思考 + 字数 + 展开），点开看完整思考。
- 工具卡片和思考一样**直接出现**，不要 `AnimatedVisibility` 的展开/滑入动画（滚动或新工具都不要）；只有消息 item 保留轻微淡入。
- 消息合并必须 `collapseMessages(page.messages).asReversed()` 后再拼接，**不要 `sortedBy { created }`**；否则队列消息的入队时间会把上一轮工具挤到 pending 后面。
- 新消息 item 只用 `animateItem` 的 `fadeIn/fadeOut`，**不要 placementSpec**（滚动时位置动画会造成“一点点错位”的抖动）；工具块本身不再做任何展开/滑入动画。
- Pending 薄行按**旧→新**从上到下排列（新的在最下面），最多 150dp 高。
- 正文仍是单列：用户气泡靠右，助手内容和工具靠左（不要做左右分栏或左右气泡，用户已确认没有必要）。

### 4.7 设置与登录

- 「断开连接」放在设置顶部**服务器信息卡片的右侧**，不要单独占一行放在最底部。
- 默认模型卡片：卡片 + 图标（Tune）+「选择模型」主标题 + 当前模型/思考强度副标题 + 右箭头；整个卡片可点。
- 模型选择器（聊天和设置共用同一个 `ModelPickerSheet`）：
  - 高度固定（`fillMaxHeight(0.82f)` + `skipPartiallyExpanded = true`），不随内容跳动。
  - **思考强度只在被选中的模型下面常驻展示**（一排 Default / 各档位按钮），不要每个模型都做展开，也不要单独放顶部。
  - 选中有档位的模型后保持面板打开，方便继续选强度；没有档位的模型选中后直接关闭。
  - 没有选档位就是 **Default**；服务端的 `variant: "default"` 归一成 null。
- 登录/断开后的连接页必须使用当前主题（跟随系统 / 浅色 / 深色），根布局要显式铺 `MaterialTheme.colorScheme.background`，
  否则 edge-to-edge 会露出黑色窗口背景。
- 外观设置 / 主题色：横向色卡（圆形色块 + 短标签），不要用竖排 OptionRow；外观三张卡（跟随系统=黑白渐变、浅色=白、深色=黑），
  主题色至少 blue/purple/cyan/green/orange/pink/red/teal + 黑白 + 系统取色；点一下立即生效。
- 卡片/顶栏/底栏的背景要有层次：浅色下 background 用灰白、surfaceVariant 要明显深一点；深色下 surface 比 background 亮、surfaceVariant 更亮。
- opencode 的 question/form：选项在上面，自定义输入在最下面；单选选中后自定义输入禁用；输入自定义内容时自动取消单选；底部必须有显眼的「提交」按钮（用 `Button`，不要只靠 TextButton），并处理 IME 遮挡（`imePadding`）。
- 外观设置（跟随系统/浅色/深色）对**所有页面**生效，包括断开后的登录页。

### 4.8 后台 shell

- 悬浮球只显示**当前会话自己的**后台 shell（`Shell.Info.metadata.sessionID == current.id`）。
- 别的会话在跑 shell 时，当前对话不显示悬浮球。
- 原版 Ctrl+B：`POST /api/session/{id}/background` 把正在阻塞的 shell 移到后台。
  - 精简模式：在运行中的 shell 那一行右侧显示「后台」。
  - 适中/完整模式：在工具卡片标题右侧显示「后台」。
  - **有 Pending 时自动调用一次**（`loadInbox` / `loadMessages` 后检查，2.5s 去抖），让队列能继续。

### 4.9 缓存页性能

- 会话缓存详情用内存缓存（最多 12 个会话）：第二次打开直接显示，后台再刷新。
- 只拉最新 100 条消息算 steps；总量/命中率用服务端 `session.tokens`，成本用 `session.cost`，不要为了统计拉全历史。
- `stepsOf` / `SessionCache` 计算放 `Dispatchers.Default`，不要在主线程做。



## 5. 与服务端的交互（效率）

官方前端（`~/opencode/packages/client/src/solid/data.ts`、`packages/app/src/session/timeline/*`）的做法，照着做：

- 首屏：`GET /api/session/{id}/message?limit=20~40&order=desc`，取最新一页，记录 `cursor.next`。
- 往更早翻：只带 `cursor`（`?limit=...&cursor=...`），不要和 `order` 混用；每页 20~40 条，滚动到顶部附近再加载。
- 刷新：事件到来时**只重新拉最新一页**（40 条），按 id 合并进现有列表；不要每次全量重拉整个会话。
- 同一 key 的事件用去抖（当前 `scheduleReload` 450ms），一次 burst 只发一次请求。
- 切换会话：用内存缓存（已加载消息 + cursor），切回来先显示缓存再静默刷新最新页；缓存最多 8 个会话。
- 完整历史（缓存统计等）才用 `allMessages()` 循环 cursor 拉全量。
- 消息接口**新的在前**，按 `time.created` 升序再渲染。

## 6. opencode v2 API 关键坑（详见 `docs/API-NOTES.md`）

- `GET /api/session/{id}/message` 默认只返回一页（50 条），**新的在前**；用 `limit` / `order` / `cursor` 分页。
- 运行状态看 `/api/session/active`，`time.busy` 只是历史时间戳。
- PTY 票据：`POST /api/pty/{id}/connect-token` 必须带 `x-opencode-ticket: 1`；WS 帧是原始 UTF-8。
- `/api/fs/list` 返回的 path 相对 location 根目录。
- 图片附件只认 `files[].uri`（data URI），传 `data/source` 会 400。
- 命令：`GET /api/command?location[directory]=…`、`POST /api/session/{id}/command {name,text,delivery?}`。
- `delivery` 只有 `steer` / `queue`，opencode 默认 `steer`；「中断后发送」是本 App 自己的行为。
- 模型来源用 **`providerID/id` 复合键**（同名模型在 `opencode` 和 `opencode-go` 都有；`deepseek-v4.1-flash` 属于 `opencode-go`，显示 **go**）。
- 上下文占用口径：`input + cache.read + cache.write`。

## 7. 代码地图

```
app/src/main/java/com/a2heng/opencode/mobile/
├── MainActivity.kt            # 启动画面、分享入口、通知权限、ON_RESUME 补连
├── ConnectionService.kt       # 前台保活通知
├── data/
│   ├── OcClient.kt            # HTTP/SSE/PTY/命令/分页消息
│   ├── Models.kt              # JSON 宽松解析、缓存统计、Attachment/ShellInfo/MessagesPage
│   ├── AppModel.kt            # 全局状态、事件、队列、重连、消息缓存与分页
│   └── UpdateClient.kt        # GitHub Release 检查/下载（仅保留功能，不发版）
└── ui/
    ├── AppRoot.kt             # 主框架 + 列表/对话/缓存页叠放
    ├── ChatScreen.kt          # 对话、标题、输入区、工具卡片、打开会话卷帘
    ├── SessionCacheScreen.kt  # 单会话缓存页
    ├── SessionsScreen.kt      # 会话列表（命中率在左、三点在右）
    ├── SettingsScreen.kt      # 默认模型/外观/主题/发送模式/显示/更新
    ├── FloatingShells.kt      # 后台 shell 悬浮球
    └── Markdown.kt / ToolViews.kt / ModelCatalog.kt / AppDialog.kt
```

## 8. 已知约束

- debug 与 release 签名不同，不能互相覆盖；换装需先卸载（会丢 App 内保存的服务器/密码）。
- 不修改 `~/.local/share/opencode` 的数据；App 只读服务端 API。
- 不在会话里随手点手机、截屏；需要验证就让用户自己看，或只做 adb 安装。
- 每次改动同步更新 `docs/API-NOTES.md` 和本文件的「用户偏好」部分，保持文档是活的。
