# Contributing to nexa

## Product rules

1. **nexa is the product body** (joinable enterprise DingTalk), not an OA→DingTalk sync adapter.
2. **Runtime**: Go domains + Node NeoX agent. No Java runtime.
3. **Default deploy**: `nexa-core` (:48080) + `nexa-iam` (:48081). Do not add new default processes without need.
4. Split modules under `services/{hr,bpm,...}` are **reference**; ship features in `services/core` (and `iam` / `agent` when required).

## Layout

| Path | Role |
|------|------|
| `services/core` | Production business binary (gateway + domains) |
| `services/iam` | Auth & tenant onboarding |
| `services/agent` | NeoX agent |
| `services/cdc-mysql` | Optional CDC |
| `services/*` (other) | Reference implementations |
| `legacy/` | Historical Java/Node only |
| `docs/` | PRODUCT, GOAL, STATUS, architecture, API |

## Dev workflow

```bash
export GOTOOLCHAIN=local
export PATH="/e/tools/go/bin:$PATH"

./scripts/start-dev.sh
./scripts/smoke.sh
./scripts/stop-dev.sh
```

Build single binary:

```bash
make build
# or
cd services/core && go build -o /tmp/nexa-core ./cmd/nexa-core
cd services/iam && go build -o /tmp/nexa-iam ./cmd/nexa-iam
```

## API conventions

- Public JSON: `{ "code": 0, "data": ... }` success; non-zero `code` + `msg` on error.
- Auth: `Authorization: Bearer <token>` on business routes.
- Tenant: gateway injects `X-Tenant-Id`, `X-User-Id`, `X-Username` after IAM introspect.
- Prefer `/v1/<domain>/...` paths; dual-register `/app-api` / `/admin-api` only for mobile compat.

## Config conventions

- Examples: `configs/config.example.json`, `configs/config.docker.json`
- Never commit real secrets; use env: `NEXA_*`
- Data dir: `dataDir` in config or `NEXA_DATA_DIR`

## Commits

- Imperative subject, scoped if useful: `feat(core): ...`, `docs: ...`, `deploy: ...`
- End with Co-authored trailer when using assistants.
- Push to `main` only with green local `go build` for core+iam.

## PRs / CI

- CI builds `core` + `iam` and boots a smoke login.
- Keep PRs small; update `docs/STATUS.md` when deploy/dev gap changes.
