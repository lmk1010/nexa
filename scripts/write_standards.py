from pathlib import Path

root = Path("E:/code/nexa")

# gitignore
(root / ".gitignore").write_text(
    """# OS
.DS_Store
Thumbs.db
desktop.ini

# IDE
.idea/
.vscode/
*.iml
*.swp
*~

# Secrets / local config
.env
**/.env
*.env.local
**/.env.local
!.env.example
!**/.env.example
**/config.yaml
!**/config.yaml.example
!**/configs/config.example.yaml
**/configs/config.yaml
**/configs/config.json
!**/configs/config.example.json
!**/configs/config.docker.json
*.pem
*.key
credentials*.json

# Go
**/bin/
**/dist/
**/vendor/
*.exe
*.test
*.out
**/coverage.out

# Runtime data (local)
data/
**/data/
!**/configs/**
.run/
exports/
**/exports/

# Node
node_modules/
.pnpm-store/
npm-debug.log*
yarn-error.log*

# Flutter
.dart_tool/
.packages
build/
*.apk
*.ipa
**/android/.gradle/
**/android/local.properties
**/ios/Pods/
**/ios/.symlinks/

# Logs / crash
*.log
hs_err_pid*
replay_pid*

# Git
*.orig

# Python cache from scripts
**/__pycache__/
*.pyc
""",
    encoding="utf-8",
)

(root / "LICENSE").write_text(
    """MIT License

Copyright (c) 2026 nexa contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
""",
    encoding="utf-8",
)

(root / "docs" / "README.md").write_text(
    """# Documentation index

| Doc | Purpose |
|-----|---------|
| [PRODUCT.md](PRODUCT.md) | Product definition (enterprise DingTalk body) |
| [GOAL.md](GOAL.md) | Milestones and hard rules |
| [STATUS.md](STATUS.md) | Deploy vs dev gaps |
| [CONVENTIONS.md](CONVENTIONS.md) | Repo / API / process standards |
| [api/README.md](api/README.md) | API cheat sheet |
| [architecture/overview.md](architecture/overview.md) | System overview |
| [architecture/ai-native.md](architecture/ai-native.md) | Skills / sense / automation |
| [architecture/capability-map.md](architecture/capability-map.md) | Capability map |
| [../CONTRIBUTING.md](../CONTRIBUTING.md) | Contributing |
| [../CHANGELOG.md](../CHANGELOG.md) | Changelog |
| [../SECURITY.md](../SECURITY.md) | Security policy |

Reference modules and legacy extraction notes: [SOURCE_MAP.md](SOURCE_MAP.md).
""",
    encoding="utf-8",
)

(root / ".gitattributes").write_text(
    """* text=auto eol=lf
*.go text eol=lf
*.sh text eol=lf
*.md text eol=lf
*.svg text eol=lf
*.png binary
*.jpg binary
""",
    encoding="utf-8",
)

# reference banners
for name in [
    "gateway",
    "hr",
    "bpm",
    "business",
    "erp",
    "finance",
    "im",
    "op",
    "ai",
    "data-center",
]:
    p = root / "services" / name / "README.md"
    banner = (
        "> **Reference module** — production uses `services/core` "
        "(+ `services/iam`). See `docs/CONVENTIONS.md`.\n\n"
    )
    if p.exists():
        t = p.read_text(encoding="utf-8", errors="ignore")
        if "Reference module" not in t:
            p.write_text(banner + t, encoding="utf-8")
    else:
        p.write_text(
            banner
            + "# Reference module\n\n"
            + "Do not deploy this service by default.\n",
            encoding="utf-8",
        )

(root / "scripts" / "stop-dev.sh").write_text(
    """#!/usr/bin/env bash
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
""",
    encoding="utf-8",
    newline="\n",
)

# README links
r = root / "README.md"
rt = r.read_text(encoding="utf-8")
if "CONTRIBUTING.md" not in rt:
    rt = rt.replace(
        """<p align="center">
  <a href="docs/PRODUCT.md">Product</a> ·
  <a href="docs/GOAL.md">Roadmap</a> ·
  <a href="docs/api/README.md">API</a> ·
  <a href="deploy/README.md">Deploy</a>
</p>""",
        """<p align="center">
  <a href="docs/PRODUCT.md">Product</a> ·
  <a href="docs/GOAL.md">Roadmap</a> ·
  <a href="docs/STATUS.md">Status</a> ·
  <a href="docs/api/README.md">API</a> ·
  <a href="deploy/README.md">Deploy</a> ·
  <a href="CONTRIBUTING.md">Contributing</a>
</p>""",
    )
if "docs/CONVENTIONS.md" not in rt:
    if "## Status" in rt:
        rt = rt.replace(
            "## Status\n\n部署 / 开发差距：**[docs/STATUS.md](docs/STATUS.md)**\n\n## Links",
            "## Status\n\n部署 / 开发差距：**[docs/STATUS.md](docs/STATUS.md)**  \n"
            "仓库规范：**[docs/CONVENTIONS.md](docs/CONVENTIONS.md)** · "
            "[CHANGELOG](CHANGELOG.md)\n\n## Links",
        )
    else:
        rt = rt.replace(
            "## Links",
            "## Status\n\n"
            "部署 / 开发差距：**[docs/STATUS.md](docs/STATUS.md)**  \n"
            "仓库规范：**[docs/CONVENTIONS.md](docs/CONVENTIONS.md)** · "
            "[CHANGELOG](CHANGELOG.md)\n\n## Links",
        )
if "TBD (CDC MIT)" in rt:
    rt = rt.replace(
        "## License\n\nTBD (CDC MIT).",
        "## License\n\n[MIT](LICENSE)",
    )
elif "## License" in rt and "[MIT](LICENSE)" not in rt:
    # replace last license section body
    idx = rt.rfind("## License")
    rt = rt[:idx] + "## License\n\n[MIT](LICENSE)\n"
r.write_text(rt, encoding="utf-8")

st = root / "docs" / "STATUS.md"
stt = st.read_text(encoding="utf-8")
if "CONVENTIONS.md" not in stt:
    st.write_text(
        stt.replace(
            "# nexa 现状：部署 vs 开发",
            "# nexa 现状：部署 vs 开发\n\n"
            "规范见 [CONVENTIONS.md](CONVENTIONS.md) · "
            "变更见 [CHANGELOG.md](../CHANGELOG.md)",
        ),
        encoding="utf-8",
    )

# AUTONOMOUS note
(root / "docs" / "AUTONOMOUS.md").write_text(
    """# Autonomous execution log

## Rule
Keep building and pushing. Do not wait for user confirmation.

## Standards
- Production path: `services/core` + `services/iam` (+ optional agent/cdc)
- Docs: PRODUCT / GOAL / STATUS / CONVENTIONS / CHANGELOG
- Always `go build` core+iam and prefer `scripts/smoke.sh`

## Product
nexa = joinable enterprise DingTalk product body.
DingTalk OpenAPI / business DB = optional connectors only.
""",
    encoding="utf-8",
)

print("standardization files written")
