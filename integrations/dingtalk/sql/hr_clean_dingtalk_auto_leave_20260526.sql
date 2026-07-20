-- Hard delete historical HR leave test/generated rows.
-- Real DingTalk leave records synced later can still be displayed normally.
DELETE FROM hr_administrative_leave
WHERE leave_type = 'dingtalk'
  AND process_instance_id LIKE 'dingtalk-leave-%'
  AND remark LIKE 'DingTalk%leave auto sync%';

DELETE FROM hr_administrative_leave
WHERE leave_type = 'rest'
  AND leave_category = 'leave'
  AND user_id = 1
  AND status = 1
  AND (
      tenant_id = 0
      OR (tenant_id = 171 AND start_time < '2026-01-01')
  )
  AND (
      remark IS NULL
      OR remark LIKE '123%'
  );
