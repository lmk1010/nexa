-- HR menu structure (idempotent)
-- Requirement:
-- 1) "同步管理" is top-level directory
-- 2) "同步管理" contains "钉钉同步" item directly
-- 3) "考勤管理" contains "考勤列表"

-- attendance root
UPDATE `system_menu`
SET `status` = 0,
    `visible` = b'1',
    `deleted` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 5023;

-- attendance list + permissions
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
  (7830,'打卡中心','',2,1,5023,'clock-in','lucide:clock-3','/attendance/clock-in/index','/attendance/clock-in/index',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
  (7831,'考勤列表','',2,2,5023,'records','lucide:clipboard-list','/attendance/records/index','/attendance/records/index',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
  (7832,'打卡操作','attendance:clock:clock',3,1,7830,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
  (7833,'打卡查询','attendance:clock:query',3,1,7831,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
  (7834,'钉钉考勤同步','attendance:clock:sync',3,2,7831,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

-- sync top-level directory
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
  (7900,'同步管理','',1,22,0,'/sync','lucide:refresh-cw',NULL,NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

-- dingtalk sync under sync directory
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
  (7890,'钉钉同步','',2,1,7900,'dingtalk','lucide:refresh-cw','/hr/integration/dingtalk/index','/hr/integration/dingtalk/index',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
  (7891,'钉钉人员同步','hr:employee:create',3,1,7890,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'),
  (7892,'钉钉考勤入库','attendance:clock:sync',3,2,7890,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

-- hide old "同步设置" node (5024) to avoid duplicate menu entry
UPDATE `system_menu`
SET `status` = 1,
    `visible` = b'0',
    `deleted` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 5024;

UPDATE `system_role_menu`
SET `deleted` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `menu_id` = 5024
  AND `deleted` = b'0';

-- recover previously soft-deleted attendance/sync grants for roles
-- that had attendance root (5023), then backfill missing grants.
UPDATE `system_role_menu` rm
JOIN (
  SELECT DISTINCT `role_id`, `tenant_id`
  FROM `system_role_menu`
  WHERE `menu_id` = 5023
) seed
  ON seed.`role_id` = rm.`role_id`
 AND seed.`tenant_id` = rm.`tenant_id`
SET rm.`deleted` = b'0',
    rm.`updater` = '1',
    rm.`update_time` = NOW()
WHERE rm.`menu_id` IN (5023,7830,7831,7832,7833,7834,7900,7890,7891,7892)
  AND rm.`deleted` = b'1';

INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT seed.`role_id`, m.`menu_id`, '1', NOW(), '1', NOW(), b'0', seed.`tenant_id`
FROM (
  SELECT DISTINCT `role_id`, `tenant_id`
  FROM `system_role_menu`
  WHERE `menu_id` = 5023
) seed
JOIN (
  SELECT 5023 AS menu_id UNION ALL
  SELECT 7830 UNION ALL
  SELECT 7831 UNION ALL
  SELECT 7832 UNION ALL
  SELECT 7833 UNION ALL
  SELECT 7834 UNION ALL
  SELECT 7900 UNION ALL
  SELECT 7890 UNION ALL
  SELECT 7891 UNION ALL
  SELECT 7892
) m
LEFT JOIN `system_role_menu` x
  ON x.`role_id` = seed.`role_id`
 AND x.`tenant_id` = seed.`tenant_id`
 AND x.`menu_id` = m.`menu_id`
 AND x.`deleted` = b'0'
WHERE x.`id` IS NULL;
