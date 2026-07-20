-- nexa-core optional MySQL schema (file store is default)
CREATE TABLE IF NOT EXISTS nexa_employee (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  mobile VARCHAR(32) NOT NULL DEFAULT '',
  dept_id BIGINT NOT NULL DEFAULT 0,
  dept_name VARCHAR(128) NOT NULL DEFAULT '',
  job_no VARCHAR(64) NOT NULL DEFAULT '',
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  dingtalk_id VARCHAR(64) NOT NULL DEFAULT '',
  updated_at DATETIME(3) NOT NULL,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_department (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_bpm_task (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  process_name VARCHAR(64) NOT NULL DEFAULT '',
  starter VARCHAR(64) NOT NULL DEFAULT '',
  assignee VARCHAR(64) NOT NULL DEFAULT '',
  status VARCHAR(32) NOT NULL,
  reason VARCHAR(512) NOT NULL DEFAULT '',
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  history_json JSON NULL,
  KEY idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_todo (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  assignee VARCHAR(64) NOT NULL DEFAULT '',
  status VARCHAR(32) NOT NULL,
  due_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_connector (
  tenant_id BIGINT NOT NULL,
  connector_id VARCHAR(64) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  config_json JSON NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (tenant_id, connector_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_im_conversation (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  unread INT NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_im_message (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  conversation_id VARCHAR(64) NOT NULL,
  sender VARCHAR(64) NOT NULL,
  body TEXT NOT NULL,
  created_at DATETIME(3) NOT NULL,
  KEY idx_conv (tenant_id, conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
