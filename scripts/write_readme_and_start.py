from pathlib import Path

Path("E:/code/nexa/scripts/start-dev.sh").write_text(
    """#!/usr/bin/env bash
# Minimal nexa: core (all business) + iam (auth). Agent optional.
set -euo pipefail
export PATH=\"/e/tools/go/bin:${PATH:-}\"
export GOTOOLCHAIN=local
ROOT=\"$(cd \"$(dirname \"$0\")/..\" && pwd)\"
BIN=\"${NEXA_BIN_DIR:-/tmp}\"
LOGDIR=\"${NEXA_LOG_DIR:-$ROOT/.run/logs}\"
PIDDIR=\"${NEXA_PID_DIR:-$ROOT/.run/pids}\"
mkdir -p \"$LOGDIR\" \"$PIDDIR\" \"$ROOT/.run/data/core\" \"$ROOT/.run/data/iam\" \"$ROOT/.run/configs\"

echo \"[build] iam\"
(cd \"$ROOT/services/iam\" && go build -o \"$BIN/nexa-iam.exe\" ./cmd/nexa-iam)
echo \"[build] core\"
(cd \"$ROOT/services/core\" && go build -o \"$BIN/nexa-core.exe\" ./cmd/nexa-core)

cat > \"$ROOT/.run/configs/iam.json\" <<JSON
{\"name\":\"nexa-iam\",\"http\":{\"addr\":\":48081\"},\"dataDir\":\"$ROOT/.run/data/iam\"}
JSON
cat > \"$ROOT/.run/configs/core.json\" <<JSON
{\"name\":\"nexa-core\",\"http\":{\"addr\":\":48080\"},\"dataDir\":\"$ROOT/.run/data/core\",\"iamUrl\":\"http://127.0.0.1:48081\",\"agentUrl\":\"http://127.0.0.1:48091\",\"auth\":{\"enabled\":true}}
JSON

start_one() {
  local name=\"$1\" bin=\"$2\" conf=\"$3\"
  local pidfile=\"$PIDDIR/$name.pid\"
  if [[ -f \"$pidfile\" ]] && kill -0 \"$(cat \"$pidfile\")\" 2>/dev/null; then
    echo \"[skip] $name running\"
    return
  fi
  echo \"[start] $name\"
  nohup \"$bin\" -config \"$conf\" >\"$LOGDIR/$name.log\" 2>&1 &
  echo $! >\"$pidfile\"
}

start_one iam \"$BIN/nexa-iam.exe\" \"$ROOT/.run/configs/iam.json\"
sleep 0.3
start_one core \"$BIN/nexa-core.exe\" \"$ROOT/.run/configs/core.json\"
sleep 0.8
echo \"[health]\"
for port in 48081 48080; do
  code=$(curl -s -o /dev/null -w \"%{http_code}\" \"http://127.0.0.1:$port/healthz\" || true)
  echo \"  :$port -> $code\"
done
echo \"processes: iam:48081 + core:48080 (all business merged)\"
echo \"optional agent: cd services/agent && AGENT_USE_MOCK=true npm run dev\"
echo \"stop: $ROOT/scripts/stop-dev.sh\"
""",
    encoding="utf-8",
    newline="\n",
)

