# opencode v2 API 笔记（Android 客户端用到的部分）

实测版本：`opencode 2.2.5`。完整定义在 `GET /openapi.json`（`/doc` 是 Web UI，不是文档）。

## 认证

- Basic：用户名固定 `opencode`，密码是 `OPENCODE_PASSWORD`（没设密码时不带 `Authorization`）。
- 服务端需 `--hostname 0.0.0.0` 才能被手机访问。

## 会话

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/session` | 会话列表，`{ data: [...] }` |
| GET | `/api/session/active` | 真正在跑的会话：`{ data: { "ses_x": {"type":"running"} } }` |
| POST | `/api/session` | 新建，body 可带 `{ location: { directory: "/abs/path" } }` |
| GET | `/api/session/{id}` | 单个会话 |
| DELETE | `/api/session/{id}` | 删除 |
| PATCH | `/api/session/{id}` | 改名等（`{ title }`） |
| POST | `/api/session/{id}/model` | `{ model: { id, providerID } }` |
| POST | `/api/session/{id}/agent` | `{ agent: "build" }` |
| POST | `/api/session/{id}/interrupt` | 中断当前执行 |

**注意**

- `time.busy` 是历史时间戳，不代表「正在运行」；判断运行中要用 `/api/session/active`。
- 通过 API 新建的会话，`model` / `agent` 字段可能缺失，客户端应从最后一条 assistant 消息里补。
- `title` 在第一次回答后由服务端自动生成。

## 消息

`GET /api/session/{id}/message` → `{ data: [...], cursor }`。

> ⚠️ **返回顺序是「新的在前」**，客户端必须按 `time.created` 升序排一遍，否则聊天界面整个是倒的。

消息 `type`：`user` / `assistant` / `system` / `synthetic` / `shell` / `skill` / `compaction` / `idle` /
`agent-selected` / `model-selected` / `location-switched` 等。

assistant 的 `content` 是 part 数组：

- `{ type: "text", text }`
- `{ type: "reasoning", text }`
- `{ type: "tool", id, name, state: { status, input, content, error } }`
  - `status`: `streaming` / `running` / `completed` / `error`
  - `content` 里可能是 `{type:"text",text}` 或 `{type:"file",uri:"data:..."}`（图片 base64 很大，别整个塞进 UI）

tokens：`{ input, output, reasoning, cache: { read, write } }`；
命中率口径与 42003 看板一致：`cache.read / (input + cache.read)`。

## 发送消息 / 队列

`POST /api/session/{id}/prompt`

```json
{ "text": "...", "delivery": "queue" }
```

- `delivery`: `"steer"`（运行中直接插入）或 `"queue"`（空闲后处理）。**不传时由服务端决定**；
  客户端在会话运行中按设置选择，默认 `queue`。
- 队列查询：`GET /api/session/{id}/inbox`（`type: user` 的项就是待发送消息）
- 取消队列项：`DELETE /api/session/{id}/inbox/{inboxID}`

### 图片附件（v2.2.5 实测）

```json
{ "text": "看看这张图", "files": [{ "uri": "data:image/jpeg;base64,...", "mime": "image/jpeg", "name": "a.jpg" }] }
```

⚠️ **`openapi.json` 写的是 `files[].data` + `source: {type:"inline"}`，但 v2.2.5 实际只认 `files[].uri`**，
传 `data` 会 400：`Missing key at ["files"][0]["uri"]`。服务端返回的消息里又会把附件规范成
`{ data, mime, name }`，客户端渲染时按这个读。

## 事件流

`GET /api/event`（SSE）：

```
data: {"id":"evt_...","created":...,"type":"session.text.delta","data":{...}}

: heartbeat
```

用到的类型（前缀）：

- `server.connected`
- `session.text.started|delta|ended` — `data.delta` 是增量文本
- `session.reasoning.started|delta|ended`
- `session.tool.input.started|delta|ended`、`session.tool.called|progress|success|failed`
- `session.execution.started|succeeded|failed|interrupted`
- `session.step.started|ended|streamed`
- `session.updated|created|deleted`
- `session.inbox.enqueued|delivered|cancelled|delivery.changed`
- `permission.asked|replied|rejected`
- `session.form.created|cancelled` / `form.created`
- `session.compaction.started|ended`

实现建议：文本/推理用 delta 追加；工具类事件节流后整表重拉消息（简单且不会错位）。

## 权限 / 表单

| 方法 | 路径 |
| --- | --- |
| GET | `/api/permission/request` |
| POST | `/api/session/{id}/permission/{requestID}/reply`，body `{ decision: "once" \| "always" \| "reject" }` |
| GET | `/api/session/{id}/form` |
| POST | `/api/session/{id}/form/{formID}/reply`，body `{ answer: { key: value } }` |

表单字段类型：`string`（可有 options）/ `number` / `integer` / `boolean` / `multiselect` / `external`。

## 文件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/fs/list?path=/abs/dir` | `data[].path` **相对 location 根目录**，目录带结尾 `/` |
| GET | `/api/fs/read/{path}` | 绝对路径，URL 编码；返回文件内容 |

`GET /api/location` → `{ directory, project }`，用作「~」显示的根。

## 终端 PTY

1. `POST /api/pty`，body `{ command: "bash", cwd?, title? }` → `{ data: Pty }`
2. `POST /api/pty/{id}/connect-token`，**必须带请求头 `x-opencode-ticket: 1`**（否则 403
   `Invalid PTY connect token request`），返回 `{ data: { ticket, expires_in } }`
3. WebSocket：`ws://host/api/pty/{id}/connect?ticket=<ticket>`
   - 带 ticket 时跳过 Basic 认证
   - 服务端 → 客户端：原始 UTF-8 文本帧（终端输出）；二进制控制帧以 `0x00` 开头，后接 JSON `{cursor}`
   - 客户端 → 服务端：直接发文本（按键/输入）
4. `PUT /api/pty/{id}`，body `{ size: { rows, cols } }` 调整窗口大小
5. `DELETE /api/pty/{id}` 结束

## 其他

- `GET /api/info` → `{ version, urls, capabilities }`
- `GET /api/agent` / `GET /api/model` → `{ location, data: [...] }`；`model.limit.context` 是上下文窗口
- `GET /api/command` → `{ data: [{ name, description }] }`
- `POST /api/session/{id}/command`，body `{ name, text, files?, delivery? }`（`text` 是命令参数）
- `POST /api/shell`，body `{ command, cwd? }`，用于「新建文件夹」（`mkdir -p`）这类一次性命令。
