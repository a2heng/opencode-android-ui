# OpenCode Mobile 项目指南

> 这份文件只放**本项目**的约定与经验。系统级的东西（磁盘、服务、代理、ADB 等）在 `~/AGENTS.md`，这里不再重复。

## 1. 项目定位

- 仓库：`a2heng/opencode-mobile`（公开 · MIT），本机 `~/opencode-mobile`。
- 原生 Kotlin + Jetpack Compose（`compileSdk 37` / `minSdk 26` / AGP 9.1.1 / Gradle 9.3.1 / Kotlin 2.4.21 / Compose 1.11.4），不是 WebView 壳。AGP 9 内置 Kotlin（**不要再加 `org.jetbrains.kotlin.android`**），`resValues = true` 要显式打开。
- 直连局域网 opencode 服务 `http://<电脑>:42000`；Basic 用户名固定 `opencode`，密码 = 服务端 `OPENCODE_PASSWORD`。
- 正式包 `com.a2heng.opencode.mobile`，开发包 `com.a2heng.opencode.mobile.debug`（名称 `OpenCode Dev`，图标带 DEV 角标，可与正式版共存）。

## 2. 构建与安装（只做 debug）

```bash
cd ~/opencode-mobile
./gradlew assembleDebug
adb -s 192.168.0.101:34049 install -r app/build/outputs/apk/debug/app-debug.apk
```

- **发版由用户指挥**：只有用户明确说「打 tag / 发版」时才执行 tag + GitHub Release；不要自己顺手打。日常只构建/安装 debug。
- **commit 可以自己做，但要攒够一个完整、验证过的改动再提交**；没解决/半成品的改动不要提交，避免"看起来解决了"和提交历史杂乱。
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
  - 左侧：38dp 圆形返回按钮。
  - 中间：**标题一行**（`titleSmall`，单行省略）+ 下面一行**小字信息**：
    `ctx xx% · ↑in ↓out · 显示档位 · Git · 会话 N`。
    - ctx、tokens 只展示；显示档位点一下循环；Git / 会话可点。
    - 这行小字用 `MetaText`（9.5sp / 行高 11sp）+ `MetaDot` 分隔，**不要横滑**。
  - **不要 ⋮ 三个点 / 下拉菜单**（用户明确去掉）。
  - 高度 50dp，背景与输入区一致（`surface` + `tonalElevation = 3.dp`）。
- 输入区下面是一条**状态底栏**（`ChatStatusBar` / `StatusChip`，26dp 高、10sp），
  只放四项、**不横向滚动**：
  1. 模型（占满剩余宽度，长名字省略）
  2. 模式（Agent）
  3. 命中（整会话累计命中率）
  4. 花费（`formatCost`，没有就是 `—`）
- 其余信息（上下文、tokens、显示细节、Git、打开的会话）都在顶栏标题下面那行，
  不要再塞回底栏，也不要再加横滑列表。
- 标题里**不要显示运行状态文字**（「正在思考」「正在使用工具」等），运行状态只放在
  消息区的 LiveBubble 里。
- **命中率用整个会话的累计口径**（`session.tokens`），**上下文 ctx 是最新一步的窗口占用**
  （input + cache.read + cache.write）。

### 4.1.1 选择器一律用全屏子页

- 模型、Agent、命令、`@文件引用` 全部改成 **`PickerPage`**（`ui/PickerPage.kt`）：
  从右侧滑入、顶部只有返回按钮 + 标题、内容自己排布。
- **不要再用 `ModalBottomSheet` / 系统 `Dialog` 做选择器**（用户嫌弹出体验差）；
  `AppDialog` 只留给确认框（清空输入、中断、回退、重命名、删除）。
- 进出场统一用 `PickerPageHost`（240ms 滑入 / 190ms 滑出 + 淡入淡出），
  放在页面根部的 Box 里铺满整屏。

### 4.2 会话列表

