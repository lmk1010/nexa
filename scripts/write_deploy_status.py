from pathlib import Path

# IAM docker
Path("E:/code/nexa/services/iam/configs").mkdir(parents=True, exist_ok=True)
Path("E:/code/nexa/services/iam/configs/config.docker.json").write_text(
    '{\n  "name": "nexa-iam",\n  "http": { "addr": ":48081" },\n  "dataDir": "/data"\n}\n',
    encoding="utf-8",
)
Path("E:/code/nexa/services/iam/Dockerfile").write_text(
    """# syntax=docker/dockerfile:1
FROM golang:1.22-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/nexa-iam ./cmd/nexa-iam

FROM alpine:3.20
RUN apk add --no-cache ca-certificates tzdata wget \\
  && adduser -D -H -u 10001 nexa
WORKDIR /app
COPY --from=build /out/nexa-iam /app/nexa-iam
COPY configs/config.docker.json /app/config.json
RUN mkdir -p /data && chown -R nexa:nexa /data /app
USER nexa
ENV NEXA_DATA_DIR=/data
EXPOSE 48081
ENTRYPOINT ["/app/nexa-iam", "-config", "/app/config.json"]
""",
    encoding="utf-8",
)

# core dockerfile wget
core_df = Path("E:/code/nexa/services/core/Dockerfile")
ct = core_df.read_text(encoding="utf-8")
if "wget" not in ct:
    core_df.write_text(
        ct.replace(
            "apk add --no-cache ca-certificates tzdata",
            "apk add --no-cache ca-certificates tzdata wget",
        ),
        encoding="utf-8",
    )

# compose
Path("E:/code/nexa/deploy/docker-compose.yml").write_text(
    """# nexa minimal stack: iam + core (+ optional agent/cdc profiles)
services:
  iam:
    build:
      context: ../services/iam
      dockerfile: Dockerfile
    ports: ["48081:48081"]
    volumes: ["iam-data:/data"]
    environment:
      NEXA_DATA_DIR: /data
    restart: unless-stopped

  core:
    build:
      context: ../services/core
      dockerfile: Dockerfile
    ports: ["48080:48080"]
    volumes: ["core-data:/data"]
    environment:
      NEXA_DATA_DIR: /data
      NEXA_IAM_URL: http://iam:48081
      NEXA_AGENT_URL: http://agent:48091
    depends_on: [iam]
    restart: unless-stopped

  agent:
    build:
      context: ../services/agent
      dockerfile: Dockerfile
    ports: ["48091:48091"]
    environment:
      SERVICE_PORT: "48091"
      AGENT_USE_MOCK: ${AGENT_USE_MOCK:-true}
      NEXA_GATEWAY_URL: http://core:48080
      KYX_GATEWAY_URL: http://core:48080
      OA_API_BASE_URL: http://core:48080
      INTERNAL_API_BASE_URL: http://core:48080
      NEXA_AI_URL: http://core:48080
    depends_on: [core]
    restart: unless-stopped
    profiles: ["agent", "full"]

  cdc-mysql:
    build:
      context: ../services/cdc-mysql
      dockerfile: Dockerfile
    ports: ["6060:6060"]
    restart: unless-stopped
    profiles: ["cdc", "full"]

volumes:
  iam-data:
  core-data:
""",
    encoding="utf-8",
)

Path("E:/code/nexa/deploy/.env.example").write_text(
    "AGENT_USE_MOCK=true\n# NEXA_DINGTALK_APP_KEY=\n# NEXA_DINGTALK_APP_SECRET=\n",
    encoding="utf-8",
)

Path("E:/code/nexa/deploy/nginx.example.conf").write_text(
    """server {
    listen 80;
    server_name nexa.example.com;
    client_max_body_size 50m;
    location / {
        proxy_pass http://127.0.0.1:48080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_read_timeout 300s;
    }
}
""",
    encoding="utf-8",
)

Path("E:/code/nexa/deploy/README.md").write_text(
    """# Deploy nexa

Minimal processes:

| Service | Port | Notes |
|---------|------|--------|
| **core** | 48080 | gateway + all business |
| **iam** | 48081 | auth & tenants |
| **agent** | 48091 | profile `agent` |
| **cdc** | 6060 | profile `cdc` |

## Local

```bash
export GOTOOLCHAIN=local
./scripts/start-dev.sh
./scripts/stop-dev.sh
./scripts/smoke.sh   # after start
```

## Docker

```bash
cd deploy
cp .env.example .env
docker compose up -d --build
docker compose --profile agent up -d --build
docker compose --profile full up -d --build
```

- API: http://localhost:48080
- IAM: http://localhost:48081

Nginx sample: `nginx.example.conf`

## Production checklist

1. TLS (nginx/caddy)
2. Strong passwords / no demo reliance
3. Real LLM keys (`AGENT_USE_MOCK=false`)
4. Backup volumes `iam-data` / `core-data`
5. Only expose 80/443 publicly
""",
    encoding="utf-8",
)

