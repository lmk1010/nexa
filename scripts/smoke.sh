#!/usr/bin/env bash
set -euo pipefail
BASE="${NEXA_BASE:-http://127.0.0.1:48080}"
echo "[smoke] health"
curl -sf "$BASE/healthz" >/dev/null
curl -sf "$BASE/v1/platform/services" >/dev/null
echo "[smoke] register+login"
UNAME="smoke_$(date +%s)"
curl -sf -X POST "$BASE/v1/iam/tenants/register" -H 'Content-Type: application/json' \
  -d "{\"company\":\"Smoke Co\",\"adminUsername\":\"$UNAME\",\"password\":\"pass123\"}" >/dev/null
TOK=$(curl -sf -X POST "$BASE/v1/iam/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$UNAME\",\"password\":\"pass123\"}" | python -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
echo "[smoke] authed"
curl -sf "$BASE/v1/hr/employees" -H "Authorization: Bearer $TOK" >/dev/null
curl -sf "$BASE/v1/bpm/tasks/todo" -H "Authorization: Bearer $TOK" >/dev/null
curl -sf "$BASE/v1/ai/skills" >/dev/null
curl -sf "$BASE/v1/ai/connectors" >/dev/null
echo "[smoke] OK"
