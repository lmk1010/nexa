-- Hard delete fully generated DingTalk auto-created HR identities.
-- Scope is intentionally narrow:
--   1) created by DingTalk AUTO_SYNC + AUTO_CREATE,
--   2) DingTalk has no real mobile,
--   3) old code generated both the "钉钉用户-*" name and 199******** mobile.

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS `tmp_hr_dingtalk_generated_identity`;
CREATE TEMPORARY TABLE `tmp_hr_dingtalk_generated_identity` (
  `profile_id` bigint NOT NULL PRIMARY KEY,
  `user_id` bigint DEFAULT NULL,
  `tenant_id` bigint NOT NULL
) ENGINE=Memory;

INSERT IGNORE INTO `tmp_hr_dingtalk_generated_identity` (`profile_id`, `user_id`, `tenant_id`)
SELECT DISTINCT p.`id`, p.`user_id`, p.`tenant_id`
FROM `hr_employee_profile` p
JOIN `hr_dingtalk_user_binding` b
  ON b.`profile_id` = p.`id`
 AND b.`tenant_id` = p.`tenant_id`
 AND b.`deleted` = b'0'
WHERE p.`deleted` = b'0'
  AND b.`source_type` = 'AUTO_SYNC'
  AND b.`match_type` = 'AUTO_CREATE'
  AND (b.`ding_mobile` IS NULL OR TRIM(b.`ding_mobile`) = '')
  AND p.`name` LIKE '钉钉用户-%'
  AND p.`mobile` REGEXP '^199[0-9]{8}$';

SELECT 'DINGTALK_GENERATED_IDENTITY_TO_DELETE' AS section,
       COUNT(*) AS row_count
FROM `tmp_hr_dingtalk_generated_identity`;

DELETE v
FROM `hr_employee_custom_field_value` v
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = v.`profile_id`;

DELETE r
FROM `hr_employee_profile_change_request` r
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = r.`profile_id`;

DELETE l
FROM `hr_employee_change_log` l
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = l.`profile_id`;

DELETE l
FROM `hr_employee_operation_log` l
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = l.`profile_id`;

DELETE r
FROM `hr_employee_document_request` r
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = r.`profile_id`;

DELETE m
FROM `hr_employee_material` m
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = m.`profile_id`;

DELETE a
FROM `hr_employee_attachment` a
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = a.`profile_id`;

DELETE e
FROM `hr_employee_education` e
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = e.`profile_id`;

DELETE f
FROM `hr_employee_family` f
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = f.`profile_id`;

DELETE g
FROM `hr_employee_growth_log` g
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = g.`profile_id`;

DELETE p
FROM `hr_employee_points` p
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = p.`profile_id`;

DELETE a
FROM `hr_employee_points_account` a
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = a.`profile_id`;

DELETE s
FROM `hr_employee_attendance_stat` s
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = s.`profile_id`;

DELETE r
FROM `hr_employee_recruitment` r
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = r.`profile_id`;

DELETE s
FROM `hr_employee_salary` s
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = s.`profile_id`;

DELETE p
FROM `hr_employee_performance` p
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = p.`profile_id`;

DELETE t2
FROM `hr_employee_training` t2
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = t2.`profile_id`;

DELETE i
FROM `hr_employee_inventory` i
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = i.`profile_id`;

DELETE e
FROM `hr_employee_entry` e
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = e.`profile_id`;

DELETE b
FROM `hr_dingtalk_user_binding` b
JOIN `tmp_hr_dingtalk_generated_identity` t
  ON (t.`profile_id` = b.`profile_id` OR (t.`user_id` IS NOT NULL AND t.`user_id` = b.`oa_user_id`))
 AND t.`tenant_id` = b.`tenant_id`;

DELETE ur
FROM `system_user_role` ur
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`user_id` = ur.`user_id`
WHERE t.`user_id` IS NOT NULL;

DELETE up
FROM `system_user_post` up
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`user_id` = up.`user_id`
WHERE t.`user_id` IS NOT NULL;

DELETE utr
FROM `system_user_tenant_relation` utr
JOIN `tmp_hr_dingtalk_generated_identity` t
  ON t.`user_id` = utr.`user_id`
 AND t.`tenant_id` = utr.`tenant_id`
WHERE t.`user_id` IS NOT NULL;

DELETE u
FROM `system_users` u
JOIN `tmp_hr_dingtalk_generated_identity` t
  ON t.`user_id` = u.`id`
 AND t.`tenant_id` = u.`tenant_id`
WHERE t.`user_id` IS NOT NULL;

DELETE p
FROM `hr_employee_profile` p
JOIN `tmp_hr_dingtalk_generated_identity` t ON t.`profile_id` = p.`id`;

SELECT 'DINGTALK_GENERATED_IDENTITY_DELETED' AS section,
       COUNT(*) AS remaining_target_rows
FROM `hr_employee_profile` p
JOIN `hr_dingtalk_user_binding` b
  ON b.`profile_id` = p.`id`
 AND b.`tenant_id` = p.`tenant_id`
 AND b.`deleted` = b'0'
WHERE p.`deleted` = b'0'
  AND b.`source_type` = 'AUTO_SYNC'
  AND b.`match_type` = 'AUTO_CREATE'
  AND (b.`ding_mobile` IS NULL OR TRIM(b.`ding_mobile`) = '')
  AND p.`name` LIKE '钉钉用户-%'
  AND p.`mobile` REGEXP '^199[0-9]{8}$';

DROP TEMPORARY TABLE IF EXISTS `tmp_hr_dingtalk_generated_identity`;
