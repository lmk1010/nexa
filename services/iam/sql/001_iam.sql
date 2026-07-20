-- nexa IAM MySQL schema (optional; file store is default)
CREATE TABLE IF NOT EXISTS nexa_tenant (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  code VARCHAR(64) NOT NULL UNIQUE,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  created_at DATETIME(3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  nickname VARCHAR(128) NOT NULL DEFAULT '',
  password_hash VARCHAR(128) NOT NULL,
  tenant_id BIGINT NOT NULL,
  roles_json JSON NOT NULL,
  perms_json JSON NOT NULL,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_invite (
  code VARCHAR(96) PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  used_by VARCHAR(64) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_token (
  token VARCHAR(128) PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  expires_at DATETIME(3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
