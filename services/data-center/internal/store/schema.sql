-- kyx-data-center 用到的 3 张表, 建在 warehouse.ordersys_dw
-- 用 root 建, 服务用 agent_ro 只读 → 需要给 agent_ro 授 INSERT/UPDATE 权限（后续 P4 处理）

-- 导出任务
CREATE TABLE IF NOT EXISTS export_jobs (
  id CHAR(36) PRIMARY KEY COMMENT 'UUID',
  owner_id BIGINT NOT NULL COMMENT '发起用户 sys_user.user_id',
  owner_name VARCHAR(100) NOT NULL COMMENT '发起用户名（冗余, 免 join）',
  template_id VARCHAR(50) NOT NULL COMMENT '模板 ID, 见 /templates/*.json',
  params_json JSON NOT NULL COMMENT '过滤参数, 提交时的原始 JSON',
  state ENUM('pending','queued','running','done','failed','cancelled') NOT NULL DEFAULT 'pending',
  rows_written INT NOT NULL DEFAULT 0 COMMENT '进度: 已写入行数',
  rows_total INT DEFAULT NULL COMMENT '预估总行数（用 COUNT 或 EXPLAIN）',
  file_path VARCHAR(255) DEFAULT NULL COMMENT '完成后 xlsx 相对路径',
  file_size BIGINT DEFAULT NULL COMMENT 'xlsx 字节数',
  error_msg TEXT DEFAULT NULL,
  queue_pos INT DEFAULT NULL COMMENT '入队时的位次（供 UI 显示）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  started_at DATETIME DEFAULT NULL,
  finished_at DATETIME DEFAULT NULL,
  INDEX idx_owner_state (owner_id, state),
  INDEX idx_state_created (state, created_at),
  INDEX idx_created (created_at)
) ENGINE=InnoDB CHARSET=utf8mb4 COMMENT='导出任务';

-- 预约导出（cron 触发）
CREATE TABLE IF NOT EXISTS export_schedule (
  id CHAR(36) PRIMARY KEY,
  owner_id BIGINT NOT NULL,
  owner_name VARCHAR(100) NOT NULL,
  template_id VARCHAR(50) NOT NULL,
  params_json JSON NOT NULL,
  cron_expr VARCHAR(64) NOT NULL COMMENT '五段 cron: 分 时 日 月 周',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  next_run_at DATETIME NOT NULL,
  last_run_at DATETIME DEFAULT NULL,
  last_job_id CHAR(36) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_next_run (enabled, next_run_at)
) ENGINE=InnoDB CHARSET=utf8mb4 COMMENT='预约导出';

-- 审计（不写主表, 单独归档用）
CREATE TABLE IF NOT EXISTS export_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  job_id CHAR(36) NOT NULL,
  owner_id BIGINT NOT NULL,
  template_id VARCHAR(50) NOT NULL,
  action ENUM('created','started','done','failed','cancelled','downloaded') NOT NULL,
  detail JSON,
  at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_job (job_id),
  INDEX idx_owner_at (owner_id, at)
) ENGINE=InnoDB CHARSET=utf8mb4 COMMENT='导出行为审计';