- 每行：**左边命中率按钮（整块可点 → 缓存页）**，右边 **3 个点**；3 个点要紧凑（30dp 按钮 / 17dp 图标），不要占太多右侧空间，避免误触命中率。
- 时间、目录、模型、token 放进副标题一行，避免右侧再堆一列。
- 命中率 0 / 没有数据时用中性色，不要显示成红色告警。
- **列表上方只留一行**（不要标题、不要刷新按钮）：`🔍 搜索` → `共 N 个会话` → 右侧 `按文件夹 / 按时间` → 最右 **28dp 圆形「+」新建**（横竖屏都在右上角）。
  - 搜索平时只是一个放大镜按钮，点开才在原来那行位置展开紧凑输入框（自动聚焦），点 × 关掉搜索并清空关键字。
  - `按文件夹 / 按时间` 是窄文本按钮（`SortLabel`，labelSmall + 2dp padding），不要用 `TextButton` 的大留白。
  - 不要底部 ExtendedFAB「新建会话」。
- **行高要压缩**：外层上下 5dp、标题 `bodyMedium`、副标题 `labelSmall`，分组头（运行中/文件夹）上下 6/3dp；一屏要多显示几条。

### 4.3 对话页 / 缓存页导航

- 竖屏：对话页和缓存页都**覆盖在会话列表之上**（`AppRoot` 里叠放），预测式返回时滑走并露出真正的上一页；不要用替换内容的方式（否则返回背景是黑的）。
- 竖屏进入要**有动画**（从右侧滑入 + 圆角），离开用预测式返回、**不要淡化**，滑动幅度要大（当前 `translationX = 0.85 * width`，scale 0.10）。
- **宽屏（宽 ≥ 600dp）不走覆盖**：会话页直接是「左列表 + 右聊天」双栏，聊天在右栏里，不整页盖住列表（见 §4.10）。
- 顶栏返回 / 展开按钮要有**按钮背景**（`FilledTonalIconButton`，40dp，与输入区左右按钮同尺寸）；图标 20dp。

### 4.4 打开会话（标签页 / 卷帘）

- 「打开的会话」= 整个 App 里**处于打开状态**的会话，不是「正在运行」。`openSessions` 最多 20 个。
- **只能手动移除**：只有点 × 或删除会话才会移出；App 重启、刷新会话、运行结束都**不能**自动消失。`openSessions` 持久化到 SharedPreferences（`open_sessions`，每行一个 id），`refreshSessions` 后 `restoreOpenSessions()` 恢复；运行中 / 有后台 shell 不会自动塞进来（入口还是"点开这个会话"）。
- 展开是**卷帘动画**（从上往下 `expandVertically`），卡片占满可视宽度，左右边距和底部输入区对齐（6dp），点外面收起。
- 每一项右侧有 **×** 关闭该标签；关闭当前会话时自动切到下一个，没了就回列表。
- 卡片头部有总数量和一个 **×**，表示退出多会话切换状态（关掉全部打开的会话回到列表）。
- 返回按钮只离开对话、不关闭标签；标签要通过 × 关。

### 4.5 输入区与按钮

- 左右按钮不要太大（当前 40dp 圆形「+」、42dp 发送/停止），否则输入框太窄、不够紧凑；左右边距 6dp。
- 输入框**多行**（`maxLines = 5`）：换行时能显示多行、光标可在行间移动，不要只显示第一行。用自绘的 `Surface + BasicTextField`（内边距 10/4/6dp），不要用 `OutlinedTextField` 的默认大内边距。
- 清空输入的 ✕ 放在**输入框右上角**（24dp 点击区），点了先弹确认「清空输入？」再清空（只清文字，不动附件），不要直接清。
- 右下角按钮始终显示：有文字/图片 → 发送（主题色箭头）；运行中空输入 → 停止（红色方块，确认后中断）；空闲空输入 → 灰色不可用箭头（不要隐藏，也不要换成叉）。
- 客户端命令**必须带斜杠**（`/compact`、`/interrupt`、`/model`、`/agent`、`/reload`）；裸写的 `compact` 等一律当普通文本发送，避免误触发。compact 时 Toast 提示「正在压缩上下文…」，compaction 事件驱动 LiveBubble 的压缩状态。
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
  - 卡片出现/消失统一走 `CardMotion`（见下方），详情卡见 `rememberOverlayProgress`。
