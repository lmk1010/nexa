package com.kyx.service.business.service.scheduler;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.business.controller.admin.scheduler.vo.SchedulerLogPageReqVO;
import com.kyx.service.business.controller.admin.scheduler.vo.SchedulerStatsRespVO;
import com.kyx.service.business.dal.dataobject.scheduler.SchedulerLogDO;
import com.kyx.service.business.dal.mysql.scheduler.SchedulerLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务执行日志 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class SchedulerLogServiceImpl implements SchedulerLogService {

    @Resource
    private SchedulerLogMapper schedulerLogMapper;

    @Override
    public PageResult<SchedulerLogDO> getSchedulerLogPage(SchedulerLogPageReqVO pageReqVO) {
        return schedulerLogMapper.selectPage(pageReqVO);
    }

    @Override
    public SchedulerLogDO getSchedulerLog(Long id) {
        return schedulerLogMapper.selectById(id);
    }

    @Override
    public Long recordJobStart(Long taskId, String jobName, String jobClass, String jobMethod, String cronExpression) {
        SchedulerLogDO schedulerLog = new SchedulerLogDO();
        schedulerLog.setTaskId(taskId);
        schedulerLog.setJobName(jobName);
        schedulerLog.setJobClass(jobClass);
        schedulerLog.setJobMethod(jobMethod);
        schedulerLog.setCronExpression(cronExpression);
        schedulerLog.setStatus(SchedulerLogDO.Status.RUNNING.getValue());
        schedulerLog.setStartTime(LocalDateTime.now());
        schedulerLog.setProgress(0);
        schedulerLog.setProgressMessage("任务开始执行");
        schedulerLog.setIsManual(false);

        schedulerLogMapper.insert(schedulerLog);
        return schedulerLog.getId();
    }

    @Override
    public Long recordJobStart(String jobName, String jobClass, String jobMethod, String cronExpression) {
        return recordJobStart(null, jobName, jobClass, jobMethod, cronExpression);
    }

    @Override
    public void recordJobSuccess(Long logId, String resultMessage) {
        if (logId == null) {
            log.warn("尝试记录任务成功但logId为null，结果信息：{}", resultMessage);
            return;
        }
        
        SchedulerLogDO schedulerLog = schedulerLogMapper.selectById(logId);
        if (schedulerLog != null) {
            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(schedulerLog.getStartTime(), endTime).toMillis();
            
            schedulerLog.setStatus(SchedulerLogDO.Status.SUCCESS.getValue());
            schedulerLog.setEndTime(endTime);
            schedulerLog.setDuration(duration);
            schedulerLog.setProgress(100); // 设置完成进度
            
            // 限制结果消息长度，避免数据库字段溢出
            String truncatedResultMessage = resultMessage;
            if (resultMessage != null && resultMessage.length() > 2000) {
                truncatedResultMessage = resultMessage.substring(0, 2000) + "...(消息已截断)";
            }
            schedulerLog.setResultMessage(truncatedResultMessage);
            
            // 解析结果消息中的统计信息
            parseAndSetStatistics(schedulerLog, resultMessage);
            
            // 清空错误相关的字段
            schedulerLog.setErrorMessage(null);
            
            schedulerLogMapper.updateById(schedulerLog);
            log.info("记录任务执行成功：logId={}, 耗时={}ms, 结果={}", logId, duration, truncatedResultMessage);
        } else {
            log.error("无法找到日志记录，logId={}，无法记录成功结果：{}", logId, resultMessage);
        }
    }

    /**
     * 解析结果消息中的统计信息
     */
    private void parseAndSetStatistics(SchedulerLogDO schedulerLog, String resultMessage) {
        if (resultMessage == null || resultMessage.trim().isEmpty()) {
            return;
        }
        
        try {
            // 解析不同格式的统计信息
            // 格式1: "用户同步完成：成功 180 条，失败 0 条"
            if (resultMessage.contains("成功") && resultMessage.contains("条")) {
                java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("成功\\s*(\\d+)\\s*条");
                java.util.regex.Matcher matcher1 = pattern1.matcher(resultMessage);
                if (matcher1.find()) {
                    int successCount = Integer.parseInt(matcher1.group(1));
                    // 根据业务逻辑，用户同步的成功可能对应更新操作
                    schedulerLog.setUpdateCount(successCount);
                }
                
                java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("失败\\s*(\\d+)\\s*条");
                java.util.regex.Matcher matcher2 = pattern2.matcher(resultMessage);
                if (matcher2.find()) {
                    int failCount = Integer.parseInt(matcher2.group(1));
                    schedulerLog.setFailureCount(failCount);
                }
            }
            
            // 格式2: "新增 X 条, 更新 Y 条, 失败 Z 条, 跳过 W 条"
            if (resultMessage.contains("新增") || resultMessage.contains("更新")) {
                java.util.regex.Pattern insertPattern = java.util.regex.Pattern.compile("新增\\s*(\\d+)\\s*条");
                java.util.regex.Matcher insertMatcher = insertPattern.matcher(resultMessage);
                if (insertMatcher.find()) {
                    schedulerLog.setInsertCount(Integer.parseInt(insertMatcher.group(1)));
                }
                
                java.util.regex.Pattern updatePattern = java.util.regex.Pattern.compile("更新\\s*(\\d+)\\s*条");
                java.util.regex.Matcher updateMatcher = updatePattern.matcher(resultMessage);
                if (updateMatcher.find()) {
                    schedulerLog.setUpdateCount(Integer.parseInt(updateMatcher.group(1)));
                }
                
                java.util.regex.Pattern failPattern = java.util.regex.Pattern.compile("失败\\s*(\\d+)\\s*条");
                java.util.regex.Matcher failMatcher = failPattern.matcher(resultMessage);
                if (failMatcher.find()) {
                    schedulerLog.setFailureCount(Integer.parseInt(failMatcher.group(1)));
                }
                
                java.util.regex.Pattern skipPattern = java.util.regex.Pattern.compile("跳过\\s*(\\d+)\\s*条");
                java.util.regex.Matcher skipMatcher = skipPattern.matcher(resultMessage);
                if (skipMatcher.find()) {
                    schedulerLog.setSkipCount(Integer.parseInt(skipMatcher.group(1)));
                }
            }
            
            log.info("解析任务统计信息成功：logId={}, insert={}, update={}, failure={}, skip={}", 
                    schedulerLog.getId(), 
                    schedulerLog.getInsertCount(),
                    schedulerLog.getUpdateCount(), 
                    schedulerLog.getFailureCount(), 
                    schedulerLog.getSkipCount());
                    
        } catch (Exception e) {
            log.warn("解析结果消息统计信息失败：{}, 错误：{}", resultMessage, e.getMessage());
        }
    }

    @Override
    public void recordJobError(Long logId, String errorMessage) {
        if (logId == null) {
            log.warn("尝试记录任务错误但logId为null，错误信息：{}", errorMessage);
            return;
        }
        
        SchedulerLogDO schedulerLog = schedulerLogMapper.selectById(logId);
        if (schedulerLog != null) {
            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(schedulerLog.getStartTime(), endTime).toMillis();
            
            schedulerLog.setStatus(SchedulerLogDO.Status.FAILED.getValue());
            schedulerLog.setEndTime(endTime);
            schedulerLog.setDuration(duration);
            
            // 限制错误消息长度，避免数据库字段溢出
            String truncatedErrorMessage = errorMessage;
            if (errorMessage != null && errorMessage.length() > 2000) {
                truncatedErrorMessage = errorMessage.substring(0, 2000) + "...(消息已截断)";
            }
            schedulerLog.setErrorMessage(truncatedErrorMessage);
            
            // 清空成功相关的字段
            schedulerLog.setResultMessage(null);
            
            schedulerLogMapper.updateById(schedulerLog);
            log.info("记录任务执行失败：logId={}, 耗时={}ms, 错误={}", logId, duration, truncatedErrorMessage);
        } else {
            log.error("无法找到日志记录，logId={}，无法记录错误：{}", logId, errorMessage);
        }
    }

    @Override
    public List<SchedulerLogDO> getRecentLogs(int limit) {
        return schedulerLogMapper.selectRecentLogs(limit);
    }

    @Override
    public SchedulerStatsRespVO getSchedulerStats() {
        SchedulerStatsRespVO stats = new SchedulerStatsRespVO();
        
        // 总计统计
        int totalCount = Math.toIntExact(schedulerLogMapper.selectCount());
        int successCount = schedulerLogMapper.countByStatus(SchedulerLogDO.Status.SUCCESS.getValue());
        int failedCount = schedulerLogMapper.countByStatus(SchedulerLogDO.Status.FAILED.getValue());
        
        stats.setTotalCount(totalCount);
        stats.setSuccessCount(successCount);
        stats.setFailedCount(failedCount);
        
        if (totalCount > 0) {
            stats.setSuccessRate((double) successCount / totalCount * 100);
        } else {
            stats.setSuccessRate(0.0);
        }
        
        // 今日统计
        // TODO: 可以添加更详细的今日统计查询
        stats.setTodayCount(0);
        stats.setTodaySuccessCount(0);
        stats.setTodayFailedCount(0);
        
        return stats;
    }

    @Override
    public int cleanupExpiredLogs(int daysToKeep) {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(daysToKeep);
        return schedulerLogMapper.deleteByCreateTimeBefore(beforeTime);
    }

    @Override
    public void deleteSchedulerLog(Long id) {
        schedulerLogMapper.deleteById(id);
    }

    @Override
    public void updateJobProgress(Long logId, int progress, String progressMessage) {
        SchedulerLogDO schedulerLog = schedulerLogMapper.selectById(logId);
        if (schedulerLog != null) {
            schedulerLog.setProgress(progress);
            schedulerLog.setProgressMessage(progressMessage);
            schedulerLogMapper.updateById(schedulerLog);
        }
    }

    @Override
    public Long recordManualJobStart(Long taskId, String jobName, String jobClass, String jobMethod) {
        SchedulerLogDO schedulerLog = new SchedulerLogDO();
        schedulerLog.setTaskId(taskId);
        schedulerLog.setJobName(jobName);
        schedulerLog.setJobClass(jobClass);
        schedulerLog.setJobMethod(jobMethod);
        schedulerLog.setStatus(SchedulerLogDO.Status.RUNNING.getValue());
        schedulerLog.setStartTime(LocalDateTime.now());
        schedulerLog.setProgress(0);
        schedulerLog.setProgressMessage("任务开始执行");
        schedulerLog.setIsManual(true);
        
        schedulerLogMapper.insert(schedulerLog);
        return schedulerLog.getId();
    }

    @Override
    public SchedulerLogDO getLatestLogByTaskId(Long taskId) {
        return schedulerLogMapper.selectLatestByTaskId(taskId);
    }

    @Override
    public void updateJobStatistics(Long logId, Integer insertCount, Integer updateCount, Integer failureCount, Integer skipCount) {
        SchedulerLogDO schedulerLog = schedulerLogMapper.selectById(logId);
        if (schedulerLog != null) {
            schedulerLog.setInsertCount(insertCount);
            schedulerLog.setUpdateCount(updateCount);
            schedulerLog.setFailureCount(failureCount);
            schedulerLog.setSkipCount(skipCount);
            schedulerLogMapper.updateById(schedulerLog);
        }
    }

    @Override
    public void incrementJobStatistics(Long logId, Integer insertIncrement, Integer updateIncrement, Integer failureIncrement, Integer skipIncrement) {
        SchedulerLogDO schedulerLog = schedulerLogMapper.selectById(logId);
        if (schedulerLog != null) {
            schedulerLog.setInsertCount((schedulerLog.getInsertCount() != null ? schedulerLog.getInsertCount() : 0) + (insertIncrement != null ? insertIncrement : 0));
            schedulerLog.setUpdateCount((schedulerLog.getUpdateCount() != null ? schedulerLog.getUpdateCount() : 0) + (updateIncrement != null ? updateIncrement : 0));
            schedulerLog.setFailureCount((schedulerLog.getFailureCount() != null ? schedulerLog.getFailureCount() : 0) + (failureIncrement != null ? failureIncrement : 0));
            schedulerLog.setSkipCount((schedulerLog.getSkipCount() != null ? schedulerLog.getSkipCount() : 0) + (skipIncrement != null ? skipIncrement : 0));
            schedulerLogMapper.updateById(schedulerLog);
        }
    }

}
