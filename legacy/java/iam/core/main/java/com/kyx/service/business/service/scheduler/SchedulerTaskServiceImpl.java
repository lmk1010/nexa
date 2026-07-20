package com.kyx.service.business.service.scheduler;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import com.kyx.service.business.controller.admin.scheduler.vo.task.*;
import com.kyx.service.business.dal.dataobject.scheduler.SchedulerTaskDO;
import com.kyx.service.business.dal.mysql.scheduler.SchedulerTaskMapper;
import com.kyx.service.business.manager.DynamicSchedulerManager;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.business.enums.ErrorCodeConstants.SCHEDULER_TASK_NOT_EXISTS;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * 定时任务管理 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class SchedulerTaskServiceImpl implements SchedulerTaskService {

    @Resource
    private SchedulerTaskMapper schedulerTaskMapper;

    @Resource
    private ApplicationContext applicationContext;
    
    @Resource
    private DynamicSchedulerManager dynamicSchedulerManager;

    @Resource
    private SchedulerLogService schedulerLogService;

    @Override
    public Long createSchedulerTask(SchedulerTaskCreateReqVO createReqVO) {
        // 插入
        SchedulerTaskDO schedulerTask = BeanUtils.toBean(createReqVO, SchedulerTaskDO.class);
        schedulerTask.setExecuteCount(0L);
        schedulerTask.setSuccessCount(0L);
        schedulerTask.setFailCount(0L);
        if (schedulerTask.getTenantId() == null) {
            schedulerTask.setTenantId(TenantContextHolder.getTenantId());
        }
        
        // 计算下次执行时间
        LocalDateTime nextExecuteTime = dynamicSchedulerManager.getNextExecuteTime(schedulerTask.getCronExpression());
        schedulerTask.setNextExecuteTime(nextExecuteTime);
        
        schedulerTaskMapper.insert(schedulerTask);
        
        // 如果任务是启用状态，立即启动定时任务
        if (schedulerTask.getTaskStatus() != null && 
            schedulerTask.getTaskStatus().equals(SchedulerTaskDO.TaskStatus.ENABLED.getValue())) {
            boolean started = dynamicSchedulerManager.startTask(schedulerTask);
            if (!started) {
                log.warn("定时任务[{}]创建成功但启动失败", schedulerTask.getTaskName());
            }
        }
        
        // 返回
        return schedulerTask.getId();
    }

    @Override
    public void updateSchedulerTask(SchedulerTaskUpdateReqVO updateReqVO) {
        // 校验存在
        SchedulerTaskDO existingTask = getSchedulerTask(updateReqVO.getId());
        if (existingTask == null) {
            throw exception(SCHEDULER_TASK_NOT_EXISTS);
        }
        
        // 更新
        SchedulerTaskDO updateObj = BeanUtils.toBean(updateReqVO, SchedulerTaskDO.class);
        
        // 如果更新了Cron表达式，重新计算下次执行时间
        if (updateObj.getCronExpression() != null && 
            !updateObj.getCronExpression().equals(existingTask.getCronExpression())) {
            LocalDateTime nextExecuteTime = dynamicSchedulerManager.getNextExecuteTime(updateObj.getCronExpression());
            updateObj.setNextExecuteTime(nextExecuteTime);
        }
        
        schedulerTaskMapper.updateById(updateObj);
        
        // 如果任务正在运行，需要重启以应用新配置
        if (dynamicSchedulerManager.isTaskRunning(updateReqVO.getId())) {
            // 重新查询最新的任务信息
            SchedulerTaskDO updatedTask = getSchedulerTask(updateReqVO.getId());
            if (updatedTask.getTaskStatus().equals(SchedulerTaskDO.TaskStatus.ENABLED.getValue())) {
                boolean restarted = dynamicSchedulerManager.restartTask(updatedTask);
                if (!restarted) {
                    log.warn("定时任务[{}]更新后重启失败", updatedTask.getTaskName());
                }
            }
        }
    }

    @Override
    public void deleteSchedulerTask(Long id) {
        // 校验存在
        validateSchedulerTaskExists(id);
        
        // 先停止正在运行的任务
        if (dynamicSchedulerManager.isTaskRunning(id)) {
            boolean stopped = dynamicSchedulerManager.stopTask(id);
            if (!stopped) {
                log.warn("定时任务[{}]停止失败，但将继续删除", id);
            }
        }
        
        // 删除
        schedulerTaskMapper.deleteById(id);
        log.info("定时任务[{}]已删除", id);
    }

    private void validateSchedulerTaskExists(Long id) {
        if (schedulerTaskMapper.selectById(id) == null) {
            throw exception(SCHEDULER_TASK_NOT_EXISTS);
        }
    }

    @Override
    public SchedulerTaskDO getSchedulerTask(Long id) {
        return schedulerTaskMapper.selectById(id);
    }

    @Override
    public PageResult<SchedulerTaskDO> getSchedulerTaskPage(SchedulerTaskPageReqVO pageReqVO) {
        return schedulerTaskMapper.selectPage(pageReqVO);
    }

    @Override
    public void executeTask(Long id) {
        // 获取任务信息
        SchedulerTaskDO task = getSchedulerTask(id);
        if (task == null) {
            throw exception(SCHEDULER_TASK_NOT_EXISTS);
        }

        // 检查任务状态
        if (task.getTaskStatus() == null || task.getTaskStatus() != SchedulerTaskDO.TaskStatus.ENABLED.getValue()) {
            throw new IllegalStateException("任务处于禁用状态，无法执行");
        }

        // 使用DynamicSchedulerManager异步执行任务
        try {
            dynamicSchedulerManager.executeTaskImmediately(task);
            log.info("任务[{}]已开始异步执行", task.getTaskName());
        } catch (Exception e) {
            log.error("启动异步任务执行失败", e);
            throw new RuntimeException("任务执行失败：" + e.getMessage(), e);
        }
    }

    @Override
    public Long executeTaskAndReturnLogId(Long id) {
        // 获取任务信息
        SchedulerTaskDO task = getSchedulerTask(id);
        if (task == null) {
            throw exception(SCHEDULER_TASK_NOT_EXISTS);
        }

        // 检查任务状态
        if (task.getTaskStatus() == null || task.getTaskStatus() != SchedulerTaskDO.TaskStatus.ENABLED.getValue()) {
            throw new IllegalStateException("任务处于禁用状态，无法执行");
        }

        // 先创建执行日志记录
        Long logId = schedulerLogService.recordManualJobStart(
            task.getId(),
            task.getTaskName(),
            task.getTaskClass(),
            task.getTaskMethod()
        );

        // 使用DynamicSchedulerManager异步执行任务，并传递logId
        try {
            dynamicSchedulerManager.executeTaskImmediatelyWithLogId(task, logId);
            log.info("任务[{}]已开始异步执行，日志ID: {}", task.getTaskName(), logId);
            return logId;
        } catch (Exception e) {
            // 如果启动失败，更新日志状态为失败
            schedulerLogService.recordJobError(logId, "启动异步任务执行失败：" + e.getMessage());
            log.error("启动异步任务执行失败", e);
            throw new RuntimeException("任务执行失败：" + e.getMessage(), e);
        }
    }

    /**
     * 带进度跟踪的任务执行方法
     * 
     * @param task 任务信息
     * @return 执行结果
     */
    public String executeTaskWithProgressTracking(SchedulerTaskDO task) {
        // 记录任务开始执行
        Long logId = schedulerLogService.recordManualJobStart(
            task.getId(),
            task.getTaskName(),
            task.getTaskClass(),
            task.getTaskMethod()
        );

        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            log.info("开始手动执行定时任务：{}", task.getTaskName());

            // 更新执行统计
            updateTaskExecuteCount(task.getId());

            // 更新进度：准备阶段
            schedulerLogService.updateJobProgress(logId, 10, "准备执行任务");

            // 获取Bean对象
            Object serviceBean = getServiceBean(task.getTaskClass());
            if (serviceBean == null) {
                updateTaskFailCount(task.getId());
                String errorMsg = "任务执行失败：无法找到服务类 " + task.getTaskClass();
                schedulerLogService.recordJobError(logId, errorMsg);
                return errorMsg;
            }

            // 更新进度：获取方法
            schedulerLogService.updateJobProgress(logId, 20, "获取执行方法");

            // 获取方法
            Method method = serviceBean.getClass().getMethod(task.getTaskMethod());
            if (method == null) {
                updateTaskFailCount(task.getId());
                String errorMsg = "任务执行失败：无法找到方法 " + task.getTaskMethod();
                schedulerLogService.recordJobError(logId, errorMsg);
                return errorMsg;
            }

            // 更新进度：开始执行
            schedulerLogService.updateJobProgress(logId, 50, "正在执行任务方法");

            // 执行方法
            Object result = method.invoke(serviceBean);
            String resultMessage = result != null ? result.toString() : "执行成功";

            // 更新进度：执行完成
            schedulerLogService.updateJobProgress(logId, 100, "任务执行完成");

            // 更新成功统计
            updateTaskSuccessCount(task.getId());

            // 记录执行成功
            schedulerLogService.recordJobSuccess(logId, resultMessage);

            long duration = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            log.info("定时任务执行完成：{}，耗时：{}ms，结果：{}", task.getTaskName(), duration, resultMessage);
            return resultMessage;

        } catch (Exception e) {
            log.error("定时任务执行失败：{}", task.getTaskName(), e);

            // 更新失败统计
            updateTaskFailCount(task.getId());

            // 记录执行失败
            String errorMsg = buildDetailedErrorMessage(e, task.getTaskName());
            schedulerLogService.recordJobError(logId, errorMsg);

            return errorMsg;
        }
    }

    /**
     * 内部任务执行方法
     * 
     * @param task 任务信息
     * @param isManual 是否手动执行
     * @return 执行结果
     */
    public String executeTaskInternal(SchedulerTaskDO task, boolean isManual) {
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            log.info("开始{}执行定时任务：{}", isManual ? "手动" : "自动", task.getTaskName());

            // 更新执行统计
            updateTaskExecuteCount(task.getId());

            // 获取Bean对象
            Object serviceBean = getServiceBean(task.getTaskClass());
            if (serviceBean == null) {
                updateTaskFailCount(task.getId());
                String errorMsg = "任务执行失败：无法找到服务类 " + task.getTaskClass();
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // 获取方法
            Method method;
            try {
                method = serviceBean.getClass().getMethod(task.getTaskMethod());
            } catch (NoSuchMethodException e) {
                updateTaskFailCount(task.getId());
                String errorMsg = String.format("任务执行失败：无法找到方法 %s.%s", task.getTaskClass(), task.getTaskMethod());
                log.error(errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            }

            // 执行方法
            Object result;
            try {
                result = method.invoke(serviceBean);
            } catch (Exception e) {
                updateTaskFailCount(task.getId());
                // 获取具体的异常信息
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                String errorMsg = String.format("任务[%s.%s]执行失败：%s", 
                    task.getTaskClass(), task.getTaskMethod(), cause.getMessage());
                log.error(errorMsg, cause);
                throw new RuntimeException(errorMsg, cause);
            }
            
            String resultMessage = result != null ? result.toString() : "执行成功";

            // 更新成功统计
            updateTaskSuccessCount(task.getId());

            long duration = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            log.info("定时任务执行完成：{}，耗时：{}ms，结果：{}", task.getTaskName(), duration, resultMessage);
            return resultMessage;

        } catch (Exception e) {
            // 确保失败统计已经更新
            updateTaskFailCount(task.getId());
            
            // 重新抛出异常以便上层处理
            if (e instanceof RuntimeException) {
                throw e;
            } else {
                String errorMsg = "任务执行失败：" + e.getMessage();
                log.error(errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            }
        }
    }

    @Override
    public void enableTask(Long id) {
        SchedulerTaskDO task = getSchedulerTask(id);
        if (task == null) {
            throw exception(SCHEDULER_TASK_NOT_EXISTS);
        }

        // 更新状态为启用
        task.setTaskStatus(SchedulerTaskDO.TaskStatus.ENABLED.getValue());
        
        // 计算下次执行时间
        LocalDateTime nextExecuteTime = dynamicSchedulerManager.getNextExecuteTime(task.getCronExpression());
        task.setNextExecuteTime(nextExecuteTime);
        
        schedulerTaskMapper.updateById(task);

        // 启动定时任务
        boolean started = dynamicSchedulerManager.startTask(task);
        if (started) {
            log.info("定时任务[{}]已启用并开始运行", task.getTaskName());
        } else {
            log.error("定时任务[{}]启用失败", task.getTaskName());
            // 回滚状态
            task.setTaskStatus(SchedulerTaskDO.TaskStatus.DISABLED.getValue());
            schedulerTaskMapper.updateById(task);
            throw new RuntimeException("定时任务启用失败：" + task.getTaskName());
        }
    }

    @Override
    public void disableTask(Long id) {
        SchedulerTaskDO task = getSchedulerTask(id);
        if (task == null) {
            throw exception(SCHEDULER_TASK_NOT_EXISTS);
        }

        // 停止定时任务
        boolean stopped = dynamicSchedulerManager.stopTask(id);
        if (!stopped) {
            log.warn("定时任务[{}]停止失败，但将继续禁用", task.getTaskName());
        }

        // 更新状态为禁用
        task.setTaskStatus(SchedulerTaskDO.TaskStatus.DISABLED.getValue());
        task.setNextExecuteTime(null);
        schedulerTaskMapper.updateById(task);
        
        log.info("定时任务[{}]已禁用", task.getTaskName());
    }

    /**
     * 获取服务Bean对象
     */
    private Object getServiceBean(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return applicationContext.getBean(clazz);
        } catch (Exception e) {
            log.error("获取服务Bean失败：{}", className, e);
            return null;
        }
    }

    /**
     * 更新任务执行次数
     */
    private void updateTaskExecuteCount(Long taskId) {
        SchedulerTaskDO task = getSchedulerTask(taskId);
        if (task != null) {
            task.setExecuteCount(task.getExecuteCount() + 1);
            task.setLastExecuteTime(LocalDateTime.now());
            task.setNextExecuteTime(dynamicSchedulerManager.getNextExecuteTime(task.getCronExpression()));
            schedulerTaskMapper.updateById(task);
        }
    }

    /**
     * 更新任务成功次数
     */
    private void updateTaskSuccessCount(Long taskId) {
        SchedulerTaskDO task = getSchedulerTask(taskId);
        if (task != null) {
            task.setSuccessCount(task.getSuccessCount() + 1);
            schedulerTaskMapper.updateById(task);
        }
    }

    /**
     * 更新任务失败次数
     */
    private void updateTaskFailCount(Long taskId) {
        SchedulerTaskDO task = getSchedulerTask(taskId);
        if (task != null) {
            task.setFailCount(task.getFailCount() + 1);
            schedulerTaskMapper.updateById(task);
        }
    }

    /**
     * 获取任务运行状态
     */
    public boolean isTaskRunning(Long taskId) {
        return dynamicSchedulerManager.isTaskRunning(taskId);
    }

    /**
     * 获取运行中的任务数量
     */
    public int getRunningTaskCount() {
        return dynamicSchedulerManager.getRunningTaskCount();
    }

    /**
     * 重启任务
     */
    public boolean restartTask(Long taskId) {
        SchedulerTaskDO task = getSchedulerTask(taskId);
        if (task == null) {
            return false;
        }
        return dynamicSchedulerManager.restartTask(task);
    }

    /**
     * 获取下次执行时间
     */
    public LocalDateTime getNextExecuteTime(String cronExpression) {
        return dynamicSchedulerManager.getNextExecuteTime(cronExpression);
    }

    /**
     * 停止所有任务
     */
    public void stopAllTasks() {
        dynamicSchedulerManager.stopAllTasks();
    }

    @Override
    public SchedulerTaskDO getSchedulerTaskByName(String taskName) {
        if (taskName == null || taskName.trim().isEmpty()) {
            return null;
        }
        
        // 通过查询所有任务并过滤来实现按名称查询
        // 在实际项目中，应该在Mapper中添加更高效的查询方法
        try {
            return schedulerTaskMapper.selectList(null)
                    .stream()
                    .filter(task -> taskName.equals(task.getTaskName()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("根据任务名称[{}]查询任务失败", taskName, e);
            return null;
        }
    }

    /**
     * 构建详细的错误信息
     * 
     * @param e 异常对象
     * @param taskName 任务名称
     * @return 详细的错误信息
     */
    private String buildDetailedErrorMessage(Exception e, String taskName) {
        StringBuilder errorBuilder = new StringBuilder();
        
        // 添加基本错误信息
        if (e.getMessage() != null) {
            errorBuilder.append("错误: ").append(e.getMessage());
        } else {
            errorBuilder.append("错误: ").append(e.getClass().getSimpleName());
        }
        
        // 添加根本原因
        Throwable rootCause = e;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        
        if (rootCause != e && rootCause.getMessage() != null && 
            !rootCause.getMessage().equals(e.getMessage())) {
            errorBuilder.append("。根本原因: ").append(rootCause.getMessage());
        }
        
        // 添加异常类型信息
        if (rootCause instanceof RuntimeException) {
            String className = rootCause.getClass().getSimpleName();
            if (!className.equals("RuntimeException")) {
                errorBuilder.append("（异常类型: ").append(className).append("）");
            }
        }
        
        return errorBuilder.toString();
    }

}
