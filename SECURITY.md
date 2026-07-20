# Security Policy

## Supported versions

| Branch | Supported |
|--------|-----------|
| `main` | Yes (pre-1.0, breaking changes possible) |

## Reporting a vulnerability

Please do **not** open a public issue for security-sensitive reports.

Contact the repository owner via GitHub private security advisory or email associated with the `lmk1010` account.

Include:

- Affected component (`core` / `iam` / `agent` / deploy)
- Reproduction steps
- Impact assessment

## Hardening baseline (operators)

- Terminate TLS in front of `:48080` (see `deploy/nginx.example.conf`)
- Do not expose `:48081` publicly if core proxies IAM
- Set strong tenant admin passwords; rotate demo seeds in production data dirs
- Keep `AGENT_USE_MOCK=false` only with real provider keys in secret store
- Restrict DingTalk keys (`NEXA_DINGTALK_*`) to the import connector env
- Backup `iam-data` / `core-data` volumes