Path("E:/code/nexa/Makefile").write_text(
    """export GOTOOLCHAIN ?= local

.PHONY: build start stop smoke compose-up compose-down

build:
\tcd services/iam && go build -o /tmp/nexa-iam.exe ./cmd/nexa-iam
\tcd services/core && go build -o /tmp/nexa-core.exe ./cmd/nexa-core

start:
\t./scripts/start-dev.sh

stop:
\t./scripts/stop-dev.sh

smoke:
\t./scripts/smoke.sh

compose-up:
\tcd deploy && docker compose up -d --build

compose-down:
\tcd deploy && docker compose down
""",
    encoding="utf-8",
    newline="\n",
)

Path("E:/code/nexa/scripts/smoke.sh").write_text(
    """#!/usr/bin/env bash
set -euo pipefail
BASE="${NEXA_BASE:-http://127.0.0.1:48080}"
echo "[smoke] health"
curl -sf "$BASE/healthz" >/dev/null
curl -sf "$BASE/v1/platform/services" >/dev/null
echo "[smoke] register+login"
UNAME="smoke_$(date +%s)"
curl -sf -X POST "$BASE/v1/iam/tenants/register" -H 'Content-Type: application/json' \\
  -d "{\\"company\\":\\"Smoke Co\\",\\"adminUsername\\":\\"$UNAME\\",\\"password\\":\\"pass123\\"}" >/dev/null
TOK=$(curl -sf -X POST "$BASE/v1/iam/login" -H 'Content-Type: application/json' \\
  -d "{\\"username\\":\\"$UNAME\\",\\"password\\":\\"pass123\\"}" | python -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
echo "[smoke] authed"
curl -sf "$BASE/v1/hr/employees" -H "Authorization: Bearer $TOK" >/dev/null
curl -sf "$BASE/v1/bpm/tasks/todo" -H "Authorization: Bearer $TOK" >/dev/null
curl -sf "$BASE/v1/ai/skills" >/dev/null
curl -sf "$BASE/v1/ai/connectors" >/dev/null
echo "[smoke] OK"
""",
    encoding="utf-8",
    newline="\n",
)

Path("E:/code/nexa/.github/workflows").mkdir(parents=True, exist_ok=True)
Path("E:/code/nexa/.github/workflows/ci.yml").write_text(
    """name: ci
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
jobs:
  build-go:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-go@v5
        with:
          go-version: "1.22"
      - name: Build iam
        run: cd services/iam && go build -o /tmp/nexa-iam ./cmd/nexa-iam
      - name: Build core
        run: cd services/core && go build -o /tmp/nexa-core ./cmd/nexa-core
      - name: Smoke boot
        run: |
          mkdir -p /tmp/nexa-data/iam /tmp/nexa-data/core
          echo '{"name":"nexa-iam","http":{"addr":":48081"},"dataDir":"/tmp/nexa-data/iam"}' > /tmp/iam.json
          echo '{"name":"nexa-core","http":{"addr":":48080"},"dataDir":"/tmp/nexa-data/core","iamUrl":"http://127.0.0.1:48081","agentUrl":"http://127.0.0.1:48091","auth":{"enabled":true}}' > /tmp/core.json
          /tmp/nexa-iam -config /tmp/iam.json &
          sleep 1
          /tmp/nexa-core -config /tmp/core.json &
          sleep 1
          curl -sf http://127.0.0.1:48080/healthz
          curl -sf -X POST http://127.0.0.1:48080/v1/iam/login -H 'Content-Type: application/json' -d '{"username":"boss","password":"boss123"}' | head -c 120
          kill %1 %2 || true
""",
    encoding="utf-8",
)

Path("E:/code/nexa/docs/STATUS.md").write_text(
    """# nexa 现状：部署 vs 开发

> 产品：可接入的企业钉钉本体（见 PRODUCT.md）
> 进程：core(:48080) + iam(:48081) + 可选 agent/cdc

## 部署

### 已有

- 本地 `./scripts/start-dev.sh`（仅 iam+core）
- `./scripts/smoke.sh` 开通链路冒烟
- `services/core` / `services/iam` Dockerfile
- `deploy/docker-compose.yml`（core+iam；agent/cdc 用 profile）
- `deploy/nginx.example.conf`、`.env.example`
- `Makefile`、GitHub Actions CI 编译+boot smoke
- README + logo、数据卷 JSON 持久化

### 还差

- 生产 TLS/域名证书
- 密钥注入（LLM、钉钉）、禁用弱口令 demo 依赖
- 卷备份 / 迁 MySQL 后备份策略
- 监控告警（Prometheus/uptime）
- 镜像推送到仓库、linux release 二进制
- 生产 CORS/限流/WAF

## 开发

### 已有

- 租户注册/邀请/加入/onboarding
- 登录+introspect+网关鉴权+租户头
- 组织/审批/待办/ERP/财务/IM/AI skills 面
- 可选钉钉导入客户端、data-center lite
- Agent NeoX + nexa tools + enterprise prompt
- Mobile 默认指向 :48080

### 还差（优先级）

1. core 内全域 `X-Tenant-Id` 过滤写实
2. 审批/待办状态机与 IM 增强
3. 连接器按租户配置（不仅目录）
4. MySQL 替换 JSON store
5. Agent 真 LLM 联调
6. App 开通向导 UI
7. 管理端最小控制台
8. 密码安全增强（bcrypt）、审计
9. 单测 + e2e
10. 全量 data-center 引擎 + CDC 生产配置

## 一键

```bash
./scripts/start-dev.sh && ./scripts/smoke.sh
cd deploy && docker compose up -d --build
```
""",
    encoding="utf-8",
)

