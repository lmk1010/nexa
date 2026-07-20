package com.kyx.service.business.manager;

import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.business.dal.dataobject.scheduler.SchedulerTaskDO;
import com.kyx.service.business.dal.mysql.scheduler.SchedulerTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 定时任务初始化器
 * 应用启动时自动加载并启动已启用的定时任务
 * 
 * @author MK
 */
@Component
@Slf4j
@Order(2) // 在SyncTaskInitializer之后执行
public class SchedulerTaskInitializer implements ApplicationRunner {

    @Resource
    private SchedulerTaskMapper schedulerTaskMapper;

    @Resource
    private DynamicSchedulerManager dynamicSchedulerManager;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== 开始加载已启用的数据库定时任务 ===");
        List<SchedulerTaskDO> enabledTasks = TenantUtils.executeIgnore(() ->
                schedulerTaskMapper.selectList(SchedulerTaskDO::getTaskStatus,
                        SchedulerTaskDO.TaskStatus.ENABLED.getValue()));
        if (enabledTasks == null || enabledTasks.isEmpty()) {
            log.info("没有需要启动的数据库定时任务");
            return;
        }

        int successCount = 0;
        int failCount = 0;
        for (SchedulerTaskDO task : enabledTasks) {
            boolean started = dynamicSchedulerManager.startTask(task);
            if (started) {
                successCount++;
            } else {
                failCount++;
            }
        }
        log.info("=== 数据库定时任务加载完成：成功 {} 个，失败 {} 个 ===", successCount, failCount);
    }
}
