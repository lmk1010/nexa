# Autonomous execution log

## Rule
Keep building and pushing. Do not wait for user confirmation.

## Standards
- Production path: `services/core` + `services/iam` (+ optional agent/cdc)
- Docs: PRODUCT / GOAL / STATUS / CONVENTIONS / CHANGELOG
- Always `go build` core+iam and prefer `scripts/smoke.sh`

## Product
nexa = joinable enterprise DingTalk product body.
DingTalk OpenAPI / business DB = optional connectors only.
