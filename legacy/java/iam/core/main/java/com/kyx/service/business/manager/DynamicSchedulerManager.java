package com.kyx.service.business.manager;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.business.dal.dataobject.scheduler.SchedulerTaskDO;
import com.kyx.service.business.dal.mysql.scheduler.SchedulerTaskMapper;
import com.kyx.service.business.service.scheduler.SchedulerLogService;
import com.kyx.service.business.service.scheduler.SchedulerTaskService;
import com.kyx.service.business.service.tenant.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态定时任务管理器
 * 结合Spring Schedule实现定时任务的动态创建、执行和监控
 * 
 * @author MK
 */
@Component
@Slf4j
public class DynamicSchedulerManager {

    @Resource
    private TaskScheduler taskScheduler;
    
    @Resource
    private ApplicationContext applicationContext;
    
    @Resource
    private SchedulerLogService schedulerLogService;
    
    @Resource
    private TenantService tenantService;
    
    @Resource
    @Lazy
    private SchedulerTaskService schedulerTaskService;
    
    @Resource
    private SchedulerTaskMapper schedulerTaskMapper;

    /**
     * 存储正在运行的定时任务
     * Key: 任务ID, Value: ScheduledFuture对象
     */
    private final Map<Long, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();

    /**
     * 启动定时任务
     * 
     * @param task 任务信息
     * @return 是否启动成功
     */
    public boolean startTask(SchedulerTaskDO task) {
        if (task == null || task.getId() == null) {
            log.warn("任务信息为空，无法启动");
            return false;
        }

        // 检查任务是否已经在运行
        if (runningTasks.containsKey(task.getId())) {
            log.warn("任务[{}]已经在运行中", task.getTaskName());
            return false;
        }

        try {
            // 验证Cron表达式
            CronExpression.parse(task.getCronExpression());
            
            // 创建任务执行器
            Runnable taskExecutor = createTaskExecutor(task);
            
            // 创建Cron触发器
            CronTrigger cronTrigger = new CronTrigger(task.getCronExpression());
            
            // 调度任务
            ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(taskExecutor, cronTrigger);
            
            // 保存到运行中的任务映射
            runningTasks.put(task.getId(), scheduledFuture);
            
            log.info("定时任务[{}]启动成功，Cron表达式：{}", task.getTaskName(), task.getCronExpression());
            return true;
            
        } catch (Exception e) {
            log.error("启动定时任务[{}]失败", task.getTaskName(), e);
            return false;
        }
    }

    /**
     * 停止定时任务
     * 
     * @param taskId 任务ID
     * @return 是否停止成功
     */
    public boolean stopTask(Long taskId) {
        ScheduledFuture<?> scheduledFuture = runningTasks.get(taskId);
        if (scheduledFuture == null) {
            log.warn("任务[{}]未在运行中", taskId);
            return false;
        }

        try {
            // 取消任务
            boolean cancelled = scheduledFuture.cancel(false);
            if (cancelled) {
                runningTasks.remove(taskId);
                log.info("定时任务[{}]停止成功", taskId);
            } else {
                log.warn("定时任务[{}]停止失败", taskId);
            }
            return cancelled;
        } catch (Exception e) {
            log.error("停止定时任务[{}]失败", taskId, e);
            return false;
        }
    }

    /**
     * 重启定时任务
     * 
     * @param task 任务信息
     * @return 是否重启成功
     */
    public boolean restartTask(SchedulerTaskDO task) {
        // 先停止
        stopTask(task.getId());
        // 再启动
        return startTask(task);
    }

    /**
     * 检查任务是否正在运行
     * 
     * @param taskId 任务ID
     * @return 是否正在运行
     */
    public boolean isTaskRunning(Long taskId) {
        ScheduledFuture<?> scheduledFuture = runningTasks.get(taskId);
        return scheduledFuture != null && !scheduledFuture.isDone() && !scheduledFuture.isCancelled();
    }

