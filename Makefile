export GOTOOLCHAIN ?= local

.PHONY: build start stop smoke compose-up compose-down

build:
	cd services/iam && go build -o /tmp/nexa-iam.exe ./cmd/nexa-iam
	cd services/core && go build -o /tmp/nexa-core.exe ./cmd/nexa-core

start:
	./scripts/start-dev.sh

stop:
	./scripts/stop-dev.sh

smoke:
	./scripts/smoke.sh

compose-up:
	cd deploy && docker compose up -d --build

compose-down:
	cd deploy && docker compose down
