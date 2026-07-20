-- ----------------------------
-- Table structure for hr_dingtalk_user_binding
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hr_dingtalk_user_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ding_user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '钉钉用户ID',
  `oa_user_id` bigint NOT NULL COMMENT 'OA用户ID',
  `profile_id` bigint DEFAULT NULL COMMENT 'HR档案ID',
  `ding_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '钉钉用户名',
  `ding_mobile` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '钉钉手机号',
  `ding_email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '钉钉邮箱',
  `ding_dept_id` bigint DEFAULT NULL COMMENT '钉钉部门ID',
  `ding_active` bit(1) DEFAULT b'1' COMMENT '钉钉在职状态',
  `match_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '匹配方式：MOBILE/NAME',
  `source_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AUTO_SYNC' COMMENT '来源：AUTO_SYNC/MANUAL',
  `last_seen_time` datetime DEFAULT NULL COMMENT '最近看到该钉钉用户时间',
  `sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `raw_payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '钉钉原始快照',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_hr_ding_user` (`tenant_id`,`ding_user_id`) USING BTREE,
  UNIQUE KEY `uk_hr_oa_user` (`tenant_id`,`oa_user_id`) USING BTREE,
  KEY `idx_hr_binding_profile` (`tenant_id`,`profile_id`) USING BTREE,
  KEY `idx_hr_binding_sync_time` (`tenant_id`,`sync_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钉钉用户与OA用户映射表';

-- ----------------------------
-- Backfill from existing DingTalk attendance records
-- ----------------------------
INSERT INTO hr_dingtalk_user_binding (
  ding_user_id, oa_user_id, profile_id, source_type, match_type, last_seen_time, sync_time, tenant_id
)
SELECT
  t.ding_user_id,
  t.user_id,
  t.profile_id,
  'AUTO_SYNC' AS source_type,
  'HISTORY' AS match_type,
  t.last_seen_time,
  NOW() AS sync_time,
  t.tenant_id
FROM (
  SELECT
    tenant_id,
    user_id,
    MAX(profile_id) AS profile_id,
    SUBSTRING_INDEX(SUBSTRING(source_record_id, 10), '-', 1) AS ding_user_id,
    MAX(clock_time) AS last_seen_time
  FROM hr_attendance_clock_record
  WHERE source_type = 'DINGTALK'
    AND source_record_id LIKE 'dingtalk-%-%'
    AND deleted = b'0'
  GROUP BY tenant_id, user_id, SUBSTRING_INDEX(SUBSTRING(source_record_id, 10), '-', 1)
) t
ON DUPLICATE KEY UPDATE
  oa_user_id = VALUES(oa_user_id),
  profile_id = VALUES(profile_id),
  source_type = VALUES(source_type),
  match_type = VALUES(match_type),
  last_seen_time = VALUES(last_seen_time),
  sync_time = VALUES(sync_time);