    /**
     * 获取下次执行时间
     * 
     * @param cronExpression Cron表达式
     * @return 下次执行时间
     */
    public LocalDateTime getNextExecuteTime(String cronExpression) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            // 使用LocalDateTime来获取下次执行时间
            LocalDateTime nextExecution = cron.next(LocalDateTime.now());
            return nextExecution;
        } catch (Exception e) {
            log.error("计算下次执行时间失败，Cron表达式：{}", cronExpression, e);
        }
        return null;
    }

    /**
     * 获取正在运行的任务数量
     * 
     * @return 运行中的任务数量
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }

    /**
     * 立即执行任务（使用Spring TaskScheduler的异步能力）
     * 
     * @param task 任务信息
     * @return ScheduledFuture对象，可用于跟踪执行状态
     */
    public ScheduledFuture<?> executeTaskImmediately(SchedulerTaskDO task) {
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("任务信息不能为空");
        }

        log.info("立即执行任务：{}", task.getTaskName());
        
        // 创建带进度跟踪的手动任务执行器
        Runnable taskExecutor = createTaskExecutor(task, true);
        
        // 使用TaskScheduler立即异步执行
        return taskScheduler.schedule(taskExecutor, new Date());
    }

    /**
     * 立即执行任务并使用指定的日志ID（使用Spring TaskScheduler的异步能力）
     * 
     * @param task 任务信息
     * @param logId 执行日志ID
     * @return ScheduledFuture对象，可用于跟踪执行状态
     */
    public ScheduledFuture<?> executeTaskImmediatelyWithLogId(SchedulerTaskDO task, Long logId) {
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("任务信息不能为空");
        }

        log.info("立即执行任务：{}，使用日志ID: {}", task.getTaskName(), logId);
        
        // 创建带指定日志ID的手动任务执行器
        Runnable taskExecutor = createTaskExecutorWithLogId(task, logId);
        
        // 使用TaskScheduler立即异步执行
        return taskScheduler.schedule(taskExecutor, new Date());
    }

    /**
     * 创建任务执行器（支持手动执行的进度跟踪）
     * 
     * @param task 任务信息
     * @return 任务执行器
     */
    public Runnable createTaskExecutor(SchedulerTaskDO task) {
        return createTaskExecutor(task, false);
    }

    /**
     * 创建带指定日志ID的任务执行器
     * 
     * @param task 任务信息
     * @param logId 执行日志ID
     * @return 任务执行器
     */
    private Runnable createTaskExecutorWithLogId(SchedulerTaskDO task, Long logId) {
        return withTaskTenantContext(task, () -> {
            LocalDateTime startTime = LocalDateTime.now();
            
            try {
                log.info("=== 手动任务开始执行：{}，日志ID: {} ===", task.getTaskName(), logId);

                // 更新任务执行次数
                updateTaskExecuteCount(task.getId());

                // 更新进度：准备执行任务
                schedulerLogService.updateJobProgress(logId, 10, "准备执行任务");

                // 检查是否有租户上下文
                String executionResult;
                Long currentTenantId = TenantContextHolder.getTenantId();
                if (currentTenantId == null) {
                    log.info("当前无租户上下文，查询系统中的所有租户并逐个执行任务");
                    schedulerLogService.updateJobProgress(logId, 20, "获取租户列表");
                    executionResult = executeTaskForAllTenants(task, logId, true);
                } else {
                    log.info("在租户[{}]上下文中执行任务", currentTenantId);
                    schedulerLogService.updateJobProgress(logId, 50, "正在执行任务方法");
                    executionResult = executeTaskForSingleTenant(task, currentTenantId);
                }

                // 手动执行完成时更新进度
                schedulerLogService.updateJobProgress(logId, 100, "任务执行完成");

                // 更新任务成功次数
                updateTaskSuccessCount(task.getId());

                // 记录成功，使用实际的执行结果
                schedulerLogService.recordJobSuccess(logId, executionResult);
                
                log.info("定时任务执行完成：{}，耗时：{}ms", 
                    task.getTaskName(), 
                    java.time.Duration.between(startTime, LocalDateTime.now()).toMillis());

            } catch (Exception e) {
                log.error("定时任务执行失败：{}", task.getTaskName(), e);
                
                // 更新任务失败次数
                updateTaskFailCount(task.getId());
                
                // 记录失败
                String errorMessage = buildDetailedErrorMessage(e, task.getTaskName());
                schedulerLogService.recordJobError(logId, errorMessage);
            }
        });
    }

    /**
     * 创建任务执行器
     * 
     * @param task 任务信息
     * @param isManual 是否为手动执行
     * @return 任务执行器
     */
    private Runnable createTaskExecutor(SchedulerTaskDO task, boolean isManual) {
        return withTaskTenantContext(task, () -> {
            Long logId = null;
            LocalDateTime startTime = LocalDateTime.now();
            
            try {
                // 记录任务开始
                if (isManual) {
                    logId = schedulerLogService.recordManualJobStart(
                        task.getId(),
                        task.getTaskName(),
                        task.getTaskClass(),
                        task.getTaskMethod()
                    );
                } else {
                    logId = schedulerLogService.recordJobStart(
                        task.getId(),
                        task.getTaskName(),
                        task.getTaskClass(),
                        task.getTaskMethod(),
                        task.getCronExpression()
                    );
                }

                log.info("=== {}任务开始执行：{} ===", isManual ? "手动" : "定时", task.getTaskName());

                // 更新任务执行次数
                updateTaskExecuteCount(task.getId());

                // 手动执行时添加进度跟踪
                if (isManual && logId != null) {
                    schedulerLogService.updateJobProgress(logId, 10, "准备执行任务");
                }

                // 检查是否有租户上下文
                String executionResult;
                Long currentTenantId = TenantContextHolder.getTenantId();
                if (currentTenantId == null) {
                    log.info("当前无租户上下文，查询系统中的所有租户并逐个执行任务");
                    if (isManual && logId != null) {
                        schedulerLogService.updateJobProgress(logId, 20, "获取租户列表");
                    }
                    executionResult = executeTaskForAllTenants(task, logId, isManual);
                } else {
                    log.info("在租户[{}]上下文中执行任务", currentTenantId);
                    if (isManual && logId != null) {
                        schedulerLogService.updateJobProgress(logId, 50, "正在执行任务方法");
                    }
                    executionResult = executeTaskForSingleTenant(task, currentTenantId);
                }

                // 手动执行完成时更新进度
                if (isManual && logId != null) {
                    schedulerLogService.updateJobProgress(logId, 100, "任务执行完成");
                }

                // 更新任务成功次数
                updateTaskSuccessCount(task.getId());

                // 记录成功，使用实际的执行结果
                schedulerLogService.recordJobSuccess(logId, executionResult);
                
                log.info("定时任务执行完成：{}，耗时：{}ms", 
                    task.getTaskName(), 
                    java.time.Duration.between(startTime, LocalDateTime.now()).toMillis());

            } catch (Exception e) {
                log.error("定时任务执行失败：{}", task.getTaskName(), e);
                
                // 更新任务失败次数
                updateTaskFailCount(task.getId());
                
                // 记录失败
                if (logId != null) {
                    String errorMessage = buildDetailedErrorMessage(e, task.getTaskName());
                    schedulerLogService.recordJobError(logId, errorMessage);
                }
            }
        });
    }

    private Runnable withTaskTenantContext(SchedulerTaskDO task, Runnable delegate) {
        return () -> {
            Long currentTenantId = TenantContextHolder.getTenantId();
            if (currentTenantId != null) {
                delegate.run();
                return;
            }

            Long taskTenantId = task == null ? null : task.getTenantId();
            if (taskTenantId != null && taskTenantId > 0) {
                TenantUtils.execute(taskTenantId, delegate);
            } else {
                TenantUtils.executeIgnore(delegate);
            }
        };
    }



    /**
     * 为所有租户执行任务
     */
    private String executeTaskForAllTenants(SchedulerTaskDO task, Long logId, boolean isManual) {
        try {
            // 获取所有租户ID列表
            List<Long> tenantIds = tenantService.getTenantIdList();
            if (tenantIds == null || tenantIds.isEmpty()) {
                log.warn("系统中没有找到任何租户，任务[{}]无法执行", task.getTaskName());
                throw new RuntimeException("系统中没有找到任何租户，无法执行任务");
            }

            log.info("找到 {} 个租户，开始逐个执行任务[{}]", tenantIds.size(), task.getTaskName());
            
            int successCount = 0;
            int failCount = 0;
            StringBuilder errorDetails = new StringBuilder();
            StringBuilder successDetails = new StringBuilder();
            
            for (Long tenantId : tenantIds) {
                try {
                    String result = executeTaskForSingleTenant(task, tenantId);
                    successCount++;
                    successDetails.append(String.format("租户[%d]: %s; ", tenantId, result));
                    log.info("租户[{}]执行任务[{}]成功: {}", tenantId, task.getTaskName(), result);
                    
                    // 更新进度
                    if (isManual && logId != null) {
                        int progress = 50 + (int)((double)(successCount + failCount) / tenantIds.size() * 40);
                        schedulerLogService.updateJobProgress(logId, progress, 
                            String.format("已处理 %d/%d 个租户", successCount + failCount, tenantIds.size()));
                    }
                } catch (Exception e) {
                    failCount++;
                    String errorMsg = String.format("租户[%d]执行失败: %s", tenantId, e.getMessage());
                    errorDetails.append(errorMsg).append("; ");
                    log.error("租户[{}]执行任务[{}]失败", tenantId, task.getTaskName(), e);
                    
                    // 更新进度
                    if (isManual && logId != null) {
                        int progress = 50 + (int)((double)(successCount + failCount) / tenantIds.size() * 40);
                        schedulerLogService.updateJobProgress(logId, progress, 
                            String.format("已处理 %d/%d 个租户", successCount + failCount, tenantIds.size()));
                    }
                }
            }
            
            String finalResult = String.format("多租户任务执行完成：成功 %d 个租户，失败 %d 个租户", successCount, failCount);
            if (successCount > 0) {
                finalResult += "。成功详情：" + successDetails.toString();
            }
            if (failCount > 0) {
                finalResult += "。失败详情：" + errorDetails.toString();
            }
            
            log.info(finalResult);
            
            // 如果有失败的租户，抛出异常以便上层记录失败状态
            if (failCount > 0) {
                throw new RuntimeException(finalResult);
            }
            
            return finalResult;
            
        } catch (Exception e) {
            log.error("获取租户列表失败，无法执行多租户任务", e);
            throw new RuntimeException("获取租户列表失败：" + e.getMessage(), e);
        }
    }

    /**
     * 为单个租户执行任务
     */
    private String executeTaskForSingleTenant(SchedulerTaskDO task, Long tenantId) {
        return TenantUtils.execute(tenantId, () -> {
            try {
                log.debug("在租户[{}]上下文中执行任务[{}]", tenantId, task.getTaskName());
                
                // 获取Bean对象
                Object serviceBean = getServiceBean(task.getTaskClass());
                if (serviceBean == null) {
                    String errorMsg = String.format("无法找到服务类：%s（租户ID：%d）", task.getTaskClass(), tenantId);
                    log.error(errorMsg);
                    throw new RuntimeException(errorMsg);
                }

                // 获取方法
                Method method;
                try {
                    method = serviceBean.getClass().getMethod(task.getTaskMethod());
                } catch (NoSuchMethodException e) {
                    String errorMsg = String.format("无法找到方法：%s.%s（租户ID：%d）", task.getTaskClass(), task.getTaskMethod(), tenantId);
                    log.error(errorMsg, e);
                    throw new RuntimeException(errorMsg, e);
                }

                // 执行方法
                Object result;
                try {
                    result = method.invoke(serviceBean);
                } catch (Exception e) {
                    // 获取具体的异常信息
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String errorMsg = String.format("租户[%d]执行任务[%s.%s]时发生异常：%s", 
                        tenantId, task.getTaskClass(), task.getTaskMethod(), cause.getMessage());
                    log.error(errorMsg, cause);
                    throw new RuntimeException(errorMsg, cause);
                }
                
                String resultMessage = result != null ? result.toString() : "执行成功";
                log.debug("租户[{}]任务[{}]执行结果：{}", tenantId, task.getTaskName(), resultMessage);
                return resultMessage;
                
            } catch (Exception e) {
                // 确保异常被正确传播到上层
                if (e instanceof RuntimeException) {
                    throw e;
                } else {
                    String errorMsg = String.format("租户[%d]执行任务[%s]时发生未知异常：%s", 
                        tenantId, task.getTaskName(), e.getMessage());
                    log.error(errorMsg, e);
                    throw new RuntimeException(errorMsg, e);
                }
            }
        });
    }

    /**
     * 获取服务Bean对象
     * 
     * @param className 类名
     * @return Bean对象
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
     * 停止所有任务
     */
    public void stopAllTasks() {
        runningTasks.forEach((taskId, future) -> {
            try {
                future.cancel(false);
                log.info("停止任务：{}", taskId);
            } catch (Exception e) {
                log.error("停止任务失败：{}", taskId, e);
            }
        });
        runningTasks.clear();
        log.info("所有定时任务已停止");
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

    /**
     * 更新任务执行次数
     */
    private void updateTaskExecuteCount(Long taskId) {
        try {
            // 从数据库获取最新的任务信息
            SchedulerTaskDO task = schedulerTaskService.getSchedulerTask(taskId);
            if (task != null) {
                Long currentCount = task.getExecuteCount() != null ? task.getExecuteCount() : 0L;
                task.setExecuteCount(currentCount + 1);
                task.setLastExecuteTime(LocalDateTime.now());
                task.setNextExecuteTime(getNextExecuteTime(task.getCronExpression()));
                schedulerTaskMapper.updateById(task);
                log.debug("更新任务[{}]执行次数: {} -> {}", taskId, currentCount, task.getExecuteCount());
            }
        } catch (Exception e) {
            log.error("更新任务执行次数失败: taskId={}", taskId, e);
        }
    }

    /**
     * 更新任务成功次数
     */
    private void updateTaskSuccessCount(Long taskId) {
        try {
            // 从数据库获取最新的任务信息
            SchedulerTaskDO task = schedulerTaskService.getSchedulerTask(taskId);
            if (task != null) {
                Long currentCount = task.getSuccessCount() != null ? task.getSuccessCount() : 0L;
                task.setSuccessCount(currentCount + 1);
                schedulerTaskMapper.updateById(task);
                log.debug("更新任务[{}]成功次数: {} -> {}", taskId, currentCount, task.getSuccessCount());
            }
        } catch (Exception e) {
            log.error("更新任务成功次数失败: taskId={}", taskId, e);
        }
    }

    /**
     * 更新任务失败次数
     */
    private void updateTaskFailCount(Long taskId) {
        try {
            // 从数据库获取最新的任务信息
            SchedulerTaskDO task = schedulerTaskService.getSchedulerTask(taskId);
            if (task != null) {
                Long currentCount = task.getFailCount() != null ? task.getFailCount() : 0L;
                task.setFailCount(currentCount + 1);
                schedulerTaskMapper.updateById(task);
                log.debug("更新任务[{}]失败次数: {} -> {}", taskId, currentCount, task.getFailCount());
            }
        } catch (Exception e) {
            log.error("更新任务失败次数失败: taskId={}", taskId, e);
        }
    }
}
