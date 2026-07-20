package com.kyx.service.business.manager;

import com.kyx.service.business.dal.dataobject.scheduler.SchedulerTaskDO;
import com.kyx.service.business.service.scheduler.SchedulerTaskService;
import com.kyx.service.business.controller.admin.scheduler.vo.task.SchedulerTaskCreateReqVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 同步任务初始化器
 * 自动在数据库中创建同步相关的定时任务记录
 * 
 * @author MK
 */
@Component
@Slf4j
@Order(1) // 确保在SchedulerTaskInitializer之前执行
@ConditionalOnProperty(name = "sync.external.enabled", havingValue = "true", matchIfMissing = false)
public class SyncTaskInitializer implements ApplicationRunner {

    @Resource
    private SchedulerTaskService schedulerTaskService;

    @Value("${sync.external.full-sync.cron:0 0 2 * * ?}")
    private String fullSyncCron;

    @Value("${sync.external.post-sync.cron:0 0 */4 * * ?}")
    private String postSyncCron;

    @Value("${sync.external.cleanup.cron:0 0 1 * * ?}")
    private String cleanupCron;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== 开始初始化同步定时任务记录 ===");
        
        try {
            // 创建完整数据同步任务
            createSyncTaskIfNotExists(
                "完整数据同步",
                "每天凌晨2点执行完整数据同步，同步所有外部系统数据",
                "com.kyx.service.business.job.SyncScheduleJob",
                "executeFullSync",
                fullSyncCron,
                SchedulerTaskDO.TaskType.SYNC.getValue(),
                true
            );

            // 创建岗位数据同步任务
            createSyncTaskIfNotExists(
                "岗位数据同步",
                "每4小时执行岗位数据同步，保持岗位信息的实时性",
                "com.kyx.service.business.job.SyncScheduleJob",
                "executePostSync",
                postSyncCron,
                SchedulerTaskDO.TaskType.SYNC.getValue(),
                true
            );

            // 创建清理过期记录任务
            createSyncTaskIfNotExists(
                "清理过期记录",
                "每天凌晨1点清理已同步的过期记录，保留30天",
                "com.kyx.service.business.job.SyncScheduleJob",
                "executeCleanup",
                cleanupCron,
                SchedulerTaskDO.TaskType.CLEAN.getValue(),
                true
            );

            log.info("=== 同步定时任务记录初始化完成 ===");

        } catch (Exception e) {
            log.error("同步定时任务记录初始化失败", e);
        }
    }

    /**
     * 创建同步任务（如果不存在）
     */
    private void createSyncTaskIfNotExists(String taskName, String description, String taskClass, 
                                         String taskMethod, String cronExpression, String taskType, 
                                         boolean enabled) {
        try {
            // 检查任务是否已存在
            if (isTaskExists(taskName)) {
                log.info("同步任务[{}]已存在，跳过创建", taskName);
                return;
            }

            // 创建任务请求对象
            SchedulerTaskCreateReqVO createReq = new SchedulerTaskCreateReqVO();
            createReq.setTaskName(taskName);
            createReq.setTaskDescription(description);
            createReq.setTaskClass(taskClass);
            createReq.setTaskMethod(taskMethod);
            createReq.setCronExpression(cronExpression);
            createReq.setTaskType(taskType);
            createReq.setTaskStatus(enabled ? SchedulerTaskDO.TaskStatus.ENABLED.getValue() : 
                                           SchedulerTaskDO.TaskStatus.DISABLED.getValue());

            // 创建任务
            Long taskId = schedulerTaskService.createSchedulerTask(createReq);
            log.info("同步任务[{}]创建成功，ID：{}", taskName, taskId);

        } catch (Exception e) {
            log.error("创建同步任务[{}]失败", taskName, e);
        }
    }

    /**
     * 检查任务是否已存在
     */
    private boolean isTaskExists(String taskName) {
        try {
            SchedulerTaskDO task = schedulerTaskService.getSchedulerTaskByName(taskName);
            return task != null;
        } catch (Exception e) {
            log.warn("检查任务[{}]是否存在时发生异常：{}", taskName, e.getMessage());
            return false;
        }
    }
}