- 精简模式工具行右侧的命中率要**右对齐、固定宽度**（当前 48dp，`TextAlign.End`），不同行要对齐。
- 流式思考框：卡片里面带头部（图标 +「思考」+ 进行中/完成 + 字数），内容最多 16 行 + 右下角「展开」弹窗；不要固定高度/内嵌滚动，也不要把标题放在卡片外面。
- 工具多行输出（shell / diff / write / read）：统一 `TOOL_PREVIEW_LINES = 20` 行阈值，超出在右下角嵌一个「展开」按钮（按钮右下圆角=卡片圆角、左上切角，不要用渐变阴影），点开工具详情弹窗；正文里不要塞滚动条，滚动效果交给系统原生 overscroll。
- 工具卡片信息去重：`read` 路径只显示一次（副标题 + 行号范围），正文去掉 `Read file …` 头；`edit` 显示路径 + `+N −M`；`write` 显示路径 + 行数。
- 工具/思考卡片整张都可以点开详情弹窗（「展开」按钮只是额外提示）；**不要用系统 `Dialog` 窗口**（厂商 ROM 会套一层「从角落飘出来」的窗口动画，Compose 关不掉），一律用 `DetailOverlay`（画在当前组合里的全屏遮罩）：遮罩和卡片由同一个进度驱动（150ms 淡入 / 110ms 淡出 + 极小缩放），没有位移；点遮罩或返回键关闭，打开时顺带收起键盘。弹窗内边距要小（10dp 左右）。
- 详情卡内容每次重组都从**最新消息 / `model.liveReasoning`** 取，不是点开那一刻的快照：运行中的工具输出、还在增长的思考内容都要在弹窗里继续更新（流式思考自动滚到底，用户往上翻过就别抢）。
- 显示模式三档：**精简（compact）** / **适中（full）** / **完整（expanded）**。
  - 精简：工具一行、思考一行，点开看详情。
  - 适中：工具/思考预览 `TOOL_PREVIEW_LINES = 20` 行（思考 16 行），超出右下角「展开」。
  - 完整：全部平铺，不做任何 clamp/展开，卡片自然变高，内容完整。
- 卡片内边距尽量小（工具/思考卡片约 8dp 横向、4dp 纵向，外面 2dp 间距）；详情弹窗左/右/下 8dp、上 14~16dp，内部 8~10dp，可滚动内容再留 5dp。
- 长按用户消息的弹窗里除了回退，还要有「复制」（复制到系统剪贴板并提示「已复制」）。
- 展开详情卡的内容高度统一用 `rememberDialogContentHeight()`（上限 = 屏幕高 − 2×设备 R 角 − 120dp，下限 = 上限 × 0.62），工具/思考共用；不要再写死 680dp。
- 内部内容要有视觉突出：思考卡左侧加 primary 竖条（`LeftAccent`），shell 输出左侧加 Sky 竖条；详情弹窗里的 shell 输出用 `ShellBlock`（自带左侧亮条），read/write 保持代码块样式。
- 详情弹窗**不要底部操作条**（不要「关闭 / 完成」横条），内容占满；左上角标题前放 32dp 的返回按钮，配合系统返回手势。
- diff 每行 `fontSize = 10.sp` 时必须显式 `lineHeight = 12.sp`（默认 body 行高会虚高），行间 padding 0.5dp。详情弹窗的圆角只加在**外层卡片**（工具详情 16dp、AppDialog 18dp，不用设备 R 角），内部滚动内容不要再单独裁 R 角。
- Markdown：自己实现的渲染器要支持标题 `#`、列表（`-`/`*`/`+`/数字，显示为 • / 数字）、引用 `>`、分隔线 `---`、竖线表格（带网格线），以及行内 code/粗体/斜体/链接；最终回答和思考详情都要用 `MarkdownText`。展开后的字号要和卡片预览一致（工具 10.5sp；思考详情用 compact Markdown 11sp），别放大导致要滚很多屏。
- 精简模式也要有一行「思考」（图标 + 思考 + 字数 + 展开），点开看完整思考。
- 卡片出现/消失统一用 **`Components.kt` 的 `CardMotion`**：淡入 110ms + 极小缩放（0.985 → 1）、淡出 90ms，**没有位移**；消息 item 用 `animateItem(fadeInSpec = CardMotion.FadeIn, placementSpec = null, fadeOutSpec = CardMotion.FadeOut)`。不要在其他地方另写动画参数。
- 消息合并必须 `collapseMessages(page.messages).asReversed()` 后再拼接，**不要 `sortedBy { created }`**；否则队列消息的入队时间会把上一轮工具挤到 pending 后面。
- 消息 item 不要 `placementSpec`（位置动画会抖）；工具/思考/正文只能跟着 `CardMotion` 淡入淡出，不允许 `slideIn`/`expandVertically` 这类位移或高度动画。
- 详情层（`DetailOverlay` / `SideDetailPane`）的遮罩和卡片必须由**同一个 `Animatable` 进度**驱动（`rememberOverlayProgress`）：遮罩 `alpha = 0.42f * progress`，卡片 `alpha/scale` 也跟 progress；禁止一上来就把遮罩铺满（会“闪一下”）。
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
- 设置里的「默认模型」**必须真正落到服务端会话**：新建会话后调 `POST /api/session/{id}/model`，再读回 `/api/session/{id}` 以服务端返回为准；写失败要在 `lastError` 里提示。只改本地 `copy(model=...)` 会在重连或换客户端后退回服务端默认。
- 登录/断开后的连接页必须使用当前主题（跟随系统 / 浅色 / 深色），根布局要显式铺 `MaterialTheme.colorScheme.background`，
  否则 edge-to-edge 会露出黑色窗口背景。
