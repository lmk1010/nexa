# Deploy nexa

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
