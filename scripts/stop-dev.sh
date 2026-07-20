#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PIDDIR="${NEXA_PID_DIR:-$ROOT/.run/pids}"
if [[ -d "$PIDDIR" ]]; then
  for f in "$PIDDIR"/*.pid; do
    [[ -f "$f" ]] || continue
    pid=$(cat "$f" || true)
    name=$(basename "$f" .pid)
    if [[ -n "${pid:-}" ]] && kill -0 "$pid" 2>/dev/null; then
      echo "[stop] $name pid=$pid"
      kill "$pid" 2>/dev/null || true
    fi
    rm -f "$f"
  done
fi
taskkill //F //IM nexa-core.exe //IM nexa-iam.exe 2>/dev/null || true
pkill -f 'nexa-core|nexa-iam' 2>/dev/null || true
echo "stopped"
