# KYX Agent Service

独立 Node.js agent 服务，运行时依赖 `@mk-co/neox-sdk`，供 App/Admin 通过 gateway 调用。

## SDK 判断

- 当前可用包是 `@mk-co/neox-sdk@0.4.1`，要求 Node.js 20+。
- `Agent.run()` 和 `Agent.stream()` 已经包含 agent loop、tool calling、stream event。
- 业务服务不需要直接依赖 `@mk-co/neox-core` 或 `@mk-co/neox-kernel`。SDK 包内部已经封装运行入口；如果后续 SDK 将 kernel 拆成公开运行依赖，再补 npm registry/package 配置。
- SDK 的类型声明里引用了 kernel 类型包，但运行时不需要安装 kernel。因此本服务先使用 ESM JavaScript，避免 TypeScript 编译被未公开类型阻塞。

## Local run

```bash
cd backend/kyx-service-agent
npm install
AGENT_USE_MOCK=true npm run dev
```

```bash
curl http://localhost:48091/health
curl -X POST http://localhost:48091/run \
  -H 'Content-Type: application/json' \
  -d '{"message":"ping"}'
```

## Gateway paths

Gateway rewrites:

- `/app-api/agent/run` -> `http://localhost:48091/run`
- `/app-api/agent/stream` -> `http://localhost:48091/stream`
- `/admin-api/agent/run` -> `http://localhost:48091/run`
- `/admin-api/agent/stream` -> `http://localhost:48091/stream`

`TokenAuthenticationFilter` adds `login-user` and forwards `tenant-id`; this service parses those headers into request context and can pass them to internal tools.

## Business tools

When `AGENT_ENABLE_INTERNAL_HTTP_TOOLS=true`, the agent uses a docs-first API access model:

- `get_request_context`: inspect sanitized request context when the agent needs current user/tenant awareness.
- `tool_search`: search KYX API categories, recommended workflows, endpoint paths, query parameters, enum notes, response shapes, and usage hints.
- `oa_read`: execute a whitelisted read-only KYX HTTP API returned by `tool_search`.
- `ordersys_read`: execute a whitelisted read-only ordersys API returned by `tool_search`.
- `fetch_url`: fetch public web documents when the user asks about external public content.

The intended flow is `tool_search -> oa_read/ordersys_read`: first find the function/API documentation, then call the selected endpoint. Do not add one custom tool per business feature; add or generate API index entries and category workflows instead.

Historical `executive_cockpit_overview` and `executive_cockpit_chat` shortcut tools are no longer exposed. Executive cockpit remains available through normal API discovery and `oa_read`.

Node.js does not read OA business tables directly. All business data still comes from Java gateway APIs, so Java authentication, `@PreAuthorize`, tenant scope, and data-scope checks remain authoritative.

## Generated API index

The agent packages a generated safe-read API index at `generated/kyx-api-index.json`.

Generate it from Java controller source:

```bash
cd backend/kyx-service-agent
npm run generate:api-index
```

The generator scans KYDev Java controllers and extracts:

- `@RequestMapping` + `@GetMapping`
- `@Operation(summary=...)`
- `@Parameter(...)`
- request VO fields annotated with `@Schema`
- `@PreAuthorize` expressions

Safety policy:

- Include only `GET` APIs under `/admin-api/**` or `/app-api/**`.
- Exclude write/approval/auth/token/file/import/export/upload/download/open/public/callback style paths and summaries.
- Exclude sensitive reads such as `api-key`, OAuth, token, password, captcha, and callback endpoints.

Runtime behavior:

- `tool_search` searches the generated index plus curated category/workflow overlays.
- `oa_read` derives its path whitelist from the same merged index.
- Curated entries override generated docs for high-value flows where source annotations are not enough, such as resolving HR `profileId` before querying attendance.

The deploy script runs `npm run generate:api-index` before building the Docker image, and the Dockerfile copies `generated/` into `/app/generated`.

## Service account auth

Agent tools prefer the user token forwarded by gateway. If a request has no `Authorization` header, the service can use a configured OA service account:

