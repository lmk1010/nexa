#!/usr/bin/env bash
set -euo pipefail
BASE=${BASE:-http://127.0.0.1:48080}
IAM=${IAM:-http://127.0.0.1:48081}
echo "== health =="
curl -fsS "$IAM/healthz" >/dev/null
curl -fsS "$BASE/healthz" >/dev/null
echo "== login =="
TOK=$(curl -fsS -X POST "$IAM/v1/iam/login" -H 'Content-Type: application/json' -d '{"username":"boss","password":"boss123"}' | python -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
AUTH=(-H "Authorization: Bearer $TOK" -H "X-Tenant-Id: 1")
echo "== core apis =="
curl -fsS "${AUTH[@]}" "$BASE/v1/workbench/summary" >/dev/null
curl -fsS "${AUTH[@]}" "$BASE/v1/bpm/processes" >/dev/null
curl -fsS "${AUTH[@]}" "$BASE/v1/hr/employees" >/dev/null
curl -fsS "${AUTH[@]}" "$BASE/v1/im/conversations" >/dev/null
code=$(curl -fsS -o /tmp/nexa-admin.html -w '%{http_code}' "$BASE/admin/"); test "$code" = 200; grep -q 'Nexa Admin' /tmp/nexa-admin.html
echo "== register tenant =="
SUF=$(date +%s)
curl -fsS -X POST "$IAM/v1/iam/tenants/register" \
  -H 'Content-Type: application/json' \
  -d "{\"company\":\"Smoke${SUF}\",\"adminUsername\":\"u${SUF}\",\"password\":\"pass123\"}" >/dev/null
echo "SMOKE OK"
