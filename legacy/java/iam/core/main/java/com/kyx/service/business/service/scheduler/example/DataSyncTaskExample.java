package com.kyx.service.business.service.scheduler.example;

import com.kyx.service.business.service.scheduler.SchedulerLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 数据同步任务示例
 * 展示如何在定时任务中使用进度跟踪和执行统计功能
 *
 * @author MK
 */
@Component
@Slf4j
public class DataSyncTaskExample {

    @Resource
    private SchedulerLogService schedulerLogService;

    /**
     * 执行数据同步任务
     * 此方法会被定时任务调度器调用
     */
    public String executeDataSync() {
        // 注意：在实际使用中，logId应该通过DynamicSchedulerManager传递
        // 这里只是展示如何使用统计功能
        Long logId = getCurrentLogId(); // 这个方法需要根据实际情况实现
        
        if (logId == null) {
            log.warn("未获取到日志ID，无法记录进度和统计信息");
            return "任务执行完成，但无法记录详细统计";
        }

        try {
            // 模拟数据处理
            return processData(logId);
        } catch (Exception e) {
            log.error("数据同步任务执行失败", e);
            schedulerLogService.updateJobProgress(logId, 100, "任务执行失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 处理数据的核心逻辑
     */
    private String processData(Long logId) {
        int totalRecords = 1000; // 假设总共有1000条数据要处理
        int insertCount = 0;
        int updateCount = 0;
        int failureCount = 0;
        int skipCount = 0;

        // 初始化进度
        schedulerLogService.updateJobProgress(logId, 0, "开始处理数据...");

        // 模拟数据处理
        for (int i = 1; i <= totalRecords; i++) {
            try {
                // 模拟处理单条数据
                ProcessResult result = processSingleRecord(i);
                
                // 根据处理结果更新统计
                switch (result) {
                    case INSERTED:
                        insertCount++;
                        break;
                    case UPDATED:
                        updateCount++;
                        break;
                    case SKIPPED:
                        skipCount++;
                        break;
                    case FAILED:
                        failureCount++;
                        break;
                }

                // 每处理100条记录更新一次进度和统计
                if (i % 100 == 0) {
                    int progress = (int) ((double) i / totalRecords * 100);
                    String progressMsg = String.format("已处理 %d/%d 条记录", i, totalRecords);
                    
                    // 更新进度
                    schedulerLogService.updateJobProgress(logId, progress, progressMsg);
                    
                    // 更新统计信息
                    schedulerLogService.updateJobStatistics(logId, insertCount, updateCount, failureCount, skipCount);
                    
                    log.info("数据同步进度: {}% - {}", progress, progressMsg);
                }

                // 模拟处理时间
                Thread.sleep(10);

            } catch (Exception e) {
                log.warn("处理第{}条记录时发生错误: {}", i, e.getMessage());
                failureCount++;
            }
        }

        // 最终更新进度和统计
        schedulerLogService.updateJobProgress(logId, 100, "数据处理完成");
        schedulerLogService.updateJobStatistics(logId, insertCount, updateCount, failureCount, skipCount);

        String result = String.format(
            "数据同步完成! 总计处理 %d 条记录: 新增 %d 条, 更新 %d 条, 失败 %d 条, 跳过 %d 条",
            totalRecords, insertCount, updateCount, failureCount, skipCount
        );
        
        log.info(result);
        return result;
    }

    /**
     * 处理单条记录
     */
    private ProcessResult processSingleRecord(int recordId) {
        // 模拟不同的处理结果
        int random = (int) (Math.random() * 100);
        
        if (random < 5) {
            // 5% 概率失败
            throw new RuntimeException("模拟处理失败");
        } else if (random < 15) {
            // 10% 概率跳过
            return ProcessResult.SKIPPED;
        } else if (random < 60) {
            // 45% 概率新增
            return ProcessResult.INSERTED;
        } else {
            // 40% 概率更新
            return ProcessResult.UPDATED;
        }
    }

    /**
     * 获取当前执行的日志ID
     * 在实际实现中，这个ID应该通过DynamicSchedulerManager或ThreadLocal传递
     */
    private Long getCurrentLogId() {
        // TODO: 实际实现中需要从执行上下文中获取logId
        // 可以使用ThreadLocal或其他方式传递
        return null;
    }

    /**
     * 处理结果枚举
     */
    private enum ProcessResult {
        INSERTED,  // 新增
        UPDATED,   // 更新
        SKIPPED,   // 跳过
        FAILED     // 失败
    }
}