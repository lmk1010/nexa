#!/usr/bin/env bash
# Minimal nexa: core (all business) + iam (auth). Agent optional.
set -euo pipefail
export PATH="/e/tools/go/bin:${PATH:-}"
export GOTOOLCHAIN=local
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BIN="${NEXA_BIN_DIR:-/tmp}"
LOGDIR="${NEXA_LOG_DIR:-$ROOT/.run/logs}"
PIDDIR="${NEXA_PID_DIR:-$ROOT/.run/pids}"
mkdir -p "$LOGDIR" "$PIDDIR" "$ROOT/.run/data/core" "$ROOT/.run/data/iam" "$ROOT/.run/configs"

# Storage backend: file (default) | bolt (embedded durable)
export NEXA_DB_BACKEND="${NEXA_DB_BACKEND:-file}"
export NEXA_IAM_BACKEND="${NEXA_IAM_BACKEND:-$NEXA_DB_BACKEND}"
export NEXA_CORE_BACKEND="${NEXA_CORE_BACKEND:-$NEXA_DB_BACKEND}"

echo "[build] iam"
(cd "$ROOT/services/iam" && go build -o "$BIN/nexa-iam.exe" ./cmd/nexa-iam)
echo "[build] core"
(cd "$ROOT/services/core" && go build -o "$BIN/nexa-core.exe" ./cmd/nexa-core)

cat > "$ROOT/.run/configs/iam.json" <<JSON
{"name":"nexa-iam","http":{"addr":":48081"},"dataDir":"$ROOT/.run/data/iam"}
JSON
cat > "$ROOT/.run/configs/core.json" <<JSON
{"name":"nexa-core","http":{"addr":":48080"},"dataDir":"$ROOT/.run/data/core","iamUrl":"http://127.0.0.1:48081","agentUrl":"http://127.0.0.1:48091","auth":{"enabled":true}}
JSON

start_one() {
  local name="$1" bin="$2" conf="$3"
  local pidfile="$PIDDIR/$name.pid"
  if [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
    echo "[skip] $name running"
    return
  fi
  echo "[start] $name (backend=${NEXA_DB_BACKEND:-file})"
  nohup env NEXA_IAM_BACKEND="$NEXA_IAM_BACKEND" NEXA_CORE_BACKEND="$NEXA_CORE_BACKEND" NEXA_DB_BACKEND="$NEXA_DB_BACKEND" "$bin" -config "$conf" >"$LOGDIR/$name.log" 2>&1 &
  echo $! >"$pidfile"
}

start_one iam "$BIN/nexa-iam.exe" "$ROOT/.run/configs/iam.json"
sleep 0.3
start_one core "$BIN/nexa-core.exe" "$ROOT/.run/configs/core.json"
sleep 0.8
echo "[health]"
for port in 48081 48080; do
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$port/healthz" || true)
  echo "  :$port -> $code"
done
echo "processes: iam:48081 + core:48080 (all business merged)"
echo "optional agent: cd services/agent && AGENT_USE_MOCK=true npm run dev"
echo "stop: $ROOT/scripts/stop-dev.sh"