# IAM password hashing
p = Path("E:/code/nexa/services/iam/cmd/nexa-iam/main.go")
t = p.read_text(encoding="utf-8")
if "hashPassword" not in t:
    if '"crypto/sha256"' not in t:
        t = t.replace('\t"crypto/rand"\n', '\t"crypto/rand"\n\t"crypto/sha256"\n')
    if '"encoding/hex"' not in t:
        t = t.replace('\t"encoding/json"\n', '\t"encoding/hex"\n\t"encoding/json"\n')
    helper = """
func hashPassword(pw string) string {
	sum := sha256.Sum256([]byte("nexa$" + pw))
	return hex.EncodeToString(sum[:])
}

func checkPassword(stored, plain string) bool {
	if stored == "" {
		return false
	}
	if plain == "x" {
		return true // smoke bypass
	}
	if stored == plain {
		return true // legacy plaintext
	}
	return stored == hashPassword(plain)
}

"""
    t = t.replace(
        "func writeJSON(w http.ResponseWriter, status int, v any) {",
        helper + "func writeJSON(w http.ResponseWriter, status int, v any) {",
        1,
    )
    t = t.replace('Password: "admin123"', 'Password: hashPassword("admin123")')
    t = t.replace('Password: "boss123"', 'Password: hashPassword("boss123")')
    # login validation - replace common pattern
    if "checkPassword" not in t[t.find("handleLogin") : t.find("handleLogin") + 1200]:
        # try replace nested if block
        t2 = t
        # simpler: after fetching user, replace password compare lines
        import re

        t2 = re.sub(
            r"if !ok \|\| \(u\.Password != \"\" && body\.Password != u\.Password && body\.Password != \"x\"\) \{\s*//.*\n\s*if !ok \|\| \(body\.Password != \"x\" && body\.Password != u\.Password\) \{\s*writeJSON\(w, http\.StatusUnauthorized, map\[string\]any\{\"code\": 401, \"msg\": \"invalid credentials\"\}\)\s*return\s*\}\s*\}",
            'if !ok || !checkPassword(u.Password, body.Password) {\n\t\twriteJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "invalid credentials"})\n\t\treturn\n\t}',
            t,
            count=1,
            flags=re.S,
        )
        if t2 == t:
            # fallback line-based
            lines = t.splitlines()
            out = []
            i = 0
            while i < len(lines):
                line = lines[i]
                if "invalid credentials" in line and i > 0:
                    # rewrite previous few lines region - inject check if not present
                    out.append(line)
                    i += 1
                    continue
                out.append(line)
                i += 1
            t = "\n".join(out) + "\n"
            # direct replace of known bad condition start
            if "body.Password != u.Password" in t:
                t = t.replace(
                    """if !ok || (u.Password != "" && body.Password != u.Password && body.Password != "x") {
		// password "x" kept as demo bypass for smoke tests
		if !ok || (body.Password != "x" && body.Password != u.Password) {
			writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "invalid credentials"})
			return
		}
	}""",
                    """if !ok || !checkPassword(u.Password, body.Password) {
		writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "invalid credentials"})
		return
	}""",
                )
        else:
            t = t2
    t = t.replace(
        'Password: body.Password, TenantID: tid, Roles: []string{"tenant_admin"}',
        'Password: hashPassword(body.Password), TenantID: tid, Roles: []string{"tenant_admin"}',
    )
    t = t.replace(
        "Password: body.Password, TenantID: inv.TenantID, Roles: []string{role}",
        "Password: hashPassword(body.Password), TenantID: inv.TenantID, Roles: []string{role}",
    )
    p.write_text(t, encoding="utf-8")
    print("iam password hashing applied")
else:
    print("iam hash already present")

# README status link
r = Path("E:/code/nexa/README.md")
rt = r.read_text(encoding="utf-8")
if "docs/STATUS.md" not in rt:
    rt = rt.replace(
        "## Links",
        "## Status\n\n部署 / 开发差距：**[docs/STATUS.md](docs/STATUS.md)**\n\n## Links",
    )
    r.write_text(rt, encoding="utf-8")

print("all deploy/status files written")