- 外观设置 / 主题色：横向色卡（圆形色块 + 短标签），不要用竖排 OptionRow；外观三张卡（跟随系统=黑白渐变、浅色=白、深色=黑），
  主题色至少 blue/purple/cyan/green/orange/pink/red/teal + 黑白 + 系统取色；点一下立即生效。
- 卡片/顶栏/底栏的背景要有层次：浅色下 background 用灰白、surfaceVariant 要明显深一点；深色下 surface 比 background 亮、surfaceVariant 更亮。
- opencode 的 question/form：语义照官方 `packages/app/src/session/requests/question-machine.ts`：**单选** `pick(value)` = `selected=[value]` 且关掉自定义（再点同一项不取消）；**输入自定义文字** = 清空单选并置 `customSelected`；**多选** 是和自定义共存的 toggle。选项必须画出选中标记（单选圆点 / 多选方块打勾）+ `primaryContainer` 底色 + primary 边框。
- 表单状态**必须用可观察容器**：`mutableStateMapOf<String, Set<String>>`（每次点击整体替换 Set）或 `mutableStateOf`；普通 `MutableList` 改了不触发重组（“点了没反应”），`SnapshotStateList` 在 `getOrPut` 里也容易写出状态错乱（两个同时亮）。
- 单选项和自定义输入**双向互斥**：点选项 → 清空自定义文字 + `focusManager.clearFocus()`；点输入框 → 立即取消单选。
  - 不能用 `onFocusChanged` 做这件事：输入框已经聚焦时再点不会再触发，必须用 `interactionSource` 收 `PressInteraction.Press`。
  - 自定义输入永远 `enabled = true`。
- 底部必须有显眼的「提交」按钮（用 `Button`，不要只靠 TextButton），并处理 IME 遮挡（`imePadding`）。
- 外观设置（跟随系统/浅色/深色）对**所有页面**生效，包括断开后的登录页。

### 4.8 后台 shell

- 悬浮球只显示**当前会话自己的**后台 shell（`Shell.Info.metadata.sessionID == current.id`）。
- `/api/shell`、`/api/shell/{id}/output`、`DELETE /api/shell/{id}` 都是 **location 作用域**，必须带 `location[directory]=<当前会话目录>`；不带会去查根目录，导致悬浮球看不到当前会话的 shell。
- 别的会话在跑 shell 时，当前对话不显示悬浮球。
- 悬浮球（收起态）：46dp 圆球，可拖动；**球的位置和展开的卡片无关**，球只是“打开卡片”的按钮。
- 展开态必须和工具/文件/思考详情**完全一致**：
  - 用 `DetailOverlay`（卡片居中、背景渐暗、点外面/返回键关闭，同一个进度驱动动画）。
  - **全部 shell 直接全展开**：没有「查看/收起」、没有单条折叠；卡片内部整体滚动，输出每 1.5s 刷新。
  - 每条 shell 最左边是**亮色竖条**（primary/tertiary/secondary/error 循环）用来区分多条；旁边是已运行时长（`12s` / `3m05s` / `1h02m`）+ 命令（同色等宽字）+ 停止按钮；下面是全宽输出块（等宽 11sp/行高 15sp）。
  - 卡片宽度 `fillMaxWidth` 限 560dp，高度最多屏幕 72%，关闭按钮在标题行右侧（X），不是「收起」。
  - 悬浮球要挂在 **Scaffold 外面**（整屏层），否则卡片只能居中在消息区。