Path("E:/code/nexa/README.md").write_text(
    """<p align="center">
  <img src="assets/logo.svg" width="128" height="128" alt="nexa logo"/>
</p>

<h1 align="center">nexa</h1>

<p align="center">
  <b>可接入的企业钉钉</b> · 开源企业协作 + 智能助手<br/>
  <sub>不是「旧 OA 对接钉钉」—— nexa 本身就是协作本体</sub>
</p>

<p align="center">
  <a href="docs/PRODUCT.md">Product</a> ·
  <a href="docs/GOAL.md">Roadmap</a> ·
  <a href="docs/api/README.md">API</a> ·
  <a href="deploy/README.md">Deploy</a>
</p>

---

## Why nexa

| 旧路 | nexa |
|------|------|
| OA + 钉钉双系统同步 | **一个产品本体** |
| 只能自用 | **企业注册接入（多租户）** |
| 十几个微服务端口 | **core 合并业务进程** |
| 聊天套壳 | **Skill / 感知 / 自动化** |

```
企业注册租户 ──► nexa ──► Agent
                  │
         组织·审批·待办·IM·工作台
                  │
         连接器：业务库 / 钉钉导入（可选）
```

## Architecture (simple)

```
┌─────────────┐   ┌──────────────┐   ┌─────────────┐
│ nexa-core   │   │ nexa-iam     │   │ nexa-agent  │
│ :48080      │◄──┤ :48081       │   │ :48091      │
│ gateway +   │   │ auth/tenant  │   │ NeoX (Node) │
│ all business│   └──────────────┘   └─────────────┘
└─────────────┘
 optional: cdc-mysql
```

**默认只跑 2 个 Go 进程 + 可选 Agent**，不浪费机器。

| Process | Port | Role |
|---------|------|------|
| **nexa-core** | 48080 | 网关 + HR/BPM/Business/ERP/Finance/IM/OP/AI/DataCenter |
| **nexa-iam** | 48081 | 登录 / 租户注册邀请（认证独立） |
| **nexa-agent** | 48091 | 对话大脑（NeoX，可选） |
| cdc-mysql | 6060 | 可选 CDC |

拆分服务源码仍在 `services/{hr,bpm,...}` 作对照；**部署优先 `services/core`**。

## Quick start

```bash
export PATH="/e/tools/go/bin:$PATH"
export GOTOOLCHAIN=local

./scripts/start-dev.sh
# :48081 iam  +  :48080 core
```

### Open a company

```bash
curl -s -X POST http://127.0.0.1:48080/v1/iam/tenants/register \\
  -H "Content-Type: application/json" \\
  -d '{"company":"Acme","adminUsername":"acme_admin","password":"pass123"}'

curl -s -X POST http://127.0.0.1:48080/v1/iam/login \\
  -H "Content-Type: application/json" \\
  -d '{"username":"acme_admin","password":"pass123"}'
```

### Agent (optional)

```bash
cd services/agent && cp .env.example .env
npm install && AGENT_USE_MOCK=true npm run dev
```

### Mobile

```bash
cd apps/mobile
flutter pub get && flutter run
```

## API (via :48080)

| | |
|--|--|
| 注册企业 | `POST /v1/iam/tenants/register` |
| 登录 | `POST /v1/iam/login` |
| 邀请/加入 | `POST /v1/iam/invites` · `/invites/accept` |
| 技能 | `GET /v1/ai/skills` |
| 连接器 | `GET /v1/ai/connectors` |
| 审批待办 | `GET /v1/bpm/tasks/todo` |
| 员工 | `GET /v1/hr/employees` |
| 导出 | `GET /v1/data-center/templates` |

业务路由：`Authorization: Bearer <token>`。

## Layout

```
nexa/
├── assets/logo.svg       # brand
├── apps/mobile           # Flutter
├── services/
│   ├── core/             # ★ merged business + gateway
│   ├── iam/              # ★ auth & tenant
│   ├── agent/            # NeoX
│   ├── cdc-mysql/        # optional
│   └── {hr,bpm,...}/     # reference only
├── docs/PRODUCT.md
└── scripts/start-dev.sh
```

## Env

| Var | Use |
|-----|-----|
| `NEXA_GATEWAY_URL` | Agent → core |
| `NEXA_DATA_DIR` | JSON data |
| `NEXA_DINGTALK_APP_KEY` | optional import |
| `GOTOOLCHAIN=local` | local Go |

## Links

- https://github.com/lmk1010/nexa
- CDC: https://github.com/lmk1010/nexa-cdc-mysql
- Product: [docs/PRODUCT.md](docs/PRODUCT.md)

## License

TBD (CDC MIT).
""",
    encoding="utf-8",
)

g = Path("E:/code/nexa/docs/GOAL.md")
gt = g.read_text(encoding="utf-8")
if "nexa-core 合并业务" not in gt:
    g.write_text(
        gt.rstrip()
        + "\n| 2026-07-20 | nexa-core 合并业务端口；logo+GitHub README；start-dev 仅 iam+core |\n",
        encoding="utf-8",
    )

p = Path("E:/code/nexa/docs/PRODUCT.md")
pt = p.read_text(encoding="utf-8")
if "nexa-core" not in pt:
    p.write_text(
        pt
        + "\n## 部署形态（省资源）\n\n- **nexa-core** 单端口承载全部业务域 + 网关\n- **nexa-iam** 独立认证/租户\n- **nexa-agent** 独立对话\n",
        encoding="utf-8",
    )

print("files written")