1. `POST /admin-api/system/auth/pre-login`
2. Select configured/default tenant
3. `POST /admin-api/system/auth/tenant-login`
4. Cache the short-lived `accessToken` in memory and refresh it before expiry

Do not configure a hard-coded long-lived access token. Put username/password in container env vars or the ignored local `.env` file only.

## API

### POST `/run`

Request:

```json
{
  "message": "帮我看一下总裁驾驶舱今天的关键指标",
  "conversationId": "optional",
  "model": "deepseek-chat",
  "maxSteps": 10
}
```

Response:

```json
{
  "code": 0,
  "data": {
    "text": "...",
    "usage": {
      "inputTokens": 0,
      "outputTokens": 0
    },
    "steps": [],
    "messages": [],
    "stopReason": "end_turn"
  },
  "msg": ""
}
```

### POST `/stream`

Server-Sent Events. Each SDK event is forwarded as:

```text
event: text_delta
data: {"type":"text_delta","delta":"..."}
```

## Environment

| Name | Default | Description |
| --- | --- | --- |
| `PORT` | `48091` | HTTP port |
| `AGENT_MODEL` | `deepseek-chat` | Default model id |
| `AGENT_PROVIDER` | `openai-compatible` | Provider type when `AGENT_API_KEY` is set |
| `AGENT_CONFIG_SOURCE` | `env` | `env` or `database`; database reads `ai_model` + `ai_api_key` |
| `AGENT_API_KEY` | empty | Explicit provider API key |
| `AGENT_BASE_URL` | empty | OpenAI-compatible base URL |
| `AGENT_TIMEOUT_MS` | `120000` | Provider timeout |
| `AGENT_MAX_STEPS` | `20` | Max agent loop steps |
| `AGENT_TOOL_SEARCH_MAX_CALLS_PER_RUN` | `2` | Per-run guard against repeated capability-directory searches |
| `AGENT_PERMISSION` | `auto` | SDK permission mode |
| `AGENT_SYSTEM_PROMPT` | built-in | Base system prompt |
| `AGENT_USE_MOCK` | `false` | Use SDK mock LLM for local smoke tests |
| `AGENT_MOCK_RESPONSE` | `ok` | Mock response text |
| `AGENT_STREAM_THINKING` | `false` | Whether SSE forwards SDK thinking events |
| `AGENT_ENABLE_INTERNAL_HTTP_TOOLS` | `false` | Enable KYX API search/read tools through Java gateway |
| `AGENT_ENABLE_RAW_INTERNAL_HTTP_TOOLS` | `false` | Enable raw internal GET/POST tools for debugging |
| `INTERNAL_API_BASE_URL` | `http://localhost:48080` | Gateway base URL for internal tools |
| `INTERNAL_API_ALLOW_PREFIXES` | `/admin-api/,/app-api/` | Allowed internal paths |
| `INTERNAL_API_MAX_CONCURRENT_REQUESTS` | `20` | Global concurrency limit for Java gateway tool calls |
| `INTERNAL_API_QUEUE_TIMEOUT_MS` | `10000` | Max time a tool call can wait for a concurrency slot |
| `AGENT_INTERNAL_API_MAX_TOOL_CALLS_PER_RUN` | `20` | Per agent run budget for Java gateway tool calls |
| `SERVICE_ACCOUNT_ENABLED` | `false` | Enable OA service-account fallback when no user token is forwarded |
| `SERVICE_ACCOUNT_BASE_URL` | `OA_API_BASE_URL`/`INTERNAL_API_BASE_URL` | OA gateway base URL for service-account login |
| `SERVICE_ACCOUNT_USERNAME` | empty | OA service-account username |
| `SERVICE_ACCOUNT_PASSWORD` | empty | OA service-account password |
| `SERVICE_ACCOUNT_TENANT_ID` | empty | Optional tenant id; production smoke test used tenant `171` |
| `SERVICE_ACCOUNT_DEVICE_TYPE` | `APP` | Device type for pre-login; `APP` skips Web image captcha in Java |
| `SERVICE_ACCOUNT_DEVICE_ID` | `kyx-agent-service` | Stable device id shown in login sessions |
| `SERVICE_ACCOUNT_REFRESH_SKEW_MS` | `300000` | Refresh access token this many ms before expiry |
| `SERVICE_ACCOUNT_TIMEOUT_MS` | `15000` | Timeout for service-account auth requests |
| `ORDERSYS_AUTH_MODE` | `direct` | Direct ordersys login mode |
| `ORDERSYS_API_BASE_URL` | `https://order.liantucn.com/api` | ordersys production API base |
| `ORDERSYS_SERVICE_ACCOUNT_USERNAME` | empty | Direct ordersys service-account username |
| `ORDERSYS_SERVICE_ACCOUNT_PASSWORD` | empty | Direct ordersys service-account password |
| `ORDERSYS_API_TIMEOUT_MS` | `12000` | Direct ordersys request timeout |
| `ORDERSYS_API_COOLDOWN_MS` | `60000` | Cooldown after slow/timeout ordersys endpoint |
| `ORDERSYS_API_PAGE_SIZE_MAX` | `30` | Global max page size for ordersys read tools |
| `ORDERSYS_MAX_TOOL_CALLS_PER_RUN` | `8` | Per-run ordersys read call budget |
| `AI_CONFIG_DB_HOST` | `localhost` | MySQL host for database-backed AI config |
| `AI_CONFIG_DB_PORT` | `3306` | MySQL port |
| `AI_CONFIG_DB_USER` | `kyx_user` | MySQL user |
| `AI_CONFIG_DB_PASSWORD` | empty | MySQL password |
| `AI_CONFIG_DB_DATABASE` | `kyx_oa` | MySQL database |
| `AI_CONFIG_TENANT_ID` | empty | Optional tenant filter for AI config |
| `AI_CONFIG_PLATFORM` | `Ark` | AI platform filter |
| `AI_CONFIG_MODEL_TYPE` | `1` | Chat model type |
| `AI_CONFIG_CACHE_MS` | `60000` | Database config cache duration |