- 原版 Ctrl+B：`POST /api/session/{id}/background` 把正在阻塞的 shell 移到后台。
  - 精简模式：在运行中的 shell 那一行右侧显示「后台」。
  - 适中/完整模式：在工具卡片标题右侧显示「后台」。
  - **有 Pending 时自动调用一次**（`loadInbox` / `loadMessages` 后检查，2.5s 去抖），让队列能继续。

### 4.9 缓存页性能

- 会话缓存详情用内存缓存（最多 12 个会话）：第二次打开直接显示，后台再刷新。
- 只拉最新 100 条消息算 steps；总量/命中率用服务端 `session.tokens`，成本用 `session.cost`，不要为了统计拉全历史。
- `stepsOf` / `SessionCache` 计算放 `Dispatchers.Default`，不要在主线程做。
- 缓存页**没有刷新按钮**：打开页面时自动加载/刷新。

### 4.10 屏幕自适应（竖屏 / 横屏）

- 判定用 `rememberIsWideLayout()`（`screenWidthDp >= 600`，横屏手机/平板），不要用 orientation。
- 宽屏外壳：左侧 **64dp rail**（图标 + 9sp 标签，选中用 `primaryContainer` 圆角块）代替底部导航，只占宽度不占高度。
  - `navigationBarsPadding()` 要加在 `.width(64.dp)` **之前**，否则横屏三键导航会把 rail 挤没。
- **没有全局顶栏**：不要每页再放「标题 + IP + 版本」那行（用户明确嫌多余）。页面顶部只靠 `statusBarsPadding()` 避让状态栏。
- 宽屏内容根节点必须 `Surface(color = background, contentColor = onBackground)`，否则各页标题会是默认黑色（深色主题下看不见）。
- 会话页宽屏 = master-detail：左 300dp 会话列表 + 1dp 分隔线 + 右聊天（`ChatScreen(wide = true)`）；没选会话时右栏显示「从左边选一个会话」。
- 聊天详情宽屏用 `SideDetailPane`（右侧 380dp supporting pane，聊天保持可见），竖屏用 `DetailOverlay`。
- 文件页宽屏 = 左 320dp 列表 + 右预览，选文件不再把列表顶掉。
- 竖屏保持底部导航 + 全屏子页；聊天顶栏 46dp。
- 所有页面**不要刷新按钮**：`MainScreen` 的 `LaunchedEffect(tab)` 进入页面自动刷新（会话/目录/PTY），回到前台由 `refreshOnResume()` 统一刷新。

## 5. 与服务端的交互（效率）

官方前端（`~/opencode/packages/client/src/solid/data.ts`、`packages/app/src/session/timeline/*`）的做法，照着做：

