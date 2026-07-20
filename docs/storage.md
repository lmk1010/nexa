# Storage

## Default: file JSON

- IAM: `{dataDir}/iam.json`
- Core: `{dataDir}/core.json`
- Config: `dataDir` or env `NEXA_DATA_DIR`

Good for local demo and single-node. Not for multi-instance HA.

## Optional: MySQL

Schemas prepared:

- `services/iam/sql/001_iam.sql`
- `services/core/sql/001_core.sql`

Runtime still defaults to file store. Next step: implement `NEXA_IAM_DSN` / `NEXA_CORE_DSN` drivers behind the same HTTP APIs.

## Recommendation

1. Demo / early customers: file store + volume backup
2. Production multi-node: MySQL (or Postgres) + object storage for exports
