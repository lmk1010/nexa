package com.kyx.service.business.service.scheduler;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.business.controller.admin.scheduler.vo.SchedulerLogPageReqVO;
import com.kyx.service.business.controller.admin.scheduler.vo.SchedulerStatsRespVO;
import com.kyx.service.business.dal.dataobject.scheduler.SchedulerLogDO;

import java.util.List;

/**
 * 定时任务执行日志 Service 接口
 *
 * @author MK
 */
public interface SchedulerLogService {

    /**
     * 获得定时任务执行日志分页
     *
     * @param pageReqVO 分页查询
     * @return 定时任务执行日志分页
     */
    PageResult<SchedulerLogDO> getSchedulerLogPage(SchedulerLogPageReqVO pageReqVO);

    /**
     * 获得定时任务执行日志详情
     *
     * @param id 编号
     * @return 定时任务执行日志详情
     */
    SchedulerLogDO getSchedulerLog(Long id);

    /**
     * 记录任务开始执行
     *
     * @param taskId 任务ID
     * @param jobName 任务名称
     * @param jobClass 任务类名
     * @param jobMethod 任务方法名
     * @param cronExpression Cron表达式
     * @return 日志ID
     */
    Long recordJobStart(Long taskId, String jobName, String jobClass, String jobMethod, String cronExpression);

    /**
     * 记录任务开始执行
     *
     * @param jobName 任务名称
     * @param jobClass 任务类名
     * @param jobMethod 任务方法名
     * @param cronExpression Cron表达式
     * @return 日志ID
     */
    Long recordJobStart(String jobName, String jobClass, String jobMethod, String cronExpression);

    /**
     * 记录任务执行成功
     *
     * @param logId 日志ID
     * @param resultMessage 结果消息
     */
    void recordJobSuccess(Long logId, String resultMessage);

    /**
     * 记录任务执行失败
     *
     * @param logId 日志ID
     * @param errorMessage 错误消息
     */
    void recordJobError(Long logId, String errorMessage);

    /**
     * 获取最近的执行日志
     *
     * @param limit 数量限制
     * @return 最近的执行日志
     */
    List<SchedulerLogDO> getRecentLogs(int limit);

    /**
     * 获取任务执行统计
     *
     * @return 任务执行统计
     */
    SchedulerStatsRespVO getSchedulerStats();

    /**
     * 清理过期的执行日志
     *
     * @param daysToKeep 保留天数
     * @return 清理的记录数
     */
    int cleanupExpiredLogs(int daysToKeep);

    /**
     * 删除定时任务执行日志
     *
     * @param id 编号
     */
    void deleteSchedulerLog(Long id);

    /**
     * 更新任务执行进度
     *
     * @param logId 日志ID
     * @param progress 进度百分比 (0-100)
     * @param progressMessage 进度描述信息
     */
    void updateJobProgress(Long logId, int progress, String progressMessage);

    /**
     * 记录任务手动执行开始
     *
     * @param taskId 任务ID
     * @param jobName 任务名称
     * @param jobClass 任务类名
     * @param jobMethod 任务方法名
     * @return 日志ID
     */
    Long recordManualJobStart(Long taskId, String jobName, String jobClass, String jobMethod);

    /**
     * 根据任务ID获取最新执行日志
     *
     * @param taskId 任务ID
     * @return 最新执行日志
     */
    SchedulerLogDO getLatestLogByTaskId(Long taskId);

    /**
     * 更新任务执行统计信息
     *
     * @param logId 日志ID
     * @param insertCount 新增记录数
     * @param updateCount 更新记录数
     * @param failureCount 失败记录数
     * @param skipCount 跳过记录数
     */
    void updateJobStatistics(Long logId, Integer insertCount, Integer updateCount, Integer failureCount, Integer skipCount);

    /**
     * 增量更新任务执行统计信息
     *
     * @param logId 日志ID
     * @param insertIncrement 新增记录数增量
     * @param updateIncrement 更新记录数增量
     * @param failureIncrement 失败记录数增量
     * @param skipIncrement 跳过记录数增量
     */
    void incrementJobStatistics(Long logId, Integer insertIncrement, Integer updateIncrement, Integer failureIncrement, Integer skipIncrement);

}