When `AGENT_API_KEY` is not set, SDK `providerFromEnv()` can still infer provider from `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `DEEPSEEK_API_KEY`, or `KIMI_API_KEY`.

For KYX AI service database-backed config, use a local `.env` file or container env vars:

```bash
AGENT_CONFIG_SOURCE=database
AGENT_MODEL=deepseek-v4-pro-260425
AI_CONFIG_PLATFORM=Ark
AI_CONFIG_MODEL=deepseek-v4-pro-260425
AI_CONFIG_TENANT_ID=171
AI_CONFIG_DB_HOST=your-mysql-host
AI_CONFIG_DB_PORT=3306
AI_CONFIG_DB_USER=kyx_user
AI_CONFIG_DB_PASSWORD=your-password
AI_CONFIG_DB_DATABASE=kyx_oa
AGENT_ENABLE_INTERNAL_HTTP_TOOLS=true
INTERNAL_API_BASE_URL=http://localhost:48080
```

For production OA API smoke tests without a user token:

```bash
INTERNAL_API_BASE_URL=http://43.139.24.244
AGENT_ENABLE_INTERNAL_HTTP_TOOLS=true
SERVICE_ACCOUNT_ENABLED=true
SERVICE_ACCOUNT_BASE_URL=http://43.139.24.244
SERVICE_ACCOUNT_USERNAME=your-oa-service-account
SERVICE_ACCOUNT_PASSWORD=your-oa-password
SERVICE_ACCOUNT_TENANT_ID=171
SERVICE_ACCOUNT_DEVICE_TYPE=APP
SERVICE_ACCOUNT_DEVICE_ID=kyx-agent-service
```

## Docker

```bash
cd backend/kyx-service-agent
docker build -t kyx-service-agent:latest .
docker run --rm -p 48091:48091 \
  -e AGENT_USE_MOCK=true \
  kyx-service-agent:latest
```

If npm needs auth for the `@mk-co` scope, build with a secret `.npmrc`:

```bash
DOCKER_BUILDKIT=1 docker build \
  --secret id=npmrc,src=$HOME/.npmrc \
  -t kyx-service-agent:latest .
```
