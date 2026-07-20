> **Reference module** — production uses `services/core` (+ `services/iam`). See `docs/CONVENTIONS.md`.

# nexa-op

**OP** — Go-only service for the nexa enterprise assistant platform.

## Runtime

**Go only.** Java is never a nexa runtime dependency.  
Legacy Java reference (read-only): `legacy/java/op/`.

## Status

Skeleton: health endpoint only. Domain APIs rewritten in Go module-by-module.

## Run

```bash
export PATH="/e/tools/go/bin:$PATH"
cd services/op
cp configs/config.example.json configs/config.json
go run ./cmd/nexa-op -config ./configs/config.json
# GET http://127.0.0.1:48088/healthz
```

## Agent integration

`services/agent` (Node + NeoX) calls this service via gateway/tools as the enterprise assistant backend.
