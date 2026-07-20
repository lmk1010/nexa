-- Add restorable leave fields for DingTalk employee sync snapshots.
-- Repeatable and MySQL 5.7 compatible.

SET NAMES utf8mb4;

SET @ddl = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_dingtalk_sync_snapshot') > 0
    AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_dingtalk_sync_snapshot' AND COLUMN_NAME = 'before_entry_leave_date') = 0,
    'ALTER TABLE `hr_dingtalk_sync_snapshot` ADD COLUMN `before_entry_leave_date` date DEFAULT NULL COMMENT ''Entry leave date before sync'' AFTER `before_entry_dept_id`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_dingtalk_sync_snapshot') > 0
    AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_dingtalk_sync_snapshot' AND COLUMN_NAME = 'before_entry_leave_reason') = 0,
    'ALTER TABLE `hr_dingtalk_sync_snapshot` ADD COLUMN `before_entry_leave_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''Entry leave reason before sync'' AFTER `before_entry_leave_date`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