- 首屏：`GET /api/session/{id}/message?limit=20~40&order=desc`，取最新一页，记录 `cursor.next`。
- 往更早翻：只带 `cursor`（`?limit=...&cursor=...`），不要和 `order` 混用；每页 20~40 条，滚动到顶部附近再加载。
- 刷新：事件到来时**只重新拉最新一页**（40 条），按 id 合并进现有列表；不要每次全量重拉整个会话。
- 同一 key 的事件用去抖（当前 `scheduleReload` 450ms），一次 burst 只发一次请求。
- 切换会话：用内存缓存（已加载消息 + cursor），切回来先显示缓存再静默刷新最新页；缓存最多 8 个会话。
- 完整历史（缓存统计等）才用 `allMessages()` 循环 cursor 拉全量。
- 消息接口**新的在前**，按 `time.created` 升序再渲染。
- 从后台 / 锁屏回到前台：SSE 可能断了一阵，界面上的「正在思考 / 第 N 步」是旧事件攒的。`AppModel.refreshOnResume()`（挂在 `ON_RESUME`）强制拉一次 `/api/session/active` + 最新一页消息 + inbox + shells；会话已经不在 active 里就清掉 `sending / liveText / liveReasoning / statusLine`。

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
client/src/main/java/com/a2heng/opencode/mobile/   # 可复用库（Adia 也依赖）
├── OpenCodeRoot.kt            # OpenCodeMobileRoot / OpenCodeMobileHost
├── ConnectionService.kt       # 前台保活通知
├── data/
│   ├── OcClient.kt            # HTTP/SSE/PTY/命令/分页消息/文件/shell
│   ├── Models.kt              # JSON 宽松解析、缓存统计、Attachment/ShellInfo/MessagesPage
│   ├── AppModel.kt            # 全局状态、事件、队列、重连、消息缓存与分页、openSessions 持久化
│   └── UpdateClient.kt        # GitHub Release 检查/下载
└── ui/
    ├── AppRoot.kt             # 主框架：竖屏底部导航 / 横屏 rail + 会话双栏 + 各页外壳（无全局顶栏）
    ├── ChatScreen.kt          # 对话、标题、输入区、工具卡片、打开会话卷帘、表单
    ├── SessionCacheScreen.kt  # 单会话缓存页
    ├── SessionsScreen.kt      # 会话列表（命中率在左、三点在右、右上角圆形 +）
    ├── FilesScreen.kt         # 文件：宽屏左列表 + 右预览
    ├── TerminalScreen.kt      # PTY 列表 / 终端
    ├── SettingsScreen.kt      # 默认模型/外观/主题/发送模式/显示/更新
    ├── FloatingShells.kt      # 后台 shell 悬浮球
    └── Markdown.kt / ToolViews.kt / ModelCatalog.kt / AppDialog.kt / Components.kt / PickerPage.kt
