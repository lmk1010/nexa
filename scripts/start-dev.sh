#!/usr/bin/env bash
# Start core nexa Go services for local demo (Windows Git Bash / Linux).
set -euo pipefail
export PATH="/e/tools/go/bin:${PATH:-}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BIN="${NEXA_BIN_DIR:-/tmp}"
LOGDIR="${NEXA_LOG_DIR:-$ROOT/.run/logs}"
PIDDIR="${NEXA_PID_DIR:-$ROOT/.run/pids}"
mkdir -p "$LOGDIR" "$PIDDIR"

build() {
  local svc="$1"
  echo "[build] $svc"
  (cd "$ROOT/services/$svc" && go build -o "$BIN/nexa-$svc.exe" "./cmd/nexa-$svc")
}

start_one() {
  local svc="$1"
  local conf="$ROOT/services/$svc/configs/config.example.json"
  if [[ -f "$ROOT/.run/configs/$svc.json" ]]; then conf="$ROOT/.run/configs/$svc.json"; fi
  local pidfile="$PIDDIR/$svc.pid"
  if [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
    echo "[skip] $svc already running pid=$(cat "$pidfile")"
    return
  fi
  if [[ ! -x "$BIN/nexa-$svc.exe" && ! -f "$BIN/nexa-$svc.exe" ]]; then
    build "$svc"
  fi
  echo "[start] $svc"
  if [[ -f "$conf" ]]; then
    nohup "$BIN/nexa-$svc.exe" -config "$conf" >"$LOGDIR/$svc.log" 2>&1 &
  else
    nohup "$BIN/nexa-$svc.exe" >"$LOGDIR/$svc.log" 2>&1 &
  fi
  echo $! >"$pidfile"
}


# file-backed configs for core services
mkdir -p "$ROOT/.run/data/iam" "$ROOT/.run/data/hr" "$ROOT/.run/data/bpm" "$ROOT/.run/configs"
for svc_port in "iam:48081" "bpm:48082" "hr:48083" "business:48084" "erp:48085" "finance:48086" "im:48087" "op:48088" "ai:48089"; do
  svc="${svc_port%%:*}"
  port="${svc_port##*:}"
  mkdir -p "$ROOT/.run/data/$svc"
  cat > "$ROOT/.run/configs/$svc.json" <<JSON
{"name":"nexa-$svc","http":{"addr":":$port"},"dataDir":"$ROOT/.run/data/$svc"}
JSON
done

SERVICES=(iam bpm hr business erp finance im op ai gateway)

for s in "${SERVICES[@]}"; do
  build "$s"
done

for s in "${SERVICES[@]}"; do
  start_one "$s"
done

sleep 1
echo "[health]"
for port in 48081 48082 48083 48084 48085 48086 48087 48088 48089 48080; do
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$port/healthz" || true)
  echo "  :$port -> $code"
done
echo "logs: $LOGDIR"
echo "stop: $ROOT/scripts/stop-dev.sh"


# data-center lite (stdlib export surface for local AI/agent)
echo "[build] data-center lite"
(cd "$ROOT/services/data-center" && go build -o "$BIN/nexa-dc-lite.exe" ./cmd/nexa-dc-lite)
mkdir -p "$ROOT/.run/data/data-center"
cat > "$ROOT/.run/configs/data-center.json" <<JSON
{"name":"nexa-data-center","http":{"addr":":48092"},"templatesDir":"$ROOT/services/data-center/templates","dataDir":"$ROOT/.run/data/data-center"}
JSON
if [[ ! -f "$PIDDIR/data-center.pid" ]] || ! kill -0 "$(cat "$PIDDIR/data-center.pid" 2>/dev/null)" 2>/dev/null; then
  nohup "$BIN/nexa-dc-lite.exe" -config "$ROOT/.run/configs/data-center.json" >"$LOGDIR/data-center.log" 2>&1 &
  echo $! > "$PIDDIR/data-center.pid"
  echo "[start] data-center lite"
fi
