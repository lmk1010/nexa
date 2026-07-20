# nexa-business

**Business** — Go-only service for the nexa enterprise assistant platform.

## Runtime

**Go only.** Java is never a nexa runtime dependency.  
Legacy Java reference (read-only): `legacy/java/business/`.

## Status

Skeleton: health endpoint only. Domain APIs rewritten in Go module-by-module.

## Run

```bash
export PATH="/e/tools/go/bin:$PATH"
cd services/business
cp configs/config.example.json configs/config.json
go run ./cmd/nexa-business -config ./configs/config.json
# GET http://127.0.0.1:48084/healthz
```

## Agent integration

`services/agent` (Node + NeoX) calls this service via gateway/tools as the enterprise assistant backend.
