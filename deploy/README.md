# Deploy

## Local (no Docker)

```bash
export PATH="/e/tools/go/bin:$PATH"
./scripts/start-dev.sh
./scripts/stop-dev.sh
```

## Docker Compose

```bash
cd deploy
docker compose up -d --build
# gateway http://localhost:48080
# agent   http://localhost:48091
```

Profiles:
- default: gateway + all domain Go services + agent
- `full`: also data-center + cdc-mysql
