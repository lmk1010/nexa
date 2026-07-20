-- HR x DingTalk roster field sync custom fields
-- 可重复执行

SET NAMES utf8mb4;

INSERT INTO `hr_employee_custom_field`
(`field_key`,`field_name`,`field_type`,`options_json`,`required_flag`,`sensitive_flag`,`visible_roles`,`sort_order`,`status`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 'id_card_name','身份证姓名','TEXT',NULL,b'0',b'1','HROwner,super_admin,tenant_admin',10,0,'1',NOW(),'1',NOW(),b'0', t.`tenant_id`
FROM (SELECT DISTINCT `tenant_id` FROM `hr_employee_profile` WHERE `deleted` = b'0') t
WHERE NOT EXISTS (
  SELECT 1 FROM `hr_employee_custom_field` f
  WHERE f.`tenant_id` = t.`tenant_id` AND f.`field_key` = 'id_card_name' AND f.`deleted` = b'0'
);

INSERT INTO `hr_employee_custom_field`
(`field_key`,`field_name`,`field_type`,`options_json`,`required_flag`,`sensitive_flag`,`visible_roles`,`sort_order`,`status`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 'certificate_expire_date','证件到期日','DATE',NULL,b'0',b'1','HROwner,super_admin,tenant_admin',20,0,'1',NOW(),'1',NOW(),b'0', t.`tenant_id`
FROM (SELECT DISTINCT `tenant_id` FROM `hr_employee_profile` WHERE `deleted` = b'0') t
WHERE NOT EXISTS (
  SELECT 1 FROM `hr_employee_custom_field` f
  WHERE f.`tenant_id` = t.`tenant_id` AND f.`field_key` = 'certificate_expire_date' AND f.`deleted` = b'0'
);

INSERT INTO `hr_employee_custom_field`
(`field_key`,`field_name`,`field_type`,`options_json`,`required_flag`,`sensitive_flag`,`visible_roles`,`sort_order`,`status`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 'residence_type','户籍类型','TEXT',NULL,b'0',b'0','HROwner,super_admin,tenant_admin',30,0,'1',NOW(),'1',NOW(),b'0', t.`tenant_id`
FROM (SELECT DISTINCT `tenant_id` FROM `hr_employee_profile` WHERE `deleted` = b'0') t
WHERE NOT EXISTS (
  SELECT 1 FROM `hr_employee_custom_field` f
  WHERE f.`tenant_id` = t.`tenant_id` AND f.`field_key` = 'residence_type' AND f.`deleted` = b'0'
);

INSERT INTO `hr_employee_custom_field`
(`field_key`,`field_name`,`field_type`,`options_json`,`required_flag`,`sensitive_flag`,`visible_roles`,`sort_order`,`status`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 'first_work_time','首次参加工作时间','DATE',NULL,b'0',b'0','HROwner,super_admin,tenant_admin',40,0,'1',NOW(),'1',NOW(),b'0', t.`tenant_id`
FROM (SELECT DISTINCT `tenant_id` FROM `hr_employee_profile` WHERE `deleted` = b'0') t
WHERE NOT EXISTS (
  SELECT 1 FROM `hr_employee_custom_field` f
  WHERE f.`tenant_id` = t.`tenant_id` AND f.`field_key` = 'first_work_time' AND f.`deleted` = b'0'
);

INSERT INTO `hr_employee_custom_field`
(`field_key`,`field_name`,`field_type`,`options_json`,`required_flag`,`sensitive_flag`,`visible_roles`,`sort_order`,`status`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 'personal_social_security_account','个人社保账号','TEXT',NULL,b'0',b'1','HROwner,super_admin,tenant_admin',50,0,'1',NOW(),'1',NOW(),b'0', t.`tenant_id`
FROM (SELECT DISTINCT `tenant_id` FROM `hr_employee_profile` WHERE `deleted` = b'0') t
WHERE NOT EXISTS (
  SELECT 1 FROM `hr_employee_custom_field` f
  WHERE f.`tenant_id` = t.`tenant_id` AND f.`field_key` = 'personal_social_security_account' AND f.`deleted` = b'0'
);

INSERT INTO `hr_employee_custom_field`
(`field_key`,`field_name`,`field_type`,`options_json`,`required_flag`,`sensitive_flag`,`visible_roles`,`sort_order`,`status`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT 'personal_housing_fund_account','个人公积金账号','TEXT',NULL,b'0',b'1','HROwner,super_admin,tenant_admin',60,0,'1',NOW(),'1',NOW(),b'0', t.`tenant_id`
FROM (SELECT DISTINCT `tenant_id` FROM `hr_employee_profile` WHERE `deleted` = b'0') t
WHERE NOT EXISTS (
  SELECT 1 FROM `hr_employee_custom_field` f
  WHERE f.`tenant_id` = t.`tenant_id` AND f.`field_key` = 'personal_housing_fund_account' AND f.`deleted` = b'0'
);