app/src/main/java/com/a2heng/opencode/mobile/
└── MainActivity.kt            # 独立 APK 薄壳：启动画面、分享入口、通知权限
```

- `:client` 是 **library**（namespace `com.a2heng.opencode.mobile`），`:app` 的
  namespace 是 `com.a2heng.opencode.mobile.app`，两者不能相同。
- 库里的代码不要引用宿主的 Activity/类；需要宿主入口就用
  `OpenCodeMobileHost.show(activity, url, password)`。
- ConnectionService 的通知点击用 `packageManager.getLaunchIntentForPackage`，
  不能直接引用 MainActivity。

## 8. 已知约束

- debug 与 release 签名不同，不能互相覆盖；换装需先卸载（会丢 App 内保存的服务器/密码）。
- 不修改 `~/.local/share/opencode` 的数据；App 只读服务端 API。
- 不在真机上随手点、截屏；真机验证让用户自己看（模拟器可以自己截图看）。
- 每次改动同步更新 `docs/API-NOTES.md` 和本文件的「用户偏好」部分，保持文档是活的。

### 4.11 终端（PTY）

- 渲染用 **`org.connectbot:termlib:0.3.14`**（Apache-2.0，libvterm + Compose，**不要换 Termux：GPL**）。
  - `TerminalEmulatorFactory.create(...)`；PTY 字节直接 `writeInput(bytes)`，不要再自己 `stripAnsi`。
  - 用户键盘输入走 `onKeyboardInput` 回调 → WebSocket。
- **PTY resize 必须 `PUT /api/pty/{id}`**（`{size:{rows,cols}}`）；用 `PATCH` 会 404、异常被吞，表现就是「右边一片空白但换行」。
- 尺寸来自视图：`onResize` 回调把 rows/cols 发给服务端；不要写死 `40x100`。
- Activity `windowSoftInputMode=adjustNothing`：键盘不改变窗口；终端尺寸固定。
- 占位：**固定比例**（竖屏底部 42% 高、横屏右侧 34% 宽），不要跟着键盘高度变化；按钮固定在占位区边缘。
- 按钮：竖屏在留白区上沿（分隔线下面）、横屏在右侧留白区顶部；两行：
  - 左下：Shift 在上，Ctrl/Alt 在下；开=主题色边框+主题色文字（加粗），关=普通灰。
  - 中间：Tab / ^C / Esc，回车 / A− / A+。
  - 右侧：Home/End，隔一个按键宽度后是独立十字方向键（↑ 上，← ↓ → 下）。
  - 回车：点=CR，长按=LF（震动）；Ctrl/Shift/Alt 影响方向键（xterm modifier）和 Shift+Tab。
- 字体：打包 `assets/fonts/SarasaFixedSC-Regular.ttf`（OFL，GB2312 子集，2.1MB，拉丁 0.5em/中文 1em）；加载失败回退 `DroidSansMono.ttf`。
- 字号 5–26sp，存 SharedPreferences（`pty_font_size`）；换字号用 `key(fontSize)` 重建视图（emulator 屏幕缓冲不丢）。
- **本机 Termux 终端（Adia）**：宿主通过 `LocalShellHost.spawner` 注入 `LocalRuntime.localShell()`，用 proot + `prefix/bin/script` 开真 PTY。
  - `PtyList(onOpenLocal = ...)` 的 lambda **不要写成 `({ { ... } })`**：那会变成「返回 lambda 的 lambda」，点「打开」什么也不发生（编译只给 `Lambda expression is never invoked` 警告）。用显式的 `val openLocal: (() -> Unit)? = if (...) { { ... } } else null`。
  - spawn 在 `Dispatchers.IO` 做（proot/toolbox 检查很慢）；`script` 不存在时先从 APK 里的 `util-linux.deb` + 依赖补装，仍失败就退回 `bash -i -l`。
  - `script` 若在 350ms 内退出（缺库/没有 `/dev/ptmx`），宿主自动改开无 PTY 的 bash，避免终端一片黑。
- **服务端 PTY**（独立 APK 连电脑）仍按上面的 `/api/pty` 流程；Android 上 bun-pty 起不来时用本机终端。

### 4.12 文件

- 预设目录用**胶囊**（无图标、自动换行、最多约 5 行后滚动）：`~`、`/`、`/tmp/opencode`、当前会话目录、最近项目。
- **`~` = 服务端 HOME**（`GET /api/location` 的 directory），**`/` = 真实文件系统根**。不要再造 rootfs/symlink 虚拟根：opencode v2 的 `readDirectoryEntries` 把 symlink 归为 `SymbolicLink`，服务端会直接过滤掉，虚拟根永远是空的。
- 目录显示用 `pathLabel(base, fsDir)`：`fsDir` 是当前目录，`base` 是宿主的 `fsRootOverride`（没有就是 `/`）；**不要**用服务端每次返回的 `location` 当根，否则路径永远显示 `/`、上一级立刻失效。
- 列目录返回的 `entry.path` 相对**当前目录**，拼绝对路径要用 `fsDir`（不是固定根）。
- 目录行右侧固定两个图标按钮：**上一级**（`ArrowUpward`，到根目录时禁用）和**新建文件夹**（`CreateNewFolder`，弹名字对话框，走服务端 shell `mkdir -p`，建完自动刷新）。
- 新建会话对话框：上一级的根固定为真实 `/`（不随服务端返回的 location 变化），`~` = HOME，`/` 胶囊 = `HOME/workspace`（真 Android 根不能当项目，会卡住选择器）。`~` 里的最近目录仍可正常列出；确认时 `expandSessionDir` 会把 `/` 兜底成 workspace。`browseDirs` 只展开 `~`，不再劫持 `/`。
- `/api/fs/list`、`/api/fs/read` 都是 **location 作用域**：列目录用 `location[directory]=<绝对目录>`，读文件用 `location[directory]=<父目录>` + 文件名（不要用 `..`，会被 URL 规范化掉）。`list` 允许列 location 之外的绝对目录，`read` 仍用「父目录当 location」来绕开限制。
- 文本解码：严格 UTF-8 → GB18030 回退；前 8KB 有 NUL 当二进制；图片走 BitmapFactory；`.md` 用 MarkdownText。
- 对话正文里的绝对路径可点开文件预览卡（`DetailOverlay` + `FilePreviewCard`）。
