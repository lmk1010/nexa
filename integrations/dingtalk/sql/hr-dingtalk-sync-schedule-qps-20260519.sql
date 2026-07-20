-- Move DingTalk employee sync away from the exact hour to avoid DingTalk department API peak throttling.
-- 02:37:17 keeps the job overnight while avoiding 02:00:00 and the existing 02:10/02:20 tasks.
UPDATE system_scheduler_task
SET cron_expression = '17 37 2 * * ?',
    next_execute_time = TIMESTAMP(
        CURRENT_DATE + INTERVAL (CASE WHEN CURTIME() < '02:37:17' THEN 0 ELSE 1 END) DAY,
        '02:37:17'
    ),
    updater = 'codex',
    update_time = NOW()
WHERE id = 4
  AND tenant_id = 171
  AND deleted = b'0'
  AND task_method = 'executeEmployeeSync';
