# Changelog

All notable changes to this project are documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/).  
This project is pre-1.0; entries are grouped by date on `main`.

## [Unreleased]

### Planned

- Full tenant isolation across core domains
- MySQL-backed stores
- Per-tenant connector config
- App onboarding UI

## [0.1.0] - 2026-07-20

### Added

- Product positioning: joinable enterprise DingTalk (`docs/PRODUCT.md`)
- `nexa-core` monolith: gateway + business domains on `:48080`
- `nexa-iam`: login, tenant register/invite/accept, onboarding status
- AI control plane: skills, intent, sense, connectors
- Agent NeoX integration + enterprise prompt + `nexa_*` tools
- Optional DingTalk OpenAPI import client
- data-center lite export surface
- Minimal deploy: Docker Compose (iam+core), nginx sample, Makefile, smoke script
- GitHub Actions CI build + boot smoke
- Brand assets `assets/logo.svg`
- STATUS matrix for deploy vs development gaps

### Changed

- Default local start boots only **iam + core** (resource-saving)
- README rewritten for GitHub-style product presentation
- IAM passwords hashed (SHA-256 with migration fallback)

### Notes

- Split services under `services/{hr,bpm,gateway,...}` retained as reference
- CDC available as optional profile / separate repo `nexa-cdc-mysql`
